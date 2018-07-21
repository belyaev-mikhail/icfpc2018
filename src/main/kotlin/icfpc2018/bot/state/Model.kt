package icfpc2018.bot.state

import org.apache.commons.compress.utils.BitInputStream
import org.organicdesign.fp.collections.PersistentHashMap
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteOrder

data class Model(val size: Int, private val data: PersistentHashMap<Int, Boolean> = PersistentHashMap.empty()) {
    val sizeCubed by lazy { size * size * size }

    val height by lazy { data.keys.map { deconvertCoordinates(it).y }.max()!! }

    operator fun get(point: Point) = get(point.x, point.y, point.z)

    operator fun get(x: Int, y: Int, z: Int): Boolean = data.containsKey(convertCoordinates(x, y, z))

    fun set(x: Int, y: Int, z: Int) = set(Point(x, y, z))

    fun set(point: Point) = run {
        val res = copy(data = data.assoc(point.index, point.isTriviallyGrounded))
        val mut = res.data.mutable()
        res.propagateGroundness(point, mut)
        res.copy(data = mut.immutable())
    }

    fun isGrounded(x: Int, y: Int, z: Int) = data[convertCoordinates(x, y, z)] == true
    @JvmName("pleasePleasePleaseGoToHellWithYourNameAmbiguity")
    fun isGrounded(point: Point) = data[point.index] == true

    private val Point.isTriviallyGrounded inline get() = y == 0
    private val Point.index inline get() = convertCoordinates(x, y, z)
    private val Point.inBounds inline get() = x in 0 until size && y in 0 until size && z in 0 until size

    private fun propagateGroundness(p: Point, mutableData: PersistentHashMap.MutableHashMap<Int, Boolean> = data.mutable()) {
        val neighbors = p.options(listOf(-1, 1), listOf(-1, 1), listOf(-1, 1)).filter {
            it.inBounds && mutableData[it.index] != null
        }
        val grounded = p.isTriviallyGrounded || mutableData[p.index] == true || neighbors.any { mutableData[it.index] == true }
        if (!grounded) return

        mutableData.assoc(p.index, true)
        val notGroundedNeighbors = neighbors.filterNot { it.isTriviallyGrounded || mutableData[it.index] == true }
        for (neighbor in notGroundedNeighbors) {
            mutableData.assoc(neighbor.index, true)
            propagateGroundness(neighbor, mutableData)
        }
        return
    }

    companion object {
        private inline fun deconvertCoordinates(coord: Int) =
                Point(coord % 256, (coord / 256) % 256, coord / (256 * 256))

        private inline fun convertCoordinates(x: Int, y: Int, z: Int) = x + 256 * y + 256 * 256 * z

        fun readMDL(bs: InputStream): Model {
            val size = bs.read()

            val stream = BitInputStream(bs, ByteOrder.LITTLE_ENDIAN)
            var data = PersistentHashMap.empty<Int, Boolean>()

            for (ix in 0 until size) {
                for (iy in 0 until size) {
                    for (iz in 0 until size) {
                        val bit = stream.readBits(1)

                        check(bit != -1L)

                        if (bit != 0L) data = data.assoc(convertCoordinates(ix, iy, iz), true) // all models are grounded by design
                    }
                }
            }
            return Model(size, data)
        }
    }

    fun writeMDL(os: OutputStream) {
        var byte = ""

        os.write(size)

        for (ix in 0 until size) {
            for (iy in 0 until size) {
                for (iz in 0 until size) {
                    byte += (if (get(ix, iy, iz)) "1" else "0")
                    if (byte.length == 8) {
                        os.write(byte.reversed().toInt(2))
                        byte = ""
                    }
                }
            }
        }
        if (byte.isNotEmpty()) os.write(byte.reversed().toInt(2))
    }
}

fun main(args: Array<String>) {
    val modelFile = File("models/LA186_tgt.mdl").inputStream()

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
    model.writeMDL(FileOutputStream(File("testModel.mdl")))

}
