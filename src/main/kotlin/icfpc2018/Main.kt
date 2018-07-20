package icfpc2018

import icfpc2018.bot.commands.Command
import icfpc2018.bot.state.*
import icfpc2018.solutions.trace.Trace
import java.io.File
import java.io.FileOutputStream

fun main(args: Array<String>) {
    for (i in 1..186) {

        val currFileName = "LA%03d".format(i)

        val targetModelFile = File("models/${currFileName}_tgt.mdl").inputStream()
        val targetModel = Model.readMDL(targetModelFile)

        val model = Model(targetModel.size)

        val bot = Bot(1, Point(0, 0, 0), (2..20).toSortedSet())

        val state = State(0, Harmonics.LOW, model, sortedSetOf(bot))

        val system = System(state)

        val traceFile = File("traces/$currFileName.nbt").inputStream()
        val commands: MutableList<Command> = mutableListOf()
        while (traceFile.available() != 0) {
            commands += Command.read(traceFile)
        }

        val solution = Trace(commands)

        solution.apply(system)

        val ofile = FileOutputStream(File("traces/$currFileName.my.nbt"))
        system.commandTrace.forEach { it.write(ofile) }

        println(system.currentState.matrix == targetModel)
    }
}
