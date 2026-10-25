package com.neojou.myants.render3d

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * render3d 不得依賴 AppSession、session、ui、input（#47）。
 * 規則與允許清單和 scripts/check-layers.sh 共用 scripts/render3d-layer-allowlist.txt。
 */
class RenderLayerGuardTest {
    private val pattern = Regex("""com\.neojou\.myants\.(AppSession|session\.|ui\.|input\.)""")

    private fun repoRoot(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            if (File(dir, "scripts/render3d-layer-allowlist.txt").isFile) return dir
            dir = dir.parentFile
        }
        fail("找不到 scripts/render3d-layer-allowlist.txt")
    }

    private fun allowlist(root: File): Set<String> =
        File(root, "scripts/render3d-layer-allowlist.txt").readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toSet()

    private fun offenders(root: File): Set<String> {
        val dir = File(root, "koolApp/src/commonMain/kotlin/com/neojou/myants/render3d")
        return dir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && pattern.containsMatchIn(it.readText()) }
            .map { it.relativeTo(dir).invariantSeparatorsPath }
            .toSet()
    }

    @Test
    fun render3dDependsOnSessionOnlyInAllowlistedFiles() {
        val root = repoRoot()
        val extra = offenders(root) - allowlist(root)
        assertTrue(extra.isEmpty(), "render3d 新增了 session/ui/input 依賴：$extra")
    }

    @Test
    fun allowlistHasNoStaleEntries() {
        val root = repoRoot()
        val stale = allowlist(root) - offenders(root)
        assertTrue(stale.isEmpty(), "允許清單已不再需要，請刪除：$stale")
    }
}
