package com.neojou.myants.render3d

import com.neojou.myants.sim.BroodStage
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.InsectKind
import de.fabmax.kool.util.Color

/**
 * Scene colors moved out of the renderers (#18). Values match the old literals.
 * UI chrome stays in MyantsTheme. Two views that differ keep both values.
 */
object WorldPalette {
    val worker = Color("#c4703a")
    val soldierHead2d = Color("#c01810")
    val soldierHead3d = Color(0.75f, 0.1f, 0.08f, 1f)
    val sugar2d = Color("#FFD700")
    val protein2d = Color("#CD5C5C")
    val sugar3d = Color(1f, 0.84f, 0.2f, 1f)
    val protein3d = Color(0.8f, 0.25f, 0.22f, 1f)
    val otherFood2d = Color(0.35f, 0.32f, 0.3f, 1f)
    val otherFood3d = Color(0.35f, 0.32f, 0.28f, 1f)

    val classicGround = Color(0.11f, 0.16f, 0.09f, 1f)
    val stone = Color(0.45f, 0.42f, 0.36f, 1f)
    val obstacle = Color(0.28f, 0.27f, 0.24f, 1f)
    val grass = Color(0.22f, 0.45f, 0.18f, 1f)
    val queen = Color(0.72f, 0.45f, 0.18f, 1f)
    val digit2d = Color(0.92f, 0.76f, 0.43f, 1f)
    val digit3d = Color("#E9C46A")
    val pane = Color(0.102f, 0.102f, 0.102f, 1f)
    val divider = Color(0.20f, 0.20f, 0.20f, 1f)
    val rain2d = Color(0.45f, 0.6f, 0.85f, 0.18f)
    val rain3d = Color(0.65f, 0.75f, 0.95f, 0.35f)
    val night = Color(0.02f, 0.03f, 0.08f, 1f)
    val letterbox = Color(0x11 / 255f, 0x11 / 255f, 0x11 / 255f, 1f)
    val transparent = Color(0f, 0f, 0f, 0f)
    val chamberFloor = Color(0.16f, 0.13f, 0.10f, 1f)
    val egg2d = Color(0.9f, 0.9f, 0.85f, 1f)
    val larva2d = Color(0.85f, 0.75f, 0.35f, 1f)
    val pupa2d = Color(0.72f, 0.55f, 0.32f, 1f)

    val sky = Color(0.45f, 0.62f, 0.82f, 1f)
    val skyZenith = Color(0.03f, 0.04f, 0.08f)
    val skyHorizon = Color(0.45f, 0.62f, 0.85f)
    val skyGround = Color(0.95f, 0.86f, 0.7f)
    val moonLight = Color(0.55f, 0.62f, 0.8f, 1f)
    val sunLight = Color(1f, 0.95f, 0.85f, 1f)
    val foodGlow = Color(4f, 4f, 4f)
    /** Warm grey; red leads blue so the sky light does not turn it blue-grey. */
    val clockStone = Color(0.50f, 0.45f, 0.38f, 1f)
    /** Same hex as MyantsTheme.colors.primary. Kept here so render does not depend on UI. */
    val selectRingInner = Color("#6750A4")
    val selectRingOuter = Color("#EADDFF")
    val pebble = Color(0.35f, 0.33f, 0.3f, 1f)
    val leaf = Color(0.45f, 0.32f, 0.12f, 1f)
    val petalRose = Color(0.85f, 0.35f, 0.45f, 1f)
    val petalGold = Color(0.95f, 0.85f, 0.35f, 1f)
    val petalLilac = Color(0.65f, 0.4f, 0.75f, 1f)
    val petalWhite = Color(0.95f, 0.95f, 0.95f, 1f)
    val stem = Color(0.2f, 0.45f, 0.16f, 1f)
    val simGrass = Color(0.22f, 0.48f, 0.18f, 1f)
    val rock = Color(0.32f, 0.3f, 0.27f, 1f)
    val aphidFallback = Color(0.50f, 0.78f, 0.26f, 1f)
    val aphidEye = Color(0.42f, 0.09f, 0.06f, 1f)
    val aphidLeg = Color(0.36f, 0.50f, 0.20f, 1f)
    val molt = Color(0.96f, 0.95f, 0.92f, 1f)
    val honey = Color(0.95f, 0.78f, 0.28f, 1f)
    val particle = Color(0.8f, 0.8f, 0.8f, 1f)

    // The present-scene readback keeps these channels, so they are the browns
    // on the panel: dark soil, a light cut, and a darker floor.
    val nestChamber = Color(0.09f, 0.06f, 0.045f, 1f)
    val nestChamberRim = Color(0.64f, 0.49f, 0.36f, 1f)
    val nestTunnel = Color(0.13f, 0.09f, 0.065f, 1f)
    val egg3d = Color(0.92f, 0.9f, 0.82f, 1f)
    val larva3d = Color(0.85f, 0.72f, 0.32f, 1f)
    val pupa3d = Color(0.7f, 0.5f, 0.28f, 1f)
    val nestSugar = Color(0.95f, 0.85f, 0.35f, 1f)
    val nestProtein = Color(0.75f, 0.25f, 0.22f, 1f)
    val nestSoil = Color(0.22f, 0.15f, 0.11f, 1f)
    val nestSoilDeep = Color(0.18f, 0.12f, 0.09f, 1f)

    val antChitin = Color(0.07f, 0.07f, 0.075f, 1f)
    val antEye = Color(0.02f, 0.02f, 0.025f, 1f)
    val antJaw = Color(0.16f, 0.12f, 0.09f, 1f)
    val rivalChitin = Color(0.20f, 0.11f, 0.07f, 1f)
    val rivalEye = Color(0.08f, 0.04f, 0.03f, 1f)
    val rivalJaw = Color(0.32f, 0.16f, 0.10f, 1f)

    val cloverPetals = arrayOf(petalRose, petalGold, petalLilac)
    val petals = arrayOf(petalRose, petalGold, petalLilac, petalWhite)

    fun insect(type: InsectKind): Color = when (type) {
        InsectKind.PREY -> Color(0.35f, 0.7f, 0.3f, 1f)
        InsectKind.APHID -> Color(0.55f, 0.8f, 0.25f, 1f)
        InsectKind.LADYBUG -> Color(0.8f, 0.15f, 0.15f, 1f)
        InsectKind.SPIDER -> Color(0.35f, 0.35f, 0.38f, 1f)
        InsectKind.PREDATOR -> Color(0.45f, 0.25f, 0.55f, 1f)
        InsectKind.BEETLE -> Color(0.25f, 0.18f, 0.12f, 1f)
    }

    fun food2d(type: FoodKind): Color = when (type) {
        FoodKind.SUGAR -> sugar2d
        FoodKind.PROTEIN -> protein2d
        else -> otherFood2d
    }

    fun food3d(type: FoodKind): Color = when (type) {
        FoodKind.SUGAR -> sugar3d
        FoodKind.PROTEIN -> protein3d
        else -> otherFood3d
    }

    fun brood2d(stage: BroodStage): Color = when (stage) {
        BroodStage.EGG -> egg2d
        BroodStage.LARVA -> larva2d
        BroodStage.PUPA -> pupa2d
    }

    fun brood3d(stage: BroodStage): Color = when (stage) {
        BroodStage.EGG -> egg3d
        BroodStage.LARVA -> larva3d
        BroodStage.PUPA -> pupa3d
    }

    fun nightOverlay(alpha: Float): Color = Color(night.r, night.g, night.b, alpha.coerceIn(0f, 1f))

    fun emission(boost: Float): Color = Color(boost, boost, boost, 1f)

    fun groundTint(k: Float, wet: Boolean): Color =
        if (wet) Color(k * 0.62f, k * 0.7f, k * 0.78f, 1f) else Color(k, k, k, 1f)

    fun scatterGrass(shade: Float): Color = Color(0.18f, 0.42f + shade * 0.2f, 0.14f, 1f)

    fun soil(n: Float): Color = Color(0.32f + n * 0.12f, 0.22f + n * 0.06f, 0.12f + n * 0.04f, 1f)
}
