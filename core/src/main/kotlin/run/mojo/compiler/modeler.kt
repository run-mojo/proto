package run.mojo.compiler

import com.google.common.reflect.TypeToken
import okio.ByteString
import run.mojo.wire.JavaKind
import run.mojo.Wire
import run.mojo.model.*
import java.lang.IllegalStateException

import javax.tools.Diagnostic
import java.lang.Package
import java.lang.reflect.*
import java.lang.reflect.Modifier
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror

interface FormatModule {

}

/**
 * Build up an object model from either "java.lang.model" or from "java.lang.reflect"
 */
open class ModelTransformer {

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

    fun register(type: Any): WireModel =
        when (type) {
            is Type -> typeToModel(type)
            is TypeMirror -> mirrorToModel(type)
            is Element -> elementToModel(type)
            else -> NOTHING
        }


    open fun mirrorToModel(mirror: TypeMirror?): WireModel = NOTHING

    open fun elementToModel(element: Element?): WireModel = mirrorToModel(element?.asType())

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


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Reflection API
    ////////////////////////////////////////////////////////////////////////////////////////////////


    internal fun toModels(types: Array<Type>): Array<WireModel> =
        types.mapNotNull { typeToModel(it) }.toTypedArray()

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Type To Wire Model
    // ---------------------------------------------------------------------------------------------
    // This is the entry point to WireModel resolution for java.lang.reflect.Type
    ////////////////////////////////////////////////////////////////////////////////////////////////

    internal fun typeToModel(type: Type): WireModel {
        if (type is TypeVariable<*>) {
            return TemplateModel(type)
        }
        if (type is WildcardType) {
            type.upperBounds
            return WildcardModel(
                type, toModels(type.lowerBounds), toModels(type.upperBounds)
            )
        }
        if (type is ParameterizedType) {
            val cls = type.rawType as Class<*>
            if (List::class.java.isAssignableFrom(cls)) {
                return paramTypeToListModel(JavaKind.LIST, type)
            }
            if (Set::class.java.isAssignableFrom(cls)) {
                return paramTypeToListModel(JavaKind.SET, type)
            }
            if (Queue::class.java.isAssignableFrom(cls)) {
                return paramTypeToListModel(JavaKind.QUEUE, type)
            }
            return if (Map::class.java.isAssignableFrom(cls)) {
                paramTypeToMapModel(type) ?: NOTHING
            } else typeToMessageModel(type) ?: NOTHING

        }
        if (type is Class<*>) {
            if (type.isEnum) {
                return classToEnumModel(type)
            }
            if (type.isArray) {
                return classToArrayModel(type) ?: NOTHING
            }

            if (List::class.java.isAssignableFrom(type)) {
                return classToListModel(JavaKind.LIST, type) ?: NOTHING
            }
            if (Set::class.java.isAssignableFrom(type)) {
                return classToListModel(JavaKind.SET, type) ?: NOTHING
            }
            if (Queue::class.java.isAssignableFrom(type)) {
                return classToListModel(JavaKind.QUEUE, type) ?: NOTHING
            }
            if (Map::class.java.isAssignableFrom(type)) {
                return classToMapModel(type) ?: NOTHING
            }
            if (Void.TYPE == type || Void::class.java == type) {
                return NOTHING
            }
            if (Boolean::class.javaPrimitiveType == type) {
                return BOOL
            }
            if (Boolean::class.java == type) {
                return BOOL_BOXED
            }
            if (Byte::class.javaPrimitiveType == type) {
                return BYTE
            }
            if (Byte::class.java == type) {
                return BYTE_BOXED
            }
            if (Short::class.javaPrimitiveType == type) {
                return SHORT
            }
            if (Short::class.java == type) {
                return SHORT_BOXED
            }
            if (Char::class.javaPrimitiveType == type) {
                return CHAR
            }
            if (Char::class.java == type) {
                return CHAR_BOXED
            }
            if (Int::class.javaPrimitiveType == type) {
                return INT
            }
            if (Int::class.java == type) {
                return INT_BOXED
            }
            if (Long::class.javaPrimitiveType == type) {
                return LONG
            }
            if (Long::class.java == type) {
                return LONG_BOXED
            }
            if (Float::class.javaPrimitiveType == type) {
                return FLOAT
            }
            if (Float::class.java == type) {
                return FLOAT_BOXED
            }
            if (Double::class.javaPrimitiveType == type) {
                return DOUBLE
            }
            if (Double::class.java == type) {
                return DOUBLE_BOXED
            }
            if (String::class.java == type) {
                return STRING
            }
            if (ByteString::class.java == type) {
                return BYTE_STRING
            }
            return if (ByteArray::class.java == type) {
                BytesModel("byte[]")
            } else typeToMessageModel(type) ?: NOTHING
        }
        return NOTHING
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Array
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun classToArrayModel(cls: Class<*>): ListModel? {
        val kind = typeToModel(cls.componentType)
        return if (kind == NOTHING) null else ListModel(JavaKind.ARRAY, kind)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // List from Class
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun classToListModel(type: JavaKind, g: Class<*>): ListModel? {
        if (g.typeParameters == null || g.typeParameters.size < 1) {
            return null
        }
        val typeVar = g.typeParameters[0]
        return ListModel(type, TemplateModel(typeVar))
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // List from Parameterized Type
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun paramTypeToListModel(type: JavaKind, parameterizedType: ParameterizedType): ListModel {
        val typeArg = parameterizedType.actualTypeArguments[0]
        if (typeArg is TypeVariable<*>) {
            return ListModel(type, TemplateModel(typeArg))
        }
        val component = typeToModel(typeArg)
        return ListModel(type, component)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Map from Class
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun classToMapModel(type: Class<*>): MapModel? {
        val keyArg = type.typeParameters[0]
        val valueArg = type.typeParameters[1]

        val key = typeToModel(keyArg)
        val value = typeToModel(valueArg)

        if (key == NOTHING || value == NOTHING) return null

        return MapModel(key, value)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Map from Parameterized Type
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun paramTypeToMapModel(parameterizedType: ParameterizedType): MapModel? {
        val keyArg = parameterizedType.actualTypeArguments[0]
        val valueArg = parameterizedType.actualTypeArguments[1]

        val key = typeToModel(keyArg)
        val value = typeToModel(valueArg)

        if (key == NOTHING || value == NOTHING) return null

        return MapModel(key, value)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Enum
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun classToEnumModel(type: Class<*>): EnumModel {
        getEnum(type.canonicalName)?.let { return it }

        // Get package.
        val pkg = packageOf(type.getPackage())

        val enclosing = classResolveEnclosing(pkg, type)
        val relativeName =
            if (enclosing != null)
                enclosing.relativeName + "." + type.simpleName.toString()
            else
                type.simpleName.toString()

        var ordinal = 0
        var lastTag = 0
        val constants = mutableListOf<EnumConstant>()


        val relativeNameBase = relativeName.replace(".", "_")
        for (constantObject in type.enumConstants) {
            //      messager.printMessage(Kind.WARNING,
            //          "ENUM TYPE: " + enclosed.toString() + "  -> " + enclosed.toString());

            val name = constantObject.toString()

            val constant = EnumConstant(
                ordinal = ordinal++,
                tag = lastTag + 1,
//                tag = enclosed.getAnnotation(Wire::class.java)?.let { it.tag } ?: lastTag+1,
                name = name,
                relativeName = relativeNameBase + "_" + name
            )

            constants += constant

            lastTag = constant.tag
        }

        val model = EnumModel.ofClass(
            enclosing,
            pkg.name,
            relativeName,
            type,
            constants
        )

        // Register in schema.
        add(model)

        if (model.enclosing == null) {
            pkg.nested[model.name] = model
        }

        return model
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Enclosing
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun classResolveEnclosing(pkg: PackageModel, type: Class<*>): EnclosingModel? {
        val enclosing = type.enclosingClass ?: return null

        var enclosingMirror: EnclosingModel? = getEnclosing(enclosing.canonicalName)
        if (enclosingMirror == null) {
            val e = classResolveEnclosing(pkg, enclosing)
            // Create an Enclosing holder.
            enclosingMirror = EnclosingModel(
                enclosing = classResolveEnclosing(pkg, enclosing),
                packageName = pkg.name,
                name = enclosing.canonicalName,
                simpleName = enclosing.simpleName,
                relativeName = e?.let { it.relativeName + "." + enclosing.simpleName } ?: enclosing.simpleName,
                type = enclosing
            )
            add(enclosingMirror)
        }

        return enclosingMirror
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Legacy Type Param resolver using Guava
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun resolveTypeParam(concreteClass: Class<*>, typeParameter: Type): Class<*> {
        try {
            return Class.forName(typeParameter.typeName)
        } catch (e: ClassNotFoundException) {
            return TypeToken.of(concreteClass).resolveType(typeParameter).rawType
        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Message
    ////////////////////////////////////////////////////////////////////////////////////////////////

    internal fun typeToMessageModel(t: Type?): EnclosingModel? {
        if (t == null) {
            return null
        }

        var typeArgs = emptyList<Type>()
        val parameterizedType: ParameterizedType?
        val type: Class<*>
        val typeVariables: List<TypeVarModel>

        if (t is ParameterizedType) {
            parameterizedType = t
            typeArgs = parameterizedType.actualTypeArguments.toList()

            typeVariables = parameterizedType.actualTypeArguments
                .map { a -> TypeVarModel.of(a) }
                .toList()

            type = parameterizedType.rawType as Class<*>
        } else if (t is Class<*>) {
            type = t
            parameterizedType = null

            typeVariables = type.typeParameters
                .map { a -> TypeVarModel.of(a) }
                .toList()
        } else {
            return null
        }

        val name = type.canonicalName
        val superClass = type.genericSuperclass

        val existing = getEnclosing(name)
        // Enclosing and Template types must be recreated. Template types may have it's Type Args
        // specified.
        if (existing != null && !existing.isEnclosing && !existing.isTemplate) {
            return existing
        }

        val pkg = packageOf(type.getPackage())

        val template: Boolean
        var impl: Boolean

        if (!typeArgs.isEmpty()) {
            template = true
            impl = true

            for (typeArg in typeArgs) {
                if (typeArg is TypeVariable<*>) {
                    impl = false
                    break
                }
            }
        } else if (Modifier.isAbstract(type.modifiers)) {
            template = true
            impl = false
        } else {
            template = false
            impl = false
        }

        var superType: MessageModel? = null
        if (superClass != null && Any::class.java != superClass) {
            superType = typeToMessageModel(superClass) as MessageModel?
        }

        // Maybe Google AutoValue?
        if (Modifier.isAbstract(type.modifiers) && type.declaredFields.size == 0) {

        }

        val message = MessageModel.ofClass(
            enclosing = classResolveEnclosing(pkg, type),
            packageName = pkg.name,
            cls = type,
            declaredType = parameterizedType,
            typeVars = typeVariables,
            superType = superType,
            template = template,
            impl = impl,

            // Annotations.
            wire = type.getAnnotation(Wire::class.java)
        )

        // If null then the package is the encloser.
        if (message.enclosing == null) {
            pkg.nested[message.name] = message
        }

        // Add to schema.
        add(message)

        // Are there inherited fields?
        message.superType?.let {
            val resolver = TypeArgResolver(message, this)

            it.fields.values.forEach { field ->
                val kind = field.model

                if (kind.isTemplate) {
                    resolver.resolve(kind)?.let {
                        message.pushField(field.resolved(message, it))
                    }
                } else {
                    message.pushField(field.resolved(message, kind))
                }
            }
        }

        type.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) && !Modifier.isNative(it.modifiers) }
            .forEach { message.registerField(this, it) }

        type.declaredMethods
            .forEach { executable -> message.matchAccessorReflection(this, executable) }

        type.constructors
            //            .filter(
            //                ctor ->
            //                    !Arrays.stream(ctor.getParameters())
            //                        .filter(
            //                            parameter -> {
            //                              Class parameterType = parameter.getType();
            //                              Package parameterTypePackage =
            // parameterType.getPackage();
            //                              if (parameterTypePackage != null) {
            //                                if (parameterTypePackage
            //                                    .getName()
            //                                    .startsWith("kotlin.jvm.internal")) {
            //                                  return true;
            //                                }
            //                              }
            //                              return false;
            //                            })
            //                        .findAny()
            //                        .isPresent())
            .map { message.registerCtor(it) }
            .toList()

        // Print out the fields if it's a concrete javaKind.
        if (!message.isTemplate) {
            message.fields
                .values
                .forEach { (name1, _, model, _, _, _, tag) ->
                    println(
                        message.toString()
                                + "."
                                + name1
                                + " = ["
                                + tag
                                + "] : "
                                + model
                    )
                }
        }

        return message
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

        val NOTHING = NothingModel

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

/**
 * Standardized Type Argument resolver that unifies a single model between reflection and java compiler model.
 */
class TypeArgResolver(val message: MessageModel, val processor: ModelTransformer) {
    val typeArgs: List<TypeVarModel> = message.superType!!.typeVars

    fun resolve(kindMirror: WireModel): WireModel? {
        if (kindMirror is WildcardModel) {

            if (kindMirror.resolved != null) {
                processor.printMessage(
                    Diagnostic.Kind.WARNING,
                    "RESOLVED WILDCARD W/O TEMPLATE: "
                            + kindMirror.type
                            + " to "
                            + kindMirror.toString()
                )
                return kindMirror.resolved
            }

            val typeVar = kindMirror.typeVars
                .stream()
                .findFirst()
                .orElse(null)

            if (typeVar == null) {
                processor.printMessage(Diagnostic.Kind.ERROR, "could not resolve wildcard: $kindMirror")
                return null
            }

            val index = typeVar.index

            if (index < 0 || index >= typeArgs.size) {
                processor.printMessage(Diagnostic.Kind.ERROR, "could not resolve wildcard: $kindMirror")
                return null
            }

            val capturedVar = typeArgs[index]
            val capturedArg = capturedVar.type

            if (capturedVar.isVariable) {
                processor.printMessage(
                    Diagnostic.Kind.WARNING,
                    "REMAPPED WILDCARD TYPE VAR: " + kindMirror + " to " + capturedArg.toString()
                )
                return TemplateModel(capturedVar)
            } else {
                processor.printMessage(
                    Diagnostic.Kind.WARNING,
                    "RESOLVED WILDCARD VAR '"
                            + typeVar.name
                            //                      + "' on '"
                            //                      + typeVar.getGenericDeclaration().toString()
                            + "' AS '"
                            + capturedArg.toString()
                            + "'"
                )

                // Matched!
                return processor.register(capturedArg)
            }
        } else if (kindMirror is TemplateModel) {
            val typeVar = kindMirror.typeVar
//            val resolved = typeArgs.stream().filter { t -> t.isMatch(typeVar) }.findFirst().orElse(null)

            val index = typeVar.index
            if (index < 0) {
                processor.printMessage(Diagnostic.Kind.WARNING, "could not resolve javaKind var '$typeVar'")
                //              messager.printMessage(
                //                  Diagnostic.Kind.ERROR, "could not resolve javaKind var '" +
                // typeVar + "'");
                return null
            }

            val capturedVar = typeArgs[index]
            val capturedArg = capturedVar.type

            if (capturedVar.isVariable) {
                processor.printMessage(
                    Diagnostic.Kind.WARNING,
                    "REMAPPED TYPE VAR: " + typeVar + " to " + capturedArg.toString()
                )
                return TemplateModel(capturedVar)
            } else {
                processor.printMessage(
                    Diagnostic.Kind.WARNING,
                    "RESOLVED TYPE VAR '"
                            + typeVar.name
                            //                      + "' on '"
                            //                      + typeVar.getGenericDeclaration().toString()
                            + "' AS '"
                            + capturedArg.toString()
                            + "'"
                )

                // Matched!
                return processor.register(capturedArg)
            }
        } else if (kindMirror is ListModel) {
            val resolved = resolve(kindMirror.component)
            return ListModel(kindMirror.javaKind, resolved!!)
        } else if (kindMirror is MapModel) {
            val key = resolve(kindMirror.key)
            val value = resolve(kindMirror.key)
            return MapModel(key!!, value!!)
        } else {
            return kindMirror
        }
    }
}
