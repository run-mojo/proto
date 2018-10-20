@file:JvmName("Main")

package example

import run.mojo.wire.compiler.ModelReflectionBuilder

fun main(vararg args: String) {

    val processor = ModelReflectionBuilder()



    val model2 = processor.register(MyMessage::class.java)

    println(Records(10, emptyList<Int>()).copy(totalCount = 20, records = listOf(10, 20)))

}
