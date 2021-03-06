package icfpc2018

import org.apache.commons.cli.*
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.math.max
import kotlin.math.min

enum class RunMode {
    ASSEMBLE, DISASSEMBLE, REASSEMBLE, SUBMIT, ALL
}

class Arguments(args: Array<String>) {
    private val options = Options()
    private val cmd: CommandLine

    companion object {
        val bounds = mapOf(
                RunMode.ASSEMBLE to (1 to 186),
                RunMode.DISASSEMBLE to (1 to 186),
                RunMode.REASSEMBLE to (1 to 115)
        )

        val modelNames = mapOf(
                RunMode.ASSEMBLE to "FA%03d",
                RunMode.DISASSEMBLE to "FD%03d",
                RunMode.REASSEMBLE to "FR%03d"
        )
    }

    init {
        setupOptions()

        val parser = DefaultParser()

        cmd = try {
            parser.parse(options, args)
        } catch (e: ParseException) {
            log.error("Error parsing command line arguments: ${e.message}")
            throw e
        }
    }

    private fun setupOptions() {
        val mode = Option(null, "mode", true, "specify run mode (a, d, r, s)")
        mode.isRequired = true
        options.addOption(mode)

        val solutionOpt = Option("s", "solution", true, "solution to use")
        solutionOpt.isRequired = false
        options.addOption(solutionOpt)

        val taskOpt = Option("m", "model", true, "file with target model")
        taskOpt.isRequired = false
        options.addOption(taskOpt)

        val from = Option("f", "from", true, "run solution on all tasks from <from> to <to>")
        from.isRequired = false
        options.addOption(from)

        val to = Option("t", "to", true, "run solution on all tasks from <from> to <to>")
        to.isRequired = false
        options.addOption(to)

        val reversed = Option("r", "reversed", false, "run models in reverse")
        reversed.isRequired = false
        options.addOption(reversed)

        val results = Option(null, "results", true, "directory for results dumping")
        results.isRequired = false
        options.addOption(results)

        val submit = Option.builder()
                .longOpt("submitDirs")
                .argName("resultsDir1,resultsDir2,...,resultsDirN")
                .hasArgs()
                .valueSeparator(',')
                .desc("merge all results from specified directories and create sibmit.zip with best of them")
                .required(false)
                .build()
        options.addOption(submit)
    }

    fun getMode() = when (getValue("mode")) {
        null -> throw IllegalArgumentException()
        "all" -> RunMode.ALL
        "a" -> RunMode.ASSEMBLE
        "d" -> RunMode.DISASSEMBLE
        "r" -> RunMode.REASSEMBLE
        "s" -> RunMode.SUBMIT
        else -> throw IllegalArgumentException()
    }

    fun getSolution() = getValue("solution") ?: throw IllegalStateException()
    fun getResults() = getValue("results") ?: "results"

    fun getSubmitDirectories() = cmd.getOptionValues("submitDirs").toList()

    fun isReversed() = cmd.hasOption("reversed")

    fun getModelNums(mode: RunMode): List<Int> {
        val model = getValue("model")?.toInt()
        if (model != null) return listOf(model)

        val from = getValue("from")?.toInt() ?: bounds.getValue(mode).first
        val to = getValue("to")?.toInt() ?: bounds.getValue(mode).second

        return when {
            isReversed() -> (from..to).reversed().toList()
            else -> (from..to).toList()
        }
    }

    fun getModels(mode: RunMode = getMode()): List<String> {
        val format = modelNames.getValue(mode)
        return getModelNums(mode).map { format.format(it) }
    }

    fun getValue(name: String): String? = cmd.getOptionValue(name)
    fun getValue(name: String, default: String) = getValue(name) ?: default

    fun printHelp() {
        val helpFormatter = HelpFormatter()
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        helpFormatter.printHelp(pw, 80, "icfpc", null, options, 1, 3, null)

        log.debug("$sw")
    }
}