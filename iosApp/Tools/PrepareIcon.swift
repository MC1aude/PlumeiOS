import Foundation
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers

// Preserve the supplied artwork. App Store-style icon assets must be square
// RGB PNGs without an alpha channel; iOS applies the home-screen mask itself.
guard CommandLine.arguments.count == 3 else { fatalError("Expected source and output paths") }
let input = URL(fileURLWithPath: CommandLine.arguments[1])
let output = URL(fileURLWithPath: CommandLine.arguments[2])
let encoded = try String(contentsOf: input, encoding: .utf8)
guard let data = Data(base64Encoded: encoded, options: .ignoreUnknownCharacters),
      let source = CGImageSourceCreateWithData(data as CFData, nil),
      let original = CGImageSourceCreateImageAtIndex(source, 0, nil),
      let context = CGContext(data: nil, width: 1024, height: 1024,
          bitsPerComponent: 8, bytesPerRow: 0, space: CGColorSpaceCreateDeviceRGB(),
          bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue) else {
    fatalError("Could not read the supplied app icon")
}
context.setFillColor(CGColor(gray: 0, alpha: 1))
context.fill(CGRect(x: 0, y: 0, width: 1024, height: 1024))
context.interpolationQuality = .high
context.draw(original, in: CGRect(x: 0, y: 0, width: 1024, height: 1024))
try FileManager.default.createDirectory(at: output.deletingLastPathComponent(), withIntermediateDirectories: true)
guard let image = context.makeImage(),
      let destination = CGImageDestinationCreateWithURL(output as CFURL, UTType.png.identifier as CFString, 1, nil) else {
    fatalError("Could not prepare the app icon")
}
CGImageDestinationAddImage(destination, image, nil)
guard CGImageDestinationFinalize(destination) else { fatalError("Could not write the app icon") }
let alpha = image.alphaInfo
guard image.width == 1024, image.height == 1024,
      alpha == .none || alpha == .noneSkipFirst || alpha == .noneSkipLast else {
    fatalError("The app icon must be opaque and 1024 x 1024")
}
print("Prepared opaque 1024 x 1024 Plume icon")
