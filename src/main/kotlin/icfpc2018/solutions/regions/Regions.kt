package icfpc2018.solutions.regions

import icfpc2018.bot.state.Point

sealed class Region

data class Rectangle(val p1: Point, val p2: Point, val p3: Point, val p4: Point) : Region()

data class Section(val first: Point, val second: Point) : Region()

data class Voxel(val point: Point) : Region()
