import Foundation

struct MusicTrack: Codable, Identifiable, Equatable, Hashable {
    var id: String
    var title: String
    var artist: String
    var artwork: String = ""
    var duration: Double = 0
    var endpoint: String = ""
    var localFile: String? = nil
    var favorite = false
    var imported = false
    var downloaded: Bool { localFile != nil }
}

struct MusicPlaylist: Codable, Identifiable {
    var id = UUID()
    var name: String
    var trackIDs: [String] = []
}

struct LibrarySnapshot: Codable {
    var tracks: [MusicTrack] = []
    var playlists: [MusicPlaylist] = []
}

enum PlumeError: LocalizedError {
    case message(String)
    var errorDescription: String? {
        switch self { case .message(let text): return text }
    }
}

@MainActor
final class MusicLibrary: ObservableObject {
    @Published private(set) var snapshot: LibrarySnapshot
    private let file: URL

    init(file: URL? = nil) {
        self.file = file ?? Self.documents.appendingPathComponent("library.json")
        snapshot = (try? Data(contentsOf: self.file))
            .flatMap { try? JSONDecoder().decode(LibrarySnapshot.self, from: $0) } ?? LibrarySnapshot()
    }

    static var documents: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    }

    func track(_ track: MusicTrack) -> MusicTrack {
        snapshot.tracks.first { $0.id == track.id } ?? track
    }

    private func update(_ incoming: MusicTrack, change: (inout MusicTrack) -> Void) throws {
        var next = snapshot
        if let index = next.tracks.firstIndex(where: { $0.id == incoming.id }) {
            change(&next.tracks[index])
        } else {
            var value = incoming
            change(&value)
            next.tracks.append(value)
        }
        try commit(next)
    }

    func toggleFavorite(_ track: MusicTrack) throws {
        try update(track) { $0.favorite.toggle() }
    }

    func setDownload(_ track: MusicTrack, file: String?) throws {
        try update(track) { $0.localFile = file }
    }

    func addImported(_ track: MusicTrack) throws {
        try update(track) { $0 = track }
    }

    func createPlaylist(_ name: String) throws {
        let name = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return }
        var next = snapshot
        next.playlists.append(MusicPlaylist(name: name))
        try commit(next)
    }

    func add(_ track: MusicTrack, to playlist: UUID) throws {
        var next = snapshot
        if !next.tracks.contains(where: { $0.id == track.id }) { next.tracks.append(track) }
        if let index = next.playlists.firstIndex(where: { $0.id == playlist }),
           !next.playlists[index].trackIDs.contains(track.id) {
            next.playlists[index].trackIDs.append(track.id)
        }
        try commit(next)
    }

    func remove(_ track: MusicTrack, from playlist: UUID) throws {
        var next = snapshot
        if let index = next.playlists.firstIndex(where: { $0.id == playlist }) {
            next.playlists[index].trackIDs.removeAll { $0 == track.id }
        }
        try commit(next)
    }

    func deletePlaylist(_ id: UUID) throws {
        var next = snapshot
        next.playlists.removeAll { $0.id == id }
        try commit(next)
    }

    func deleteImported(_ track: MusicTrack) throws {
        var next = snapshot
        next.tracks.removeAll { $0.id == track.id }
        for index in next.playlists.indices { next.playlists[index].trackIDs.removeAll { $0 == track.id } }
        try commit(next)
    }

    func localURL(_ track: MusicTrack) -> URL? {
        guard let path = self.track(track).localFile else { return nil }
        // HLS packages are managed by iOS outside Documents. Store paths relative to
        // the app home because its absolute sandbox path may change after an update.
        if path.hasPrefix("home:") {
            return URL(fileURLWithPath: NSHomeDirectory()).appendingPathComponent(String(path.dropFirst(5)))
        }
        return Self.documents.appendingPathComponent(path)
    }

    func containsLocalFile(_ track: MusicTrack) -> Bool {
        localURL(track).map { FileManager.default.fileExists(atPath: $0.path) } ?? false
    }

    private func commit(_ next: LibrarySnapshot) throws {
        let data = try JSONEncoder().encode(next)
        try FileManager.default.createDirectory(at: file.deletingLastPathComponent(), withIntermediateDirectories: true)
        try data.write(to: file, options: .atomic)
        snapshot = next
    }
}
