package icfpc2018

import icfpc2018.bot.commands.Command
import icfpc2018.bot.state.*
import icfpc2018.bot.util.persistentTreeSetOf
import icfpc2018.solutions.getSolutionByName
import icfpc2018.solutions.trace.Trace
import org.organicdesign.fp.collections.PersistentTreeSet
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
        log.info("Running with model $targetModelName")
        val targetModelFile = File("models/${targetModelName}_tgt.mdl").inputStream()
        val targetModel = Model.readMDL(targetModelFile)

        val model = Model(targetModel.size)
        val bot = Bot(1, Point(0, 0, 0), PersistentTreeSet.of(2..Config.maxBots))
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
            else -> getSolutionByName(solutionName, targetModel, system)
        }
        log.info(solution::class.java.name)

        solution.solve()

        log.info { "Energy: " + system.currentState.energy }

        val success = system.currentState.matrix == targetModel


        log.info(if (success) "Success" else "Fail")

        if (success) {
            if (isSubmit) {
                val submitFile = "submit/${targetModelName}.nbt"
                log.info("Writing $submitFile")
                val submitstream = FileOutputStream(File(submitFile).apply { this.parentFile.mkdirs() })
                system.commandTrace.forEach { it.write(submitstream) }

            } else {
                val resultTraceFile = "results/${targetModelName}_$solutionName.nbt"
                results.addNewResult(targetModelName, solutionName, system.currentState.energy, resultTraceFile)

                val ofile = FileOutputStream(File(resultTraceFile).apply { this.parentFile.mkdirs() })
                system.commandTrace.forEach { it.write(ofile) }

                run {
                    val resultTraceFile = "results/${targetModelName}_${solutionName}_inverse.nbt"
                    val reversedTrace = Trace(system.commandTrace, System(state)).inverted()

                    val ofile = FileOutputStream(File(resultTraceFile).apply { this.parentFile.mkdirs() })
                    reversedTrace.forEach { it.write(ofile) }
                }

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
