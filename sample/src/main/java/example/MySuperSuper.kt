package example

open class MySuperSuperSuper<C>(var list: List<C>? = null)

/**
 *
 */
//@Wire
open class MySuperSuper<I, C>(
    var id: I? = null,
    var count: C? = null
): MySuperSuperSuper<C>()