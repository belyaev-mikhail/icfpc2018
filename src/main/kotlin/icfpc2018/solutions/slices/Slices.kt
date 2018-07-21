package icfpc2018.solutions.slices

import icfpc2018.bot.commands.*
import icfpc2018.bot.state.*
import icfpc2018.solutions.Solution
import icfpc2018.solutions.initialLinearFission


val Model.indices: IntRange
    get() = 0 until size

class Slices(val target: Model, val system: System) : Solution {

    override fun solve() {
        initialLinearFission(target.size, system)
        column()
    }

    private fun column() {
        if (system.currentState.harmonics != Harmonics.HIGH) flip()
        for (i in 0 .. target.height) {
            up()
            build()
            for (j in 0 until target.size - 1) {
                move(i % 2 == 0)
                build()
            }
        }
    }

    private fun flip() {
        val commands = ArrayList<Command>()
        commands.add(Flip)
        for (i in 1 until system.currentState.bots.size)
            commands.add(Wait)
        system.timeStep(commands)
    }

    private fun up() {
        val commands = ArrayList<Command>()
        for (i in 0 until system.currentState.bots.size) {
            commands.add(SMove(LongCoordDiff(dy = 1)))
        }
        system.timeStep(commands)
    }

    private fun build() {
        val commands = ArrayList<Command>()
        for (bot in system.currentState.bots) {
            val diff = NearCoordDiff(0, -1, 0)
            val position = bot.position
            val isFill = target[position + diff]
            commands.add(if (isFill) Fill(diff) else Wait)
        }
        system.timeStep(commands)
    }

    private fun move(isForward: Boolean) {
        val commands = ArrayList<Command>()
        for (i in 0 until system.currentState.bots.size) {
            val dz = if (isForward) 1 else -1
            commands.add(SMove(LongCoordDiff(dz = dz)))
        }
        system.timeStep(commands)
    }
}