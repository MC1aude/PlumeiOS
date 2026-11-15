import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                // Compose Multiplatform handles the keyboard insets itself.
                .ignoresSafeArea(.keyboard)
        }
    }
}
