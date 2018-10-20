package run.mojo.wire

import com.squareup.wire.ProtoAdapter

import java.util.function.Function

/**  */
@Suppress("UNCHECKED_CAST")
object AdapterRepo {
    private val protoProviders = mutableListOf(
        GeneratedProvider() as Function<Class<*>, ProtoAdapter<*>>
    )

    private val protoCache = mutableMapOf<String, ProtoAdapter<*>>()

    @Synchronized
    fun <T> proto(cls: Class<T>): ProtoAdapter<T>? {
        protoCache[cls.canonicalName].apply {
            return@proto this as ProtoAdapter<T>
        }
        protoProviders.forEach {
            it.apply(cls).apply {
                return@proto it as ProtoAdapter<T>
            }
        }
        return null
    }

    private fun enclosing(cls: Class<*>): String? {
        if (cls.enclosingClass == null) {
            return null
        } else {
            val enclosing = enclosing(cls)
            if (enclosing != null) {
                return enclosing + "." + cls.simpleName
            } else {
                return cls.simpleName
            }
        }
    }

    private fun generatedOuter(cls: Class<*>): String {
        val pkg = cls.`package`.name
        val enclosing = enclosing(cls)
        return pkg + "Wire_" + enclosing
    }

    private class GeneratedProvider : Function<Class<*>, ProtoAdapter<*>?> {
        override fun apply(t: Class<*>): ProtoAdapter<*>? {
            val outerName = generatedOuter(t)
            try {
                val outer = Class.forName(outerName) ?: return null
                val protoField = outer.getDeclaredField("PROTO") ?: return null
                val result = protoField.get(outer)
                if (result !is ProtoAdapter<*>) {
                    return null
                } else {
                    return result
                }
            } catch (e: Throwable) {
                return null
            }
        }
    }
}

