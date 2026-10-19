package com.neojou.myants

import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import com.neojou.tools.SystemSettings
import de.fabmax.kool.KoolApplication

private const val TAG = "APP"

/** 共用的 app 內容：desktop 與 js 的 main 都呼叫這裡。 */
fun KoolApplication.koolStudyApp(launch: LaunchConfig = LaunchConfig()) {
    SystemSettings.initOnce()
    MyLog.add(TAG, "System Ready", LogLevel.DEBUG)
    val session = AppSession(launch)
    addAntSimScene(session)
    addAntSimUi(session)
    addAntSimInput(session)
}

/** 視窗與分頁標題，例如 `kool_study 0.1.0`。 */
val appTitle: String get() = "${AppVersion.APP_NAME} ${AppVersion.NAME}"
