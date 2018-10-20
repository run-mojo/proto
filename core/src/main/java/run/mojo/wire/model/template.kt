package run.mojo.wire.model

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import com.squareup.wire.schema.ProtoType
import run.mojo.wire.JavaKind
import java.lang.reflect.Type
import java.util.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import javax.lang.model.type.WildcardType

/** Concrete implementation of a message template.  */
class ImplModel(
    override var enclosing: EnclosingModel?,
    val message: MessageModel,
    override val packageName: String,
    override var name: String,
    override var simpleName: String
) : WireModel(JavaKind.OBJECT, ProtoType.get(name)), DeclaredModel {
    override val relativeName: String
    val genericName: String
    val toString: String

    override val isImpl: Boolean
        get() = true

    override val isMessage: Boolean
        get() = true

    override val isEnclosing: Boolean
        get() = false

    init {
        this.relativeName = if (enclosing != null) enclosing!!.relativeName + "." + simpleName else simpleName
        this.genericName = message.declaredType.toString()
        this.toString = "$name implements $genericName"
    }

    override fun toString(): String {
        return toString
    }
}


/**
 * Generics support. Data messages can accept Type Arguments and proto can resolve at compile time.
 * Any javaKind with a javaKind variable is treated as though it's an abstract class. Subclasses
 * turn into concrete implementations since protobuf and flatbuffers don't support Java based
 * inheritance.
 */
class TemplateModel : WireModel {

    // Use this to match a declared javaKind.
    val typeVar: TypeVarModel

    override val isTemplate: Boolean
        get() = true

    constructor(typeVar: TypeVarModel) : super(JavaKind.TEMPLATE, ProtoType.get(typeVar.name)) {
        this.typeVar = typeVar
    }

    constructor(typeVar: TypeVariable) : super(JavaKind.TEMPLATE, ProtoType.get(typeVar.toString())) {
        this.typeVar = TypeVarModel.of(typeVar)
    }

    constructor(typeVar: java.lang.reflect.TypeVariable<*>) : super(JavaKind.TEMPLATE, ProtoType.get(typeVar.name)) {
        this.typeVar = TypeVarModel.of(typeVar)
    }

    override fun toString(): String {
        return typeVar.toString()
    }
}

/**  */
class WildcardModel(val type: Any?, val lower: Array<WireModel>, val upper: Array<WireModel>) :
    WireModel(JavaKind.TEMPLATE, ProtoType.BYTES) {
    val resolved: WireModel?
    val typeVars: MutableList<TypeVarModel>

    override val isTemplate: Boolean
        get() = true

    init {
        this.typeVars = ArrayList<TypeVarModel>(1)
        val resolvedUpper = extractTypeVars(upper, typeVars)
        val resolvedLower = extractTypeVars(lower, typeVars)
        if (resolvedUpper != null) {
            this.resolved = resolvedUpper
        } else {
            this.resolved = resolvedLower
        }
    }

    private fun extractTypeVars(bounds: Array<WireModel>?, vars: MutableList<TypeVarModel>): WireModel? {
        var resolved: WireModel? = null
        if (bounds != null && bounds.size > 0) {
            for (model in bounds) {
                if (model is TemplateModel) {
                    vars.add(model.typeVar)
                } else {
                    resolved = model
                }
            }
        }
        return resolved
    }

    override fun toString(): String {
        return type?.toString() ?: javaKind.toString()
    }
}


/**
 * Utility for keeping track of Type Params/Args for both annotation processing and reflection.
 *
 * @param <T>
</T> */
interface TypeVarModel {

    val type: Any

    val name: String

    val index: Int
        get() = -1

    val isType: Boolean
        get() = this is ModelType || this is ReflectType

    val isVariable: Boolean
        get() = (this is ModelVar
                || this is ReflectVar
                || this is ModelWildcard
                || this is ReflectWildcard)

    open fun isMatch(typeVar: TypeVarModel): Boolean {
        return typeVar.javaClass == this.javaClass && typeVar.name == this.name
    }

    fun resolve(): WireModel? {
        return TemplateModel(this)
    }

    fun toTypeName(boxed: Boolean): TypeName {
        return resolve()?.toTypeName(boxed) ?: ClassName.bestGuess(name) as TypeName
    }

    companion object {

        fun of(type: TypeMirror): TypeVarModel {
            if (type is TypeVariable) {
                return ModelVar(type)
            }
            return if (type is WildcardType) {
                ModelWildcard(type)
            } else ModelType(type)
        }

        fun of(type: Type): TypeVarModel {
            if (type is java.lang.reflect.WildcardType) {
                return ReflectWildcard(type)
            } else if (type is java.lang.reflect.TypeVariable<*>) {
                return ReflectVar(type)
            }
            return ReflectType(type)
        }

        fun getTypeArgumentIndex(typeVar: java.lang.reflect.TypeVariable<*>): Int {
            val original = typeVar.genericDeclaration
            for (i in 0 until original.typeParameters.size) {
                val typeVariable = original.typeParameters[i]
                if (typeVariable.name == typeVar.name) {
                    return i
                }
            }
            return -1
        }

        fun getTypeArgumentIndex(typeVar: TypeVariable): Int {
            val original = typeVar.asElement().enclosingElement.asType() as DeclaredType
            for (i in 0 until original.typeArguments.size) {
                if (original.typeArguments[i].toString() == typeVar.toString()) {
                    return i
                }
            }
            return -1
        }
    }
}


class ModelType(override val type: TypeMirror) : TypeVarModel {

    override val name: String
        get() = type.toString()

    override val isVariable: Boolean
        get() = false
}

class ModelWildcard(override val type: WildcardType) : TypeVarModel {

    val superBound: TypeVarModel?
    val extendsBound: TypeVarModel?

    override val index: Int
        get() = extendsBound?.index ?: (superBound?.index ?: -1)

    override val name: String
        get() = type.toString()

    init {

        this.extendsBound = TypeVarModel.of(type.extendsBound)
        this.superBound = TypeVarModel.of(type.superBound)
    }

    override fun isMatch(typeVar: TypeVarModel): Boolean {
        if (typeVar.name == name) {
            return true
        }
        if (typeVar is ModelVar) {
            if (extendsBound!!.name == typeVar.name) {
                return true
            }
            if (superBound!!.name == typeVar.name) {
                return true
            }
        } else if (typeVar is ModelWildcard) {
            if (typeVar.extendsBound != null
                && extendsBound != null
                && typeVar.extendsBound.name == extendsBound.name
            ) {
                return true
            }
            if (typeVar.superBound != null
                && superBound != null
                && typeVar.superBound.name == superBound.name
            ) {
                return true
            }
        }
        return false
    }
}

/** Annotation processing model.  */
class ModelVar(override val type: TypeVariable) : TypeVarModel {
    override val index: Int

    override val name: String
        get() = type.toString()

    init {
        this.index = TypeVarModel.getTypeArgumentIndex(type)
    }

    override fun toString(): String {
        return type.toString()
    }
}

/** Reflection model.  */
class ReflectVar(override val type: java.lang.reflect.TypeVariable<*>) : TypeVarModel {
    override val index: Int

    override val name: String
        get() = type.name

    init {
        this.index = TypeVarModel.getTypeArgumentIndex(type)
    }

    override fun toString(): String {
        return type.name
    }
}

class ReflectWildcard(override val type: java.lang.reflect.WildcardType) : TypeVarModel {

    val upperBound: List<TypeVarModel>
    val lowerBound: List<TypeVarModel>

    override val index: Int
        get() {
            for (`var` in upperBound) {
                if (`var`.index > -1) {
                    return `var`.index
                }
            }
            for (`var` in lowerBound) {
                if (`var`.index > -1) {
                    return `var`.index
                }
            }
            return -1
        }

    override val name: String
        get() = type.toString()

    init {
        this.upperBound = type.upperBounds?.map {
            TypeVarModel.of(it)
        }?.toList() ?: emptyList()
        this.lowerBound = type.lowerBounds?.map {
            TypeVarModel.of(it)
        }?.toList() ?: emptyList()
    }

    override fun isMatch(typeVar: TypeVarModel): Boolean {
        if (typeVar.name == name) {
            return true
        }
        if (typeVar is ReflectVar) {
            for (upper in upperBound) {
                if (upper.isMatch(typeVar)) {
                    return true
                }
            }
            for (upper in lowerBound) {
                if (upper.isMatch(typeVar)) {
                    return true
                }
            }
        } else if (typeVar is ReflectWildcard) {
            for (upper in typeVar.upperBound) {
                for (upper2 in upperBound) {
                    if (upper2.isMatch(upper)) {
                        return true
                    }
                }
            }
            for (lower in typeVar.lowerBound) {
                for (lower2 in lowerBound) {
                    if (lower2.isMatch(lower)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun toString(): String {
        return type.toString()
    }
}

class ReflectType(override val type: Type) : TypeVarModel {

    override val name: String
        get() = type.typeName

    override fun toString(): String {
        return type.toString()
    }
}