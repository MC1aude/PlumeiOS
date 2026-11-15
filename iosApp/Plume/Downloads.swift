import AVFoundation
import Combine

@MainActor
final class Downloads: NSObject, ObservableObject, AVAssetDownloadDelegate {
    @Published private(set) var active: Set<String> = []
    @Published var error: String?
    private let api: SoundCloudClient
    private let library: MusicLibrary
    private var tasks: [String: Task<Void, Never>] = [:]
    private var assets: [Int: MusicTrack] = [:]
    private var assetLocations: [Int: URL] = [:]
    private lazy var session: AVAssetDownloadURLSession = {
        let config = URLSessionConfiguration.background(withIdentifier: "com.example.plume.hls")
        return AVAssetDownloadURLSession(configuration: config, assetDownloadDelegate: self, delegateQueue: .main)
    }()

    init(api: SoundCloudClient, library: MusicLibrary) {
        self.api = api; self.library = library
        super.init()
        // Reconnect background HLS tasks after a process restart.
        session.getAllTasks { [weak self] tasks in
            Task { @MainActor [weak self] in
                guard let self else { return }
                for task in tasks {
                    guard let id = task.taskDescription,
                          let track = self.library.snapshot.tracks.first(where: { $0.id == id }) else {
                        task.cancel(); continue
                    }
                    self.assets[task.taskIdentifier] = track
                    self.active.insert(id)
                }
            }
        }
    }

    func download(_ input: MusicTrack) {
        let track = library.track(input)
        guard !active.contains(track.id) else { return }
        active.insert(track.id)
        tasks[track.id] = Task {
            var handedToBackground = false
            defer {
                tasks[track.id] = nil
                if !handedToBackground { active.remove(track.id) }
            }
            do {
                let source = try await api.source(track)
                try Task.checkCancellation()
                if source.hls {
                    // Persist metadata before launching a resumable system download.
                    try library.setDownload(track, file: nil)
                    let asset = AVURLAsset(url: source.url)
                    guard let task = session.makeAssetDownloadTask(asset: asset, assetTitle: track.title,
                        assetArtworkData: nil, options: nil) else {
                        throw PlumeError.message("Could not start offline HLS download.")
                    }
                    task.taskDescription = track.id
                    assets[task.taskIdentifier] = track
                    handedToBackground = true
                    task.resume()
                } else {
                    let (temporary, response) = try await URLSession.shared.download(from: source.url)
                    defer { try? FileManager.default.removeItem(at: temporary) }
                    guard let response = response as? HTTPURLResponse, (200...299).contains(response.statusCode) else {
                        throw URLError(.badServerResponse)
                    }
                    let asset = AVURLAsset(url: temporary)
                    let audio = try await asset.loadTracks(withMediaType: .audio)
                    guard !audio.isEmpty else { throw PlumeError.message("The server did not return an audio file.") }
                    let mime = response.mimeType ?? ""
                    let ext = mime.contains("mp4") ? "m4a" : mime.contains("aac") ? "aac" : "mp3"
                    let name = "\(track.id).\(ext)"
                    let target = MusicLibrary.documents.appendingPathComponent(name)
                    if FileManager.default.fileExists(atPath: target.path) { try FileManager.default.removeItem(at: target) }
                    try FileManager.default.copyItem(at: temporary, to: target)
                    do { try library.setDownload(track, file: name) }
                    catch { try? FileManager.default.removeItem(at: target); throw error }
                }
            } catch is CancellationError { }
            catch { self.error = error.localizedDescription }
        }
    }

    func remove(_ track: MusicTrack) throws {
        if let url = library.localURL(track), FileManager.default.fileExists(atPath: url.path) {
            try FileManager.default.removeItem(at: url)
        }
        try library.setDownload(track, file: nil)
    }

    nonisolated func urlSession(_ session: URLSession, assetDownloadTask: AVAssetDownloadTask,
                               didFinishDownloadingTo location: URL) {
        Task { @MainActor in self.assetLocations[assetDownloadTask.taskIdentifier] = location }
    }

    nonisolated func urlSession(_ session: URLSession, task: URLSessionTask,
                               didCompleteWithError error: Error?) {
        Task { @MainActor in
            let key = task.taskIdentifier
            guard let track = self.assets.removeValue(forKey: key) else { return }
            defer { self.active.remove(track.id) }
            let location = self.assetLocations.removeValue(forKey: key)
            do {
                if let error { throw error }
                guard let location else { throw PlumeError.message("The download did not produce an offline file.") }
                let path = try OfflineAssetLocation.storedPath(for: location)
                try self.library.setDownload(track, file: path)
            } catch {
                if let location { try? FileManager.default.removeItem(at: location) }
                self.error = error.localizedDescription
            }
        }
    }

    nonisolated func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
        Task { @MainActor in
            let completion = BackgroundSession.completion
            BackgroundSession.completion = nil
            completion?()
        }
    }

    func importAudio(_ url: URL) async throws {
        let access = url.startAccessingSecurityScopedResource()
        defer { if access { url.stopAccessingSecurityScopedResource() } }
        let id = UUID().uuidString
        let name = "\(id).\(url.pathExtension)"
        let target = MusicLibrary.documents.appendingPathComponent(name)
        try FileManager.default.copyItem(at: url, to: target)
        do {
            let asset = AVURLAsset(url: target)
            guard try await !asset.loadTracks(withMediaType: .audio).isEmpty else {
                throw PlumeError.message("Please select an audio file.")
            }
            let duration = try await asset.load(.duration).seconds
            var track = MusicTrack(id: id, title: url.deletingPathExtension().lastPathComponent,
                artist: "iPhone", duration: duration.isFinite ? duration : 0)
            track.localFile = name; track.imported = true
            try library.addImported(track)
        } catch { try? FileManager.default.removeItem(at: target); throw error }
    }
}
