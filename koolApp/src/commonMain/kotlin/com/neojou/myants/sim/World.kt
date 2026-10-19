package com.neojou.myants.sim

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Queen.layTimer is a Double and uses `--`, which Kotlin only defines for integers. */
operator fun Double.inc(): Double = this + 1.0
operator fun Double.dec(): Double = this - 1.0

enum class ParticleKind { DEFAULT, BLOOD, DUST }

class Particle(
    var x: Double,
    var y: Double,
    var vx: Double,
    var vy: Double,
    var life: Double,
    var color: String,
    var type: ParticleKind,
)

class NestParticle(
    var x: Double,
    var y: Double,
    var vx: Double,
    var vy: Double,
    var life: Double,
    var color: String? = null,
)

class GrassTuft(val x: Double, val y: Double, val size: Double, val angle: Double)

class World {
    val terrain: Terrain
    val insects = ArrayList<Insect>()
    val foods = ArrayList<Food>()
    val graveyard = ArrayList<Food>()
    val spatialGrid: SpatialGrid<Ant>
    val foodGrid: SpatialGrid<Food>
    val insectGrid: SpatialGrid<Insect>
    val colonies = ArrayList<Colony>()

    val queen: Queen get() = colonies[0].queen
    val nest: Nest get() = colonies[0].nest
    var nestGrid: PheromoneGrid
        get() = colonies[0].nestGrid
        set(value) { colonies[0].nestGrid = value }
    var grid: PheromoneGrid
        get() = colonies[0].outdoorField
        set(value) { colonies[0].outdoorField = value }
    val ants: MutableList<Ant> get() = colonies[0].ants
    val brood: MutableList<Brood> get() = colonies[0].brood
    val eggs: Int get() = colonies[0].eggs
    val larvae: Int get() = colonies[0].larvae
    val pupae: Int get() = colonies[0].pupae
    var sugarStockpile: Double
        get() = colonies[0].sugarStockpile
        set(value) { colonies[0].sugarStockpile = value }
    var proteinStockpile: Double
        get() = colonies[0].proteinStockpile
        set(value) { colonies[0].proteinStockpile = value }
    var trophallaxisCount: Int
        get() = colonies[0].trophallaxisCount
        set(value) { colonies[0].trophallaxisCount = value }

    fun storageCapacity(): Double = colonies[0].storageCapacity()

    var age: Int = 0
    var timeOfDay: Double = 0.0
    var dayLength: Int = 6000
    var raining: Boolean = false
    var rainTimer: Int = 0

    val particles = ArrayList<Particle>()
    val nestParticles = ArrayList<NestParticle>()
    val grass = ArrayList<GrassTuft>()
    val observer: SimObserver = SimObserver()

    init {
        terrain = Terrain()
        val nest0 = Nest()
        val nestGrid0 = PheromoneGrid(SimConfig.nestWidth, SimConfig.nestHeight)
        spatialGrid = SpatialGrid(SimConfig.width, SimConfig.height, 50.0)
        foodGrid = SpatialGrid(SimConfig.width, SimConfig.height, 100.0)
        insectGrid = SpatialGrid(SimConfig.width, SimConfig.height, 100.0)
        val ls = nest0.height > nest0.width
        val queen0 = Queen()
        val queenChamber = nest0.getChamber(ChamberRole.QUEEN)
        queen0.x = queenChamber.x
        queen0.y = queenChamber.y
        colonies.add(Colony(0, nest0, queen0, nestGrid0, if (ls) EntranceSide.RIGHT else EntranceSide.BOTTOM))
        initContent()
        if (SimConfig.colonyCount > 1) {
            val nest1 = Nest()
            val nestGrid1 = PheromoneGrid(SimConfig.nestWidth, SimConfig.nestHeight)
            val queen1 = Queen()
            val qc1 = nest1.getChamber(ChamberRole.QUEEN)
            queen1.x = qc1.x
            queen1.y = qc1.y
            val colony1 = Colony(1, nest1, queen1, nestGrid1, if (ls) EntranceSide.LEFT else EntranceSide.TOP)
            colonies.add(colony1)
            populateColony(colony1)
        }
    }

    fun rebuildPheromoneGrids() {
        for (c in colonies) {
            c.outdoorField = PheromoneGrid(SimConfig.width, SimConfig.height)
            c.nestGrid = PheromoneGrid(SimConfig.nestWidth, SimConfig.nestHeight)
        }
    }

    private fun initContent() {
        val width = SimConfig.width.toDouble()
        val height = SimConfig.height.toDouble()
        for (i in 0 until 200) {
            grass.add(GrassTuft(
                Rng.rand() * width,
                Rng.rand() * height,
                2.0 + Rng.rand() * 3.0,
                Rng.rand() * PI * 2.0,
            ))
        }
        populateColony(colonies[0])
        var s = 0.0
        while (s < SimConfig.sugarSourceCount) {
            spawnFood(FoodKind.SUGAR)
            s += 1.0
        }
    }

    fun populateColony(c: Colony) {
        for (i in 0 until SimConfig.initialWorkers) {
            c.spawnAnt(AntKind.WORKER)
            val ant = c.ants[c.ants.size - 1]
            ant.energy = SimConfig.antMaxEnergy * (0.5 + Rng.rand() * 0.5)
        }
        c.spawnAnt(AntKind.SOLDIER)
        val broodChamber = c.nest.getChamber(ChamberRole.BROOD)
        fun spawnBrood(stage: BroodStage, count: Int) {
            for (i in 0 until count) {
                val b = Brood(
                    broodChamber.x + (Rng.rand() - 0.5) * 40.0,
                    broodChamber.y + (Rng.rand() - 0.5) * 40.0,
                )
                b.stage = stage
                b.age = when (stage) {
                    BroodStage.EGG -> Rng.rand() * SimConfig.brood.eggDuration.toDouble()
                    BroodStage.LARVA -> Rng.rand() * SimConfig.brood.larvaDuration.toDouble()
                    BroodStage.PUPA -> Rng.rand() * SimConfig.brood.pupaDuration.toDouble()
                }
                c.brood.add(b)
            }
        }
        spawnBrood(BroodStage.PUPA, 5)
        spawnBrood(BroodStage.LARVA, 8)
        spawnBrood(BroodStage.EGG, 10)
    }

    fun spawnAnt(type: AntKind) {
        colonies[0].spawnAnt(type)
    }

    fun addParticle(x: Double, y: Double, color: String, type: ParticleKind = ParticleKind.DEFAULT) {
        val angle = Rng.rand() * PI * 2.0
        val speed = if (type == ParticleKind.BLOOD) Rng.rand() * 0.5 else Rng.rand() * 2.0
        particles.add(Particle(
            x, y,
            cos(angle) * speed,
            sin(angle) * speed,
            if (type == ParticleKind.BLOOD) 2.0 else 1.0,
            color,
            type,
        ))
    }

    fun spawnFood(type: FoodKind) {
        var x = 0.0
        var y = 0.0
        var valid = false
        val width = SimConfig.width.toDouble()
        val height = SimConfig.height.toDouble()
        while (!valid) {
            x = Rng.rand() * width
            y = Rng.rand() * height
            val isLandscape = SimConfig.width > SimConfig.height
            val entranceX = if (isLandscape) width else width / 2.0
            val entranceY = if (isLandscape) height / 2.0 else height
            val maxDim = max(width, height)
            val minDistance = maxDim * 0.25
            val dx = x - entranceX
            val dy = y - entranceY
            if (dx * dx + dy * dy >= minDistance * minDistance && !terrain.isBlocked(x, y, 45.0)) valid = true
        }
        var amount = 1000.0
        if (type == FoodKind.SUGAR) amount = 100.0 + floor(Rng.rand() * 200.0)
        foods.add(Food(x, y, type, amount))
    }

    fun placeFood(x: Double, y: Double, type: FoodKind) {
        if (!terrain.isBlocked(x, y)) {
            val amount = if (type == FoodKind.SUGAR) 200.0 else 150.0
            foods.add(Food(x, y, type, amount))
        }
    }

    fun spawnEnemyAt(x: Double, y: Double, type: InsectKind = InsectKind.PREDATOR) {
        if (!terrain.isBlocked(x, y)) insects.add(Insect(x, y, type))
    }

    fun getSafePosition(): Vec2 {
        var x = 0.0
        var y = 0.0
        var valid = false
        var attempts = 0
        val width = SimConfig.width.toDouble()
        val height = SimConfig.height.toDouble()
        while (!valid && attempts < 20) {
            x = Rng.rand() * width
            y = Rng.rand() * height
            if (!terrain.isBlocked(x, y) && x > 10.0 && x < width - 10.0 && y > 10.0 && y < height - 10.0) {
                valid = true
            }
            attempts++
        }
        return Vec2(x, y)
    }

    fun dayBrightness(): Double {
        val t = timeOfDay
        if (t >= 0.2 && t <= 0.7) return 1.0
        if (t < 0.2) return t / 0.2
        if (t < 0.8) return 1.0 - (t - 0.7) / 0.1
        return 0.0
    }

    fun activityFactor(): Double {
        val minNight = SimConfig.environment.nightActivityMin
        return minNight + (1.0 - minNight) * dayBrightness()
    }

    fun totalAntCount(): Int {
        var n = 0
        for (c in colonies) n += c.ants.size
        return n
    }

    fun update() {
        age++
        timeOfDay = (age % dayLength).toDouble() / dayLength.toDouble()
        val env = SimConfig.environment
        if (raining) {
            rainTimer--
            if (rainTimer <= 0) raining = false
            else for (c in colonies) c.outdoorField.scaleAll(env.rainWashout)
        } else if (Rng.rand() < env.rainChance) {
            raining = true
            rainTimer = env.rainMinDuration + floor(Rng.rand() * (env.rainMaxDuration - env.rainMinDuration).toDouble()).toInt()
        }
        if (age % SimConfig.pheromone.updateSkip == 0) {
            for (c in colonies) {
                c.outdoorField.update()
                c.nestGrid.update()
            }
        }
        for (c in colonies) c.queen.update(this)
        spatialGrid.clear()
        for (c in colonies) for (ant in c.ants) spatialGrid.add(ant)
        foodGrid.clear()
        for (f in foods) foodGrid.add(f)
        insectGrid.clear()
        for (ins in insects) insectGrid.add(ins)
        for (c in colonies) updateBrood(c)
        var pi = particles.size - 1
        while (pi >= 0) {
            val p = particles[pi]
            if (p.type == ParticleKind.BLOOD) {
                p.x += p.vx
                p.y += p.vy
                p.vx *= 0.9
                p.vy *= 0.9
                p.life -= 0.05
            } else {
                p.x += p.vx
                p.y += p.vy
                p.life -= 0.05
            }
            if (p.life <= 0.0) particles.removeAt(pi)
            pi--
        }
        var ni = nestParticles.size - 1
        while (ni >= 0) {
            val p = nestParticles[ni]
            p.x += p.vx
            p.y += p.vy
            p.vx *= 0.92
            p.vy *= 0.92
            p.life -= 0.02
            if (p.life <= 0.0) nestParticles.removeAt(ni)
            ni--
        }
        for (c in colonies) maybeLaunchRaid(c)
        for (c in colonies) updateAnts(c)

        val antCount = totalAntCount()
        val currentMaxPredators = SimConfig.maxPredators + floor(antCount / 50.0).toInt()
        val currentMaxSpiders = SimConfig.maxSpiders + floor(antCount / 100.0).toInt()
        val currentMaxBeetles = SimConfig.maxBeetles + floor(antCount / 150.0).toInt()
        val currentMaxLadybugs = SimConfig.maxLadybugs + floor(antCount / 40.0).toInt()

        if (countInsects(InsectKind.PREY) < SimConfig.maxPrey) {
            if (Rng.rand() < SimConfig.preySpawnRate) {
                val pos = getSafePosition()
                insects.add(Insect(pos.x, pos.y, InsectKind.PREY))
            }
        }
        if (age > SimConfig.gracePeriod && countInsects(InsectKind.PREDATOR) < currentMaxPredators) {
            if (Rng.rand() < SimConfig.predatorSpawnRate) {
                val pos = getSafePosition()
                insects.add(Insect(pos.x, pos.y, InsectKind.PREDATOR))
            }
        }
        if (age > SimConfig.gracePeriod && countInsects(InsectKind.SPIDER) < currentMaxSpiders) {
            if (Rng.rand() < SimConfig.spiderSpawnRate) {
                val pos = getSafePosition()
                insects.add(Insect(pos.x, pos.y, InsectKind.SPIDER))
            }
        }
        if (age > SimConfig.gracePeriod && countInsects(InsectKind.BEETLE) < currentMaxBeetles) {
            if (Rng.rand() < SimConfig.beetleSpawnRate) {
                val pos = getSafePosition()
                insects.add(Insect(pos.x, pos.y, InsectKind.BEETLE))
            }
        }
        if (age > SimConfig.gracePeriod && countInsects(InsectKind.LADYBUG) < currentMaxLadybugs) {
            if (Rng.rand() < SimConfig.ladybugSpawnRate) {
                val pos = getSafePosition()
                insects.add(Insect(pos.x, pos.y, InsectKind.LADYBUG))
            }
        }
        val aphids = insects.filter { it.type == InsectKind.APHID }
        if (aphids.size < 8) {
            if (Rng.rand() < 0.003) {
                val x: Double
                val y: Double
                if (aphids.isNotEmpty()) {
                    var mx = 0.0
                    var my = 0.0
                    for (a in aphids) {
                        mx += a.x
                        my += a.y
                    }
                    mx /= aphids.size.toDouble()
                    my /= aphids.size.toDouble()
                    val width = SimConfig.width.toDouble()
                    val height = SimConfig.height.toDouble()
                    x = max(20.0, min(width - 20.0, mx + (Rng.rand() - 0.5) * 50.0))
                    y = max(20.0, min(height - 20.0, my + (Rng.rand() - 0.5) * 50.0))
                } else {
                    val pos = getSafePosition()
                    x = pos.x
                    y = pos.y
                }
                insects.add(Insect(x, y, InsectKind.APHID))
            }
        }

        var ii = insects.size - 1
        while (ii >= 0) {
            val insect = insects[ii]
            insect.update(this)
            if (insect.health <= 0.0) {
                var dropX = insect.x
                var dropY = insect.y
                if (terrain.isBlocked(dropX, dropY)) {
                    for (r in 10..50 step 10) {
                        var found = false
                        var a = 0.0
                        while (a < PI * 2.0) {
                            val tx = dropX + cos(a) * r.toDouble()
                            val ty = dropY + sin(a) * r.toDouble()
                            if (!terrain.isBlocked(tx, ty)) {
                                dropX = tx
                                dropY = ty
                                found = true
                                break
                            }
                            a += PI / 4.0
                        }
                        if (found) break
                    }
                }
                if (!terrain.isBlocked(dropX, dropY)) {
                    val isLandscape = SimConfig.width > SimConfig.height
                    val width = SimConfig.width.toDouble()
                    val height = SimConfig.height.toDouble()
                    var nearEntrance = false
                    if (isLandscape) {
                        if (dropX > width - 100.0 && abs(dropY - height / 2.0) < 100.0) nearEntrance = true
                    } else {
                        if (dropY > height - 100.0 && abs(dropX - width / 2.0) < 100.0) nearEntrance = true
                    }
                    if (!nearEntrance) {
                        val food = Food(dropX, dropY, FoodKind.CORPSE, 10.0)
                        food.corpseType = insect.type.name
                        food.corpseAngle = insect.angle
                        foods.add(food)
                    }
                }
                insects.removeAt(ii)
            }
            ii--
        }

        observer.observe(this)

        var fi = foods.size - 1
        while (fi >= 0) {
            foods[fi].update()
            if (foods[fi].amount <= 0.0) foods.removeAt(fi)
            fi--
        }
        var gi = graveyard.size - 1
        while (gi >= 0) {
            graveyard[gi].update()
            if (graveyard[gi].amount <= 0.0) graveyard.removeAt(gi)
            gi--
        }
        var sugarSources = 0
        for (f in foods) if (f.type == FoodKind.SUGAR) sugarSources++
        if (sugarSources.toDouble() < SimConfig.sugarSourceCount) {
            if (Rng.rand() < 0.05) spawnFood(FoodKind.SUGAR)
        }

        for (c in colonies) {
            if (c.sugarStockpile > 0.0) {
                c.sugarStockpile = max(0.0, c.sugarStockpile - SimConfig.colonyUpkeep * c.ants.size.toDouble())
            }
            if (c.proteinStockpile > 0.0) {
                c.proteinStockpile = max(0.0, c.proteinStockpile - SimConfig.broodProteinUpkeep * c.larvae.toDouble())
            }
            val targetExtra = min(
                SimConfig.nest.maxExtraChambers,
                floor(c.ants.size.toDouble() / SimConfig.nest.excavateEvery.toDouble()).toInt(),
            )
            if (c.nest.extraChambers < targetExtra) {
                if (c.digSite == null) {
                    val face = c.nest.chambers[c.nest.chambers.size - 1]
                    c.digSite = Vec2(face.x, face.y)
                }
                if (c.digProgress.toDouble() >= SimConfig.nest.digWorkPerChamber) {
                    c.digProgress = 0
                    c.digSite = null
                    if (c.nest.growStage()) spawnNestDust(c.nest)
                }
            } else {
                c.digSite = null
                c.digProgress = 0
            }
        }
    }

    private fun countInsects(kind: InsectKind): Int {
        var n = 0
        for (ins in insects) if (ins.type == kind) n++
        return n
    }

    private fun maybeLaunchRaid(c: Colony) {
        if (colonies.size < 2) return
        if (c.raidCooldown > 0) {
            c.raidCooldown--
            return
        }
        val enemy = colonies[if (c.id == 0) 1 else 0]
        if (enemy.ants.isEmpty()) return
        if (c.ants.size.toDouble() <= enemy.ants.size.toDouble() * SimConfig.combat.raidPopEdge) return
        if (c.ants.size.toDouble() > enemy.ants.size.toDouble() * SimConfig.combat.raidMercyRatio) return
        val available = c.ants.filter { a ->
            a.type == AntKind.SOLDIER && a.location == AntPlace.WORLD && a.state == AntState.PATROLLING && a.health > 0.0
        }
        if (available.size < SimConfig.combat.raidMinSoldiers) return
        val party = available.take(SimConfig.combat.raidPartySize)
        for (a in party) {
            a.state = AntState.RAIDING
            a.raidTarget = Vec2(enemy.entranceWorld.x, enemy.entranceWorld.y)
            a.patrolTarget = null
        }
        c.raidCooldown = SimConfig.combat.raidCooldown
    }

    private fun updateBrood(c: Colony) {
        val proteinRich = c.proteinStockpile > SimConfig.brood.soldierProteinLevel
        var i = c.brood.size - 1
        while (i >= 0) {
            val b = c.brood[i]
            if (proteinRich) b.provision()
            val readyToHatch = b.update()
            if (readyToHatch) {
                if (c.ants.size < PerformanceManager.settings.maxAnts) {
                    var workers = 0
                    for (a in c.ants) if (a.type == AntKind.WORKER) workers++
                    var caste = b.destinedCaste ?: AntKind.WORKER
                    if (caste == AntKind.SOLDIER) {
                        val soldiers = c.ants.size - workers
                        val tooMany = soldiers.toDouble() >= c.ants.size.toDouble() * SimConfig.brood.maxSoldierFraction
                        if (workers <= SimConfig.soldierUnlockThreshold || tooMany) caste = AntKind.WORKER
                    }
                    c.spawnAnt(caste)
                }
                c.brood.removeAt(i)
            } else if (b.stage == BroodStage.LARVA && b.hunger > 100.0) {
                c.brood.removeAt(i)
            }
            i--
        }
    }

    private fun updateAnts(c: Colony) {
        var i = c.ants.size - 1
        while (i >= 0) {
            val ant = if (i < c.ants.size) c.ants[i] else null
            if (ant != null) {
                ant.update(this)
                if (ant.health <= 0.0) {
                    if (ant.location == AntPlace.WORLD) {
                        val corpse = Food(ant.x, ant.y, FoodKind.CORPSE, 100.0)
                        corpse.corpseType = "ANT"
                        corpse.corpseAngle = ant.angle
                        foods.add(corpse)
                        foodGrid.add(corpse)
                    }
                    if (i < c.ants.size) c.ants.removeAt(i)
                }
            }
            i--
        }
    }

    fun spawnNestDust(nest: Nest = this.nest) {
        if (nest.chambers.isEmpty()) return
        val c = nest.chambers[nest.chambers.size - 1]
        for (i in 0 until 14) {
            val a = Rng.rand() * PI * 2.0
            val sp = Rng.rand() * 1.5
            nestParticles.add(NestParticle(
                c.x + (Rng.rand() - 0.5) * c.radius,
                c.y + (Rng.rand() - 0.5) * c.radius,
                cos(a) * sp,
                sin(a) * sp,
                1.0,
                null,
            ))
        }
    }
}
