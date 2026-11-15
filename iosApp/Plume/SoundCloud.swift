import Foundation

struct SoundCloudUser: Decodable {
    var username: String?
    var avatar_url: String?
}
struct Transcoding: Decodable {
    struct Format: Decodable { var protocolName: String?; var mime_type: String?
        enum CodingKeys: String, CodingKey { case protocolName = "protocol", mime_type }
    }
    var url: String
    var format: Format
}
struct CloudTrack: Decodable {
    struct Media: Decodable { var transcodings: [Transcoding]? }
    var id: Int64
    var title: String?
    var user: SoundCloudUser?
    var artwork_url: String?
    var duration: Double?
    var streamable: Bool?
    var media: Media?

    var music: MusicTrack? {
        guard streamable != false, let title = title,
              let stream = media?.transcodings?.first(where: { $0.format.protocolName == "progressive" })
                ?? media?.transcodings?.first(where: {
                    $0.format.protocolName == "hls" && $0.format.mime_type?.contains("ogg") != true
                }) else { return nil }
        return MusicTrack(id: String(id), title: title, artist: user?.username ?? "SoundCloud",
                          artwork: (artwork_url ?? user?.avatar_url ?? "")
                            .replacingOccurrences(of: "-large.", with: "-t500x500."),
                          duration: (duration ?? 0) / 1000, endpoint: stream.url)
    }
}

struct AudioSource { var url: URL; var hls: Bool }

actor SoundCloudClient {
    private var clientID = "QhVEWRrJKvW4uSU5u5EolOFTXYUgz"
    private let session: URLSession
    init(session: URLSession = .shared) { self.session = session }

    private func request(_ url: URL) async throws -> Data {
        var request = URLRequest(url: url)
        request.timeoutInterval = 35
        request.setValue("Mozilla/5.0", forHTTPHeaderField: "User-Agent")
        let (data, response) = try await session.data(for: request)
        guard let response = response as? HTTPURLResponse else {
            throw PlumeError.message("Invalid server response")
        }
        guard (200...299).contains(response.statusCode) else {
            throw NSError(domain: "SoundCloud", code: response.statusCode,
                          userInfo: [NSLocalizedDescriptionKey: "SoundCloud HTTP \(response.statusCode)"])
        }
        return data
    }

    private func api<T: Decodable>(_ endpoint: String, query: [URLQueryItem] = []) async throws -> T {
        guard var components = URLComponents(string: endpoint) else { throw URLError(.badURL) }
        // Only send the public client ID to SoundCloud API hosts.
        guard let host = components.host, ["api-v2.soundcloud.com", "api.soundcloud.com"].contains(host) else {
            throw URLError(.unsupportedURL)
        }
        func url(_ id: String) -> URL {
            var value = components
            value.queryItems = (components.queryItems ?? []).filter { $0.name != "client_id" }
                + query + [URLQueryItem(name: "client_id", value: id)]
            return value.url!
        }
        do {
            return try JSONDecoder().decode(T.self, from: await request(url(clientID)))
        } catch let error as NSError where error.domain == "SoundCloud" && [401, 403].contains(error.code) {
            clientID = try await refreshedClientID()
            return try JSONDecoder().decode(T.self, from: await request(url(clientID)))
        }
    }

    private func matches(_ pattern: String, in text: String) -> [String] {
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
        return regex.matches(in: text, range: NSRange(text.startIndex..., in: text)).compactMap {
            guard let range = Range($0.range(at: 1), in: text) else { return nil }
            return String(text[range])
        }
    }

    private func refreshedClientID() async throws -> String {
        let html = String(decoding: try await request(URL(string: "https://soundcloud.com")!), as: UTF8.self)
        let pattern = #"client_id["']?\s*[:=]\s*["']([A-Za-z0-9]{32})["']"#
        if let id = matches(pattern, in: html).first { return id }
        for script in matches(#"<script[^>]+src="([^"]+)""#, in: html).reversed().prefix(25) {
            try Task.checkCancellation()
            guard let url = URL(string: script), url.host?.hasSuffix(".sndcdn.com") == true else { continue }
            guard let data = try? await request(url) else { continue }
            if let id = matches(pattern, in: String(decoding: data, as: UTF8.self)).first { return id }
        }
        throw PlumeError.message("SoundCloud: unable to refresh access. Please try again later.")
    }

    func search(_ query: String) async throws -> [MusicTrack] {
        struct Response: Decodable { var collection: [CloudTrack]? }
        let response: Response = try await api("https://api-v2.soundcloud.com/search/tracks",
            query: [.init(name: "q", value: query), .init(name: "limit", value: "40")])
        return response.collection?.compactMap(\.music) ?? []
    }

    func feed() async throws -> [MusicTrack] {
        struct Entry: Decodable { var track: CloudTrack? }
        struct Response: Decodable { var collection: [Entry]? }
        do {
            let response: Response = try await api("https://api-v2.soundcloud.com/charts", query: [
                .init(name: "kind", value: "trending"), .init(name: "genre", value: "soundcloud:genres:all-music"),
                .init(name: "limit", value: "40")])
            let tracks = response.collection?.compactMap { $0.track?.music } ?? []
            if !tracks.isEmpty { return tracks }
        } catch is CancellationError { throw CancellationError() }
        catch { /* Charts may not be available in every region; search remains usable. */ }
        return try await search("popular music")
    }

    func source(_ track: MusicTrack) async throws -> AudioSource {
        struct Response: Decodable { var url: String? }
        let details: CloudTrack = try await api("https://api-v2.soundcloud.com/tracks/\(track.id)")
        let streams = details.media?.transcodings ?? []
        let compatible = streams.filter {
            $0.format.protocolName == "progressive" ||
                ($0.format.protocolName == "hls" && $0.format.mime_type?.contains("ogg") != true)
        }.sorted { ($0.format.protocolName == "progressive" ? 0 : 1) < ($1.format.protocolName == "progressive" ? 0 : 1) }
        var lastError: Error = PlumeError.message("No audio stream compatible with iPhone is available for this track.")
        for stream in compatible {
            do {
                let response: Response = try await api(stream.url)
                guard let raw = response.url, let url = URL(string: raw), url.scheme == "https" else {
                    throw URLError(.badURL)
                }
                return AudioSource(url: url, hls: stream.format.protocolName == "hls")
            } catch is CancellationError { throw CancellationError() }
            catch { lastError = error }
        }
        throw lastError
    }
}
