package com.neojou.myants

import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import org.lwjgl.system.JNI.invokePPJ
import org.lwjgl.system.JNI.invokePPP
import org.lwjgl.system.JNI.invokePPPP
import org.lwjgl.system.JNI.invokePPPPV
import org.lwjgl.system.JNI.invokePPPV
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.system.macosx.ObjCRuntime.class_addMethod
import org.lwjgl.system.macosx.ObjCRuntime.getLibrary
import org.lwjgl.system.macosx.ObjCRuntime.objc_allocateClassPair
import org.lwjgl.system.macosx.ObjCRuntime.objc_getClass
import org.lwjgl.system.macosx.ObjCRuntime.objc_registerClassPair
import org.lwjgl.system.macosx.ObjCRuntime.sel_getUid
import org.lwjgl.system.macosx.ObjCRuntime.sel_registerName

private const val TAG = "MacOsApplicationName"
private const val ABOUT_CLASS = "KoolStudyAboutTarget"
private const val ABOUT_SELECTOR = "showAbout:"

/**
 * GLFW builds the macOS application menu from the `java` executable name, and
 * its About item opens the standard panel with the JDK bundle version.
 * `-Xdock:name` does not change either of those.
 *
 * AppKit ignores a plain [NSMenu.setTitle] on the application menu. A trailing
 * `\u001b` is the usual way to make the menu-bar name update.
 */
internal fun applyMacOsApplicationName(appName: String, version: String) {
    val osName = System.getProperty("os.name").orEmpty()
    if (!osName.contains("mac", ignoreCase = true)) return
    try {
        renameApplicationMenu(appName, version)
    } catch (error: Throwable) {
        MyLog.add(TAG, "Failed to rename the macOS application menu: ${error.message}", LogLevel.WARN)
    }
}

private fun renameApplicationMenu(appName: String, version: String) {
    val send = getLibrary().getFunctionAddress("objc_msgSend")
    val app = invokePPP(objc_getClass("NSApplication"), sel_getUid("sharedApplication"), send)
    val mainMenu = invokePPP(app, sel_getUid("mainMenu"), send)
    if (mainMenu == 0L) {
        MyLog.add(TAG, "NSApp.mainMenu is missing", LogLevel.WARN)
        return
    }
    val appMenuItem = invokePPPP(mainMenu, sel_getUid("itemAtIndex:"), 0L, send)
    val appMenu = invokePPP(appMenuItem, sel_getUid("submenu"), send)
    if (appMenu == 0L) {
        MyLog.add(TAG, "Application menu is missing", LogLevel.WARN)
        return
    }

    invokePPPV(appMenu, sel_getUid("setTitle:"), nsString(send, appName + "\u001b"), send)

    val processInfo = invokePPP(objc_getClass("NSProcessInfo"), sel_getUid("processInfo"), send)
    invokePPPV(processInfo, sel_getUid("setProcessName:"), nsString(send, appName), send)

    val aboutTarget = installAboutTarget(send, appName, version)
    val showAbout = sel_getUid(ABOUT_SELECTOR)
    val itemCount = invokePPJ(appMenu, sel_getUid("numberOfItems"), send)
    var renamed = 0
    var aboutHooked = false
    for (index in 0L until itemCount) {
        val item = invokePPPP(appMenu, sel_getUid("itemAtIndex:"), index, send)
        if (item == 0L) continue
        val title = nsStringText(send, invokePPP(item, sel_getUid("title"), send))
        if (!title.endsWith(" java")) continue
        val updated = title.removeSuffix(" java") + " " + appName
        invokePPPV(item, sel_getUid("setTitle:"), nsString(send, updated), send)
        renamed++
        if (title.startsWith("About ") && aboutTarget != 0L) {
            invokePPPV(item, sel_getUid("setTarget:"), aboutTarget, send)
            invokePPPV(item, sel_getUid("setAction:"), showAbout, send)
            aboutHooked = true
        }
    }
    MyLog.add(TAG, "Application menu set to $appName $version ($renamed items, about=$aboutHooked)")
}

private var aboutHandler: AboutPanelCallback? = null

private fun installAboutTarget(send: Long, appName: String, version: String): Long {
    val handler = aboutHandler ?: AboutPanelCallback {
        try {
            showStandardAbout(appName, version)
        } catch (error: Throwable) {
            MyLog.add(TAG, "About panel failed: ${error.message}", LogLevel.WARN)
        }
    }.also { aboutHandler = it }
    val selector = sel_registerName(ABOUT_SELECTOR)
    var cls = objc_getClass(ABOUT_CLASS)
    if (cls == 0L) {
        cls = objc_allocateClassPair(objc_getClass("NSObject"), ABOUT_CLASS, 0)
        if (cls == 0L || !class_addMethod(cls, selector, handler.address(), "v@:@")) {
            MyLog.add(TAG, "Could not register the About target", LogLevel.WARN)
            return 0
        }
        objc_registerClassPair(cls)
    }
    val allocated = invokePPP(cls, sel_getUid("alloc"), send)
    return invokePPP(allocated, sel_getUid("init"), send)
}

private fun showStandardAbout(appName: String, version: String) {
    val send = getLibrary().getFunctionAddress("objc_msgSend")
    val options = invokePPP(objc_getClass("NSMutableDictionary"), sel_getUid("dictionary"), send)
    putOption(send, options, "ApplicationName", appName)
    putOption(send, options, "ApplicationVersion", version)
    // Empty build version, so the panel does not append the JDK version.
    putOption(send, options, "Version", "")
    val app = invokePPP(objc_getClass("NSApplication"), sel_getUid("sharedApplication"), send)
    invokePPPV(app, sel_getUid("orderFrontStandardAboutPanelWithOptions:"), options, send)
}

private fun putOption(send: Long, options: Long, key: String, value: String) {
    invokePPPPV(
        options,
        sel_getUid("setObject:forKey:"),
        nsString(send, value),
        nsString(send, key),
        send,
    )
}

private fun nsString(send: Long, text: String): Long {
    MemoryStack.stackPush().use { stack ->
        return invokePPPP(
            objc_getClass("NSString"),
            sel_getUid("stringWithUTF8String:"),
            memAddress(stack.UTF8(text)),
            send,
        )
    }
}

private fun nsStringText(send: Long, nsString: Long): String {
    if (nsString == 0L) return ""
    val utf8 = invokePPP(nsString, sel_getUid("UTF8String"), send)
    if (utf8 == 0L) return ""
    return memUTF8(utf8)
}
