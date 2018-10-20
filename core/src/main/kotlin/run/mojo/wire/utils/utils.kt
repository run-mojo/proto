package run.mojo.wire.utils

import java.util.Collections
import java.util.HashMap

object GuavaTypeMap {
    private val TYPE_TO_GUAVA_TYPE: Map<String, String>

    init {
        val m = HashMap<String, String>()

        m["java.util.NavigableSet"] = "ImmutableSortedSet"
        m["java.util.NavigableMap"] = "ImmutableSortedMap"
        m["java.util.SortedSet"] = "ImmutableSortedSet"
        m["java.util.SortedMap"] = "ImmutableSortedMap"
        m["java.util.Set"] = "ImmutableSet"
        m["java.util.Map"] = "ImmutableMap"
        m["java.util.Collection"] = "ImmutableList"
        m["java.util.List"] = "ImmutableList"

        m["com.google.common.collect.ImmutableSet"] = "ImmutableSet"
        m["com.google.common.collect.ImmutableSortedSet"] = "ImmutableSortedSet"
        m["com.google.common.collect.ImmutableMap"] = "ImmutableMap"
        m["com.google.common.collect.ImmutableBiMap"] = "ImmutableBiMap"
        m["com.google.common.collect.ImmutableSortedMap"] = "ImmutableSortedMap"
        m["com.google.common.collect.ImmutableList"] = "ImmutableList"
        m["com.google.common.collect.ImmutableCollection"] = "ImmutableList"
        m["com.google.common.collect.ImmutableTable"] = "ImmutableTable"

        TYPE_TO_GUAVA_TYPE = Collections.unmodifiableMap(m)
    }

    fun getGuavaTypeName(fqn: String): String {
        val target = TYPE_TO_GUAVA_TYPE[fqn]
        return target ?: "ImmutableList"
    }
}