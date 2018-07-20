package icfpc2018

import icfpc2018.bot.commands.Command
import icfpc2018.bot.state.*
import icfpc2018.solutions.trace.Trace
import java.io.File
import java.io.FileOutputStream

fun main(args: Array<String>) {

    val targetModelFile = File("models/LA001_tgt.mdl").inputStream()
    val targetModel = Model.readMDL(targetModelFile)

    val model = Model(targetModel.size)

    val bot = Bot(1, Point(0, 0, 0), (2..20).toSortedSet())

    val state = State(0, Harmonics.LOW, model, sortedSetOf(bot))

    val system = System(state)

    val traceFile = File("traces/LA001.nbt").inputStream()
    val commands: MutableList<Command> = mutableListOf()
    while (traceFile.available() != 0) {
        commands += Command.read(traceFile)
    }
    println(commands.joinToString("\n"))

    val solution = Trace(commands)

    solution.apply(system)

    val ofile = FileOutputStream(File("traces/LA001.my.nbt"))
    system.commandTrace.forEach { it.write(ofile) }
}
