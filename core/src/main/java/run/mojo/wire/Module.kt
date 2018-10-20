package run.mojo.wire

import com.google.common.collect.ImmutableMap

/**
 * Collection of Message and ImportAction descriptors.
 */
interface Module {
    val name: String
    val version: String
    val hash: Long

    val localId: Int

    val actions: ImmutableMap<String, FunctionDescriptor<*, *>>

    val messages: ImmutableMap<String, MessageDescriptor<*>>

    val packages: ImmutableMap<String, AbstractPackage>

    fun protoSchema()

    fun actionNamed(name: String): FunctionDescriptor<*, *>?

    fun actionByPath(name: String): FunctionDescriptor<*, *>?

    fun actionByClass(cls: Class<*>): FunctionDescriptor<*, *>?

    fun messageByClassName(cls: Class<*>): MessageDescriptor<*>?

    fun messageByClass(cls: Class<*>): MessageDescriptor<*>?
}


object ModuleManager {
    val THREAD_LOCAL = ThreadLocal<Array<ModuleLocal>>()
    var counter = 0

    fun localOf(localId: Int) {
        THREAD_LOCAL.get()
    }
}

class ModuleLocal(val module: Module) {

}