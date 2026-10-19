package com.neojou.myants.sim

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class Chamber(
    var x: Double,
    var y: Double,
    var radius: Double,
    var type: ChamberRole,
    var parent: Chamber? = null,
)

class NestNode(
    var x: Double,
    var y: Double,
    var radius: Double,
    var type: NestNodeKind,
)

class Nest {
    val width: Double = SimConfig.nestWidth.toDouble()
    val height: Double = SimConfig.nestHeight.toDouble()
    val nodes = ArrayList<NestNode>()
    val chambers = ArrayList<Chamber>()
    val roles = HashMap<ChamberRole, Chamber>()
    var entrances: MutableList<Vec2> = ArrayList()
    var extraChambers: Int = 0

    init {
        initNest()
    }

    fun initNest() {
        val minDim = min(width, height)
        val rScale = minDim / SimConfig.nestScaleRef
        val cx = width / 2.0
        val cy = height / 2.0
        val startR = 70.0 * rScale
        val founding = addChamber(cx, cy, startR, ChamberRole.QUEEN)
        roles[ChamberRole.QUEEN] = founding
        roles[ChamberRole.BROOD] = founding
        roles[ChamberRole.STORAGE] = founding
        roles[ChamberRole.CEMETERY] = founding
        if (width > height) {
            entrances = mutableListOf(Vec2(cx, 0.0))
            createOrganicTunnel(cx, cy, startR, cx, 0.0, 30.0 * rScale, 30.0 * rScale)
        } else {
            entrances = mutableListOf(Vec2(0.0, cy))
            createOrganicTunnel(cx, cy, startR, 0.0, cy, 30.0 * rScale, 30.0 * rScale)
        }
    }

    fun addChamber(x: Double, y: Double, radius: Double, type: ChamberRole): Chamber {
        nodes.add(NestNode(x, y, radius, NestNodeKind.CHAMBER))
        val chamber = Chamber(x, y, radius, type)
        chambers.add(chamber)
        return chamber
    }

    fun addNode(x: Double, y: Double, radius: Double, type: NestNodeKind) {
        nodes.add(NestNode(x, y, radius, type))
    }

    fun getChamber(role: ChamberRole): Chamber = roles[role]!!

    fun getChambers(role: ChamberRole): List<Chamber> = chambers.filter { it.type == role }

    fun nearestChamber(role: ChamberRole, x: Double, y: Double): Chamber? {
        var best: Chamber? = null
        var bestD = Double.POSITIVE_INFINITY
        for (c in chambers) {
            if (c.type != role) continue
            val dx = x - c.x
            val dy = y - c.y
            val d2 = dx * dx + dy * dy
            if (d2 < bestD) {
                bestD = d2
                best = c
            }
        }
        return best
    }

    private fun digChamber(): Chamber? {
        val minDim = min(width, height)
        val rScale = minDim / SimConfig.nestScaleRef
        val newR = (45.0 + Rng.rand() * 25.0) * rScale
        val gap = 15.0 * rScale
        val hub = chambers[0]
        for (attempt in 0 until 40) {
            val parent = chambers[floor(Rng.rand() * chambers.size.toDouble()).toInt()]
            val ox = parent.x - hub.x
            val oy = parent.y - hub.y
            val parentDist = hypot(ox, oy)
            val baseAngle = if (parentDist < 1.0) Rng.rand() * PI * 2.0 else atan2(oy, ox)
            val spread = if (parentDist < 1.0) PI * 2.0 else PI * 0.9
            val angle = baseAngle + (Rng.rand() - 0.5) * spread
            val d = parent.radius + newR + gap
            val nx = parent.x + cos(angle) * d
            val ny = parent.y + sin(angle) * d
            if (nx < newR + 4.0 || nx > width - newR - 4.0) continue
            if (ny < newR + 4.0 || ny > height - newR - 4.0) continue
            val childDist2 = (nx - hub.x) * (nx - hub.x) + (ny - hub.y) * (ny - hub.y)
            if (parentDist >= 1.0 && childDist2 <= parentDist * parentDist) continue
            var tooClose = false
            for (c in chambers) {
                val ddx = nx - c.x
                val ddy = ny - c.y
                val minSep = c.radius + newR * 0.4
                if (ddx * ddx + ddy * ddy < minSep * minSep) {
                    tooClose = true
                    break
                }
            }
            if (tooClose) continue
            val chamber = addChamber(nx, ny, newR, ChamberRole.STORAGE)
            chamber.parent = parent
            createOrganicTunnel(parent.x, parent.y, parent.radius, nx, ny, newR, 22.0 * rScale)
            return chamber
        }
        return null
    }

    fun growStage(): Boolean {
        val chamber = digChamber() ?: return false
        val role = when {
            extraChambers == 0 -> ChamberRole.BROOD
            extraChambers == 1 -> ChamberRole.STORAGE
            extraChambers == 2 -> ChamberRole.CEMETERY
            extraChambers % 3 == 0 -> ChamberRole.BROOD
            else -> ChamberRole.STORAGE
        }
        chamber.type = role
        if (chambers.none { it !== chamber && it.type == role }) {
            roles[role] = chamber
        }
        extraChambers++
        return true
    }

    fun createOrganicTunnel(
        x1: Double,
        y1: Double,
        r1: Double,
        x2: Double,
        y2: Double,
        r2: Double,
        tunnelRadius: Double,
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val dist = sqrt(dx * dx + dy * dy)
        val stepSize = tunnelRadius * 0.05
        val steps = ceil(dist / stepSize).toInt()
        val avgR = (r1 + r2) / 2.0
        val k = 3.0 / (avgR * avgR)
        for (i in 0..steps) {
            val t = i.toDouble() / steps.toDouble()
            val d = t * dist
            val dFromEnd = dist - d
            val excessStart = (r1 - tunnelRadius) * exp(-k * d * d)
            val excessEnd = (r2 - tunnelRadius) * exp(-k * dFromEnd * dFromEnd)
            val currentRadius = tunnelRadius + excessStart + excessEnd
            nodes.add(NestNode(x1 + dx * t, y1 + dy * t, currentRadius, NestNodeKind.TUNNEL))
        }
    }

    fun isInside(x: Double, y: Double, buffer: Double = 0.0): Boolean {
        for (node in nodes) {
            val dx = x - node.x
            val dy = y - node.y
            val r = node.radius - buffer
            if (dx * dx + dy * dy < r * r) return true
        }
        return false
    }

    fun getEntrance(): Vec2 = entrances[0]

    fun getNearestNode(x: Double, y: Double): NestNode? {
        var nearest: NestNode? = null
        var minDist = Double.POSITIVE_INFINITY
        for (node in nodes) {
            val dx = x - node.x
            val dy = y - node.y
            val d2 = dx * dx + dy * dy
            if (d2 < minDist) {
                minDist = d2
                nearest = node
            }
        }
        return nearest
    }

    fun lineClear(x1: Double, y1: Double, x2: Double, y2: Double, buffer: Double = 3.0): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val dist = hypot(dx, dy)
        val n = min(16, maxOf(1, ceil(dist / 10.0).toInt()))
        for (i in 1 until n) {
            val t = i.toDouble() / n.toDouble()
            if (!isInside(x1 + dx * t, y1 + dy * t, buffer)) return false
        }
        return true
    }

    fun chamberAt(x: Double, y: Double): Chamber? {
        var best: Chamber? = null
        var bestD = Double.POSITIVE_INFINITY
        for (c in chambers) {
            val d2 = (x - c.x) * (x - c.x) + (y - c.y) * (y - c.y)
            if (d2 < c.radius * c.radius && d2 < bestD) {
                bestD = d2
                best = c
            }
        }
        return best
    }

    fun nearestChamberAny(x: Double, y: Double): Chamber? {
        var best: Chamber? = null
        var bestD = Double.POSITIVE_INFINITY
        for (c in chambers) {
            val d2 = (x - c.x) * (x - c.x) + (y - c.y) * (y - c.y)
            if (d2 < bestD) {
                bestD = d2
                best = c
            }
        }
        return best
    }

    private fun chamberHops(a: Chamber, b: Chamber): Int {
        if (a === b) return 0
        val anc = HashMap<Chamber, Int>()
        var n: Chamber? = a
        var d = 0
        while (n != null) {
            anc[n] = d
            n = n.parent
            d++
        }
        var m: Chamber? = b
        var e = 0
        while (m != null) {
            val da = anc[m]
            if (da != null) return da + e
            m = m.parent
            e++
        }
        return d + e
    }

    private fun nextChamberToward(from: Chamber, to: Chamber): Chamber? {
        if (from === to) return null
        val toChain = HashSet<Chamber>()
        var c: Chamber? = to
        while (c != null) {
            toChain.add(c)
            c = c.parent
        }
        if (toChain.contains(from)) {
            var step = to
            while (step.parent != null && step.parent !== from) {
                step = step.parent!!
            }
            return step
        }
        return from.parent
    }

    fun nestWaypoint(x: Double, y: Double, targetX: Double, targetY: Double): Vec2? {
        val clear = SimConfig.nestRouteClearance
        if (lineClear(x, y, targetX, targetY, clear)) return Vec2(targetX, targetY)
        val targetCh = chamberAt(targetX, targetY) ?: nearestChamberAny(targetX, targetY) ?: return null
        var bestFrom: Chamber? = null
        var bestHops = Int.MAX_VALUE
        var bestDist = Double.POSITIVE_INFINITY
        for (ch in chambers) {
            if (!lineClear(x, y, ch.x, ch.y, clear)) continue
            val hops = chamberHops(ch, targetCh)
            val dist = (x - ch.x) * (x - ch.x) + (y - ch.y) * (y - ch.y)
            if (hops < bestHops || (hops == bestHops && dist < bestDist)) {
                bestHops = hops
                bestDist = dist
                bestFrom = ch
            }
        }
        val from = bestFrom ?: return nearestChamberAny(x, y)?.let { Vec2(it.x, it.y) }
        if (from === targetCh) return Vec2(from.x, from.y)
        val next = nextChamberToward(from, targetCh)
        return if (next != null) Vec2(next.x, next.y) else Vec2(targetCh.x, targetCh.y)
    }

    fun getNextNodeTowards(x: Double, y: Double, targetX: Double, targetY: Double): Vec2? {
        return nestWaypoint(x, y, targetX, targetY) ?: getNearestNode(x, y)?.let { Vec2(it.x, it.y) }
    }
}
