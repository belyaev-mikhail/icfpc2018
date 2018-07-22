package icfpc2018.bot.state

import icfpc2018.bot.commands.*

enum class Mode {
    DEBUG, PRODUCTION
}

class ExecutionError : Exception()

class UnsupportedGroupCommandError : Exception()

class GroupCommandError : Exception()

class VolatileCoordError(msg: String) : Exception(msg)

class GroundError : Exception()

data class TimeStamp(val commandStamp: Int, val stateStamp: Int)

open class System(var currentState: State, var mode: Mode = Mode.DEBUG) {

    val numBots: Int
        get() = currentState.bots.size

    val score: Long
        get() = currentState.energy

    var commandTrace = mutableListOf<Command>()

    var stateTrace = mutableListOf(currentState)

    fun timeStamp() = TimeStamp(commandTrace.size, stateTrace.size)

    fun rollBackTo(timeStamp: TimeStamp) {
        commandTrace.subList(timeStamp.commandStamp, commandTrace.size).clear()
        stateTrace.subList(timeStamp.stateStamp, stateTrace.size).clear()
        currentState = stateTrace.last()
    }

    open fun timeStep(commands: List<Command>): Boolean {
        if (currentState.bots.isEmpty()) throw ExecutionError()

        commandTrace.addAll(commands)

        var energy = currentState.energy

        energy += when (currentState.harmonics) {
            Harmonics.LOW -> 3 * currentState.matrix.sizeCubed
            Harmonics.HIGH -> 30 * currentState.matrix.sizeCubed
        }

        energy += 20 * currentState.bots.size

        var execState = currentState.copy(energy = energy)

        val (simpleCommands, groupedCommands) = degroup(commands)

        val volatileCoords = mutableMapOf<Point, Pair<List<Bot>, Command>>()

        for ((bot, cmd) in simpleCommands) {
            val myVolatile = cmd.volatileCoords(bot)
            val conflict = myVolatile.find { it in volatileCoords }
            if (conflict != null) throw VolatileCoordError(
                    "Conflict point $conflict for ${bot to cmd} and ${volatileCoords[conflict]}"
            )
            myVolatile.forEach { volatileCoords[it] = listOf(bot) to cmd }
        }

        for ((bots, cmd) in groupedCommands) {
            val myVolatile = cmd.volatileCoords(bots)
            val conflict = myVolatile.find { it in volatileCoords }
            if (conflict != null) throw VolatileCoordError(
                    "Conflict point $conflict for ${bots to cmd} and ${volatileCoords[conflict]}"
            )
            myVolatile.forEach { volatileCoords[it] = bots to cmd }
        }

        for ((bot, cmd) in simpleCommands) {
            if (Mode.DEBUG == mode) cmd.check(bot, execState)
            execState = cmd.apply(bot, execState)
        }

        for ((bots, cmd) in groupedCommands) {
            if (Mode.DEBUG == mode) cmd.check(bots, execState)
            execState = cmd.apply(bots, execState)
        }

        stateTrace.add(execState)

        currentState = execState

        if (currentState.harmonics == Harmonics.LOW &&
                !currentState.matrix.isEverybodyGrounded)
            throw GroundError()

        return true
    }

    fun degroup(commands: List<Command>): Pair<MutableList<Pair<Bot, SimpleCommand>>, MutableList<Pair<List<Bot>, GroupCommand>>> {
        val fusionPrimary = mutableListOf<Pair<Bot, FusionP>>()
        val fusionSecondary = mutableListOf<Pair<Bot, FusionS>>()
        val gFills = mutableListOf<Pair<Bot, GFill>>()
        val gVoids = mutableListOf<Pair<Bot, GVoid>>()

        val simpleCommands = mutableListOf<Pair<Bot, SimpleCommand>>()

        for ((bot, cmd) in currentState.bots.zip(commands)) {
            when (cmd) {
                is FusionP -> {
                    fusionPrimary.add(bot to cmd)
                }
                is FusionS -> {
                    fusionSecondary.add(bot to cmd)
                }
                is GFill -> {
                    gFills.add(bot to cmd)
                }
                is GVoid -> {
                    gVoids.add(bot to cmd)
                }
                is SimpleCommand -> {
                    simpleCommands.add(bot to cmd)
                }
                is GroupCommand -> throw UnsupportedGroupCommandError()
            }
        }

        if (fusionPrimary.size != fusionSecondary.size) throw GroupCommandError()

        val groupedCommands = mutableListOf<Pair<List<Bot>, GroupCommand>>()

        for ((botP, cmdP) in fusionPrimary) {
            for ((botS, cmdS) in fusionSecondary) {
                val posP = botP.position + cmdP.nd
                val posS = botS.position + cmdS.nd
                if (posP == botS.position && posS == botP.position) {
                    groupedCommands.add(listOf(botP, botS) to FusionT(cmdP, cmdS))
                }
            }
        }

        val regionGFill = gFills.groupBy {
            (it.first.position + it.second.nd to it.first.position + it.second.nd + it.second.fd).normalize()
        }

        val regionGVoid = gVoids.groupBy {
            (it.first.position + it.second.nd to it.first.position + it.second.nd + it.second.fd).normalize()
        }

        for ((region, data) in regionGFill) {
            val (bots, components) = data.unzip()
            groupedCommands.add(bots to GFillT(components))
        }

        for ((region, data) in regionGVoid) {
            val (bots, components) = data.unzip()
            groupedCommands.add(bots to GVoidT(components))
        }
        return Pair(simpleCommands, groupedCommands)
    }
}
