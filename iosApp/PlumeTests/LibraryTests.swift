import XCTest
@testable import Plume

final class LibraryTests: XCTestCase {
    @MainActor
    func testSavingAndDownloadingAreIndependentAndPersist() throws {
        let folder = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: folder) }
        let file = folder.appendingPathComponent("library.json")
        let store = MusicLibrary(file: file)
        let stale = MusicTrack(id: "123", title: "Song", artist: "Artist")
        try store.toggleFavorite(stale)
        try store.setDownload(stale, file: "123.mp3")
        XCTAssertTrue(store.track(stale).favorite)
        XCTAssertTrue(store.track(stale).downloaded)
        try store.createPlaylist("Mom")
        try store.add(stale, to: store.snapshot.playlists[0].id)
        XCTAssertTrue(store.track(stale).favorite)
        XCTAssertEqual(store.track(stale).localFile, "123.mp3")
        let restored = MusicLibrary(file: file)
        XCTAssertTrue(restored.track(stale).favorite)
        XCTAssertEqual(restored.snapshot.playlists[0].trackIDs, ["123"])
        try restored.toggleFavorite(stale)
        XCTAssertFalse(restored.track(stale).favorite)
        XCTAssertTrue(restored.track(stale).downloaded)
        try restored.toggleFavorite(stale)
        try restored.setDownload(stale, file: nil)
        XCTAssertTrue(restored.track(stale).favorite)
        XCTAssertFalse(restored.track(stale).downloaded)
    }

    @MainActor
    func testOfflineAssetAcceptsSymlinkAliasAndCanBeReopenedAfterReload() throws {
        let folder = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: folder) }
        let home = folder.appendingPathComponent("Sandbox", isDirectory: true)
        let alias = folder.appendingPathComponent("Alias", isDirectory: true)
        let relative = "Library/Offline Music/track.movpkg"
        let asset = home.appendingPathComponent(relative, isDirectory: true)
        try FileManager.default.createDirectory(at: asset, withIntermediateDirectories: true)
        try FileManager.default.createSymbolicLink(at: alias, withDestinationURL: home)

        let stored = try OfflineAssetLocation.storedPath(for: alias.appendingPathComponent(relative), home: home)
        XCTAssertEqual(stored, "home:" + relative)
        XCTAssertEqual(try OfflineAssetLocation.storedPath(for: asset, home: alias), stored)
        let reopened = home.appendingPathComponent(String(stored.dropFirst(5)))
        XCTAssertTrue(FileManager.default.fileExists(atPath: reopened.path))

        let libraryFile = folder.appendingPathComponent("library.json")
        let track = MusicTrack(id: "offline", title: "Offline", artist: "Artist")
        let store = MusicLibrary(file: libraryFile)
        try store.setDownload(track, file: stored)
        XCTAssertEqual(MusicLibrary(file: libraryFile).track(track).localFile, stored)
    }

    func testOfflineAssetRejectsOutsideAndSiblingContainers() throws {
        let home = URL(fileURLWithPath: NSHomeDirectory())
        XCTAssertThrowsError(try OfflineAssetLocation.storedPath(for: home, home: home))
        XCTAssertThrowsError(try OfflineAssetLocation.storedPath(
            for: URL(fileURLWithPath: home.path + "-neighbor/track.movpkg"), home: home))
        XCTAssertThrowsError(try OfflineAssetLocation.storedPath(
            for: URL(string: "https://example.com/track.movpkg")!, home: home))
    }

    func testTranscodingMappingRejectsOggForIPhone() throws {
        let json = """
        {"id":42,"title":"Music","streamable":true,"media":{"transcodings":[
          {"url":"https://api-v2.soundcloud.com/media/test","format":{"protocol":"hls","mime_type":"audio/ogg"}}
        ]}}
        """
        XCTAssertNil(try JSONDecoder().decode(CloudTrack.self, from: Data(json.utf8)).music)
    }
}
