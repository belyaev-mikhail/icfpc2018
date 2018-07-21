package icfpc2018

import com.xenomachina.argparser.ArgParser
import icfpc2018.bot.state.*
import icfpc2018.bot.util.persistentTreeSetOf
import icfpc2018.solutions.sections.Sections
import java.io.File
import java.io.FileOutputStream

class Arguments(parser: ArgParser) {
    val models by parser.positionalList("Model files", 0..Int.MAX_VALUE)
}

fun main(args: Array<String>) {
    val arguments = ArgParser(args).parseInto(::Arguments)

    for (i in 1..186) {

        val currFileName = "LA%03d".format(i)

        val targetModelFile = File("models/${currFileName}_tgt.mdl").inputStream()
        val targetModel = Model.readMDL(targetModelFile)

        val model = Model(targetModel.size)
        val bot = Bot(1, Point(0, 0, 0), (2..20).toSortedSet())
        val state = State(0, Harmonics.LOW, model, persistentTreeSetOf(bot))
        val system = System(state)

        val solution = Sections(targetModel, system)

        solution.solve()

        val ofile = FileOutputStream(File("traces/$currFileName.my.nbt"))
        system.commandTrace.forEach { it.write(ofile) }

        log.info { "Energy: " + system.currentState.energy }
        val success = system.currentState.matrix == targetModel
        log.info(if (success) "Success" else "Fail")
        if (success) {
            val ofile = FileOutputStream(File("solutions/sections/$currFileName.nbt"))
            system.commandTrace.forEach { it.write(ofile) }
        }
    }
}
