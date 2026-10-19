package com.neojou.myants

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJs
import kotlinx.browser.document

/** Browser entry. kool 畫在 index.html 的 <canvas id="glCanvas">。 */
fun main() {
    document.title = appTitle
    KoolApplication(config = KoolConfigJs(canvasName = "glCanvas")) {
        koolStudyApp(browserLaunch())
    }
}
