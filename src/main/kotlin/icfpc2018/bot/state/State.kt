package icfpc2018.bot.state

import icfpc2018.bot.state.LinearCoordDiff.Axis.*
import org.organicdesign.fp.collections.ImSortedSet
import org.organicdesign.fp.collections.PersistentTreeSet
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

enum class Harmonics {
    LOW, HIGH
}

data class Bot(val id: Int, val position: Point, val seeds: ImSortedSet<Int>) : Comparable<Bot> {
    override fun compareTo(other: Bot): Int = id - other.id
}

data class State(
        val energy: Long,
        val harmonics: Harmonics,
        val matrix: Model,
        val volatileModel: VolatileModel,
        val bots: PersistentTreeSet<Bot>
) {
    fun canMoveTo(p: Point) = !matrix[p] && !volatileModel[p] && bots.none { it.position == p }
}

data class Point(val x: Int, val y: Int, val z: Int) {
    companion object {
        val ZERO = Point(0, 0, 0)

        val ZERO_TO_ONE = listOf(0, 1)

        val MINUS_ONE_TO_ONE = listOf(-1, 0, 1)

        val MINUS_ONE_AND_ONE = listOf(-1, 1)
    }
}

open class CoordDiff(val dx: Int, val dy: Int, val dz: Int) {
    val mlen by lazy {
        abs(dx) + abs(dy) + abs(dz)
    }

    val clen by lazy {
        max(abs(dx), max(abs(dy), abs(dz)))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CoordDiff) return false

        if (dx != other.dx) return false
        if (dy != other.dy) return false
        if (dz != other.dz) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dx
        result = 31 * result + dy
        result = 31 * result + dz
        return result
    }

    override fun toString(): String {
        return "CoordDiff(dx=$dx, dy=$dy, dz=$dz)"
    }
}

operator fun Point.plus(cd: CoordDiff) = Point(x + cd.dx, y + cd.dy, z + cd.dz)
operator fun Point.minus(that: Point) = CoordDiff(this.x - that.x, this.y - that.y, this.z - that.z)

fun Point.immediateNeighbours() = listOf(
        Point(x + 1, y, z),
        Point(x - 1, y, z),
        Point(x, y + 1, z),
        Point(x, y - 1, z),
        Point(x, y, z + 1),
        Point(x, y, z - 1)
)

fun Point.options(dx: List<Int>, dy: List<Int>, dz: List<Int>): Set<Point> {
    val res = mutableSetOf<Point>()
    for (xx in dx) {
        for (yy in dy) {
            for (zz in dz) {
                res.add(Point(x + xx, y + yy, z + zz))
            }
        }
    }
    res.remove(this)
    return res
}

fun Point.inRange(model: Model): Boolean {
    val indices = 0 until model.size
    return x in indices &&
            y in indices &&
            z in indices
}

fun Set<Point>.inRange(model: Model) =
        filterTo(mutableSetOf()) { it.inRange(model) }

open class LinearCoordDiff(dx: Int, dy: Int, dz: Int) : CoordDiff(dx, dy, dz) {
    enum class Axis { X, Y, Z }

    lateinit var axis: Axis

    init {
        val xZero = if (dx != 0) {
            axis = X
            1
        } else 0
        val yZero = if (dy != 0) {
            axis = Y
            1
        } else 0
        val zZero = if (dz != 0) {
            axis = Z
            1
        } else 0
        assert(1 == xZero + yZero + zZero)
    }

    fun affectedCoords(origin: Point): Set<Point> {
        with(origin) {
            return when (axis) {
                X -> (0..dx).map { Point(x + it, y, z) }.toSet()
                Y -> (0..dy).map { Point(x, y + it, z) }.toSet()
                Z -> (0..dz).map { Point(x, y, z + it) }.toSet()
            }
        }
    }

    companion object {
        fun fromAxis(axis: Axis, length: Int): LinearCoordDiff {
            return when (axis) {
                Axis.X -> LinearCoordDiff(length, 0, 0)
                Axis.Y -> LinearCoordDiff(0, length, 0)
                Axis.Z -> LinearCoordDiff(0, 0, length)
            }
        }
    }
}

class LongCoordDiff(dx: Int = 0, dy: Int = 0, dz: Int = 0) : LinearCoordDiff(dx, dy, dz) {
    init {
        assert(mlen <= 15)
    }

    companion object {
        fun fromAxis(axis: Axis, length: Int): LongCoordDiff {
            return when (axis) {
                Axis.X -> LongCoordDiff(length, 0, 0)
                Axis.Y -> LongCoordDiff(0, length, 0)
                Axis.Z -> LongCoordDiff(0, 0, length)
            }
        }
    }
}

operator fun LongCoordDiff.unaryMinus() = LongCoordDiff(-dx, -dy, -dz)

class ShortCoordDiff(dx: Int = 0, dy: Int = 0, dz: Int = 0) : LinearCoordDiff(dx, dy, dz) {
    init {
        assert(mlen <= 5)
    }
}

operator fun ShortCoordDiff.unaryMinus() = ShortCoordDiff(-dx, -dy, -dz)

class NearCoordDiff(dx: Int, dy: Int, dz: Int) : CoordDiff(dx, dy, dz) {
    init {
        assert(mlen in 1..2 && clen == 1)
    }

    companion object {
        fun fromPoints(origin: Point, target: Point) =
                NearCoordDiff(target.x - origin.x, target.y - origin.y, target.z - origin.z)
    }
}

operator fun NearCoordDiff.unaryMinus() = NearCoordDiff(-dx, -dy, -dz)

class FarCoordDiff(dx: Int, dy: Int, dz: Int) : CoordDiff(dx, dy, dz) {
    init {
        assert(clen in 1..30)
    }
}

fun CoordDiff.toFarCoordDiff() = FarCoordDiff(dx, dy, dz)

typealias Region = Pair<Point, Point>

fun Region.normalize(): Region {
    val fromX = min(first.x, second.x)
    val toX = max(first.x, second.x)
    val fromY = min(first.y, second.y)
    val toY = max(first.y, second.y)
    val fromZ = min(first.z, second.z)
    val toZ = max(first.z, second.z)

    return Point(fromX, fromY, fromZ) to Point(toX, toY, toZ)
}

fun Region.coords(): List<Point> {
    val res = mutableListOf<Point>()
    val norm = normalize()
    for (x in norm.first.x..norm.second.x) {
        for (y in norm.first.y..norm.second.y) {
            for (z in norm.first.z..norm.second.z) {
                res.add(Point(x, y, z))
            }
        }
    }
    return res
}

operator fun Region.contains(p: Point): Boolean {
    val norm = normalize()
    return p.x in norm.first.x..norm.second.x &&
            p.y in norm.first.y..norm.second.y &&
            p.z in norm.first.z..norm.second.z
}

fun Region.dim(): Int {
    var res = 0
    if (first.x != second.x) res++
    if (first.y != second.y) res++
    if (first.z != second.z) res++
    return res
}
