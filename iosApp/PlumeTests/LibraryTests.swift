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

    func testTranscodingMappingRejectsOggForIPhone() throws {
        let json = """
        {"id":42,"title":"Music","streamable":true,"media":{"transcodings":[
          {"url":"https://api-v2.soundcloud.com/media/test","format":{"protocol":"hls","mime_type":"audio/ogg"}}
        ]}}
        """
        XCTAssertNil(try JSONDecoder().decode(CloudTrack.self, from: Data(json.utf8)).music)
    }
}
