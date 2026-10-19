package com.neojou.myants.sim

interface SpatialItem {
    val x: Double
    val y: Double
}

data class Vec2(var x: Double = 0.0, var y: Double = 0.0)

enum class Pheromone { HOME, SUGAR, PROTEIN, DANGER }

enum class AntKind { WORKER, SOLDIER, QUEEN }

enum class AntState {
    FORAGING, RETURNING, IDLE, NURSING, PATROLLING, FLEEING, ATTACKING,
    TRANSPORTING, HARVESTING, HUNGRY, MILKING, RESTING, RAIDING, DIGGING,
}

enum class CarryKind { NONE, SUGAR, PROTEIN, BROOD, CORPSE }

enum class AntPlace { WORLD, NEST }

enum class FoodKind { SUGAR, PROTEIN, CORPSE }

enum class BroodStage { EGG, LARVA, PUPA }

enum class QueenState { IDLE, LAYING, STRESSED }

enum class InsectKind { PREY, PREDATOR, APHID, SPIDER, BEETLE, LADYBUG }

enum class ChamberRole { QUEEN, BROOD, STORAGE, CEMETERY }

enum class NestNodeKind { TUNNEL, CHAMBER }

enum class EntranceSide { RIGHT, LEFT, BOTTOM, TOP }

enum class QualityLevel { ULTRA_LOW, LOW, MEDIUM, HIGH, ULTRA }

/** Float32 store: JS Float32Array assignment rounds a float64 expression. */
fun f32(value: Double): Float = value.toFloat()

fun f32mul(current: Float, factor: Double): Float = (current.toDouble() * factor).toFloat()

/** `Math.min(1, float32 + amount)` then store as float32. */
fun f32addCap1(current: Float, amount: Double): Float {
    val sum = current.toDouble() + amount
    return (if (sum < 1.0) sum else 1.0).toFloat()
}
