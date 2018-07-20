package icfpc2018.bot.state

import icfpc2018.bot.commands.Command

enum class Mode {
    DEBUG, PRODUCTION
}

class ExecutionError : Exception()

class CommandCheckError : Exception()

class System(var currentState: State, var mode: Mode = Mode.DEBUG) {

    val commandTrace = ArrayList<Command>()

    val stateTrace = ArrayList<State>()

    fun timeStep(commands: List<Command>) {
        if (currentState.bots.isEmpty()) throw ExecutionError()

        var energy = currentState.energy

        energy += when (currentState.harmonics) {
            Harmonics.LOW -> 30 // * R
            Harmonics.HIGH -> 3 // * R
        }

        energy += 20 * currentState.bots.size

        var execState = currentState.setEnergy(energy)

        for ((bot, command) in currentState.bots.zip(commands)) {
            if (Mode.DEBUG == mode) if (!command.check(bot, execState)) throw CommandCheckError()
            execState = command.apply(bot, execState)
            commandTrace.add(command)
            stateTrace.add(currentState)
        }
    }
}
