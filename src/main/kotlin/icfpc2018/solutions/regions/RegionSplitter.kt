package icfpc2018.solutions.regions

import icfpc2018.bot.state.Model
import icfpc2018.bot.state.Point
import org.eclipse.elk.alg.common.Point as RealPoint
import icfpc2018.bot.state.System
import javax.script.ScriptEngineManager
import javax.script.Invocable
import java.io.FileReader
import jdk.nashorn.api.scripting.NashornScriptEngine
import java.io.File
import org.eclipse.elk.alg.common.RectilinearConvexHull
import org.eclipse.elk.core.math.ElkRectangle
import kotlin.collections.ArrayList
import java.util.LinkedList


class RegionSplitter(val target: Model, val system: System) {

    val rectangles = split(target)

    companion object {
        fun split(model: Model): List<List<Region>> {
            val layerRectangles = ArrayList<List<Region>>();
            for (y in 0 until model.size) {
                val layerPoints = HashSet<Pair<Int, Int>>()
                for (z in 0 until model.size)
                    for (x in 0 until model.size)
                        if (model[x, y, z]) {
                            layerPoints.add(x to z)
                        }
                val groupedPoints = groupSetOfPoints(layerPoints)
                val pointPolygons = groupedPoints.map { RectilinearConvexHull.of(it) }
                val pointRectangles = pointPolygons.map { it.splitIntoRectangles() }
                val pointWithHolesPolygons = pointRectangles.map { it to holesInPolygon(it, model, y) }
                val completeRectangles = pointWithHolesPolygons.filter { it.second.isEmpty() }.map { it.first }
                val polyWithHoles = pointWithHolesPolygons.map { it.second }.zip(pointPolygons)
                        .filter { it.first.isNotEmpty() }
                        .map { (holes, poly) -> poly to holes }
                val polyWithHolesPoints = polyWithHoles.map { (poly, holes) ->
                    listOf(getPoints(poly, true)) + holes.map { getPoints(it, false) }
                }
                val rectanglesWithHoles = polyWithHolesPoints.map { splitOnRectangles(it) }
                val completed = completeRectangles.flatten()
                        .map { getPointsFromRectangle(it) } + rectanglesWithHoles.flatten()
                val regions = completed.map { buildRegions(it, y) }
                layerRectangles.add(regions)
            }
            return layerRectangles
        }

        fun buildRegions(cornerPoints: List<Int>, currentY: Int): Region {
            val (bottomLeftX, bottomLeftZ, topRightX, topRightZ) = cornerPoints

            if (bottomLeftX == topRightX && bottomLeftZ == topRightZ)
                return Voxel(Point(bottomLeftX, currentY, bottomLeftZ))
            if (bottomLeftX == topRightX || bottomLeftZ == topRightZ)
                return Section(Point(bottomLeftX, currentY, bottomLeftZ), Point(topRightX, currentY, topRightZ))

            val p1 = Point(bottomLeftX, currentY, bottomLeftZ)
            val p2 = Point(bottomLeftX, currentY, topRightZ)
            val p3 = Point(topRightX, currentY, topRightZ)
            val p4 = Point(topRightX, currentY, bottomLeftZ)
            return Rectangle(p1, p2, p3, p4)

        }

        fun getPoints(hull: RectilinearConvexHull, clockwise: Boolean): List<List<Int>> {
            val pointSet = hull.hull
            val res = pointSet
                    .map { listOf(it.x.toInt(), it.y.toInt()) }
//                    .fold(listOf(-100, -100) to mutableListOf<List<Int>>()) { (prev, res), e ->
//                        if (e != prev) res.add(e); e to res
//                    }.second
            if (clockwise)
                return res
            else
                return res.asReversed()
        }

        fun getPointsFromRectangle(rect: ElkRectangle): List<Int> {
            val xFrom = rect.x.toInt()
            val xTo = (rect.x + rect.width).toInt()
            val zFrom = rect.y.toInt()
            val zTo = (rect.y + rect.height).toInt()
            return listOf(xFrom, zFrom, xTo, zTo)
        }

        fun holesInPolygon(rectangles: List<ElkRectangle>, model: Model, y: Int): List<RectilinearConvexHull> {
            val polygonHoles = HashSet<Pair<Int, Int>>()
            for (rect in rectangles) {
                val (xFrom, zFrom, xTo, zTo) = getPointsFromRectangle(rect)
                for (x in xFrom..xTo)
                    for (z in zFrom..zTo)
                        if (!model[x, y, z])
                            polygonHoles.add(x to z)

            }
            val groupedHoles = groupSetOfPoints(polygonHoles)
            return groupedHoles.map { RectilinearConvexHull.of(it) }
        }

        fun groupSetOfPoints(points: Set<Pair<Int, Int>>): List<List<RealPoint>> {
            val pointGroups = ArrayList<List<RealPoint>>()
            var ungroupedPoints: Set<Pair<Int, Int>> = points
            while (ungroupedPoints.isNotEmpty()) {
                val pointGroup = floodFill(ungroupedPoints)
                ungroupedPoints -= pointGroup
                pointGroups.add(pointGroup.map { RealPoint(it.first.toDouble(), it.second.toDouble()) })
            }
            return pointGroups
        }

        fun floodFill(allPoints: Set<Pair<Int, Int>>): Set<Pair<Int, Int>> {
            val visitedPoints = HashSet<Pair<Int, Int>>()
            val queue = LinkedList<Pair<Int, Int>>()
            queue.add(allPoints.first())

            while (queue.isNotEmpty()) {
                val point = queue.poll()
                if (visitedPoints.contains(point)) continue
                if (!allPoints.contains(point)) continue

                visitedPoints.add(point)

                queue.add(point.first + 1 to point.second)
                queue.add(point.first - 1 to point.second)
                queue.add(point.first to point.second + 1)
                queue.add(point.first to point.second - 1)
            }

            return visitedPoints

        }

        val rectangleSplitter by lazy {
            val engine = ScriptEngineManager().getEngineByName("nashorn") as NashornScriptEngine
            val scriptFileName = "npm-fucking/browserify.js"
            val scriptFile = FileReader(scriptFileName)
            engine.eval("var global = new Object();")
            engine.eval(scriptFile)
            val global = engine.get("global")
            val invocable = engine as Invocable
            { args: List<List<List<Int>>> -> invocable.invokeMethod(global, "decomposeRegion", args) }
        }

        fun splitOnRectangles(points: List<List<List<Int>>>): ArrayList<List<Int>> {
            val rectangles = rectangleSplitter(points)
            val result = ArrayList<List<Int>>()
            for (rect in (rectangles as Map<*, *>).values) {
                val rectCoordinates = rect as Map<*, *>
                val bottomLeft = rectCoordinates["0"] as Map<*, *>
                val topRight = rectCoordinates["1"] as Map<*, *>
                val bottomLeftX = (bottomLeft["0"] as Double).toInt()
                val bottomLeftZ = (bottomLeft["1"] as Double).toInt()
                val topRightX = (topRight["0"] as Double).toInt()
                val topRightZ = (topRight["1"] as Double).toInt()
                result.add(listOf(bottomLeftX, bottomLeftZ, topRightX, topRightZ))
            }
            return result
        }

    }
}

fun main(args: Array<String>) {
    val filename = "models/FA004_tgt.mdl"
    val targetModelFile = File(filename).inputStream()
    val targetModel = Model.readMDL(targetModelFile)
    RegionSplitter.split(targetModel)
}
