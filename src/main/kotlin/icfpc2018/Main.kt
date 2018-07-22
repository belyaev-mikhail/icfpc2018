package icfpc2018

import icfpc2018.bot.commands.Command
import icfpc2018.bot.state.*
import icfpc2018.bot.util.persistentTreeSetOf
import icfpc2018.solutions.getSolutionByName
import icfpc2018.solutions.trace.Trace
import org.organicdesign.fp.collections.PersistentTreeSet
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

fun getModeByModelName(name: String): RunMode {
    val start = name.take(2)
    return when (start) {
        "FA" -> RunMode.ASSEMBLE
        "FD" -> RunMode.DISASSEMBLE
        else -> RunMode.REASSEMBLE
    }
}

fun writeTraceFile(system: System, directory: String, name: String) {
    val traceFile = "$directory/$name.nbt"
    log.info("Writing $traceFile")
    val stream = FileOutputStream(File(traceFile).apply { this.parentFile.mkdirs() })
    system.commandTrace.forEach { it.write(stream) }
}

fun submit(resultDirs: List<String>) {
    val results = resultDirs.map { Results.readFromDirectory(it) }
    val merged = results.reduce { acc, res -> acc.merge(res) }
    for ((task, result) in merged) {
        val bestSolution = result.getSortedSolutions().first().second
        Files.copy(File(bestSolution.trace).toPath(), File("submit/$task.nbt").toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
    }
    ZipWriter().createZip("submit/")
}

fun submitChecked(resultDirs: List<String>) {
    val results = resultDirs.map { Results.readFromDirectory(it) }
    val merged = results.reduce { acc, res -> acc.merge(res) }
    for ((task, result) in merged) {
        val mode = getModeByModelName(task)
        if (mode == RunMode.REASSEMBLE) {
            val bestSolution = result.getSortedSolutions().first().second
            Files.copy(File(bestSolution.trace).toPath(), File("submit/$task.nbt").toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
        } else {
            val targetModel = when (mode) {
                RunMode.ASSEMBLE -> Model.readMDL(File("models/${task}_tgt.mdl").inputStream())
                else -> Model.readMDL(File("models/${task}_src.mdl").inputStream())
            }
            val model = Model(targetModel.size)
            val bot = Bot(1, Point(0, 0, 0), PersistentTreeSet.of(2..Config.maxBots))
            val state = State(0, Harmonics.LOW, model, persistentTreeSetOf(bot))

            var haveSolution = false
            for ((solutionName, solution) in result.getSortedSolutions()) {
                val traceFile = File(solution.trace).inputStream()
                val commands: MutableList<Command> = mutableListOf()
                while (traceFile.available() != 0) {
                    commands += Command.read(traceFile)
                }
                val system = System(state)
                try {
                    Trace(commands, system).solve()
                } catch (e: Exception) {
                    log.info("Solution $solutionName failed on task $task (trace ${solution.trace}): exception $e")
                    continue
                }

                if (system.currentState.matrix != targetModel) {
                    log.info("Solution $solutionName failed on task $task (trace ${solution.trace}): resulting model is not complete")
                    continue
                }

                Files.copy(File(solution.trace).toPath(), File("submit/$task.nbt").toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
                haveSolution = true
                break
            }
            if (!haveSolution) {
                log.error("Have no solution for task $task to submit")
                return
            }
        }
    }

    ZipWriter().createZip("submit/")
}

fun assemble(solutionName: String, targetModels: List<String>, resultsDir: String) {
    val results = Results.readFromDirectory(resultsDir)
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

        try {
            solution.solve()
        } catch (e: Exception) {
            log.error("Solution $solution throwed exception $e")
            continue
        }

        log.info { "Energy: " + system.currentState.energy }

        val success = system.currentState.matrix == targetModel


        log.info(if (success) "Success" else "Fail")

        if (success) {
            val resultTraceFile = "$resultsDir/${targetModelName}_$solutionName.nbt"
            results.addNewResult(targetModelName, solutionName, system.currentState.energy, resultTraceFile)

            val ofile = FileOutputStream(File(resultTraceFile))
            system.commandTrace.forEach { it.write(ofile) }
            log.info("Results dumped")
        }
    }

    results.writeToDirectory(resultsDir)
}

fun disassemble(solutionName: String, targetModels: List<String>, resultsDir: String) {
    val results = Results.readFromDirectory(resultsDir)
    for (targetModelName in targetModels) {
        log.info("Running with model $targetModelName")
        val targetModelFile = File("models/${targetModelName}_src.mdl").inputStream()
        val targetModel = Model.readMDL(targetModelFile)

        val model = Model(targetModel.size)
        val bot = Bot(1, Point(0, 0, 0), PersistentTreeSet.of(2..Config.maxBots))
        val state = State(0, Harmonics.LOW, model, persistentTreeSetOf(bot))

        val assembleSystem = System(state)
        val solution = when (solutionName) {
            "trace" -> {
                val traceFile = File("traces/$targetModelName.nbt").inputStream()
                val commands: MutableList<Command> = mutableListOf()
                while (traceFile.available() != 0) {
                    commands += Command.read(traceFile)
                }
                Trace(commands, assembleSystem)
            }
            else -> getSolutionByName(solutionName, targetModel, assembleSystem)
        }
        log.info(solution::class.java.name)

        try {
            solution.solve()
        } catch (e: Exception) {
            log.error("Solution $solution throwed exception $e")
            continue
        }

        val resultingTrace = Trace(assembleSystem.commandTrace, System(state)).inverted()

        log.info { "Energy: " + assembleSystem.currentState.energy }

        val success = assembleSystem.currentState.matrix == targetModel


        log.info(if (success) "Success" else "Fail")

        if (success) {
            val resultTraceFile = "$resultsDir/${targetModelName}_$solutionName.nbt"
            results.addNewResult(targetModelName, solutionName, assembleSystem.currentState.energy, resultTraceFile)

            val ofile = FileOutputStream(File(resultTraceFile))
            resultingTrace.forEach { it.write(ofile) }
            log.info("Results dumped")
        }
    }

    results.writeToDirectory(resultsDir)
}

fun main(args: Array<String>) {
    val arguments = Arguments(args)

    when (arguments.getMode()) {
        RunMode.ASSEMBLE -> assemble(arguments.getSolution(), arguments.getModels(), arguments.getResults())
        RunMode.DISASSEMBLE -> disassemble(arguments.getSolution(), arguments.getModels(), arguments.getResults())
        RunMode.SUBMIT -> submit(arguments.getSubmitDirectories())
        else -> throw IllegalArgumentException()
    }
}
