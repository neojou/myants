package com.neojou.myants.ui

import com.neojou.myants.AppSession
import com.neojou.myants.input.NestRect
import com.neojou.myants.render3d.InspectRing
import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.render3d.RingSpace
import com.neojou.myants.render3d.WorldInput
import com.neojou.myants.render3d.WorldPalette
import com.neojou.myants.render3d.inspectRing
import com.neojou.myants.render3d.nest.NestFrame
import com.neojou.myants.render3d.rgbColor
import com.neojou.myants.session.InspectTarget
import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.Colony
import com.neojou.myants.toggleUserFollow
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.Color

private val T = MyantsTheme.colors

/** Card geometry from the #61 spec. */
internal object InspectCardSpec {
    val padding = MyantsTheme.space.s2
    val rowGap = MyantsTheme.space.gapInGroup
    val buttonGap = MyantsTheme.space.gapGroup
    /** Left accent bar width, in px as drawn by the spec. */
    const val ACCENT_PX = 3f
    /** Distance between the selection and the card, in px. */
    const val OFFSET_PX = 24f
    val labelWidth = Dp(78f)
    val valueWidth = Dp(64f)
}

/** Follow button colors, IconButton-style (#61): rest surfaceVariant, hover primaryHover, following primary + 1dp outline. */
internal data class FollowButtonVisual(val background: Color, val foreground: Color, val border: Color?)

internal fun followButtonVisual(following: Boolean, hovered: Boolean): FollowButtonVisual {
    val c = MyantsTheme.colors
    return when {
        following -> FollowButtonVisual(c.primary, c.onPrimary, c.primaryContainer)
        hovered -> FollowButtonVisual(c.surfaceVariant.mix(c.primaryHover.withAlpha(1f), c.primaryHover.a), c.onSurface, null)
        else -> FollowButtonVisual(c.surfaceVariant, c.onSurface, null)
    }
}

/** Accent bar color: the colony tint for colony things, otherwise the species / material color. */
internal fun inspectAccent(target: InspectTarget, playerColor: Color): Color {
    fun colony(c: Colony?): Color = when {
        c == null -> MyantsTheme.colors.primary
        c.id == 0 -> playerColor
        else -> rgbColor(c.workerTint)
    }
    fun antColony(a: Ant): Colony? = runCatching { a.colony }.getOrNull()
    return when (target) {
        is InspectTarget.WorldAnt -> colony(antColony(target.subject))
        is InspectTarget.NestAnt -> colony(antColony(target.subject))
        is InspectTarget.QueenTarget -> colony(target.colony)
        is InspectTarget.BroodItem -> colony(target.colony)
        is InspectTarget.ChamberItem -> colony(target.colony)
        is InspectTarget.FoodItem -> WorldPalette.food3d(target.food.type)
        is InspectTarget.InsectItem -> WorldPalette.insect(target.insect.type)
        is InspectTarget.StoneItem -> if (target.clock) WorldPalette.clockStone else WorldPalette.stone
        is InspectTarget.GrassItem -> WorldPalette.grass
        is InspectTarget.LeafItem -> WorldPalette.leaf
        is InspectTarget.PheromoneSample -> MyantsTheme.colors.primary
    }
}

/** Window pixel of the selection, or null when it is not on screen in the current view. */
internal fun inspectAnchor(
    ring: InspectRing,
    nest3d: Boolean,
    toScreen: ((Float, Float) -> Vec2f?)?,
    nest: NestRect,
): Vec2f? = when (ring.space) {
    RingSpace.WORLD -> toScreen?.invoke(ring.x.toFloat(), ring.y.toFloat())
    RingSpace.NEST -> if (nest3d && nest.fit > 0f) {
        val (x, y) = nest.toScreen(ring.x, ring.y)
        Vec2f(x, y)
    } else {
        null
    }
}

/**
 * Top-left of the card: right of the anchor, flipped to the left when it would leave the window,
 * then clamped inside. Without an anchor it docks below the HUD at [fallback].
 */
internal fun placeInspectCard(anchor: Vec2f?, cardW: Float, cardH: Float, winW: Float, winH: Float, fallback: Vec2f): Vec2f {
    if (anchor == null) return fallback
    val off = InspectCardSpec.OFFSET_PX
    var x = anchor.x + off
    if (x + cardW > winW) x = anchor.x - off - cardW
    val y = anchor.y - cardH / 2f
    return Vec2f(x.coerceIn(0f, maxOf(0f, winW - cardW)), y.coerceIn(0f, maxOf(0f, winH - cardH)))
}

/** Floating Inspector card (#61 item 1). Follows the selection's screen position each frame. */
internal fun Scene.addInspectCard(session: AppSession, hud: UiSurface): UiSurface {
    val pos = mutableStateOf(Vec2f.ZERO)
    return addPanelSurface(
    backgroundColor = { null },
    width = FitContent,
    height = FitContent,
) {
    installLanguageFont()
    val inspect = session.tools.inspectPanel.use()
    val following = session.following.use()
    val at = pos.use()
    if (inspect == null) {
        modifier.size(Dp(0f), Dp(0f))
        return@addPanelSurface
    }
    val target = session.tools.inspectTarget()
    val accent = target?.let { inspectAccent(it, session.antColor) } ?: T.primary
    modifier
        .align(AlignmentX.Start, AlignmentY.Top)
        .margin(start = Dp.fromPx(at.x), top = Dp.fromPx(at.y))
        .padding(InspectCardSpec.padding)
        .background(RoundRectBackground(T.surfaceRaised, MyantsTheme.radius.lg))
        .border(RoundRectBorder(T.outline, MyantsTheme.radius.lg, MyantsTheme.stroke.selected))
        .onPointer { it.pointer.consume() }
    Row {
        Box {
            modifier
                .width(Dp.fromPx(InspectCardSpec.ACCENT_PX))
                .height(Grow.Std)
                .margin(end = InspectCardSpec.padding)
                .background(RoundRectBackground(accent, Dp(1.5f)))
        }
        Column {
            Text(inspect.title) {
                modifier.font(sizes.normalText.derive(MyantsTheme.text.lg)).textColor(T.onSurface)
            }
            for (row in inspect.rows) {
                Row {
                    modifier.margin(top = InspectCardSpec.rowGap)
                    Text(row.label) {
                        modifier.width(InspectCardSpec.labelWidth)
                            .font(sizes.smallText.derive(MyantsTheme.text.md))
                            .textColor(T.onSurfaceVar)
                    }
                    Text(row.value) {
                        modifier.width(InspectCardSpec.valueWidth)
                            .textAlignX(AlignmentX.End)
                            .font(sizes.smallText.derive(MyantsTheme.text.md))
                            .textColor(T.onSurface)
                    }
                }
            }
            if (session.followAnchor() != null) FollowButton(following) { session.toggleUserFollow() }
        }
    }
}.also { card ->
    card.applyControlRadius()
    card.onUpdate += {
        val ring = session.tools.inspectTarget()?.let(::inspectRing)
        if (ring != null) {
            val d3 = session.renderer == RenderMode.D3
            val anchor = inspectAnchor(
                ring,
                d3 && session.nest3dActive,
                if (d3) WorldInput.groundToScreen else null,
                NestRect(NestFrame.ox, NestFrame.oy, NestFrame.fit),
            )
            val node = card.viewport.children.firstOrNull()
            val w = node?.widthPx ?: 0f
            val h = node?.heightPx ?: 0f
            val hudNode = hud.viewport.children.firstOrNull()
            val fallback = Vec2f(
                (hudNode?.leftPx ?: 0f),
                (hudNode?.let { it.topPx + it.heightPx } ?: 0f) + 8f,
            )
            val next = placeInspectCard(anchor, w, h, card.viewportWidth.value, card.viewportHeight.value, fallback)
            if (pos.value.x != next.x || pos.value.y != next.y) pos.set(next)
        }
    }
}
}

private fun UiScope.FollowButton(following: Boolean, onClick: () -> Unit) {
    Box {
        val hovered = remember { mutableStateOf(false) }
        val v = followButtonVisual(following, hovered.use())
        modifier
            .margin(top = InspectCardSpec.buttonGap)
            .padding(horizontal = MyantsTheme.space.s2, vertical = Dp(4f))
            .background(RoundRectBackground(v.background, MyantsTheme.radius.md))
            .onEnter { hovered.set(true) }
            .onExit { hovered.set(false) }
            .onClick { onClick() }
        if (v.border != null) modifier.border(RoundRectBorder(v.border, MyantsTheme.radius.md, MyantsTheme.stroke.selected))
        Text(tr(if (following) UiText.INSPECT_FOLLOWING else UiText.INSPECT_FOLLOW)) {
            modifier.font(sizes.smallText.derive(MyantsTheme.text.md)).textColor(v.foreground)
        }
    }
}
