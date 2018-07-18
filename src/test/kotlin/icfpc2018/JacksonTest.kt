package icfpc2018

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.testng.annotations.Test
import ru.spbstu.ktuples.Tuple
import ru.spbstu.ktuples.Tuple3
import ru.spbstu.ktuples.jackson.KTuplesModule
import kotlin.test.assertEquals

data class Bar(
        val lst: List<String>,
        val tup: Tuple3<Int, Double, String>
)

data class Foo(
        val bar: Bar,
        val quiz: Int?,
        val name: String
)

class JacksonTest {

    val om = ObjectMapper().apply { registerModules(KTuplesModule(), KotlinModule()) }

    @Test
    fun test() {

        val json =
                // language=JSON
                """
                    {
                        "bar": {
                          "lst": ["hello", "world"],
                          "tup": [1, 3.0, "haha"]
                        },
                        "quiz": 43,
                        "name": "Daniil"
                    }
                """
        val obj = Foo(Bar(listOf("hello", "world"), Tuple(1, 3.0, "haha")), 43, "Daniil")

        assertEquals(
                obj,
                om.readValue(json)
        )

        assertEquals(
                om.writeValueAsString(om.readTree(json)), // this is just basic formatting
                om.writeValueAsString(obj)
        )


    }

}
