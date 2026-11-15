import XCTest

final class LaunchTests: XCTestCase {
    func testLaunchAndLibraryNavigation() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.staticTexts["PLUME"].waitForExistence(timeout: 15))
        app.buttons["Бібліотека"].tap()
        XCTAssertTrue(app.staticTexts["БІБЛІОТЕКА"].waitForExistence(timeout: 5))
        app.buttons["Завантаження"].tap()
        XCTAssertTrue(app.staticTexts["Тут буде твоя музика"].exists)
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "Plume-iPhone-library"
        attachment.lifetime = .keepAlways
        add(attachment)
        app.buttons["Пошук"].tap()
        XCTAssertTrue(app.textFields["Трек або виконавець"].exists)
    }
}
