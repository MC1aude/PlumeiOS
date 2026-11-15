import XCTest
import AVFoundation
@testable import Plume

final class CloudLiveTests: XCTestCase {
    // Run explicitly in CI to distinguish external service failures from app regressions.
    func testLiveSearchAndAudioSource() async throws {
        guard ProcessInfo.processInfo.environment["PLUME_LIVE_TESTS"] == "1" else {
            throw XCTSkip("Enable PLUME_LIVE_TESTS for live SoundCloud validation")
        }
        let client = SoundCloudClient()
        let tracks = try await client.search("piano")
        XCTAssertFalse(tracks.isEmpty)
        let track = try XCTUnwrap(tracks.first)
        let source = try await client.source(track)
        let asset = AVURLAsset(url: source.url)
        let audio = try await asset.loadTracks(withMediaType: .audio)
        XCTAssertFalse(audio.isEmpty, "Resolved stream must contain an iPhone-compatible audio track")
    }
}
