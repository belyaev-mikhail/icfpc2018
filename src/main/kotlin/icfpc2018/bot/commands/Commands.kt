package icfpc2018.bot.commands

import icfpc2018.bot.state.*
import icfpc2018.bot.state.Harmonics.HIGH
import icfpc2018.bot.state.Harmonics.LOW
import icfpc2018.bot.util.minus
import icfpc2018.bot.util.plus
import org.apache.commons.compress.utils.BitInputStream
import org.organicdesign.fp.collections.PersistentTreeSet
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteOrder
import java.util.*

fun Int.toBinary(size: Int) = ("0".repeat(32) + toString(2)).takeLast(size)
fun Long.toBinary(size: Int = 8) = ("0".repeat(64) + toString(2)).takeLast(size)

inline fun sbb(number: Int, from: Int, to: Int) = (((1 shl (to - from + 1)) - 1) and (number ushr from))
inline fun Int.subBits(range: IntRange) = sbb(this, 7 - range.endInclusive, 7 - range.start)
inline fun Long.subBits(range: IntRange) = toInt().subBits(range)

inline fun Int.endsWithBits(bits: Int) = this and bits == bits
inline fun Long.endsWithBits(bits: Int) = this.toInt() and bits == bits

interface Command {
    fun write(stream: OutputStream) {
        when (this) {
            is Halt -> stream.write(0b11111111)
            is Wait -> stream.write(0b11111110)
            is Flip -> stream.write(0b11111101)
            is SMove -> {
                val llda = when {
                    lld.dx != 0 -> 1
                    lld.dy != 0 -> 2
                    lld.dz != 0 -> 3
                    else -> throw IllegalArgumentException()
                }
                val lldi = lld.dx + lld.dy + lld.dz + 15

                stream.write("00${llda.toBinary(2)}0100".toInt(2))
                stream.write("000${lldi.toBinary(5)}".toInt(2))
            }
            is LMove -> {
                val sld2a = when {
                    sld2.dx != 0 -> 1
                    sld2.dy != 0 -> 2
                    sld2.dz != 0 -> 2
                    else -> throw IllegalArgumentException()
                }
                val sld2i = sld2.dx + sld2.dy + sld2.dz + 5

                val sld1a = when {
                    sld1.dx != 0 -> 1
                    sld1.dy != 0 -> 2
                    sld1.dz != 0 -> 2
                    else -> throw IllegalArgumentException()
                }
                val sld1i = sld1.dx + sld1.dy + sld1.dz + 5

                stream.write("${sld2a.toBinary(2)}${sld1a.toBinary(2)}1100".toInt(2))
                stream.write("${sld2i.toBinary(4)}${sld1i.toBinary(4)}".toInt(2))
            }
            is FusionP -> {
                val nd = (nd.dx + 1) * 9 + (nd.dy + 1) * 3 + nd.dz + 1
                stream.write("${nd.toBinary(5)}111".toInt(2))
            }
            is FusionS -> {
                val nd = (nd.dx + 1) * 9 + (nd.dy + 1) * 3 + nd.dz + 1
                stream.write("${nd.toBinary(5)}110".toInt(2))
            }
            is Fission -> {
                val nd = (nd.dx + 1) * 9 + (nd.dy + 1) * 3 + nd.dz + 1
                stream.write("${nd.toBinary(5)}101".toInt(2))
                stream.write(m)
            }
            is Fill -> {
                val nd = (nd.dx + 1) * 9 + (nd.dy + 1) * 3 + nd.dz + 1
                stream.write("${nd.toBinary(5)}011".toInt(2))
            }
        }
    }

    companion object {
        fun read(stream: InputStream): Command {
            val bits = BitInputStream(stream, ByteOrder.LITTLE_ENDIAN)

            val firstByte = bits.readBits(8)

            return when (firstByte) {
                0b11111111L -> Halt
                0b11111110L -> Wait
                0b11111101L -> Flip
                else -> {
                    when {
                        firstByte.endsWithBits(0b0100) -> {
                            val llda = firstByte.subBits(2..3)
                            val secondByteEnc = bits.readBits(8)
                            val lldi = secondByteEnc.subBits(3..7)
                            val ldiff = when (llda) {
                                1 -> LongCoordDiff(dx = lldi - 15)
                                2 -> LongCoordDiff(dy = lldi - 15)
                                3 -> LongCoordDiff(dz = lldi - 15)
                                else -> throw IllegalStateException()
                            }
                            SMove(ldiff)
                        }
                        firstByte.endsWithBits(0b1100) -> {
                            val sld2a = firstByte.subBits(0..1)
                            val sld1a = firstByte.subBits(2..3)
                            val secondByteEnc = bits.readBits(8)
                            val sld2i = secondByteEnc.subBits(0..3)
                            val sld1i = secondByteEnc.subBits(4..7)

                            val sdiff1 = when (sld1a) {
                                1 -> ShortCoordDiff(dx = sld1i - 5)
                                2 -> ShortCoordDiff(dy = sld1i - 5)
                                3 -> ShortCoordDiff(dz = sld1i - 5)
                                else -> throw IllegalStateException()
                            }
                            val sdiff2 = when (sld2a) {
                                1 -> ShortCoordDiff(dx = sld2i - 5)
                                2 -> ShortCoordDiff(dy = sld2i - 5)
                                3 -> ShortCoordDiff(dz = sld2i - 5)
                                else -> throw IllegalStateException()
                            }
                            LMove(sdiff1, sdiff2)
                        }

                        firstByte.endsWithBits(0b111) -> {
                            val nd = firstByte.subBits(0..4)
                            val dz = nd % 3 - 1
                            val dy = (nd / 3) % 3 - 1
                            val dx = nd / 9 - 1
                            FusionP(NearCoordDiff(dx, dy, dz))
                        }

                        firstByte.endsWithBits(0b110) -> {
                            val nd = firstByte.subBits(0..4)
                            val dz = nd % 3 - 1
                            val dy = (nd / 3) % 3 - 1
                            val dx = nd / 9 - 1
                            FusionS(NearCoordDiff(dx, dy, dz))
                        }

                        firstByte.endsWithBits(0b101) -> {
                            val nd = firstByte.subBits(0..4)
                            val dz = nd % 3 - 1
                            val dy = (nd / 3) % 3 - 1
                            val dx = nd / 9 - 1

                            val secondByte = bits.readBits(8)

                            Fission(NearCoordDiff(dx, dy, dz), secondByte.toInt())
                        }

                        firstByte.endsWithBits(0b011) -> {
                            val nd = firstByte.subBits(0..4)
                            val dz = nd % 3 - 1
                            val dy = (nd / 3) % 3 - 1
                            val dx = nd / 9 - 1
                            Fill(NearCoordDiff(dx, dy, dz))
                        }

                        else -> throw IllegalStateException("Cannot read byte: ${firstByte.toBinary()}")
                    }
                }
            }

        }
    }
}

interface SimpleCommand : Command {
    fun apply(bot: Bot, state: State): State = state

    fun volatileCoords(bot: Bot) = listOf(bot.position)

    fun check(bot: Bot, state: State) = true
}

interface GroupCommand {
    fun apply(bots: List<Bot>, state: State): State = state

    fun volatileCoords(bots: List<Bot>) = bots.map { it.position }

    fun check(bots: List<Bot>, state: State) = true

    val innerCommands: List<SimpleCommand>
}

object Halt : SimpleCommand {
    override fun toString(): String {
        return "Halt"
    }

    override fun apply(bot: Bot, state: State): State =
            state.copy(bots = PersistentTreeSet.empty())

    override fun check(bot: Bot, state: State): Boolean {
        return when {
            bot.position != Point.ZERO -> false
            state.bots != sortedSetOf(bot) -> false
            state.harmonics != LOW -> false
            else -> true
        }
    }
}

object Wait : SimpleCommand {
    override fun toString(): String {
        return "Wait"
    }
}

object Flip : SimpleCommand {
    override fun toString(): String {
        return "Flip"
    }

    override fun apply(bot: Bot, state: State): State {
        return state.copy(harmonics = when (state.harmonics) {
            LOW -> HIGH
            HIGH -> LOW
        })
    }
}

data class SMove(val lld: LongCoordDiff) : SimpleCommand {
    override fun apply(bot: Bot, state: State) =
            state.copy(
                    energy = state.energy + 2 * lld.mlen,
                    bots = state.bots - bot + bot.copy(position = bot.position + lld)
            )

    override fun volatileCoords(bot: Bot): List<Point> =
            lld.affectedCoords(bot.position)

    override fun check(bot: Bot, state: State): Boolean {
        val newPos = bot.position + lld
        if (!newPos.inRange(state.matrix)) return false
        for (ac in volatileCoords(bot)) {
            if (state.matrix[ac]) return false
        }
        return true
    }
}

data class LMove(val sld1: ShortCoordDiff, val sld2: ShortCoordDiff) : SimpleCommand {
    override fun apply(bot: Bot, state: State) =
            state.copy(
                    energy = state.energy + 2 * (sld1.mlen + 2 + sld2.mlen),
                    bots = state.bots - bot + bot.copy(position = bot.position + sld1 + sld2)
            )

    override fun volatileCoords(bot: Bot): List<Point> =
            sld1.affectedCoords(bot.position) + sld2.affectedCoords(bot.position + sld1)

    override fun check(bot: Bot, state: State): Boolean {
        val newPos1 = bot.position + sld1
        if (!newPos1.inRange(state.matrix)) return false
        val newPos2 = newPos1 + sld2
        if (!newPos2.inRange(state.matrix)) return false
        for (ac in volatileCoords(bot)) {
            if (state.matrix[ac]) return false
        }
        return true
    }
}

data class Fission(val nd: NearCoordDiff, val m: Int) : SimpleCommand {
    override fun apply(bot: Bot, state: State): State {
        val bids = bot.seeds.withIndex().groupBy { (idx, _) ->
            when (idx) {
                0 -> 0
                in 1..m -> 1
                else -> 2
            }
        }
        return state.copy(
                energy = state.energy + 24,
                bots = state.bots - bot
                        + bot.copy(seeds = TreeSet(bids.getOrDefault(2, emptyList()).map { it.value }))
                        + Bot(
                        id = bot.seeds.first(),
                        position = bot.position + nd,
                        seeds = TreeSet(bids.getOrDefault(1, emptyList()).map { it.value }))
        )
    }

    override fun volatileCoords(bot: Bot): List<Point> =
            listOf(bot.position, bot.position + nd)

    override fun check(bot: Bot, state: State): Boolean {
        if (bot.seeds.isEmpty()) return false
        val newPos = bot.position + nd
        if (!newPos.inRange(state.matrix)) return false
        if (state.matrix[newPos]) return false
        if (bot.seeds.size < m + 1) return false
        return true
    }
}

data class Fill(val nd: NearCoordDiff) : SimpleCommand {
    override fun apply(bot: Bot, state: State): State {
        val newPos = bot.position + nd
        val (newEnergy, shouldUpdate) = if (state.matrix[newPos]) {
            state.energy + 6 to false
        } else {
            state.energy + 12 to true
        }
        return state.copy(
                energy = newEnergy,
                matrix = if (shouldUpdate) state.matrix.set(newPos) else state.matrix
        )
    }

    override fun volatileCoords(bot: Bot): List<Point> =
            listOf(bot.position, bot.position + nd)

    override fun check(bot: Bot, state: State): Boolean {
        val newPos = bot.position + nd
        if (!newPos.inRange(state.matrix)) return false
        return true
    }
}

data class Void(val nd: NearCoordDiff) : SimpleCommand {
    override fun apply(bot: Bot, state: State): State = TODO()

    override fun volatileCoords(bot: Bot): List<Point> =
            listOf(bot.position, bot.position + nd)

    override fun check(bot: Bot, state: State): Boolean {
        val newPos = bot.position + nd
        if (!newPos.inRange(state.matrix)) return false
        return true
    }
}

data class FusionP(val nd: NearCoordDiff) : SimpleCommand {
    override fun apply(bot: Bot, state: State): State = TODO()
}

data class FusionS(val nd: NearCoordDiff) : SimpleCommand {
    override fun apply(bot: Bot, state: State): State = TODO()
}

data class FusionT(val p: FusionP, val s: FusionS) : GroupCommand {
    override val innerCommands: List<SimpleCommand> by lazy {
        listOf(p, s)
    }

    override fun apply(bots: List<Bot>, state: State): State {
        val (botP, botS) = bots
        return state.copy(
                energy = state.energy - 24,
                bots = state.bots - botS
                        - botP
                        + botP.copy(seeds = TreeSet(botP.seeds + botS.id + botP.seeds))
        )
    }

    override fun check(bots: List<Bot>, state: State): Boolean {
        if (bots.size != 2) return false
        val (botP, botS) = bots
        if (botP.position + p.nd != botS.position) return false
        if (botP.position != botS.position + s.nd) return false
        return true
    }
}

data class GFill(val nd: NearCoordDiff, val fd: FarCoordDiff) : SimpleCommand {
    override fun apply(bot: Bot, state: State): State = TODO()
    override fun volatileCoords(bot: Bot): List<Point> = TODO()
    override fun check(bot: Bot, state: State): Boolean = TODO()
}
data class GVoid(val nd: NearCoordDiff, val fd: FarCoordDiff) : SimpleCommand {
    override fun apply(bot: Bot, state: State): State = TODO()
    override fun volatileCoords(bot: Bot): List<Point> = TODO()
    override fun check(bot: Bot, state: State): Boolean = TODO()
}

val allPossibleMoves = (1..15).flatMap {
    listOf(SMove(LongCoordDiff(dx = it)),
            SMove(LongCoordDiff(dy = it)),
            SMove(LongCoordDiff(dz = it)))
} + (1..5).flatMap { s0 ->
    (1..5).flatMap { s1 ->
        listOf(
                LMove(ShortCoordDiff(dx = s0), ShortCoordDiff(dy = s1)),
                LMove(ShortCoordDiff(dx = s0), ShortCoordDiff(dz = s1)),
                LMove(ShortCoordDiff(dy = s0), ShortCoordDiff(dx = s1)),
                LMove(ShortCoordDiff(dy = s0), ShortCoordDiff(dz = s1)),
                LMove(ShortCoordDiff(dz = s0), ShortCoordDiff(dx = s1)),
                LMove(ShortCoordDiff(dz = s0), ShortCoordDiff(dy = s1))
        )
    }
}
