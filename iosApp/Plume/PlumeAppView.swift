import SwiftUI
import UniformTypeIdentifiers
import AVKit

@MainActor
final class PlumeModel: ObservableObject {
    let api = SoundCloudClient()
    let library: MusicLibrary
    let player: Playback
    let downloads: Downloads
    @Published var home: [MusicTrack] = []
    @Published var results: [MusicTrack] = []
    @Published var query = ""
    @Published var loadingHome = false
    @Published var searching = false
    @Published var message: String?
    @Published var homeError: String?
    private var searchTask: Task<Void, Never>?
    init() {
        let library = MusicLibrary()
        self.library = library
        player = Playback(api: api, library: library)
        downloads = Downloads(api: api, library: library)
    }
    func loadHome() async {
        guard !loadingHome else { return }
        loadingHome = true; homeError = nil
        defer { loadingHome = false }
        do { home = try await api.feed() }
        catch is CancellationError { }
        catch { homeError = error.localizedDescription }
    }
    func search() {
        searchTask?.cancel()
        let text = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { results = []; searching = false; return }
        searching = true
        searchTask = Task {
            do {
                try await Task.sleep(nanoseconds: 400_000_000)
                let tracks = try await api.search(text)
                try Task.checkCancellation()
                results = tracks; searching = false
            } catch is CancellationError { }
            catch { searching = false; message = error.localizedDescription }
        }
    }
    func attempt(_ operation: () throws -> Void) {
        do { try operation() } catch { message = error.localizedDescription }
    }
}

struct PlumeAppView: View {
    @StateObject private var model = PlumeModel()
    var body: some View {
        PlumeShell(model: model, library: model.library, player: model.player, downloads: model.downloads)
    }
}

struct PlumeShell: View {
    @ObservedObject var model: PlumeModel
    @ObservedObject var library: MusicLibrary
    @ObservedObject var player: Playback
    @ObservedObject var downloads: Downloads
    @AppStorage("plume.language") private var language = "uk"
    @AppStorage("plume.light") private var lightMode = false
    @State private var tab = 0
    @State private var libraryTab = 0
    @State private var showPlayer = false
    @State private var showQueue = false
    @State private var showImporter = false
    @State private var showCreate = false
    @State private var playlistName = ""
    @State private var selectedPlaylist: UUID?
    @State private var addTrack: MusicTrack?
    @State private var shareURL: URL?
    @State private var deleteTrack: MusicTrack?
    @State private var deletePlaylist: MusicPlaylist?

    private func t(_ uk: String, _ ru: String, _ en: String) -> String {
        language == "uk" ? uk : language == "ru" ? ru : en
    }
    private var ink: Color { lightMode ? .black : .white }
    private var paper: Color { lightMode ? Color(white: 0.96) : Color(white: 0.035) }
    private var panel: Color { lightMode ? Color(white: 0.9) : Color(white: 0.10) }

    var body: some View {
        VStack(spacing: 0) {
            Group {
                switch tab {
                case 0: homeScreen
                case 1: searchScreen
                case 2: libraryScreen
                default: settingsScreen
                }
            }.frame(maxWidth: .infinity, maxHeight: .infinity)
            if let current = player.current { miniPlayer(current) }
            bottomBar
        }
        .foregroundColor(ink).background(paper.ignoresSafeArea())
        .tint(ink).preferredColorScheme(lightMode ? .light : .dark)
        .task { await model.loadHome() }
        .sheet(isPresented: $showPlayer) { fullPlayer }
        .sheet(isPresented: $showQueue) { queueSheet }
        .sheet(item: $addTrack) { track in playlistPicker(track) }
        .sheet(isPresented: Binding(get: { shareURL != nil }, set: { if !$0 { shareURL = nil } })) {
            if let shareURL { ShareSheet(url: shareURL) }
        }
        .fileImporter(isPresented: $showImporter, allowedContentTypes: [.audio]) { result in
            switch result {
            case .success(let url):
                Task {
                    do { try await downloads.importAudio(url) }
                    catch { model.message = error.localizedDescription }
                }
            case .failure(let error): model.message = error.localizedDescription
            }
        }
        .alert(t("Новий плейлист", "Новый плейлист", "New playlist"), isPresented: $showCreate) {
            TextField(t("Назва", "Название", "Name"), text: $playlistName)
            Button(t("Створити", "Создать", "Create")) {
                model.attempt { try library.createPlaylist(playlistName) }
                playlistName = ""
            }
            Button(t("Скасувати", "Отмена", "Cancel"), role: .cancel) {}
        }
        .alert(t("Видалити файл?", "Удалить файл?", "Delete file?"),
               isPresented: Binding(get: { deleteTrack != nil }, set: { if !$0 { deleteTrack = nil } })) {
            Button(t("Видалити", "Удалить", "Delete"), role: .destructive) {
                if let track = deleteTrack {
                    model.attempt {
                        if player.current?.id == track.id { player.stop() }
                        try downloads.remove(track)
                        if track.imported { try library.deleteImported(track) }
                    }
                }
                deleteTrack = nil
            }
            Button(t("Скасувати", "Отмена", "Cancel"), role: .cancel) { deleteTrack = nil }
        }
        .alert(t("Видалити плейлист?", "Удалить плейлист?", "Delete playlist?"),
               isPresented: Binding(get: { deletePlaylist != nil }, set: { if !$0 { deletePlaylist = nil } })) {
            Button(t("Видалити", "Удалить", "Delete"), role: .destructive) {
                if let playlist = deletePlaylist { model.attempt { try library.deletePlaylist(playlist.id) } }
                deletePlaylist = nil; selectedPlaylist = nil
            }
            Button(t("Скасувати", "Отмена", "Cancel"), role: .cancel) { deletePlaylist = nil }
        }
        .alert("Plume", isPresented: Binding(get: { model.message != nil || player.error != nil || downloads.error != nil },
            set: { if !$0 { model.message = nil; player.error = nil; downloads.error = nil } })) {
                Button("OK") { model.message = nil; player.error = nil; downloads.error = nil }
            } message: { Text(model.message ?? player.error ?? downloads.error ?? "") }
    }

    private var homeScreen: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                HStack(spacing: 12) {
                    Image(systemName: "waveform").font(.title2).padding(12).background(panel, in: Circle())
                    VStack(alignment: .leading, spacing: 4) {
                        Text("PLUME").font(.system(size: 25, weight: .black)).tracking(3)
                        Text(t("ТВОЯ МУЗИКА", "ТВОЯ МУЗЫКА", "YOUR MUSIC")).font(.system(size: 10, weight: .semibold)).tracking(2).opacity(0.6)
                    }
                    Spacer()
                    Image(systemName: "sparkles").padding(12).background(panel, in: Circle())
                }
                Text(t("Знайди свій ритм", "Найди свой ритм", "Find your rhythm"))
                    .font(.system(size: 34, weight: .bold))
                if model.loadingHome && model.home.isEmpty { ProgressView().frame(maxWidth: .infinity).padding(40) }
                if let error = model.homeError {
                    Text(error).font(.subheadline).opacity(0.7)
                    Button(t("Спробувати знову", "Повторить", "Try again")) { Task { await model.loadHome() } }
                }
                if !model.home.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 16) {
                            ForEach(Array(model.home.prefix(8))) { track in
                                Button { player.play(track, in: model.home) } label: {
                                    VStack(alignment: .leading, spacing: 8) {
                                        Artwork(source: track.artwork).frame(width: 160, height: 160).clipShape(RoundedRectangle(cornerRadius: 22))
                                        Text(track.title).font(.subheadline.bold()).lineLimit(1)
                                        Text(track.artist).font(.caption).opacity(0.55).lineLimit(1)
                                    }.frame(width: 160)
                                }.buttonStyle(.plain)
                            }
                        }
                    }
                    sectionTitle(t("Рекомендовано", "Рекомендации", "Recommended"))
                    trackList(model.home)
                }
            }.padding(20)
        }.refreshable { await model.loadHome() }
    }

    private var searchScreen: some View {
        VStack(alignment: .leading, spacing: 16) {
            sectionTitle(t("ПОШУК", "ПОИСК", "SEARCH")).padding(.horizontal, 20)
            HStack {
                Image(systemName: "magnifyingglass").opacity(0.6)
                TextField(t("Трек або виконавець", "Трек или исполнитель", "Track or artist"), text: $model.query)
                    .autocorrectionDisabled().submitLabel(.search)
                    .onChange(of: model.query) { model.search() }
                if !model.query.isEmpty {
                    Button { model.query = "" } label: { Image(systemName: "xmark.circle.fill") }
                }
            }.padding(16).background(panel, in: RoundedRectangle(cornerRadius: 18)).padding(.horizontal, 20)
            if model.searching { ProgressView().frame(maxWidth: .infinity) }
            ScrollView {
                if model.results.isEmpty && !model.searching {
                    emptyState("magnifyingglass", model.query.isEmpty
                        ? t("Знайди улюблену музику", "Найди любимую музыку", "Find your favorite music")
                        : t("Нічого не знайдено", "Ничего не найдено", "No results"))
                }
                trackList(model.results).padding(.horizontal, 20)
            }.scrollDismissesKeyboard(.interactively)
        }.padding(.top, 20)
    }

    private var libraryScreen: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                if selectedPlaylist != nil {
                    Button { selectedPlaylist = nil } label: { Image(systemName: "chevron.left").padding(8) }
                }
                sectionTitle(selectedPlaylist.flatMap { id in library.snapshot.playlists.first { $0.id == id }?.name }
                    ?? t("БІБЛІОТЕКА", "БИБЛИОТЕКА", "LIBRARY"))
                Spacer()
                Button { showImporter = true } label: { Image(systemName: "square.and.arrow.down").padding(8) }
                    .accessibilityLabel(t("Імпорт із Файлів", "Импорт из Файлов", "Import from Files"))
                Button { showCreate = true } label: { Image(systemName: "plus").padding(8) }
            }.padding(.horizontal, 20)
            if selectedPlaylist == nil {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(0..<4) { index in
                            Button { libraryTab = index } label: {
                                Text([t("Збереження", "Сохранённое", "Saved"), t("Завантаження", "Загрузки", "Downloads"),
                                      t("Плейлисти", "Плейлисты", "Playlists"), t("На iPhone", "На iPhone", "On iPhone")][index])
                                    .font(.subheadline.weight(.semibold)).padding(.horizontal, 16).padding(.vertical, 11)
                                    .foregroundColor(libraryTab == index ? paper : ink)
                                    .background(libraryTab == index ? ink : panel, in: Capsule())
                            }
                        }
                    }.padding(.horizontal, 20)
                }
            }
            ScrollView {
                if let id = selectedPlaylist, let playlist = library.snapshot.playlists.first(where: { $0.id == id }) {
                    let tracks = playlist.trackIDs.compactMap { id in library.snapshot.tracks.first { $0.id == id } }
                    if tracks.isEmpty { emptyState("music.note.list", t("Плейлист порожній", "Плейлист пуст", "Playlist is empty")) }
                    trackList(tracks, playlist: id).padding(.horizontal, 20)
                } else if libraryTab == 2 {
                    VStack(spacing: 12) {
                        ForEach(library.snapshot.playlists) { playlist in
                            Button { selectedPlaylist = playlist.id } label: {
                                HStack {
                                    Image(systemName: "music.note.list").font(.title).frame(width: 56, height: 56).background(panel, in: RoundedRectangle(cornerRadius: 14))
                                    VStack(alignment: .leading) {
                                        Text(playlist.name).font(.headline)
                                        Text("\(playlist.trackIDs.count) " + t("треків", "треков", "tracks")).font(.caption).opacity(0.6)
                                    }
                                    Spacer()
                                    Image(systemName: "chevron.right").opacity(0.5)
                                }
                            }.buttonStyle(.plain).contextMenu {
                                Button(role: .destructive) { deletePlaylist = playlist } label: {
                                    Label(t("Видалити", "Удалить", "Delete"), systemImage: "trash")
                                }
                            }
                        }
                        if library.snapshot.playlists.isEmpty { emptyState("music.note.list", t("Створи перший плейлист", "Создай первый плейлист", "Create your first playlist")) }
                    }.padding(.horizontal, 20)
                } else {
                    let tracks = library.snapshot.tracks.filter {
                        libraryTab == 0 ? $0.favorite : libraryTab == 1 ? $0.downloaded && !$0.imported : $0.imported
                    }
                    if tracks.isEmpty { emptyState("music.note", t("Тут буде твоя музика", "Здесь будет твоя музыка", "Your music will appear here")) }
                    trackList(tracks).padding(.horizontal, 20)
                }
            }
        }.padding(.top, 20)
    }

    private var settingsScreen: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                sectionTitle(t("НАЛАШТУВАННЯ", "НАСТРОЙКИ", "SETTINGS"))
                VStack(alignment: .leading, spacing: 16) {
                    Label(t("Мова", "Язык", "Language"), systemImage: "globe").font(.headline)
                    Picker("Language", selection: $language) {
                        Text("Українська").tag("uk"); Text("Русский").tag("ru"); Text("English").tag("en")
                    }.pickerStyle(.segmented)
                }.padding(20).background(panel, in: RoundedRectangle(cornerRadius: 20))
                Toggle(t("Світла тема", "Светлая тема", "Light appearance"), isOn: $lightMode)
                    .padding(20).background(panel, in: RoundedRectangle(cornerRadius: 20))
                VStack(alignment: .leading, spacing: 12) {
                    Text("PLUME").font(.title.bold()).tracking(3)
                    Text(t("Музика, яка завжди поруч.", "Музыка, которая всегда рядом.", "Music that stays with you.")).opacity(0.65)
                    Text(t("HLS-завантаження зберігаються всередині Plume. Звичайні аудіофайли можна експортувати через «Поділитися».",
                           "HLS-загрузки хранятся внутри Plume. Обычные аудиофайлы можно экспортировать через «Поделиться».",
                           "Offline HLS stays inside Plume. Regular audio files can be exported using Share.")).font(.footnote).opacity(0.6)
                    Link("SoundCloud", destination: URL(string: "https://soundcloud.com")!).font(.footnote)
                }.padding(20).background(panel, in: RoundedRectangle(cornerRadius: 20))
            }.padding(20)
        }
    }

    private var bottomBar: some View {
        HStack(spacing: 0) {
            navButton(0, "house", t("Головна", "Главная", "Home"))
            navButton(1, "magnifyingglass", t("Пошук", "Поиск", "Search"))
            navButton(2, "square.stack", t("Бібліотека", "Библиотека", "Library"))
            navButton(3, "slider.horizontal.3", t("Меню", "Меню", "Menu"))
        }.padding(.top, 14).padding(.bottom, 8).background(paper)
    }
    private func navButton(_ index: Int, _ icon: String, _ title: String) -> some View {
        Button { tab = index } label: {
            VStack(spacing: 5) {
                Image(systemName: icon).font(.system(size: 20, weight: tab == index ? .bold : .regular))
                Text(title).font(.system(size: 10, weight: .semibold))
            }.frame(maxWidth: .infinity).opacity(tab == index ? 1 : 0.4)
        }
    }
    private func sectionTitle(_ title: String) -> some View {
        Text(title).font(.system(size: 22, weight: .black)).tracking(1.2)
    }
    private func emptyState(_ icon: String, _ text: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: icon).font(.system(size: 44)).opacity(0.3)
            Text(text).font(.subheadline).opacity(0.6)
        }.frame(maxWidth: .infinity).padding(.vertical, 70)
    }

    private func trackList(_ tracks: [MusicTrack], playlist: UUID? = nil) -> some View {
        LazyVStack(spacing: 4) {
            ForEach(tracks) { original in
                let track = library.track(original)
                HStack(spacing: 12) {
                    Button { player.play(track, in: tracks) } label: {
                        HStack(spacing: 12) {
                            Artwork(source: track.artwork).frame(width: 52, height: 52).clipShape(RoundedRectangle(cornerRadius: 12))
                            VStack(alignment: .leading, spacing: 5) {
                                Text(track.title).font(.system(size: 14, weight: .semibold)).lineLimit(1)
                                HStack(spacing: 5) {
                                    if track.downloaded { Image(systemName: "arrow.down.circle.fill").font(.caption2) }
                                    Text(track.artist).font(.system(size: 12)).lineLimit(1)
                                }.opacity(0.5)
                            }
                            Spacer(minLength: 0)
                            if player.current?.id == track.id { Image(systemName: "waveform").font(.subheadline) }
                        }.contentShape(Rectangle())
                    }.buttonStyle(.plain)
                    Menu {
                        trackActions(track)
                        if let playlist {
                            Button(role: .destructive) { model.attempt { try library.remove(track, from: playlist) } } label: {
                                Label(t("Прибрати з плейлиста", "Убрать из плейлиста", "Remove from playlist"), systemImage: "minus.circle")
                            }
                        }
                    } label: {
                        if downloads.active.contains(track.id) { ProgressView().frame(width: 40, height: 44) }
                        else { Image(systemName: "ellipsis").frame(width: 40, height: 44) }
                    }
                }.padding(.vertical, 6)
            }
        }
    }

    @ViewBuilder private func trackActions(_ track: MusicTrack) -> some View {
        Button { model.attempt { try library.toggleFavorite(track) } } label: {
            Label(track.favorite ? t("Прибрати зі збережень", "Убрать из сохранённого", "Unsave")
                  : t("Зберегти", "Сохранить", "Save"), systemImage: track.favorite ? "heart.slash" : "heart")
        }
        if track.downloaded {
            Button(role: .destructive) { deleteTrack = track } label: {
                Label(t("Видалити завантаження", "Удалить загрузку", "Remove download"), systemImage: "trash")
            }
            if !track.imported || library.localURL(track) != nil {
                if let url = library.localURL(track), url.pathExtension != "movpkg" {
                    Button { shareURL = url } label: {
                        Label(t("Поділитися файлом", "Поделиться файлом", "Share file"), systemImage: "square.and.arrow.up")
                    }
                }
            }
        } else {
            Button { downloads.download(track) } label: {
                Label(t("Завантажити", "Скачать", "Download"), systemImage: "arrow.down.circle")
            }.disabled(downloads.active.contains(track.id))
        }
        Button { addTrack = track } label: {
            Label(t("До плейлиста", "В плейлист", "Add to playlist"), systemImage: "text.badge.plus")
        }
    }

    private func miniPlayer(_ track: MusicTrack) -> some View {
        VStack(spacing: 0) {
            GeometryReader { geometry in
                Rectangle().fill(ink.opacity(0.4)).frame(width: geometry.size.width * min(1, player.position / max(1, player.duration)))
            }.frame(height: 2)
            HStack(spacing: 12) {
                Button { showPlayer = true } label: {
                    HStack(spacing: 12) {
                        Artwork(source: track.artwork).frame(width: 44, height: 44).clipShape(RoundedRectangle(cornerRadius: 10))
                        VStack(alignment: .leading, spacing: 4) {
                            Text(track.title).font(.system(size: 13, weight: .semibold)).lineLimit(1)
                            Text(track.artist).font(.caption).opacity(0.5).lineLimit(1)
                        }
                        Spacer(minLength: 0)
                    }
                }.buttonStyle(.plain)
                Button { player.toggle() } label: {
                    if player.loading { ProgressView().frame(width: 40, height: 44) }
                    else { Image(systemName: player.playing ? "pause.fill" : "play.fill").font(.title3).frame(width: 40, height: 44) }
                }
                Button { player.next() } label: { Image(systemName: "forward.end.fill").frame(width: 36, height: 44) }
            }.padding(.horizontal, 16).padding(.vertical, 10).background(panel)
        }
    }

    private var fullPlayer: some View {
        GeometryReader { geometry in
            ScrollView {
                if let original = player.current {
                    let track = library.track(original)
                    VStack(spacing: 24) {
                        HStack {
                            Button { showPlayer = false } label: { Image(systemName: "chevron.down").padding(12) }
                            Spacer()
                            Text(t("ЗАРАЗ ГРАЄ", "СЕЙЧАС ИГРАЕТ", "NOW PLAYING")).font(.caption.bold()).tracking(2)
                            Spacer()
                            Menu { trackActions(track) } label: { Image(systemName: "ellipsis").padding(12) }
                        }
                        Artwork(source: track.artwork)
                            .frame(width: min(geometry.size.width - 56, 360), height: min(geometry.size.width - 56, 360))
                            .clipShape(RoundedRectangle(cornerRadius: 30))
                            .shadow(color: .black.opacity(0.3), radius: 25, y: 15)
                        HStack {
                            VStack(alignment: .leading, spacing: 8) {
                                Text(track.title).font(.title2.bold()).lineLimit(2)
                                Text(track.artist).font(.subheadline).opacity(0.5)
                            }
                            Spacer()
                            Button { model.attempt { try library.toggleFavorite(track) } } label: {
                                Image(systemName: track.favorite ? "heart.fill" : "heart").font(.title2).padding(8)
                            }
                        }
                        VStack(spacing: 4) {
                            Slider(value: Binding(get: { min(player.position, max(1, player.duration)) },
                                set: { player.seek($0) }), in: 0...max(1, player.duration))
                            HStack {
                                Text(time(player.position)); Spacer(); Text(time(player.duration))
                            }.font(.caption.monospacedDigit()).opacity(0.5)
                        }
                        HStack {
                            Button { player.shuffle.toggle() } label: {
                                Image(systemName: "shuffle").opacity(player.shuffle ? 1 : 0.3)
                            }.frame(maxWidth: .infinity)
                            Button { player.previous() } label: { Image(systemName: "backward.end.fill").font(.title) }.frame(maxWidth: .infinity)
                            Button { player.toggle() } label: {
                                Group {
                                    if player.loading { ProgressView().tint(paper) }
                                    else { Image(systemName: player.playing ? "pause.fill" : "play.fill").font(.title) }
                                }.foregroundColor(paper).frame(width: 76, height: 76).background(ink, in: Circle())
                            }
                            Button { player.next() } label: { Image(systemName: "forward.end.fill").font(.title) }.frame(maxWidth: .infinity)
                            Button { player.repeatMode = (player.repeatMode + 1) % 3 } label: {
                                Image(systemName: player.repeatMode == 2 ? "repeat.1" : "repeat").opacity(player.repeatMode == 0 ? 0.3 : 1)
                            }.frame(maxWidth: .infinity)
                        }
                        HStack {
                            AirPlayButton().frame(width: 44, height: 44)
                            Spacer()
                            Button { showPlayer = false; DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { showQueue = true } } label: {
                                Label(t("Черга", "Очередь", "Queue"), systemImage: "list.bullet")
                            }
                        }
                    }.padding(24)
                }
            }
        }.foregroundColor(ink).background(paper.ignoresSafeArea()).preferredColorScheme(lightMode ? .light : .dark)
    }

    private var queueSheet: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack {
                sectionTitle(t("ЧЕРГА", "ОЧЕРЕДЬ", "QUEUE")); Spacer()
                Button { showQueue = false } label: { Image(systemName: "xmark").padding(10) }
            }
            ScrollView { trackList(player.queue) }
        }.padding(20).foregroundColor(ink).background(paper.ignoresSafeArea())
    }

    private func playlistPicker(_ track: MusicTrack) -> some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack {
                sectionTitle(t("До плейлиста", "В плейлист", "Add to playlist"))
                Spacer()
                Button { addTrack = nil } label: { Image(systemName: "xmark").padding(10) }
            }
            ForEach(library.snapshot.playlists) { playlist in
                Button {
                    model.attempt { try library.add(track, to: playlist.id) }
                    addTrack = nil
                } label: { Label(playlist.name, systemImage: "music.note.list").padding(.vertical, 10) }
            }
            Button {
                addTrack = nil
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { showCreate = true }
            } label: { Label(t("Новий плейлист", "Новый плейлист", "New playlist"), systemImage: "plus") }
            Spacer()
        }.padding(24).foregroundColor(ink).background(paper.ignoresSafeArea())
    }
    private func time(_ seconds: Double) -> String {
        let value = seconds.isFinite ? Int(max(0, seconds)) : 0
        return String(format: "%d:%02d", value / 60, value % 60)
    }
}

struct Artwork: View {
    let source: String
    var body: some View {
        AsyncImage(url: URL(string: source)) { phase in
            if let image = phase.image { image.resizable().scaledToFill() }
            else {
                ZStack {
                    LinearGradient(colors: [Color(white: 0.22), Color(white: 0.08)], startPoint: .topLeading, endPoint: .bottomTrailing)
                    Image(systemName: "music.note").font(.largeTitle).foregroundColor(.white.opacity(0.4))
                }
            }
        }.clipped().accessibilityHidden(true)
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let url: URL
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }
    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
struct AirPlayButton: UIViewRepresentable {
    func makeUIView(context: Context) -> AVRoutePickerView { AVRoutePickerView() }
    func updateUIView(_ view: AVRoutePickerView, context: Context) {}
}
