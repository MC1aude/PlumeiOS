import SwiftUI
import Shared

/// Hosts the shared Compose Multiplatform UI exported by the `:shared` module.
///
/// `MainViewControllerKt` is the Swift name of the generated file class for
/// `shared/src/iosMain/kotlin/com/example/plume/MainViewController.kt`.
struct ComposeView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // The shared Compose tree updates itself.
    }
}
