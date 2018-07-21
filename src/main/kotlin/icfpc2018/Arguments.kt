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
        val taskOpt = Option("m", "model", true, "file with target model")
        taskOpt.isRequired = true
        options.addOption(taskOpt)

        val solutionOpt = Option("s", "solution", true, "solution to use")
        solutionOpt.isRequired = true
        options.addOption(solutionOpt)
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