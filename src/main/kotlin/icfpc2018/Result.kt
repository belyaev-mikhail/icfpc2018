package icfpc2018

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import java.io.File
import java.io.Reader
import java.io.StringReader

private val klaxon = Klaxon()

class Results(val elements: MutableMap<String, Result>) : MutableMap<String, Result> by elements {
    fun toJson() = klaxon.toJsonString(this)

    fun writeToDirectory(dir: String) {
        val file = File("$dir/results.json").apply { this.parentFile.mkdirs() }
        val writer = file.writer()
        writer.write(this.toJson())
        writer.flush()
    }

    companion object {
        fun fromJson(json: Reader): Results {
            val jsonObject = klaxon.parseJsonObject(json)
            @Suppress("UNCHECKED_CAST")
            val jsonMap = jsonObject.map as Map<String, JsonObject>
            val elements = jsonMap.map { it.key to Result.fromJson(it.value) }.toMap()
            return Results(elements.toMutableMap())
        }

        fun readFromDirectory(dir: String): Results {
            val file = File("$dir/results.json").apply { this.parentFile.mkdirs() }
            val reader = when {
                file.exists() -> file.reader()
                else -> StringReader("{}")
            }
            return fromJson(reader)
        }
    }


    fun addNewResult(task: String, solution: String, energy: Long, trace: String) {
        val solutions = getOrPut(task) { Result(mutableMapOf()) }
        solutions[solution] = Solution(energy, trace)
    }

    fun merge(other: Results): Results {
        val merged = mutableMapOf<String, Result>()
        val mergedKeys = this.keys.plus(other.keys).toSet()
        for (key in mergedKeys) {
            val thisResult = this[key]
            val otherResult = other[key]
            val mergedResult = when {
                thisResult != null && otherResult != null -> thisResult.merge(otherResult)
                thisResult != null -> thisResult
                otherResult != null -> otherResult
                else -> throw IllegalStateException()
            }
            merged[key] = mergedResult
        }
        return Results(merged)
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

    fun merge(other: Result): Result {
        val merged = mutableMapOf<String, Solution>()
        val mergedKeys = this.keys.plus(other.keys).toSet()
        for (key in mergedKeys) {
            val thisSolution = this[key]
            val otherSolution = other[key]

            val bestSolution = when {
                thisSolution != null && otherSolution != null -> when {
                    thisSolution.energy < otherSolution.energy -> thisSolution
                    else -> otherSolution
                }
                thisSolution != null -> thisSolution
                otherSolution != null -> otherSolution
                else -> throw IllegalStateException()
            }
            merged[key] = bestSolution
        }
        return Result(merged)
    }


    fun getSortedSolutions() = this.map { it.key to it.value }.sortedBy { (_, solution) -> solution.energy }.toList()
}

data class Solution(
        val energy: Long,
        val trace: String
)
