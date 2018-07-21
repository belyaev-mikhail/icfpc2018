package icfpc2018.bot.util

import java.util.*

class AStar<T>(
        val neighbours: (T) -> Collection<T>,
        val heuristic: (T) -> Long,
        val goal: (T) -> Boolean) {

    inner class History(val value: T, val len: Int = 0, val previous: History? = null) {
        fun next(value: T) = History(value, len + 1, this)

        override fun equals(other: Any?) = other is AStar<*>.History && other.value == value
        override fun hashCode(): Int = value?.hashCode() ?: 0
    }

    val closed: MutableSet<History> = mutableSetOf()
    val open: PriorityQueue<History> = PriorityQueue(Comparator.comparing<History, Long> { it.len + heuristic(it.value) })
    val roots: MutableMap<T, History> = mutableMapOf()

    fun run(start: T): History {
        open += History(start)
        while(open.isNotEmpty()) {
            val current = open.remove()

            if(goal(current.value)) return current

            open.poll()
            closed += current

            for(neighbour in neighbours(current.value)) {

                val hist = current.next(neighbour)
                if(hist in closed) continue

                if(hist !in open) {
                    open += hist
                }
                else if(hist.len >= roots[neighbour]!!.len) continue

                roots[neighbour] = hist
            }
        }
        throw IllegalStateException()
    }


}