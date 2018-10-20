package run.mojo.wire.compiler

import com.google.common.reflect.TypeToken
import okio.ByteString
import run.mojo.wire.JavaKind
import run.mojo.wire.Wire
import run.mojo.wire.model.*

import java.lang.reflect.*
import java.util.*

/**
 * Builds models using reflection of already compiled classes.
 */
class ModelReflectionBuilder : ModelBuilder() {

    override fun register(type: Any): WireModel? {
        if (type is Type) {
            return createModel(type)
        }
        return null
    }

    fun register(type: Type): WireModel? {
        return createModel(type)
    }

    internal fun toModels(types: Array<Type>): Array<WireModel> =
        types.mapNotNull { createModel(it) }.toTypedArray()

    internal fun createModel(type: Type): WireModel? {
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
                return toListKind(JavaKind.LIST, type)
            }
            if (Set::class.java.isAssignableFrom(cls)) {
                return toListKind(JavaKind.SET, type)
            }
            if (Queue::class.java.isAssignableFrom(cls)) {
                return toListKind(JavaKind.QUEUE, type)
            }
            return if (Map::class.java.isAssignableFrom(cls)) {
                toMapKind(type)
            } else toMessage(type)

        }
        if (type is Class<*>) {
            if (type.isEnum) {
                return toEnumKind(type)
            }
            if (type.isArray) {
                return toArrayKind(type)
            }

            if (List::class.java.isAssignableFrom(type)) {
                return toListKind(JavaKind.LIST, type)
            }
            if (Set::class.java.isAssignableFrom(type)) {
                return toListKind(JavaKind.SET, type)
            }
            if (Queue::class.java.isAssignableFrom(type)) {
                return toListKind(JavaKind.QUEUE, type)
            }
            if (Map::class.java.isAssignableFrom(type)) {
                return toMapKind(type)
            }
            if (Void.TYPE == type || Void::class.java == type) {
                return null
            }
            if (Boolean::class.javaPrimitiveType == type) {
                return ModelBuilder.Companion.BOOL
            }
            if (Boolean::class.java == type) {
                return ModelBuilder.Companion.BOOL_BOXED
            }
            if (Byte::class.javaPrimitiveType == type) {
                return ModelBuilder.Companion.BYTE
            }
            if (Byte::class.java == type) {
                return ModelBuilder.Companion.BYTE_BOXED
            }
            if (Short::class.javaPrimitiveType == type) {
                return ModelBuilder.Companion.SHORT
            }
            if (Short::class.java == type) {
                return ModelBuilder.Companion.SHORT_BOXED
            }
            if (Char::class.javaPrimitiveType == type) {
                return ModelBuilder.Companion.CHAR
            }
            if (Char::class.java == type) {
                return ModelBuilder.Companion.CHAR_BOXED
            }
            if (Int::class.javaPrimitiveType == type) {
                return ModelBuilder.Companion.INT
            }
            if (Int::class.java == type) {
                return ModelBuilder.Companion.INT_BOXED
            }
            if (Long::class.javaPrimitiveType == type) {
                return ModelBuilder.Companion.LONG
            }
            if (Long::class.java == type) {
                return ModelBuilder.Companion.LONG_BOXED
            }
            if (Float::class.javaPrimitiveType == type) {
                return ModelBuilder.Companion.FLOAT
            }
            if (Float::class.java == type) {
                return ModelBuilder.Companion.FLOAT_BOXED
            }
            if (Double::class.javaPrimitiveType == type) {
                return ModelBuilder.Companion.DOUBLE
            }
            if (Double::class.java == type) {
                return ModelBuilder.Companion.DOUBLE_BOXED
            }
            if (String::class.java == type) {
                return ModelBuilder.Companion.STRING
            }
            if (ByteString::class.java == type) {
                return ModelBuilder.Companion.BYTE_STRING
            }
            return if (ByteArray::class.java == type) {
                BytesModel("byte[]")
            } else toMessage(type)

        }
        return null
    }

    /**
     * @param cls
     * @return
     */
    private fun toArrayKind(cls: Class<*>): ListModel? {
        val kind = createModel(cls.componentType)
        return if (kind == null) null else ListModel(JavaKind.ARRAY, kind)
    }

    private fun toListKind(type: JavaKind, g: Class<*>): ListModel? {
        if (g.typeParameters == null || g.typeParameters.size < 1) {
            return null
        }
        val typeVar = g.typeParameters[0]
        return ListModel(type, TemplateModel(typeVar))
    }

    /**
     * @param type
     * @param parameterizedType
     * @return
     */
    private fun toListKind(type: JavaKind, parameterizedType: ParameterizedType): ListModel {
        val typeArg = parameterizedType.actualTypeArguments[0]
        if (typeArg is TypeVariable<*>) {
            return ListModel(type, TemplateModel(typeArg))
        }
        val component = createModel(typeArg)
        return ListModel(type, component!!)
    }

    private fun toMapKind(type: Class<*>): MapModel {
        val keyArg = type.typeParameters[0]
        val valueArg = type.typeParameters[1]

        val key = createModel(keyArg)
        val value = createModel(valueArg)

        return MapModel(key!!, value!!)
    }

    private fun toMapKind(parameterizedType: ParameterizedType): MapModel {
        val keyArg = parameterizedType.actualTypeArguments[0]
        val valueArg = parameterizedType.actualTypeArguments[1]

        val key = createModel(keyArg)
        val value = createModel(valueArg)

        return MapModel(key!!, value!!)
    }

    /**
     * @param type
     * @return
     */
    private fun toEnumKind(type: Class<*>): EnumModel {
        getEnum(type.canonicalName)?.let { return it }

        // Get package.
        val pkg = packageOf(type.getPackage())

        val enclosing = getEnclosing(pkg, type)
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

    private fun getEnclosing(pkg: PackageModel, type: Class<*>): EnclosingModel? {
        val enclosing = type.enclosingClass ?: return null

        var enclosingMirror: EnclosingModel? = getEnclosing(enclosing.canonicalName)
        if (enclosingMirror == null) {
            val e = getEnclosing(pkg, enclosing)
            // Create an Enclosing holder.
            enclosingMirror = EnclosingModel(
                enclosing = getEnclosing(pkg, enclosing),
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

    /**
     * @param concreteClass
     * @param typeParameter
     * @return
     */
    private fun resolveTypeParam(concreteClass: Class<*>, typeParameter: Type): Class<*> {
        try {
            return Class.forName(typeParameter.typeName)
        } catch (e: ClassNotFoundException) {
            return TypeToken.of(concreteClass).resolveType(typeParameter).rawType
        }

    }

    internal fun toMessage(_type: Type?): EnclosingModel? {
        if (_type == null) {
            return null
        }

        var typeArgs = emptyList<Type>()
        val parameterizedType: ParameterizedType?
        val type: Class<*>
        val typeVariables: List<TypeVarModel>

        if (_type is ParameterizedType) {
            parameterizedType = _type
            typeArgs = parameterizedType.actualTypeArguments.toList()

            typeVariables = parameterizedType.actualTypeArguments
                .map { a -> TypeVarModel.of(a) }
                .toList()

            type = parameterizedType.rawType as Class<*>
        } else if (_type is Class<*>) {
            type = _type
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
            superType = toMessage(superClass) as MessageModel?
        }

        // Maybe Google AutoValue?
        if (Modifier.isAbstract(type.modifiers) && type.declaredFields.size == 0) {

        }

        val message = MessageModel.ofClass(
            enclosing = getEnclosing(pkg, type),
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

            it.fields.values.forEach {
                val kind = it.model

                if (kind.isTemplate) {
                    val resolved = resolver.resolve(kind)
                    message.pushField(it.resolved(message, resolved))
                } else {
                    message.pushField(it.resolved(message, kind))
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
}
