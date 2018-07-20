package icfpc2018.system

import icfpc2018.bot.commands.Command
import icfpc2018.bot.state.Bot
import icfpc2018.bot.state.Harmonics
import icfpc2018.bot.state.Model

data class State(val trace: MutableList<Command>, var energy: Int, var harmonics: Harmonics, val matrix: Model, val bots: List<Bot>)

class System(initialState: State) {

    var currentState: State = initialState

    var commandTrace = ArrayList<Command>()

    var stateTrace = ArrayList<State>()
}