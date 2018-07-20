package icfpc2018.bot.state

enum class Mode {
    DEBUG, PRODUCTION
}

class CommandCheckError : Exception()

class Executor(val state: State, var mode: Mode = Mode.DEBUG) {
    fun timeStep(): Boolean {
        with(state) {
            if (bots.isEmpty()) return false

            when (state.harmonics) {
                Harmonics.LOW -> state.energy += 30 // * R
                Harmonics.HIGH -> state.energy += 3 // * R
            }

            state.energy += 20 * bots.size

            val commands = trace.take(bots.size)
            val es = bots.zip(commands)

            if (Mode.DEBUG == mode) {
                for ((bot, command) in es) {
                    if (!command.check(bot, this)) throw CommandCheckError()
                }
            }

            for ((bot, command) in es) {
                command.apply(bot, this)
            }

            // trace = trace.drop(bots.size)

            return true
        }
    }
}
