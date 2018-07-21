package icfpc2018.solutions.slices

import icfpc2018.bot.commands.*
import icfpc2018.bot.state.*
import icfpc2018.solutions.Solution
import icfpc2018.solutions.initialLinearFission


class Slices(val target: Model, val system: System) : Solution {

    var isZForward = false

    override fun solve() {
        initialLinearFission(target.size, system)
        val numBots = system.numBots
        val numColumns = target.size / numBots
        val lastColumn = target.size % numBots
        flipTo(Harmonics.HIGH)
        for (i in 0 until numColumns) {
            if (i > 0)
                shift()
            column()
        }
        if (lastColumn != 0) {
            merge(lastColumn)
            shift()
            column()
        }
        flipTo(Harmonics.LOW)
        merge(1)
        goToBase()
        halt()
    }

    private fun column() {
        down()
        for (i in 0..target.box.top) {
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

    private fun flipTo(harmonics: Harmonics) = if (system.currentState.harmonics != harmonics) flip() else Unit

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

    private fun forward() {
        val commands = ArrayList<Command>()
        for (i in 0 until system.numBots) {
            val dz = if (isZForward) 1 else -1
            commands.add(SMove(LongCoordDiff(dz = dz)))
        }
        system.timeStep(commands)
    }

    private fun merge(numBots: Int) {
        val botsKillCount = system.numBots - numBots
        val botToKill = FusionS(NearCoordDiff(1, 0, 0))
        val botIntoKill = FusionP(NearCoordDiff(-1, 0, 0))
        for (i in 0 until botsKillCount) {
            val commands = ArrayList<Command>()
            commands.add(botToKill)
            commands.add(botIntoKill)
            for (j in 0 until system.numBots - 2) {
                commands.add(Wait)
            }
            system.timeStep(commands)
        }
    }

    private fun shift() {
        val firstStep = system.numBots / 15
        val secondStep = system.numBots % 15
        if (firstStep != 0) atomicShift(15)
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