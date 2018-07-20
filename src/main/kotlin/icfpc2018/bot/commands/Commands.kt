package icfpc2018.bot.commands

import icfpc2018.bot.state.*
import org.apache.commons.compress.utils.BitInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteOrder

fun Int.toBinary(size: Int) = ("0".repeat(32) + toString(2)).takeLast(size)
fun Long.toBinary(size: Int = 8) = ("0".repeat(64) + toString(2)).takeLast(size)

interface Command {
    fun apply(bot: Bot, state: State): State

    fun volatileCoords(bot: Bot) = emptyList<Point>()

    fun check(bot: Bot, state: State) = true

    fun write(stream: OutputStream) {
        when(this) {
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

            return when(firstByte) {
                0b11111111L -> Halt
                0b11111110L -> Wait
                0b11111101L -> Flip
                else -> {
                    val firstByteEnc = firstByte.toBinary()
                    when {
                        firstByteEnc.endsWith("0100") -> {
                            val llda = firstByteEnc.substring(2..3).toInt(2)
                            val secondByteEnc = bits.readBits(8).toBinary()
                            val lldi = secondByteEnc.substring(3..7).toInt(2)
                            val ldiff = when(llda) {
                                1 -> LongCoordDiff(dx = lldi - 15)
                                2 -> LongCoordDiff(dy = lldi - 15)
                                3 -> LongCoordDiff(dz = lldi - 15)
                                else -> throw IllegalStateException()
                            }
                            SMove(ldiff)
                        }

                        firstByteEnc.endsWith("1100") -> {
                            val sld2a = firstByteEnc.substring(0..1).toInt(2)
                            val sld1a = firstByteEnc.substring(2..3).toInt(2)
                            val secondByteEnc = bits.readBits(8).toBinary()
                            val sld2i = secondByteEnc.substring(0..3).toInt(2)
                            val sld1i = secondByteEnc.substring(4..7).toInt(2)

                            val sdiff1 = when(sld1a) {
                                1 -> ShortCoordDiff(dx = sld1i - 5)
                                2 -> ShortCoordDiff(dy = sld1i - 5)
                                3 -> ShortCoordDiff(dz = sld1i - 5)
                                else ->  throw IllegalStateException()
                            }
                            val sdiff2 = when(sld2a) {
                                1 -> ShortCoordDiff(dx = sld2i - 5)
                                2 -> ShortCoordDiff(dy = sld2i - 5)
                                3 -> ShortCoordDiff(dz = sld2i - 5)
                                else ->  throw IllegalStateException()
                            }
                            LMove(sdiff1, sdiff2)
                        }

                        firstByteEnc.endsWith("111") -> {
                            val nd = firstByteEnc.substring(0..4).toInt(2)
                            val dz = nd % 3 - 1
                            val dy = (nd / 3) % 3 - 1
                            val dx = nd / 9 - 1
                            FusionP(NearCoordDiff(dx, dy, dz))
                        }

                        firstByteEnc.endsWith("110") -> {
                            val nd = firstByteEnc.substring(0..4).toInt(2)
                            val dz = nd % 3 - 1
                            val dy = (nd / 3) % 3 - 1
                            val dx = nd / 9 - 1
                            FusionS(NearCoordDiff(dx, dy, dz))
                        }

                        firstByteEnc.endsWith("101") -> {
                            val nd = firstByteEnc.substring(0..4).toInt(2)
                            val dz = nd % 3 - 1
                            val dy = (nd / 3) % 3 - 1
                            val dx = nd / 9 - 1

                            val secondByte = bits.readBits(8)

                            Fission(NearCoordDiff(dx, dy, dz), secondByte.toInt())
                        }

                        firstByteEnc.endsWith("011") -> {
                            val nd = firstByteEnc.substring(0..4).toInt(2)
                            val dz = nd % 3 - 1
                            val dy = (nd / 3) % 3 - 1
                            val dx = nd / 9 - 1
                            Fill(NearCoordDiff(dx, dy, dz))
                        }

                        else -> throw IllegalStateException("Cannot read byte: $firstByteEnc")
                    }
                }
            }

        }
    }
}

fun main(args: Array<String>) {
    val traceFile = File("traces/LA001.nbt").inputStream()
    val commands: MutableList<Command> = mutableListOf()
    while(traceFile.available() != 0) {
        commands += Command.read(traceFile)
    }
    println(commands.joinToString("\n"))

    val ofile = FileOutputStream(File("traceText.nbt"))
    commands.forEach { it.write(ofile) }
}

object Halt : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

object Wait : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

object Flip : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

data class SMove(val lld: LongCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

data class LMove(val sld1: ShortCoordDiff, val sld2: ShortCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

data class Fission(val nd: NearCoordDiff, val m: Int) : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

data class Fill(val nd: NearCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

data class FusionP(val nd: NearCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

data class FusionS(val nd: NearCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}
