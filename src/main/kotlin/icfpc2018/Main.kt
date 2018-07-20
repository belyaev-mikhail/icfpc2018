package icfpc2018

import icfpc2018.bot.state.*
import icfpc2018.solutions.simple.Simple
import java.io.File

val solution = Simple

fun main(args: Array<String>) {

    val modelFile = File("models/LA001_tgt.mdl").inputStream()

    val model = Model.readMDL(modelFile)

    val bot = Bot(1, Point(0, 0, 0), (2..20).toSet())

    val state = State(0, Harmonics.LOW, model, listOf(bot))

    val system = System(state)

    solution.apply(system)

    println(system.commandTrace)
}