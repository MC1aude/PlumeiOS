package com.example.plume.shared

import platform.UIKit.UIDevice

private class IosPlatform : Platform {
    override val name: String = with(UIDevice.currentDevice) {
        "$systemName $systemVersion"
    }
}

actual fun currentPlatform(): Platform = IosPlatform()
