import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(BackgroundSession.self) private var appDelegate
    var body: some Scene {
        WindowGroup {
            PlumeAppView()
        }
    }
}
