package icfpc2018

import icfpc2018.bot.commands.Command
import icfpc2018.bot.state.*
import icfpc2018.bot.util.persistentTreeSetOf
import icfpc2018.solutions.groundedSlices.GroundedSlices
import icfpc2018.solutions.sections.Sections
import icfpc2018.solutions.slices.Slices
import icfpc2018.solutions.trace.Trace
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader

fun main(args: Array<String>) {
    val arguments = Arguments(args)
    val solutionName = arguments.getValue("solution") ?: throw IllegalArgumentException()
    val isSubmit = arguments.isSubmit()

    val resultsFile = File("results/results.json")
    val resultReader = when {
        resultsFile.exists() -> resultsFile.reader()
        else -> StringReader("{}")
    }
    val results = Results.fromJson(resultReader)

    val targetModels = arguments.getModels()

    for (targetModelName in targetModels) {
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
                Trace(commands, system)
            }
            "sections" -> Sections(targetModel, system)
            "slices" -> Slices(targetModel, system)
            "grounded_slices" -> GroundedSlices(targetModel, system)
            else -> throw IllegalArgumentException()
        }
        log.info(solution::class.java.name)

        solution.solve()

        log.info { "Energy: " + system.currentState.energy }

        val success = system.currentState.matrix == targetModel


        log.info(if (success) "Success" else "Fail")

        if (success) {
            val resultTraceFile = "results/${targetModelName}_$solutionName.nbt"

            results.addNewResult(targetModelName, solutionName, system.currentState.energy, resultTraceFile)

            val ofile = FileOutputStream(File(resultTraceFile).apply { this.parentFile.mkdirs() })
            system.commandTrace.forEach { it.write(ofile) }

            if (isSubmit) {
                val submitFile = "submit/${targetModelName}.nbt"
                val submitstream = FileOutputStream(File(submitFile).apply { this.parentFile.mkdirs() })
                system.commandTrace.forEach { it.write(submitstream) }
            }
        }
    }

    val writer = resultsFile.writer()
    writer.write(results.toJson())
    writer.flush()

    if (isSubmit) {
        ZipWriter().createZip("submit/")
    }
}
