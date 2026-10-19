package com.neojou.myants

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJs
import kotlinx.browser.document

/** Browser entry. kool draws into index.html `<canvas id="glCanvas">`. */
fun main() {
    document.title = appTitle
    KoolApplication(config = KoolConfigJs(canvasName = "glCanvas")) {
        myantsApp(browserLaunch())
    }
}
