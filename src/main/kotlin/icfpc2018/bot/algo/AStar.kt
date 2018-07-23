package icfpc2018.bot.algo

import icfpc2018.log
import java.util.*

class AStar<T>(
        val neighbours: (T) -> Iterable<T>,
        val heuristic: (T) -> Int,
        val goal: (T) -> Boolean) {

    inner class History(val value: T, val len: Int = 0, val previous: History? = null) {
        fun next(value: T) = History(value, len + 1, this)

        override fun equals(other: Any?) = other is AStar<*>.History && other.value == value
        override fun hashCode(): Int = value?.hashCode() ?: 0

        fun asList(): List<T> {
            val mut: MutableList<T> = mutableListOf()
            var current = this ?: null
            while(current != null) {
                mut += current.value
                current = current.previous
            }
            return mut.reversed()
        }

        override fun toString(): String {
            return "History(value=$value, len=$len)"
        }

        val score by lazy { len + heuristic(value) }

    }

    val closed: MutableSet<History> = mutableSetOf()
    val open: PriorityQueue<History> = PriorityQueue(Comparator.comparing(AStar<T>.History::score))
    val roots: MutableMap<T, History> = mutableMapOf()

    fun run(start: T): History? {
        if(goal(start)) return History(start)

        open += History(start)
        while(open.isNotEmpty()) {
            val current = open.remove()

            if(goal(current.value)) {
                return current
            }

            closed += current

            for(neighbour in neighbours(current.value)) {

                val hist = current.next(neighbour)
                if(hist in closed) continue

                val oldScore = roots[neighbour]?.score ?: Int.MAX_VALUE
                if(hist.score >= oldScore) continue

                open += hist
                roots[neighbour] = hist
            }
        }
        return null
    }


}