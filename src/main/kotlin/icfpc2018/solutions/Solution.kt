package icfpc2018.solutions

import icfpc2018.bot.state.Model
import icfpc2018.bot.state.System
import icfpc2018.solutions.boundedSlices.BoundedSlices
import icfpc2018.solutions.groundedBoundedSlices.GroundedBoundedSlices
import icfpc2018.solutions.groundedSlices.GroundedSlices
import icfpc2018.solutions.portfolio.Portfolio
import icfpc2018.solutions.sections.Sections
import icfpc2018.solutions.slices.Slices
import icfpc2018.solutions.tripleSlices.TripleSlices

interface Solution {
    fun solve()
}

fun getSolutionByName(name: String, target: Model, system: System) = when (name) {
    "sections" -> Sections(target, system)
    "slices" -> Slices(target, system)
    "bounded_slices" -> BoundedSlices(target, system)
    "grounded_slices" -> GroundedSlices(target, system)
    "portfolio" -> Portfolio(target, system)
    "triple_slices" -> TripleSlices(target, system)
    "regions" -> Regions(target, system)
    "grounded_bounded_slices" -> GroundedBoundedSlices(target, system)
    else -> throw IllegalArgumentException()
}
