package example

import run.mojo.wire.Wire
import java.util.*

/**
 *
 */
@Wire
data class Records<T>(
    @Wire(tag = 10)
    val totalCount: Int = 0,
    var records: List<T> = Collections.emptyList()
)

interface Base {
    val code: Int

    fun isError() = code != 0
}

/**
 *
 */
//@Wire
class RecordsClass<T>(
    override val code: Int,
    var totalCount: Int = 0,
    var name: String? = null,
    var records: List<T>
) : Base

fun create() {
//    val r = Records(totalCount = 0, records = listOf(""))
//    val r2 = Records(totalCount = 10, records = listOf(10, 11))

}
