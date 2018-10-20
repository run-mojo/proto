package example

import lombok.Data
import lombok.Value
import run.mojo.wire.Wire

/**
 *
 */
@Data
@Value
data class Records<T>(
    @Wire(tag = 10)
    var totalCount: Int = 0,
    val records: List<T>
)


/**
 *
 */
@Data
@Value
class RecordsClass<T>(
    var totalCount: Int = 0,
    var records: List<T>? = null
)

fun create() {
    val r = Records(totalCount = 0, records = listOf(""))


}
