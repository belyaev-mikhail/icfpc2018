package icfpc2018

import org.apache.commons.cli.*
import java.io.PrintWriter
import java.io.StringWriter

class Arguments(args: Array<String>) {
    private val options = Options()
    private val cmd: CommandLine

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
        val solutionOpt = Option("s", "solution", true, "solution to use")
        solutionOpt.isRequired = true
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

    }

    fun getModelNums(): List<Int> {
        val model = getValue("model")?.toInt()
        if (model != null) return listOf(model)

        val from = getValue("from")?.toInt() ?: 1
        val to = getValue("to")?.toInt() ?: 186

        return (from..to).toList()
    }

    fun getModels(): List<String> = getModelNums().map { "LA%03d".format(it) }

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