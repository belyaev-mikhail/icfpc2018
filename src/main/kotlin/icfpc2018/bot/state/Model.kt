package icfpc2018.bot.state

import org.apache.commons.compress.utils.BitInputStream
import org.pcollections.HashTreePSet
import org.pcollections.MapPSet
import java.io.File
import java.io.InputStream
import java.nio.ByteOrder

data class Model(val size: Int, private val data: MapPSet<Long> = HashTreePSet.empty()) {
    val sizeCubed by lazy { size * size * size }

    operator fun get(point: Point) = get(point.x, point.y, point.z)

    operator fun get(x: Int, y: Int, z: Int): Boolean = data.contains(convertCoordinates(x, y, z))

    fun set(x: Int, y: Int, z: Int, value: Boolean = true) = when (value) {
        true -> copy(data = data + convertCoordinates(x, y, z))
        false -> copy(data = data - convertCoordinates(x, y, z))
    }

    operator fun set(point: Point, value: Boolean) = set(point.x, point.y, point.z, value)

    companion object {
        private inline fun convertCoordinates(x: Int, y: Int, z: Int) = x + 256L * y + 256L * 256L * z

        fun readMDL(bs: InputStream): Model {
            val size = bs.read()

            val stream = BitInputStream(bs, ByteOrder.LITTLE_ENDIAN)
            var data = HashTreePSet.empty<Long>()

            for (ix in 0 until size) {
                for (iy in 0 until size) {
                    for (iz in 0 until size) {
                        val bit = stream.readBits(1)

                        check(bit != -1L)

                        if (bit != 0L) data = data + convertCoordinates(ix, iy, iz)
                    }
                }
            }
            return Model(size, data)
        }
    }
}

fun main(args: Array<String>) {
    val modelFile = File("models/LA001_tgt.mdl").inputStream()

    val model = Model.readMDL(modelFile)

    println(model)

    for (y in 0..model.size) {
        println("-".repeat(model.size * 2))

        for (x in 0..model.size) {
            for (z in 0..model.size) {
                if (model[x, y, z]) {
                    print("X ")
                } else {
                    print("  ")
                }
            }
            println()
        }

        println("-".repeat(model.size * 2))
    }
}
