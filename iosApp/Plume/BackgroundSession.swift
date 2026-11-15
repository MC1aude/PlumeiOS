import UIKit

@MainActor
final class BackgroundSession: NSObject, UIApplicationDelegate {
    static var completion: (() -> Void)?
    func application(_ application: UIApplication,
                     handleEventsForBackgroundURLSession identifier: String,
                     completionHandler: @escaping () -> Void) {
        Self.completion = completionHandler
    }
}
