package icfpc2018

import icfpc2018.bot.state.Model
import icfpc2018.solutions.regions.RegionSplitter
import java.io.File

fun main(args: Array<String>) {
    for (i in 1..186) {
        try {
            val filename = "models/FA%03d_tgt.mdl".format(i)
            val targetModelFile = File(filename).inputStream()
            val targetModel = Model.readMDL(targetModelFile)
            val regions = RegionSplitter.split(targetModel)
            val allCorrect = regions.flatten().map { it.points }.flatten().all { targetModel[it] }
            if (!allCorrect) {
                println("Incorrect $i")
            }
        } catch (ex: Exception) {
            println("$i: $ex")
        }
    }

}