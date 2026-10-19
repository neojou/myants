package com.neojou.myants

import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addUiScene
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private const val TAG = "AppUi"

/**
 * 疊在 3D 場景上的 UI：左上角 About 按鈕，以及兩行的 About 面板
 * （第一行 [AppVersion.APP_NAME]，第二行 `Version` + [AppVersion.NAME]）。
 */
fun KoolApplication.addAppUiScene(): Scene = addUiScene {
    lateinit var aboutSurface: UiSurface

    // Top menu bar: one leaf item "About".
    addPanelSurface(backgroundColor = { null }) {
        modifier
            .width(FitContent)
            .height(FitContent)
            .align(AlignmentX.Start, AlignmentY.Top)
            .margin(sizes.gap)
        Button("About") {
            modifier
                .padding(horizontal = sizes.largeGap, vertical = sizes.smallGap)
                .onClick {
                    MyLog.add(TAG, "About clicked", LogLevel.DEBUG)
                    aboutSurface.isVisible = true
                }
        }
    }

    // About dialog, hidden until the button is clicked.
    aboutSurface = addPanelSurface {
        modifier
            .size(360.dp, 180.dp)
            .align(AlignmentX.Center, AlignmentY.Center)
            .background(RoundRectBackground(colors.background, 12.dp))
            .padding(sizes.largeGap)
        Text(AppVersion.APP_NAME) {
            modifier.alignX(AlignmentX.Center).margin(top = sizes.largeGap).font(sizes.largeText)
        }
        Text("Version ${AppVersion.NAME}") {
            modifier.alignX(AlignmentX.Center).margin(top = sizes.gap)
        }
        Button("OK") {
            modifier
                .alignX(AlignmentX.Center)
                .margin(top = sizes.largeGap)
                .padding(horizontal = sizes.largeGap, vertical = sizes.smallGap)
                .onClick { aboutSurface.isVisible = false }
        }
    }
    aboutSurface.isVisible = false
}
