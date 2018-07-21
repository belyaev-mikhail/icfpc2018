package icfpc2018

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import java.io.Reader

private val klaxon = Klaxon()

class Results(val elements: MutableMap<String, Result>) : MutableMap<String, Result> by elements {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: Reader): Results {
            val jsonObject = klaxon.parseJsonObject(json)
            @Suppress("UNCHECKED_CAST")
            val jsonMap = jsonObject.map as Map<String, JsonObject>
            val elements = jsonMap.map { it.key to Result.fromJson(it.value) }.toMap()
            return Results(elements.toMutableMap())
        }
    }


    fun addNewResult(task: String, solution: String, energy: Long, trace: String) {
        val solutions = getOrPut(task) { Result(mutableMapOf()) }
        solutions[solution] = Solution(energy, trace)
    }
}

data class Result(
        val solutions: MutableMap<String, Solution>
) : MutableMap<String, Solution> by solutions {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: JsonObject): Result {
            @Suppress("UNCHECKED_CAST")
            val soutionMap = (json.map as Map<String, JsonObject>)
            val solutions = soutionMap.map { it.key to klaxon.parseFromJsonObject<Solution>(it.value)!! }.toMap()
            return Result(solutions.toMutableMap())
        }
    }
}

data class Solution(
        val energy: Long,
        val trace: String
)
