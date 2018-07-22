package icfpc2018

import org.testng.annotations.Test

class HelloTest {

    fun sbb(number: Int, from: Int, to: Int) = (((1 shl (to - from + 1)) - 1) and (number ushr from))
    inline fun Int.subBits(range: IntRange) = sbb(this, 7 - range.endInclusive, 7 - range.start)

    @Test
    fun test() {
        println(0b10111100.subBits(0..7).toString(2))
        println(0b110110.subBits(2..3).toString(2))
    }
}
