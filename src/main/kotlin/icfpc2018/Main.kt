package icfpc2018

import icfpc2018.bot.state.*
import icfpc2018.bot.util.persistentTreeSetOf
import icfpc2018.solutions.sections.Sections
import java.io.File
import java.io.FileOutputStream

fun main(args: Array<String>) {
    for (i in 1..1) {
        val currFileName = "LA%03d".format(i)

        val targetModelFile = File("models/${currFileName}_tgt.mdl").inputStream()
        val targetModel = Model.readMDL(targetModelFile)

        val model = Model(targetModel.size)
        val bot = Bot(1, Point(0, 0, 0), (2..20).toSortedSet())
        val state = State(0, Harmonics.LOW, model, persistentTreeSetOf(bot))
        val system = System(state)

        val solution = Sections(targetModel, system)

        solution.solve()

        println("Energy = ${system.currentState.energy}")
        val success = system.currentState.matrix == targetModel
        println(if (success) "Success" else "Fail")
        if (success) {
            val ofile = FileOutputStream(File("solutions/sections/$currFileName.nbt"))
            system.commandTrace.forEach { it.write(ofile) }
        }
    }
}
