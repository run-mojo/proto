package run.mojo.wire.compiler

import okio.ByteString
import run.mojo.wire.JavaKind
import run.mojo.wire.model.*
import java.lang.IllegalStateException

import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.tools.Diagnostic
import java.lang.Package
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.TreeMap

/**
 * Build up an object model from either "java.lang.model" or from "java.lang.reflect"
 */
abstract class ModelBuilder {

    private val declared = TreeMap<String, DeclaredModel>()
//    val messages = TreeMap<String, EnclosingModel>()
//    val enums = TreeMap<String, EnumModel>()
    val packages = TreeMap<String, PackageModel>()

    fun getEnclosing(name: String): EnclosingModel? {
        return declared[name]?.let {
            return if (it is EnclosingModel) it else null
        }
    }

    fun getEnum(name: String): EnumModel? {
        return declared[name]?.let {
            return if (it is EnumModel) it else null
        }
    }

    fun add(model: DeclaredModel) {
        declared.put(model.name, model)?.let { old ->
            // Ensure proper enclosing reference.
            old.enclosing?.let {
                it.nested[model.name] = model
            }

            if (old is EnclosingModel) {
                if (model is EnclosingModel) {
                    old.nested.values.forEach { it.enclosing = model }
                } else {
                    throw IllegalStateException("duplicate conflicting types at name '${model.name}'")
                }
            }
        }
    }

    open fun printMessage(kind: Diagnostic.Kind, msg: CharSequence) {
        System.err.println(kind.name + " -> " + msg.toString())
    }

    fun packageOf(pkg: Package?): PackageModel {
        val name: String
        if (pkg == null) {
            name = ""
        } else {
            name = pkg.name
        }
        var mirror: PackageModel? = packages[name]
        if (mirror == null) {
            mirror = PackageModel.ofPackage(pkg)
            packages[name] = mirror
        }
        return mirror
    }

    fun packageOf(pkg: PackageElement?): PackageModel {
        val name: String
        if (pkg == null) {
            name = ""
        } else {
            name = pkg.qualifiedName.toString()
        }
        var mirror: PackageModel? = packages[name]
        if (mirror == null) {
            mirror = PackageModel.ofElement(pkg!!)
            packages[name] = mirror
        }
        return mirror
    }

    abstract fun register(type: Any): WireModel?

    fun nameOf(obj: Any?): String {
        if (obj == null) {
            return ""
        }
        if (obj is TypeElement) {
            return obj.qualifiedName.toString()
        } else if (obj is VariableElement) {
            return obj.simpleName.toString()
        } else if (obj is ExecutableElement) {
            return obj.simpleName.toString()
        } else if (obj is Class<*>) {
            return obj.canonicalName
        } else if (obj is Method) {
            return obj.name
        } else if (obj is Package) {
            return obj.name
        } else if (obj is Field) {
            return obj.name
        }

        return obj.toString()
    }

    fun simpleNameOf(obj: Any?): String {
        if (obj == null) {
            return ""
        }
        if (obj is TypeElement) {
            return obj.simpleName.toString()
        } else if (obj is VariableElement) {
            return obj.simpleName.toString()
        } else if (obj is ExecutableElement) {
            return obj.simpleName.toString()
        } else if (obj is Class<*>) {
            return obj.simpleName
        } else if (obj is Method) {
            return obj.name
        } else if (obj is Package) {
            return obj.name
        } else if (obj is Field) {
            return obj.name
        }

        return obj.toString()
    }

    fun isInvalidPropMethod(methodName: String): Boolean {
        // Ignore common methods.
        when (methodName) {
            "toString", "equals", "clone" -> return true
        }

        return false
    }

    companion object {
        val ERROR_NONEXISTENT = "error.NonExistentClass"
        val JAVA_LANG_OBJECT = "java.lang.Object"

        val LOMBOK_DATA = "lombok.Data"
        val LOMBOK_VALUE = "lombok.Value"
        val LOMBOK_BUILDER = "lombok.MessageBuilder"
        val KOTLIN_METADATA = "kotlin.Metadata"

        val JAVA_LANG_STRING = "java.lang.String"
        val JAVA_LANG_BOOLEAN = "java.lang.Boolean"
        val JAVA_LANG_INTEGER = "java.lang.Integer"
        val JAVA_LANG_LONG = "java.lang.Long"
        val JAVA_LANG_FLOAT = "java.lang.Float"
        val JAVA_LANG_DOUBLE = "java.lang.Double"
        val JAVA_LANG_BYTE = "java.lang.Byte"
        val JAVA_LANG_SHORT = "java.lang.Short"
        val JAVA_LANG_CHARACTER = "java.lang.Character"
        val JAVA_UTIL_LIST = "java.util.List<?>"
        val JAVA_UTIL_LIST_UNTYPED = "java.util.List"
        val JAVA_UTIL_ARRAYLIST = "java.util.ArrayList<?>"
        val JAVA_UTIL_ARRAYLIST_UNTYPED = "java.util.ArrayList"
        val JAVA_UTIL_HASHMAP = "java.util.HashMap<?,?>"
        val JAVA_UTIL_HASHMAP_UNTYPED = "java.util.HashMap"
        val JAVA_UTIL_MAP_UNTYPED = "java.util.Map"
        val JAVA_UTIL_QUEUE = "java.util.Queue<?>"
        val JAVA_UTIL_QUEUE_UNTYPED = "java.util.Queue"
        val JAVA_UTIL_SET = "java.util.Set<?>"
        val JAVA_UTIL_SET_UNTYPED = "java.util.Set"
        val JAVA_LANG_ENUM = "java.lang.Enum<?>"

        val BOOL = PrimitiveModel(JavaKind.BOOL)
        val BOOL_BOXED = PrimitiveModel(JavaKind.BOXED_BOOL)
        val BYTE = PrimitiveModel(JavaKind.BYTE)
        val BYTE_BOXED = PrimitiveModel(JavaKind.BOXED_BYTE)
        val SHORT = PrimitiveModel(JavaKind.SHORT)
        val SHORT_BOXED = PrimitiveModel(JavaKind.BOXED_SHORT)
        val CHAR = PrimitiveModel(JavaKind.CHAR)
        val CHAR_BOXED = PrimitiveModel(JavaKind.BOXED_CHAR)
        val INT = PrimitiveModel(JavaKind.INT)
        val INT_BOXED = PrimitiveModel(JavaKind.BOXED_INT)
        val LONG = PrimitiveModel(JavaKind.LONG)
        val LONG_BOXED = PrimitiveModel(JavaKind.BOXED_LONG)
        val FLOAT = PrimitiveModel(JavaKind.FLOAT)
        val FLOAT_BOXED = PrimitiveModel(JavaKind.BOXED_FLOAT)
        val DOUBLE = PrimitiveModel(JavaKind.DOUBLE)
        val DOUBLE_BOXED = PrimitiveModel(JavaKind.BOXED_DOUBLE)

        val STRING = StringModel(String::class.java.canonicalName)
        val BYTE_STRING = StringModel(ByteString::class.java.canonicalName)

        fun getterName(field: FieldModel, fluent: Boolean): String {
            return if (fluent) {
                field.name
            } else if (field.model.javaKind == JavaKind.BOOL) {
                "is" + field.name.substring(0, 1).toUpperCase() + field.name.substring(1)
            } else {
                "get" + field.name.substring(0, 1).toUpperCase() + field.name.substring(1)
            }
        }

        fun setterName(field: FieldModel, fluent: Boolean): String {
            return if (fluent) {
                field.name
            } else {
                "set" + field.name.substring(0, 1).toUpperCase() + field.name.substring(1)
            }
        }
    }
}
