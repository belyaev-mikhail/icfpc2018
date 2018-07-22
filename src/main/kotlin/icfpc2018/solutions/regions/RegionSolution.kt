package icfpc2018.solutions.regions

import icfpc2018.Config
import icfpc2018.bot.commands.*
import icfpc2018.bot.state.*
import icfpc2018.solutions.BotManager
import icfpc2018.solutions.Solution
import icfpc2018.solutions.linearFission
import icfpc2018.solutions.sections.indices


class RegionSolution(val target: Model, val system: System) : Solution {

    private val regions = split(target)

    private fun linearFission() {
        val maxBotIndex = Integer.min(target.box.width, Config.maxBots) - 1
        for (i in 0 until maxBotIndex) {
            val commands = ArrayList<Command>()
            for (j in 0 until i) {
                commands.add(Wait)
            }
            commands.add(Fission(NearCoordDiff(1, 0, 0), maxBotIndex - i - 1))
            system.timeStep(commands)
        }
    }

    override fun solve() {
        linearFission()
        flipTo(Harmonics.HIGH)
        for (i in 0 until target.box.top) {
            layer(regions[i])
            if (i != target.box.top - 2)
                goToBase()
        }
        flipTo(Harmonics.LOW)
        goToBase()
        merge(1)
        halt()
    }

    private fun flip() {
        val commands = ArrayList<Command>()
        commands.add(Flip)
        for (i in 1 until system.numBots)
            commands.add(Wait)
        system.timeStep(commands)
    }

    private fun flipTo(harmonics: Harmonics) = if (system.currentState.harmonics != harmonics) flip() else Unit

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

    private fun layer(layer: List<Region>) {
        val manager = BotManager(system)
        for (region in layer) {
            val task = when (region) {
                is Rectangle -> RectangleTask(region, manager)
                is Section -> SectionTask(region, manager)
                is Voxel -> VoxelTask(region, manager)
            }
            manager.add(task)
        }
        manager.apply()
    }

    private fun goToBase() {
        val manager = BotManager(system)
        manager.add(GoToBase(system, manager))
        manager.apply()
    }

    private fun halt() = system.timeStep(listOf(Halt))

    companion object {
        private fun Section.toVoxelIfCan() = if (first == second) Voxel(first) else this

        fun split(model: Model): List<List<Region>> {
            val regions = ArrayList<List<Region>>()
            for (y in model.indices) {
                val layer = ArrayList<Region>()
                for (z in model.indices) {
                    val strawSections = ArrayList<Section>()
                    var begin: Point? = null
                    for (x in model.indices) {
                        val isFill = model[x, y, z]
                        if (isFill && begin == null) {
                            begin = Point(x, y, z)
                        }
                        if (!isFill && begin != null) {
                            val end = Point(x - 1, y, z)
                            val section = Section(begin, end)
                            strawSections.add(section)
                            begin = null
                        }
                    }
                    if (begin != null) {
                        val end = Point(model.size - 1, y, z)
                        val section = Section(begin, end)
                        strawSections.add(section)
                    }
                    val converted = strawSections.map { it.toVoxelIfCan() }
                    layer.addAll(converted)
                }
                regions.add(layer)
            }
            return regions
        }
    }
}