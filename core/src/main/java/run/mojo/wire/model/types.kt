package run.mojo.wire.model

import com.google.common.base.Preconditions
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.wire.schema.ProtoType
import run.mojo.wire.FlatKind
import run.mojo.wire.JavaKind
import run.mojo.wire.JsonKind
import java.util.*
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

/** Represents a user defined "declared" type which is a message or an enum.  */
interface DeclaredModel {

    /**
     * Type that encloses this type.
     *
     * @return
     */
    var enclosing: EnclosingModel?

    val packageName: String

    val name: String

    val simpleName: String

    val relativeName: String
}

interface GenerationModel {
    val messageName: TypeName

    val outerName: TypeName

    val protoName: ClassName

    val jsonName: ClassName

    val
}


/**  */
class PackageModel private constructor(
    val type: Any?,
    val name: String,
    val simpleName: String,
    val nested: TreeMap<String, DeclaredModel> = TreeMap(),
    var parent: PackageModel? = null,
    var children: TreeMap<String, PackageModel> = TreeMap()
) {

    fun isNameTaken(name: String) = nested.containsKey(name)

    fun isNameAvailable(name: String) = !nested.containsKey(name)

    companion object {
        fun ofElement(element: PackageElement): PackageModel {
            return PackageModel(
                element, element.qualifiedName.toString(), element.simpleName.toString()
            )
        }

        fun ofType(type: Any?, name: String?): PackageModel {
            var name = name
            if (name == null) {
                name = ""
            }
            val parts = name.split("[.]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val simpleName: String
            if (parts.size > 0) {
                simpleName = parts[parts.size - 1]
            } else {
                simpleName = ""
            }
            return PackageModel(type, name, simpleName)
        }

        fun ofPackage(pkg: Package?): PackageModel {
            return ofType(pkg, if (pkg != null) pkg.name else "")
        }
    }
}


/**  */
abstract class WireModel(val javaKind: JavaKind, val protoType: ProtoType) {

    init {
        Preconditions.checkNotNull(javaKind, "javaKind is null")
        Preconditions.checkNotNull(protoType, "protoType is null")
    }

    open val packageName: String
        get() = ""

    open val name: String
        get() = ""

    open val simpleName: String
        get() = ""

    open val relativeName: String
        get() = ""

    /** Json type.  */
    open val jsonKind: JsonKind
        get() = JsonKind.OBJECT

    /** Flatbuffer type.  */
    open val flatKind: FlatKind
        get() = FlatKind.TABLE

    /** @return
     */
    open val isEnum: Boolean
        get() = false

    /** @return
     */
    open val isEnclosing: Boolean
        get() = false

    open val isMap: Boolean
        get() = false

    open val isList: Boolean
        get() = false

    open val isRepeated: Boolean
        get() = isList

    open val isPacked: Boolean
        get() = false

    open val isMessage: Boolean
        get() = false

    open val isTemplate: Boolean
        get() = false

    open val isImpl: Boolean
        get() = false

    open fun toTypeName(boxed: Boolean): TypeName {
        return ClassName.get(javaKind.asClass(true))
    }
}


/**  */
class PrimitiveModel(javaType: JavaKind) : WireModel(javaType, asProtoType(javaType)) {

    override fun toString(): String {
        return javaKind.name
    }

    companion object {

        private fun asProtoType(javaType: JavaKind): ProtoType {
            when (javaType) {
                JavaKind.BOOL, JavaKind.BOXED_BOOL -> return ProtoType.BOOL

                JavaKind.BYTE, JavaKind.BOXED_BYTE, JavaKind.SHORT, JavaKind.BOXED_SHORT, JavaKind.CHAR, JavaKind.BOXED_CHAR, JavaKind.INT, JavaKind.BOXED_INT -> return ProtoType.INT32

                JavaKind.LONG, JavaKind.BOXED_LONG -> return ProtoType.INT64

                JavaKind.FLOAT, JavaKind.BOXED_FLOAT -> return ProtoType.FLOAT

                JavaKind.DOUBLE, JavaKind.BOXED_DOUBLE -> return ProtoType.DOUBLE

                JavaKind.BYTES -> return ProtoType.BYTES

                JavaKind.STRING -> return ProtoType.STRING
            }
            return ProtoType.get("UnknownType")
        }
    }
}

/**  */
class StringModel(var type: Any) : WireModel(JavaKind.STRING, ProtoType.STRING) {

    override fun toString(): String {
        return javaKind.name
    }
}


/**  */
class BytesModel(var type: Any) : WireModel(JavaKind.BYTES, ProtoType.BYTES) {

    override fun toString(): String {
        return javaKind.name
    }
}

/**  */
class ListModel(type: JavaKind, val component: WireModel) : WireModel(type, component.protoType) {

    override val isList: Boolean
        get() = true

    override val isTemplate: Boolean
        get() = component.isTemplate

    override val isRepeated: Boolean
        get() = true

    override fun toTypeName(boxed: Boolean): TypeName {
        return when (javaKind) {
            JavaKind.ARRAY -> ArrayTypeName.of(component.toTypeName(true))
            JavaKind.LIST -> ParameterizedTypeName.get(ClassName.get(List::class.java), component.toTypeName(boxed))
            JavaKind.SET -> ParameterizedTypeName.get(ClassName.get(Set::class.java), component.toTypeName(boxed))
            JavaKind.QUEUE -> ParameterizedTypeName.get(ClassName.get(Queue::class.java), component.toTypeName(boxed))
            else -> ClassName.bestGuess(name)
        }
    }

    override fun toString(): String {
        return javaKind.toString() + "<" + component.toString() + ">"
    }
}

/**  */
class MapModel(val key: WireModel, val value: WireModel) : WireModel(
    JavaKind.MAP, ProtoType.get(
        "map<" + key.protoType.toString() + ", " + value.protoType.toString() + ">"
    )
) {

    override val isTemplate: Boolean
        get() = key.isTemplate || value.isTemplate

    override val isMap: Boolean
        get() = true

    override fun toTypeName(boxed: Boolean): TypeName {
        return ParameterizedTypeName.get(
            ClassName.get(Map::class.java), key.toTypeName(boxed), value.toTypeName(boxed)
        )
    }

    override fun toString(): String {
        return javaKind.toString() + "<" + key.toString() + ", " + value.toString() + ">"
    }
}

data class EnumConstant(
    val ordinal: Int,
    val tag: Int,
    val name: String,
    val relativeName: String
)

/**  */
class EnumModel(
    override var enclosing: EnclosingModel?,
    override val name: String,
    override val simpleName: String,
    override val packageName: String,
    override val relativeName: String,
    val type: Any,
    val constants: List<EnumConstant>
) : WireModel(JavaKind.ENUM, ProtoType.get(name)), DeclaredModel {
    init {
        enclosing?.let { it.nested[this.name] = this }
    }

    override val isEnum: Boolean
        get() = true

    override fun toString(): String {
        return javaKind.toString() + "<" + name + ">"
    }

    companion object {

        fun ofElement(
            enclosing: EnclosingModel?,
            packageName: String,
            relativeName: String,
            element: TypeElement,
            constants: List<EnumConstant>
        ): EnumModel {
            return EnumModel(
                enclosing,
                element.qualifiedName.toString(),
                element.simpleName.toString(),
                packageName,
                relativeName,
                element,
                constants
            )
        }

        fun ofClass(
            enclosing: EnclosingModel?,
            packageName: String,
            relativeName: String,
            cls: Class<*>,
            constants: List<EnumConstant>
        ): EnumModel {
            return EnumModel(
                enclosing,
                cls.canonicalName,
                cls.simpleName,
                packageName,
                relativeName,
                cls,
                constants
            )
        }
    }
}

/**  */
open class EnclosingModel(
    // Allow mutability due to the way it gets built. It's possible to initially build an enclosing
    // type that should actually be a message type initially and later when the compiler discovers
    // a message dependency then it converts from enclosing to message.
    override var enclosing: EnclosingModel?,
    override val packageName: String,
    override val name: String,
    override val simpleName: String,
    override val relativeName: String,
    val type: Any
) : WireModel(JavaKind.OBJECT, ProtoType.get(name)), DeclaredModel {
    val nested = TreeMap<String, DeclaredModel>()

    override val isEnclosing: Boolean
        get() = true

    init {
        enclosing?.let { it.nested[this.name] = this }
    }

    private fun relativeName(enclosing: EnclosingModel?): String {
        if (enclosing != null) {
            enclosing.nested[this.name] = this
            return enclosing.relativeName + "." + this.simpleName
        }
        return this.simpleName
    }

    override fun toString(): String {
        return name
    }
}
