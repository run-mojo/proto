@file:JvmName("Main")

package example

import run.mojo.compiler.ModelTransformer

fun main(vararg args: String) {

    val processor = ModelTransformer()

    val model2 = processor.register(MyMessage::class.java)

    println(Records(10, emptyList<Int>()).copy(totalCount = 20, records = listOf(10, 20)))

}
