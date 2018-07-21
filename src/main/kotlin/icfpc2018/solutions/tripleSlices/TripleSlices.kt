package icfpc2018.solutions.tripleSlices

import icfpc2018.bot.commands.*
import icfpc2018.bot.state.*
import icfpc2018.bot.state.LinearCoordDiff.Axis.X
import icfpc2018.solutions.Solution
import kotlin.math.min


class TripleSlices(val target: Model, val system: System) : Solution {

    var isZForward = false

    override fun solve() {
        val botCount = min(20, target.size / 3)

        initialLinearFission(botCount, system)
        val numBots = system.numBots
        val numColumns = target.size / (numBots * 3)
        val lastColumn = Math.ceil(target.size % (numBots * 3) / 3.0).toInt()
        flip()
        for (i in 0 until numColumns) {
            if (i > 0)
                shift()
            column()
        }
        if (lastColumn != 0) {
            merge(lastColumn, system.currentState.matrix.size - 2)
            // shift()
            column()
            // 233044766
            // 58987842
        }
        flip()
        merge(1, system.currentState.matrix.size - 1)
        goToBase()
        halt()
    }

    private fun column() {
        down()
        for (i in 0..target.height) {
            isZForward = !isZForward
            up()
            build()
            for (j in 0 until target.size - 1) {
                forward()
                build()
            }
        }
    }

    private fun flip() {
        val commands = ArrayList<Command>()
        commands.add(Flip)
        for (i in 1 until system.numBots)
            commands.add(Wait)
        system.timeStep(commands)
    }

    private fun up() {
        val commands = ArrayList<Command>()
        for (i in 0 until system.numBots) {
            commands.add(SMove(LongCoordDiff(dy = 1)))
        }
        system.timeStep(commands)
    }

    private fun down() {
        val trace = ArrayList<List<Command>>()
        val y = system.currentState.bots.first()!!.position.y
        for (i in 0 until y / 15) {
            val commands = ArrayList<Command>()
            for (k in 0 until system.numBots) {
                commands.add(SMove(LongCoordDiff(dy = -15)))
            }
            trace.add(commands)
        }
        if (y % 15 != 0) {
            val commands = ArrayList<Command>()
            for (k in 0 until system.numBots) {
                commands.add(SMove(LongCoordDiff(dy = -(y % 15))))
            }
            trace.add(commands)
        }
        for (commands in trace)
            system.timeStep(commands)
    }

    val bottoms = listOf(
            NearCoordDiff(-1, -1, 0),
            NearCoordDiff(0, -1, 0),
            NearCoordDiff(1, -1, 0)
    )

    private fun build() {
        for (diff in bottoms) {
            val commands = ArrayList<Command>()
            for (bot in system.currentState.bots) {
                val position = bot.position
                val isInside = (position + diff).inRange(system.currentState.matrix)
                val isFill = target[position + diff]
                commands.add(if (isInside && isFill) Fill(diff) else Wait)
            }
            system.timeStep(commands)
        }
    }

    private fun forward() {
        val commands = ArrayList<Command>()
        for (i in 0 until system.numBots) {
            val dz = if (isZForward) 1 else -1
            commands.add(SMove(LongCoordDiff(dz = dz)))
        }
        system.timeStep(commands)
    }

    private fun merge(numBots: Int, originX: Int) {
        while (system.currentState.bots.size != numBots) {
            mergeStep()
        }

        order(originX)
    }

    private fun mergeStep() {
        val bots = system.currentState.bots.withIndex()
        val sortedBots = bots.sortedByDescending { it.value.position.x }

        val fusionCmds = Array<Command>(sortedBots.size) { Wait }

        for (w in sortedBots.windowed(2, 2)) {
            if (w.size != 2) continue

            val (botP, botS) = w

            val step = botP.value.position.x - botS.value.position.x - 1

            var repeat = step / 15
            while (repeat-- != 0) {
                val moveCmds = Array<Command>(sortedBots.size) { Wait }
                moveCmds[botS.index] = SMove(LongCoordDiff.fromAxis(X, if (step > 0) 15 else -15))
                system.timeStep(moveCmds.toList())
            }

            val rest = step % 15
            if (rest != 0) {
                val moveCmds = Array<Command>(sortedBots.size) { Wait }
                moveCmds[botS.index] = SMove(LongCoordDiff.fromAxis(X, rest))
                system.timeStep(moveCmds.toList())
            }

            fusionCmds[botP.index] = FusionP(NearCoordDiff(-1, 0, 0))
            fusionCmds[botS.index] = FusionS(NearCoordDiff(1, 0, 0))
        }

        system.timeStep(fusionCmds.toList())
    }

    private fun order(originX: Int) {
        val bots = system.currentState.bots.withIndex()
        val sortedBots = bots.sortedByDescending { it.value.position.x }

        var currOrigin = originX

        for ((idx, bot) in sortedBots) {
            val step = currOrigin - bot.position.x

            var repeat = step / 15
            while (repeat-- != 0) {
                val moveCmds = Array<Command>(sortedBots.size) { Wait }
                moveCmds[idx] = SMove(LongCoordDiff.fromAxis(X, if (step > 0) 15 else -15))
                system.timeStep(moveCmds.toList())
            }

            val rest = step % 15
            if (rest != 0) {
                val moveCmds = Array<Command>(sortedBots.size) { Wait }
                moveCmds[idx] = SMove(LongCoordDiff.fromAxis(X, rest))
                system.timeStep(moveCmds.toList())
            }

            currOrigin -= 3
        }
    }

    private fun shift() {
        var firstStep = 3 * system.numBots / 15
        val secondStep = 3 * system.numBots % 15
        while (firstStep-- != 0) atomicShift(15)
        if (secondStep != 0) atomicShift(secondStep)
    }

    private fun atomicShift(size: Int) {
        val move = SMove(LongCoordDiff(dx = size))
        for (i in 0 until system.numBots) {
            val commands = ArrayList<Command>()
            for (j in (0 until system.numBots).reversed()) {
                commands.add(if (j == i) move else Wait)
            }
            system.timeStep(commands)
        }
    }

    private fun goToBase() {
        val bot = system.currentState.bots.first()!!
        val x = bot.position.x
        val y = bot.position.y
        val z = bot.position.z
        val commands = ArrayList<Command>()
        for (i in 0 until x / 15)
            commands.add(SMove(LongCoordDiff(dx = -15)))
        if (x % 15 != 0)
            commands.add(SMove(LongCoordDiff(dx = -(x % 15))))
        for (i in 0 until z / 15)
            commands.add(SMove(LongCoordDiff(dz = -15)))
        if (z % 15 != 0)
            commands.add(SMove(LongCoordDiff(dz = -(z % 15))))
        for (i in 0 until y / 15)
            commands.add(SMove(LongCoordDiff(dy = -15)))
        if (y % 15 != 0)
            commands.add(SMove(LongCoordDiff(dy = -(y % 15))))
        for (command in commands)
            system.timeStep(listOf(command))
    }

    private fun halt() = system.timeStep(listOf(Halt))
}