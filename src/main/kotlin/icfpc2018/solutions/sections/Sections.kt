package icfpc2018.solutions.sections

import icfpc2018.bot.state.Model
import icfpc2018.bot.state.Point
import icfpc2018.bot.state.System
import icfpc2018.info
import icfpc2018.log
import icfpc2018.solutions.Solution

data class Section(val begin: Point, val end: Point)

val Model.indices: IntRange
    get() = 0 until size

class Sections(target: Model, system: System) : Solution {

    private val sections = crash(target)

    private val maxStrawsInLayer = sections.map { it.size }.max()!!

    override fun solve() {
        log.info { maxStrawsInLayer.toString() }
    }

    companion object {
        fun crash(model: Model): List<List<List<Section>>> {
            val sections = ArrayList<List<List<Section>>>()
            for (y in model.indices) {
                val layerSections = ArrayList<List<Section>>()
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
                    layerSections.add(strawSections)
                }
                sections.add(layerSections)
            }
            return sections
        }
    }
}