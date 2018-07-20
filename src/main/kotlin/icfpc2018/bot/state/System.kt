package icfpc2018.bot.state

import icfpc2018.bot.commands.*

enum class Mode {
    DEBUG, PRODUCTION
}

class ExecutionError : Exception()

class CommandCheckError : Exception()

class GroupCommandError : Exception()

class System(var currentState: State, var mode: Mode = Mode.DEBUG) {

    val commandTrace = ArrayList<Command>()

    val stateTrace = arrayListOf(currentState)

    fun timeStep(commands: List<Command>) {
        if (currentState.bots.isEmpty()) throw ExecutionError()

        var energy = currentState.energy

        energy += when (currentState.harmonics) {
            Harmonics.LOW -> 30 // * R
            Harmonics.HIGH -> 3 // * R
        }

        energy += 20 * currentState.bots.size

        var execState = currentState.setEnergy(energy)

        val ungroupedCommands = mutableListOf<Pair<Bot, Command>>()

        for ((bot, command) in currentState.bots.zip(commands)) {
            when (command) {
                is SimpleCommand -> {
                    if (Mode.DEBUG == mode) if (!command.check(bot, execState)) throw CommandCheckError()
                    execState = command.apply(bot, execState)
                    commandTrace.add(command)
                }
                is FusionP, is FusionS -> {
                    ungroupedCommands.add(bot to command)
                }
            }
        }

        val fusionPrimary = ungroupedCommands.filter { it.second is FusionP }
        val fusionSecondary = ungroupedCommands.filter { it.second is FusionS }

        if (fusionPrimary.size != fusionSecondary.size) throw GroupCommandError()

        val groupedCommands = mutableListOf<Pair<List<Bot>, GroupCommand>>()

        for ((botP, cmdP) in fusionPrimary) {
            for ((botS, cmdS) in fusionSecondary) {
                if (cmdP !is FusionP) throw GroupCommandError()
                if (cmdS !is FusionS) throw GroupCommandError()

                val posP = botP.position + cmdP.nd
                val posS = botS.position + cmdS.nd
                if (posP == botS.position && posS == botP.position) {
                    groupedCommands.add(listOf(botP, botS) to FusionT(cmdP, cmdS))
                }
            }
        }

        if (groupedCommands.size != fusionPrimary.size) throw GroupCommandError()

        for ((bots, cmd) in groupedCommands) {
            if (Mode.DEBUG == mode) if (!cmd.check(bots, execState)) throw CommandCheckError()
            execState = cmd.apply(bots, execState)
            cmd.innerCommands.forEach { commandTrace.add(it) }
        }

        stateTrace.add(execState)

        currentState = execState
    }
}
