package icfpc2018.bot.commands

import icfpc2018.bot.commands.CheckError.Companion.OutOfRangeError
import icfpc2018.bot.commands.CheckError.Companion.PositionFullError
import icfpc2018.bot.state.*
import icfpc2018.bot.state.Harmonics.HIGH
import icfpc2018.bot.state.Harmonics.LOW
import icfpc2018.bot.util.minus
import icfpc2018.bot.util.plus
import org.apache.commons.compress.utils.BitInputStream
import org.organicdesign.fp.collections.PersistentTreeSet
import java.io.InputStream
import java.io.OutputStream
import java.lang.Math.pow
import java.nio.ByteOrder

fun Int.toBinary(size: Int) = ("0".repeat(32) + toString(2)).takeLast(size)
fun Long.toBinary(size: Int = 8) = ("0".repeat(64) + toString(2)).takeLast(size)

inline fun sbb(number: Int, from: Int, to: Int) = (((1 shl (to - from + 1)) - 1) and (number ushr from))
inline fun Int.subBits(range: IntRange) = sbb(this, 7 - range.endInclusive, 7 - range.start)
inline fun Long.subBits(range: IntRange) = toInt().subBits(range)

inline fun Int.endsWithBits(bits: Int): Boolean {
    val maskSize = java.lang.Long.highestOneBit(bits.toLong())
    val mask = (1 shl (maskSize + 1).toInt()) - 1
    return (this and mask) == bits
}

inline fun Long.endsWithBits(bits: Int): Boolean {
    val maskSize = 64L - java.lang.Long.numberOfLeadingZeros(bits.toLong())
    val mask = (1 shl maskSize.toInt()) - 1
    return (this.toInt() and mask) == bits
}

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
            is Void -> {
                val nd = (nd.dx + 1) * 9 + (nd.dy + 1) * 3 + nd.dz + 1
                stream.write("${nd.toBinary(5)}010".toInt(2))
            }
            is GFill -> {
                val nd = (nd.dx + 1) * 9 + (nd.dy + 1) * 3 + nd.dz + 1
                stream.write("${nd.toBinary(5)}001".toInt(2))
                stream.write(fd.dx)
                stream.write(fd.dy)
                stream.write(fd.dz)
            }
            is GVoid -> {
                val nd = (nd.dx + 1) * 9 + (nd.dy + 1) * 3 + nd.dz + 1
                stream.write("${nd.toBinary(5)}000".toInt(2))
                stream.write(fd.dx)
                stream.write(fd.dy)
                stream.write(fd.dz)
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

                        firstByte.endsWithBits(0b010) -> {
                            val nd = firstByte.subBits(0..4)
                            val dz = nd % 3 - 1
                            val dy = (nd / 3) % 3 - 1
                            val dx = nd / 9 - 1
                            Void(NearCoordDiff(dx, dy, dz))
                        }

                        firstByte.endsWithBits(0b001) -> {
                            val nd = firstByte.subBits(0..4)
                            val dz = nd % 3 - 1
                            val dy = (nd / 3) % 3 - 1
                            val dx = nd / 9 - 1
                            val fdx = bits.readBits(8).toInt()
                            val fdy = bits.readBits(8).toInt()
                            val fdz = bits.readBits(8).toInt()
                            GFill(NearCoordDiff(dx, dy, dz), FarCoordDiff(fdx, fdy, fdz))
                        }

                        firstByte.endsWithBits(0b000) -> {
                            val nd = firstByte.subBits(0..4)
                            val dz = nd % 3 - 1
                            val dy = (nd / 3) % 3 - 1
                            val dx = nd / 9 - 1
                            val fdx = bits.readBits(8).toInt()
                            val fdy = bits.readBits(8).toInt()
                            val fdz = bits.readBits(8).toInt()
                            GVoid(NearCoordDiff(dx, dy, dz), FarCoordDiff(fdx, fdy, fdz))
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

    fun volatileCoords(bot: Bot) = setOf(bot.position)

    fun check(bot: Bot, state: State) {}
}

interface GroupCommand : Command {
    fun apply(bots: List<Bot>, state: State): State = state

    fun volatileCoords(bots: List<Bot>) = bots.map { it.position }.toSet()

    fun check(bots: List<Bot>, state: State) {}

    val innerCommands: List<SimpleCommand>
}

data class CheckError(val from: Command, val msg: String) : Exception(msg) {
    companion object {
        fun OutOfRangeError(from: Command, pos: Point, state: State) =
                CheckError(
                        from,
                        "$pos out of range for [${state.matrix.size}]"
                )

        fun PositionFullError(from: Command, pos: Point) =
                CheckError(
                        from,
                        "Position $pos is Full"
                )
    }
}

object Halt : SimpleCommand {
    override fun toString(): String {
        return "Halt"
    }

    override fun apply(bot: Bot, state: State): State =
            state.copy(bots = PersistentTreeSet.empty())

    override fun check(bot: Bot, state: State) {
        when {
            bot.position != Point.ZERO -> throw CheckError(
                    this, "Bot position is ${bot.position}"
            )
            bot !in state.bots || 1 != state.bots.size -> throw CheckError(
                    this, "Bot state is ${state.bots}, bot is $bot"
            )
            state.harmonics != LOW -> throw CheckError(
                    this, "State harmonics is ${state.harmonics}"
            )
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

    override fun volatileCoords(bot: Bot): Set<Point> =
            lld.affectedCoords(bot.position)

    override fun check(bot: Bot, state: State) {
        val newPos = bot.position + lld
        if (!newPos.inRange(state.matrix)) throw OutOfRangeError(
                this, newPos, state
        )
        for (vc in volatileCoords(bot)) {
            if (state.matrix[vc]) throw CheckError(
                    this, "Position $vc is Full"
            )
        }
    }
}

data class LMove(val sld1: ShortCoordDiff, val sld2: ShortCoordDiff) : SimpleCommand {
    override fun apply(bot: Bot, state: State) =
            state.copy(
                    energy = state.energy + 2 * (sld1.mlen + 2 + sld2.mlen),
                    bots = state.bots - bot + bot.copy(position = bot.position + sld1 + sld2)
            )

    override fun volatileCoords(bot: Bot): Set<Point> =
            sld1.affectedCoords(bot.position) +
                    sld2.affectedCoords(bot.position + sld1)

    override fun check(bot: Bot, state: State) {
        val newPos1 = bot.position + sld1
        if (!newPos1.inRange(state.matrix)) throw OutOfRangeError(
                this, newPos1, state
        )
        val newPos2 = newPos1 + sld2
        if (!newPos2.inRange(state.matrix)) throw OutOfRangeError(
                this, newPos2, state
        )
        for (vc in volatileCoords(bot)) {
            if (state.matrix[vc]) throw PositionFullError(
                    this, vc
            )
        }
    }
}

data class Fission(val nd: NearCoordDiff, val m: Int) : SimpleCommand {
    override fun apply(bot: Bot, state: State): State {
        val bids = bot.seeds
                .withIndex()
                .filter { (idx, _) -> idx in setOf(0, 1, m + 1) }

        val ids = bids.map { it.value }

        val (newId) = ids

        val from = if (ids.size > 1) ids[1] else Int.MAX_VALUE

        val to = if (ids.size > 2) ids[2] else Int.MAX_VALUE

        return state.copy(
                energy = state.energy + 24,
                bots = state.bots
                        - bot
                        + bot.copy(seeds = bot.seeds.tailSet(to))
                        + Bot(
                        id = newId,
                        position = bot.position + nd,
                        seeds = bot.seeds.subSet(from, to)
                )
        )
    }

    override fun volatileCoords(bot: Bot): Set<Point> =
            setOf(bot.position, bot.position + nd)

    override fun check(bot: Bot, state: State) {
        if (bot.seeds.isEmpty()) throw CheckError(
                this, "Bot seeds are empty"
        )
        val newPos = bot.position + nd
        if (!newPos.inRange(state.matrix)) throw OutOfRangeError(
                this, newPos, state
        )
        if (state.matrix[newPos]) throw PositionFullError(
                this, newPos
        )
        if (bot.seeds.size < m + 1) throw CheckError(
                this, "Need ${m + 1} seeds for fission, have ${bot.seeds.size}"
        )
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

    override fun volatileCoords(bot: Bot): Set<Point> =
            setOf(bot.position, bot.position + nd)

    override fun check(bot: Bot, state: State) {
        val newPos = bot.position + nd
        if (!newPos.inRange(state.matrix)) throw OutOfRangeError(
                this, newPos, state
        )
    }
}

data class Void(val nd: NearCoordDiff) : SimpleCommand {
    override fun apply(bot: Bot, state: State): State {
        val newPos = bot.position + nd
        val (newEnergy, shouldUpdate) = if (state.matrix[newPos]) {
            state.energy - 12 to true
        } else {
            state.energy + 3 to false
        }
        return state.copy(
                energy = newEnergy,
                matrix = if (shouldUpdate) state.matrix.unset(newPos) else state.matrix
        )
    }

    override fun volatileCoords(bot: Bot): Set<Point> =
            setOf(bot.position, bot.position + nd)

    override fun check(bot: Bot, state: State) {
        val newPos = bot.position + nd
        if (!newPos.inRange(state.matrix)) throw OutOfRangeError(
                this, newPos, state
        )
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
                bots = state.bots
                        - botS
                        - botP
                        + botP.copy(seeds = botP.seeds.put(botS.id).union(botS.seeds))
        )
    }

    override fun check(bots: List<Bot>, state: State) {
        if (bots.size != 2) throw CheckError(
                this, "Wrong bots for fusion: $bots"
        )
        val (botP, botS) = bots
        if (botP.position + p.nd != botS.position) throw CheckError(
                this, "$botP + $p != $botS"
        )
        if (botP.position != botS.position + s.nd) throw CheckError(
                this, "$botS + $s != $botP"
        )
    }
}

data class GFill(val nd: NearCoordDiff, val fd: FarCoordDiff) : SimpleCommand {
    override fun apply(bot: Bot, state: State): State = TODO()
}

data class GFillT(val components: List<GFill>) : GroupCommand {
    override val innerCommands: List<SimpleCommand>
        get() = components

    override fun apply(bots: List<Bot>, state: State): State {
        val bot = bots.first()
        val gFill = components.first()
        val region = bot.position + gFill.nd to bot.position + gFill.nd + gFill.fd

        var res = state
        for (coord in region.coords()) {
            val (newEnergy, shouldUpdate) = if (state.matrix[coord]) {
                state.energy + 6 to false
            } else {
                state.energy + 12 to true
            }
            res = res.copy(
                    energy = newEnergy,
                    matrix = if (shouldUpdate) state.matrix.set(coord) else state.matrix
            )
        }
        return res
    }

    override fun volatileCoords(bots: List<Bot>): Set<Point> {
        val res = bots.map { it.position }.toSet()
        val bot = bots.first()
        val gFill = components.first()
        val region = bot.position + gFill.nd to bot.position + gFill.nd + gFill.fd
        return res + region.coords()
    }

    override fun check(bots: List<Bot>, state: State) {
        if (bots.size == 1) throw CheckError(
                this, "Fuck you"
        )

        if (bots.size != components.size) throw CheckError(
                this, "Difference in bot / component sizes"
        )

        val bot = bots.first()
        val gFill = components.first()
        val region = (bot.position + gFill.nd to bot.position + gFill.nd + gFill.fd).normalize()

        if (bots.size != pow(2.0, region.dim().toDouble()).toInt()) throw CheckError(
                this, "${bots.size} bots are not valid for $region"
        )

        val zipped = bots.zip(components)

        for ((b, gf) in zipped) {
            val near = b.position + gf.nd
            val far = near + gf.fd

            if (!near.inRange(state.matrix)) throw OutOfRangeError(
                    this, near, state
            )
            if (!far.inRange(state.matrix)) throw OutOfRangeError(
                    this, far, state
            )

            val r = (near to far).normalize()
            if (region != r) throw CheckError(
                    this, "$bot:$gFill and $b:$gf have different regions"
            )
        }

        if (bots.any { it.position in region }) throw CheckError(
                this, "Some of the $bots are inside $region"
        )

        for ((b1, gf1) in zipped) {
            for ((b2, gf2) in zipped) {
                if (b1.id == b2.id) continue
                if (b1.position + gf1.nd == b2.position + gf2.nd) throw CheckError(
                        this, "$b1:$gf1 and $b2:$gf2 are conflicting"
                )
            }
        }
    }
}

data class GVoid(val nd: NearCoordDiff, val fd: FarCoordDiff) : SimpleCommand {
    override fun apply(bot: Bot, state: State): State = TODO()
}

data class GVoidT(val components: List<GVoid>) : GroupCommand {
    override val innerCommands: List<SimpleCommand>
        get() = components

    override fun apply(bots: List<Bot>, state: State): State {
        val bot = bots.first()
        val gVoid = components.first()
        val region = bot.position + gVoid.nd to bot.position + gVoid.nd + gVoid.fd

        var res = state
        for (coord in region.coords()) {
            val (newEnergy, shouldUpdate) = if (state.matrix[coord]) {
                state.energy - 12 to true
            } else {
                state.energy + 3 to false
            }
            res = res.copy(
                    energy = newEnergy,
                    matrix = if (shouldUpdate) state.matrix.unset(coord) else state.matrix
            )
        }
        return res
    }

    override fun volatileCoords(bots: List<Bot>): Set<Point> {
        val res = bots.map { it.position }.toSet()
        val bot = bots.first()
        val gVoid = components.first()
        val region = bot.position + gVoid.nd to bot.position + gVoid.nd + gVoid.fd
        return res + region.coords()
    }

    override fun check(bots: List<Bot>, state: State) {
        if (bots.size == 1) throw CheckError(
                this, "Fuck you"
        )

        if (bots.size != components.size) throw CheckError(
                this, "Difference in bot / component sizes"
        )

        val bot = bots.first()
        val gVoid = components.first()
        val region = (bot.position + gVoid.nd to bot.position + gVoid.nd + gVoid.fd).normalize()

        if (bots.size != pow(2.0, region.dim().toDouble()).toInt()) throw CheckError(
                this, "${bots.size} bots are not valid for $region"
        )

        val zipped = bots.zip(components)

        for ((b, gv) in zipped) {
            val near = b.position + gv.nd
            val far = near + gv.fd

            if (!near.inRange(state.matrix)) throw OutOfRangeError(
                    this, near, state
            )
            if (!far.inRange(state.matrix)) throw OutOfRangeError(
                    this, far, state
            )

            val r = (near to far).normalize()
            if (region != r) throw CheckError(
                    this, "$bot:$gVoid and $b:$gv have different regions"
            )
        }

        if (bots.any { it.position in region }) throw CheckError(
                this, "Some of the $bots are inside $region"
        )

        for ((b1, gv1) in zipped) {
            for ((b2, gv2) in zipped) {
                if (b1.id == b2.id) continue
                if (b1.position + gv1.nd == b2.position + gv2.nd) throw CheckError(
                        this, "$b1:$gv1 and $b2:$gv2 are conflicting"
                )
            }
        }
    }
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
