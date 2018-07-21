package icfpc2018

import icfpc2018.bot.commands.Command
import icfpc2018.bot.state.*
import icfpc2018.bot.util.persistentTreeSetOf
import icfpc2018.solutions.trace.Trace
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader

fun main(args: Array<String>) {
    val arguments = Arguments(args)
    val targetModelName = arguments.getValue("model") ?: throw IllegalArgumentException()
    val solutionName = arguments.getValue("solution") ?: throw IllegalArgumentException()

    val resultsFile = File("results/results.json")
    val resultReader = when {
        resultsFile.exists() -> resultsFile.reader()
        else -> StringReader("{}")
    }
    val results = Results.fromJson(resultReader)

    val targetModelFile = File("models/${targetModelName}_tgt.mdl").inputStream()
    val targetModel = Model.readMDL(targetModelFile)

    val model = Model(targetModel.size)

    val bot = Bot(1, Point(0, 0, 0), (2..20).toSortedSet())

    val state = State(0, Harmonics.LOW, model, persistentTreeSetOf(bot))

    val system = System(state)


    val solution = when (solutionName) {
        "trace" -> {
            val traceFile = File("traces/$targetModelName.nbt").inputStream()
            val commands: MutableList<Command> = mutableListOf()
            while (traceFile.available() != 0) {
                commands += Command.read(traceFile)
            }
            Trace(commands)
        }
        else -> throw IllegalArgumentException()

    }
    solution.apply(system)

    val resultTraceFie = "results/${targetModelName}_$solutionName.nbt"
    val ofile = FileOutputStream(File(resultTraceFie))
    system.commandTrace.forEach { it.write(ofile) }

    println(system.currentState.matrix == targetModel)
    results.addNewResult(targetModelName, solutionName, system.currentState.energy.toLong(), resultTraceFie)

    val writer = resultsFile.writer()
    writer.write(results.toJson())
    writer.flush()
}
