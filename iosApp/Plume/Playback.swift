import AVFoundation
import Combine
import MediaPlayer

@MainActor
final class Playback: ObservableObject {
    @Published private(set) var current: MusicTrack?
    @Published private(set) var queue: [MusicTrack] = []
    @Published private(set) var playing = false
    @Published private(set) var loading = false
    @Published private(set) var position: Double = 0
    @Published private(set) var duration: Double = 0
    @Published var shuffle = false
    @Published var repeatMode = 0 // off, all, one
    @Published var error: String?
    private let api: SoundCloudClient
    private let library: MusicLibrary
    private let player = AVPlayer()
    private var resolveTask: Task<Void, Never>?
    private var timer: Any?
    private var subscriptions = Set<AnyCancellable>()
    private var statusObservation: NSKeyValueObservation?
    private var timeObservation: NSKeyValueObservation?

    init(api: SoundCloudClient, library: MusicLibrary) {
        self.api = api
        self.library = library
        timer = player.addPeriodicTimeObserver(forInterval: CMTime(seconds: 0.5, preferredTimescale: 600), queue: .main) {
            [weak self] time in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.position = time.seconds.isFinite ? max(0, time.seconds) : 0
                if let seconds = self.player.currentItem?.duration.seconds, seconds.isFinite {
                    self.duration = max(0, seconds)
                }
                self.nowPlaying()
            }
        }
        NotificationCenter.default.publisher(for: .AVPlayerItemDidPlayToEndTime).receive(on: DispatchQueue.main)
            .sink { [weak self] note in
                guard let self, let item = note.object as? AVPlayerItem, item === self.player.currentItem else { return }
                if self.repeatMode == 2 { self.seek(0); self.player.play() }
                else { self.next(automatic: true) }
            }.store(in: &subscriptions)
        NotificationCenter.default.publisher(for: AVAudioSession.interruptionNotification)
            .receive(on: DispatchQueue.main).sink { [weak self] note in
                guard let type = note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt else { return }
                if type == AVAudioSession.InterruptionType.began.rawValue { self?.pause() }
            }.store(in: &subscriptions)
        NotificationCenter.default.publisher(for: AVAudioSession.routeChangeNotification)
            .receive(on: DispatchQueue.main).sink { [weak self] note in
                if let reason = note.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt,
                   reason == AVAudioSession.RouteChangeReason.oldDeviceUnavailable.rawValue { self?.pause() }
            }.store(in: &subscriptions)
        timeObservation = player.observe(\.timeControlStatus, options: [.new]) { [weak self] player, _ in
            let status = player.timeControlStatus
            Task { @MainActor [weak self] in
                self?.playing = status == .playing
                self?.loading = status == .waitingToPlayAtSpecifiedRate
                self?.nowPlaying()
            }
        }
        let commands = MPRemoteCommandCenter.shared()
        commands.playCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.resume() }; return .success
        }
        commands.pauseCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.pause() }; return .success
        }
        commands.nextTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.next() }; return .success
        }
        commands.previousTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.previous() }; return .success
        }
        commands.changePlaybackPositionCommand.addTarget { [weak self] event in
            guard let event = event as? MPChangePlaybackPositionCommandEvent else { return .commandFailed }
            Task { @MainActor in self?.seek(event.positionTime) }; return .success
        }
    }

    func play(_ track: MusicTrack, in tracks: [MusicTrack]? = nil) {
        if let tracks { queue = tracks.contains(where: { $0.id == track.id }) ? tracks : [track] + tracks }
        else if !queue.contains(where: { $0.id == track.id }) { queue = [track] }
        resolveTask?.cancel()
        player.pause()
        player.replaceCurrentItem(with: nil)
        current = library.track(track)
        position = 0
        duration = track.duration
        loading = true
        resolveTask = Task {
            do {
                let url: URL
                if let local = library.localURL(track), library.containsLocalFile(track) { url = local }
                else if track.imported { throw PlumeError.message("The local audio file is unavailable.") }
                else { url = try await api.source(track).url }
                try Task.checkCancellation()
                try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
                try AVAudioSession.sharedInstance().setActive(true)
                let item = AVPlayerItem(url: url)
                statusObservation = item.observe(\.status, options: [.new]) { [weak self] item, _ in
                    let failed = item.status == .failed
                    let message = item.error?.localizedDescription
                    Task { @MainActor [weak self] in
                        if failed { self?.loading = false; self?.error = message ?? "Playback failed" }
                    }
                }
                player.replaceCurrentItem(with: item)
                player.play()
                nowPlaying()
            } catch is CancellationError { }
            catch { self.loading = false; self.error = error.localizedDescription }
        }
    }

    func toggle() { playing ? pause() : resume() }
    func pause() { player.pause(); playing = false; nowPlaying() }
    func resume() {
        if player.currentItem == nil, let current { play(current); return }
        do {
            try AVAudioSession.sharedInstance().setActive(true)
            player.play()
        } catch { self.error = error.localizedDescription }
    }
    func stop() {
        resolveTask?.cancel(); player.pause(); player.replaceCurrentItem(with: nil)
        current = nil; playing = false; loading = false
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
    }
    func seek(_ value: Double) {
        let seconds = max(0, min(value, duration))
        player.seek(to: CMTime(seconds: seconds, preferredTimescale: 600))
        position = seconds
    }
    func next(automatic: Bool = false) {
        guard let current, let index = queue.firstIndex(where: { $0.id == current.id }) else { return }
        if shuffle, let track = queue.filter({ $0.id != current.id }).randomElement() { play(track); return }
        if index + 1 < queue.count { play(queue[index + 1]) }
        else if repeatMode == 1, let first = queue.first { play(first) }
        else if automatic { pause(); seek(0) }
    }
    func previous() {
        guard position < 3, let current, let index = queue.firstIndex(where: { $0.id == current.id }), index > 0 else {
            seek(0); return
        }
        play(queue[index - 1])
    }
    private func nowPlaying() {
        guard let current else { return }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = [
            MPMediaItemPropertyTitle: current.title, MPMediaItemPropertyArtist: current.artist,
            MPMediaItemPropertyPlaybackDuration: duration,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: position,
            MPNowPlayingInfoPropertyPlaybackRate: playing ? 1.0 : 0.0
        ]
    }
}
