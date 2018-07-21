package icfpc2018.solutions.groundedBoundedSlices

import icfpc2018.bot.commands.*
import icfpc2018.bot.state.*
import icfpc2018.solutions.Solution
import kotlin.math.abs

data class Rectangle(val left: Int, val right: Int, val top: Int, val bottom: Int)

class GroundedBoundedSlices(val target: Model, val system: System) : Solution {

    var isZForward = false

    override fun solve() {
        linearFission()
        val numBots = system.numBots
        val numColumns = target.box.width / numBots
        val lastColumn = target.box.width % numBots
        goToStart()
        for (i in 0 until numColumns) {
            if (i > 0)
                shift(system.numBots)
            column()
        }
        if (lastColumn != 0) {
            merge(lastColumn)
            shift(system.numBots)
            column()
        }
        flipTo(Harmonics.LOW)
        merge(1)
        goToBase()
        halt()
    }

    private fun linearFission() {
        val maxBotIndex = Integer.min(target.box.width, 20) - 1
        for (i in 0 until maxBotIndex) {
            val commands = ArrayList<Command>()
            for (j in 0 until i) {
                commands.add(Wait)
            }
            commands.add(Fission(NearCoordDiff(1, 0, 0), maxBotIndex - i - 1))
            system.timeStep(commands)
        }
    }

    private fun goToStart() {
        shift(target.box.left)
        val z = target.box.middle
        val y = target.box.bottom
        val trace1 = ArrayList<List<Command>>()
        for (i in 0 until y / 15) {
            val commands = ArrayList<Command>()
            for (k in 0 until system.numBots) {
                commands.add(SMove(LongCoordDiff(dy = 15)))
            }
            trace1.add(commands)
        }
        if (y % 15 != 0) {
            val commands = ArrayList<Command>()
            for (k in 0 until system.numBots) {
                commands.add(SMove(LongCoordDiff(dy = (y % 15))))
            }
            trace1.add(commands)
        }
        val trace2 = ArrayList<List<Command>>()
        for (i in 0 until z / 15) {
            val commands = ArrayList<Command>()
            for (k in 0 until system.numBots) {
                commands.add(SMove(LongCoordDiff(dz = 15)))
            }
            trace2.add(commands)
        }
        if (z % 15 != 0) {
            val commands = ArrayList<Command>()
            for (k in 0 until system.numBots) {
                commands.add(SMove(LongCoordDiff(dz = (z % 15))))
            }
            trace2.add(commands)
        }
        for (commands in trace1)
            system.timeStep(commands)
        for (commands in trace2)
            system.timeStep(commands)
    }

    private fun column() {
        down()
        for (i in 0..target.box.top) {
            isZForward = !isZForward
            up()
            build()
            for (j in 0 until target.box.depth - 1) {
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
        val timeStamp = system.timeStamp()
        atomicBuild()
        if (system.currentState.matrix.isEverybodyGrounded) {
            flipTo(Harmonics.LOW)
            return
        }
        system.rollBackTo(timeStamp)
        flipTo(Harmonics.HIGH)
        atomicBuild()
    }

    private fun atomicBuild() {
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

    private fun shift(by: Int) {
        var step = by / 15
        val dir = step > 0
        step = abs(step)
        while (step != 0) {
            atomicShift(if (dir) 15 else -15)
            if (dir) step-- else step++
        }
        val rest = by % 15
        if (rest != 0) {
            atomicShift(rest)
        }
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