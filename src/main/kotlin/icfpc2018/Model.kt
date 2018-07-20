package icfpc2018

import icfpc2018.util.BitInputStream
import org.pcollections.HashTreePSet
import org.pcollections.MapPSet
import java.io.InputStream

data class Model(val data: MapPSet<Long> = HashTreePSet.empty()) {
    operator fun get(x: Int, y: Int, z: Int): Boolean = data.contains(convertCoordinates(x,y,z))

    fun set(x: Int, y: Int, z: Int, value: Boolean = true) = when(value) {
        true -> copy(data = data + convertCoordinates(x,y,z))
        false -> copy(data = data - convertCoordinates(x,y,z))
    }

    companion object {
        private inline fun convertCoordinates(x: Int, y: Int, z: Int) = x + 256L * y + 256L * 256L * z

        fun readMDL(bs: InputStream): Model {
            val size = bs.read()

            val stream = BitInputStream(bs)
            var data = HashTreePSet.empty<Long>()

            for(ix in 0..size) {
                for(iy in 0..size) {
                    for(iz in 0..size) {
                        val bit = stream.readBits(1)

                        if(bit != 0) data = data + convertCoordinates(ix, iy, iz)
                    }
                }
            }
            return Model(data)
        }
    }

}


