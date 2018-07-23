package icfpc2018

import icfpc2018.bot.state.Model
import icfpc2018.bot.state.Point
import icfpc2018.solutions.regions.RegionSplitter
import org.organicdesign.fp.collections.PersistentHashMap
import org.organicdesign.fp.collections.PersistentHashSet
import java.io.File
import java.io.FileOutputStream

fun Model.privateData(): PersistentHashMap<Int, Boolean> {
    val field = Model::class.java.getDeclaredField("data")
    field.isAccessible = true
    return field.get(this) as PersistentHashMap<Int, Boolean>
}

fun makeModel(points: Set<Point>, targetModel: Model): Model {
    val convertedPoints = points.map { Model.convertCoordinates(it.x, it.y, it.z) }
    val data = PersistentHashMap.empty<Int, Boolean>().mutable()
    convertedPoints.forEach { data.assoc(it, true) }
    return Model(targetModel.size, data.immutable())
}

fun main(args: Array<String>) {


    for (i in 15..25) {
        try {
            val filename = "models/FA%03d_tgt.mdl".format(i)
            val targetModelFile = File(filename).inputStream()
            val targetModel = Model.readMDL(targetModelFile)
            val regions = RegionSplitter.split(targetModel)
            val modelData = targetModel.privateData().entries.mutable().filter { it.value }
            val modelPoints = modelData.map { Model.deconvertCoordinates(it.key) }.toSet()
            val regionPoints = regions.flatten().map { it.points }.flatten().toSet()

            val regionMorePoints = regionPoints - modelPoints
            val regionLessPoints = modelPoints - regionPoints

            if (regionMorePoints.isNotEmpty() || regionLessPoints.isNotEmpty()) {
                val model = makeModel(regionPoints, targetModel)
                model.writeMDL(FileOutputStream(File("tmp/FA%03d_tgt.mdl".format(i))))
                println("Incorrect $i: ${regionMorePoints.size} more points and ${regionLessPoints.size} less points")
            }
        } catch (ex: Exception) {
            println("$i: $ex")
        }
    }

}