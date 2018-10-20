package run.mojo.wire.model

import com.google.auto.value.AutoValue
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import run.mojo.wire.JavaKind
import run.mojo.wire.Wire
import run.mojo.wire.compiler.ModelBuilder
import java.beans.ConstructorProperties
import java.lang.reflect.*
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.element.Modifier
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**  */
enum class NamingConvention {
    BEAN,
    FLUENT
}

/**  */
enum class Flavor {
    /**
     * Standard java bean is a fully mutable class. This is the most flexible when integrating with additional
     * object mappers such as those that represent SQL.
     */
    BEAN,
    /**
     * Data messages may be a blend of mutable and immutable.
     */
    DATA,
    /**
     * Every field is final upon construction.
     */
    VALUE
}

/**  */
enum class Formats {
    PROTOBUF,
    JSON,
    FLATBUF
}

/**  */
class BuilderModel(val provider: String, val message: MessageModel, val fields: List<BuilderField>)

class BuilderField(val field: FieldModel, val setter: AccessorModel) {
    companion object {

        fun of(field: FieldModel): BuilderField {
            return BuilderField(field, AccessorModel.setterOf(field, false))
        }

        fun fluent(field: FieldModel): BuilderField {
            return BuilderField(field, AccessorModel.setterOf(field, true))
        }
    }
}


/**  */
class ConstructorModel(val type: Any, val fields: List<ConstructorParam>) {
    fun isValid(): Boolean {
        return fields.filter { !it.isValid() }.count() == fields.size
    }

    companion object {

        fun ofElement(type: ExecutableElement, fields: List<ConstructorParam>): ConstructorModel {
            return ConstructorModel(type, fields)
        }

        fun ofType(type: Constructor<*>, fields: List<ConstructorParam>): ConstructorModel {
            return ConstructorModel(type, fields)
        }
    }
}

/**  */
class ConstructorParam(
    val parameter: Any? = null,
    val field: FieldModel? = null
) {
    fun isValid() = field != null
}


/**  */
class MessageModel(
    enclosing: EnclosingModel?,
    packageName: String,
    name: String,
    simpleName: String,
    relativeName: String,
    type: Any,
    val declaredType: Any,
    val typeVars: List<TypeVarModel>,
    val superType: MessageModel?,
    /**
     * Type as Type parameters that are not set effectively making this a template.
     *
     * @return
     */
    override val isTemplate: Boolean,
    val impl: Boolean,
    // Annotations.
    val wire: Wire?,
    // Can provide added insight into Kotlin code gen.
    val kot: Boolean,
    // Lombok is handled specially since it's very hacky and the full info is available during
    // compilation.
    // Luckily the annotations are available during compilation so we can infer final generated
    // output.
    val lombokData: Boolean,
    val lombokValue: Boolean,
    val lombokBuilder: Boolean,
    // Google's AutoAnnotation is used.
    val autoValueAnnotation: Boolean
) : EnclosingModel(enclosing, packageName, name, simpleName, relativeName, type) {

    val fields = LinkedHashMap<String, FieldModel>()
    val fieldsByTag = TreeMap<Int, FieldModel>()
    val constructors: MutableList<ConstructorModel> = ArrayList(2)

    //  public List<ImplModel> impls = Collections.emptyList();
    // Type Variables.

    var builder: BuilderModel? = null
    var factoryCtor: ConstructorModel? = null
    var emptyCtor: ConstructorModel? = null
    private val nameCounter: Int = 0

    override fun toTypeName(boxed: Boolean): TypeName {
        if (typeVars.isEmpty()) {
            return ClassName.bestGuess(name)
        }
        if (isTemplate) {
            return ClassName.bestGuess(name)
        }
        typeVars.map {
            it.resolve()
        }
        return ClassName.get("", "")
    }

    /**
     * Impls are when a Template type is fully specified resulting in a new type
     *
     * @return
     */
    override val isImpl: Boolean
        get() = superType != null && superType.isTemplate

    override val isEnclosing: Boolean
        get() = false

    override val isEnum: Boolean
        get() = false

    override val isMessage: Boolean
        get() = true


    val modelTypeArguments: List<TypeMirror>
        get() = if (declaredType is DeclaredType) {
            declaredType.typeArguments
        } else {
            emptyList()
        }

    val reflectTypeArguments: List<Type>
        get() = if (declaredType is ParameterizedType) {
            Arrays.asList(*declaredType.actualTypeArguments)
        } else {
            emptyList()
        }

    override fun toString(): String {
        return name
    }

    /**
     * Return the next getTag number to use.
     *
     * @return getTag number
     */
    fun nextTag(): Int {
        return if (fieldsByTag.isEmpty()) {
            1
        } else fieldsByTag.lastKey() + 1
    }


    fun pushField(field: FieldModel?) {
        fields[field!!.name] = field
        fieldsByTag[field.tag] = field
    }

    fun registerImpl(field: FieldModel): ImplModel {
        val message = field.model as MessageModel

        var simpleName = field.name.substring(0, 1).toUpperCase() + field.name.substring(1)

        // Create an impl name.
        var name = "$name.$simpleName"

        if (nested.containsKey(name)) {
            val counter = 1
            var nextSimpleName = simpleName
            do {
                nextSimpleName = simpleName + Integer.toString(counter)
                name = "$name.$nextSimpleName"
            } while (nested.containsKey(name))

            simpleName = nextSimpleName
        }

        val implModel = ImplModel(this, message, packageName, name, simpleName)

        nested[name] = implModel

        return implModel
    }

    fun registerField(processor: ModelBuilder, typeModel: Any): FieldModel? {
        val field = FieldBuilder(this, this)
        if (typeModel is VariableElement) {
            field.name = typeModel.simpleName.toString()
            field.type = typeModel.asType()
            field.wire = typeModel.getAnnotation(Wire::class.java)
            // Get WireSpec.
            field.model = processor.register(field.type!!)
            if (field.model == null) {
                // Invalid type.
                return null
            }
            field.isPublic = typeModel.modifiers.contains(Modifier.PUBLIC)
            field.isPackagePrivate = field.isPublic || !typeModel.modifiers.contains(Modifier.PRIVATE) &&
                    !typeModel.modifiers.contains(Modifier.PROTECTED)
            field.isFinal = typeModel.modifiers.contains(Modifier.FINAL)

            // Create phantom getter and setter methods for lombok Data messages.
            // This methods will magically appear during actual compilation.
            // Reflection API doesn't have this problem since lombok modifies the bytecode.
            if (lombokData || lombokValue) {
                // Create phantom Getter.
                field.getter = AccessorModel(
                    "get" + field.name.substring(0, 1).toUpperCase() + field.name.substring(1),
                    false,
                    false,
                    true
                )

                // Create phantom setter for mutable fields.
                if (lombokData && !field.isFinal) {
                    field.setter = AccessorModel(
                        "set" + field.name.substring(0, 1).toUpperCase() + field.name.substring(1),
                        false,
                        false,
                        true
                    )
                }
            }
        } else if (typeModel is Field) {
            field.name = typeModel.name
            field.type = typeModel.genericType
            if (field.type == null) {
                field.type = typeModel.type
            }
            field.wire = typeModel.getAnnotation(Wire::class.java)
            field.model = processor.register(field.type!!)
            field.isPublic = java.lang.reflect.Modifier.isPublic(typeModel.modifiers)
            field.isPackagePrivate = field.isPublic || !java.lang.reflect.Modifier.isPrivate(typeModel.modifiers) &&
                    !java.lang.reflect.Modifier.isProtected(typeModel.modifiers)
            field.isFinal = java.lang.reflect.Modifier.isFinal(typeModel.modifiers)
        } else {
            return null
        }

        if (!field.isValid()) {
            return null
        }

        val wire = field.wire
        if (wire != null) {
            if (wire.tag > 0) {
                field.tag = wire.tag
            } else {
                field.tag = nextTag()
            }
        } else {
            field.tag = nextTag()
        }

        if (fieldsByTag.containsKey(field.tag)) {
            System.err.println(
                name
                        + "."
                        + processor.nameOf(typeModel)
                        + " has duplicate Tag number '"
                        + field.tag
                        + "'"
            )
        }

        val f = field.build() ?: return null

        //    compiler.printMessage(Diagnostic.Kind.WARNING, getName() + "." + field.name);

        if (f.model.javaKind == JavaKind.OBJECT && f.model is MessageModel) {
            val messageModel = field.model as MessageModel

            if (messageModel.impl) {
                registerImpl(f)
            }
        }

        pushField(f)
        return f
    }

    fun matchAccessorReflection(processor: ModelBuilder, executable: Method) {
        if (executable.parameterCount == 0) {
            getterCandidate(
                processor,
                executable.name,
                java.lang.reflect.Modifier.isPublic(executable.modifiers)
            )
        } else if (executable.parameterCount == 1) {
            setterCandidate(
                processor,
                executable.returnType != null && executable.returnType != Void.TYPE,
                executable.name,
                java.lang.reflect.Modifier.isPublic(executable.modifiers)
            )
        }
    }

    fun matchAccessorElement(processor: ModelBuilder, executable: ExecutableElement) {
        if (executable.parameters == null || executable.parameters.isEmpty()) {
            getterCandidate(
                processor,
                executable.simpleName.toString(),
                executable.modifiers.contains(Modifier.PUBLIC)
            )
        } else if (executable.parameters.size == 1) {
            setterCandidate(
                processor,
                executable.returnType != null && executable.returnType.kind != TypeKind.VOID,
                executable.simpleName.toString(),
                executable.modifiers.contains(Modifier.PUBLIC)
            )
        }
    }

    fun getterCandidate(processor: ModelBuilder, methodName: String, isPublic: Boolean) {
        if (processor.isInvalidPropMethod(methodName)) {
            return
        }
        var fluent = true
        var fieldName = methodName
        if (methodName.startsWith("get") && methodName.length > 3) {
            fluent = false
            fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4)
        } else if (methodName.startsWith("is") && methodName.length > 2) {
            fluent = false
            fieldName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3)
        }

        val field = fields[fieldName] ?: return

        val getter = field.getter
        if (getter != null) {
            System.err.println("Found another getter: " + name + "." + getter.name)
        }
        field.getter = AccessorModel(methodName, fluent, false, isPublic)
    }

    fun setterCandidate(
        processor: ModelBuilder, fluent: Boolean, methodName: String, isPublic: Boolean
    ) {
        if (processor.isInvalidPropMethod(methodName)) {
            return
        }

        var fluentNaming = true
        var fieldName = methodName
        if (methodName.startsWith("set") && methodName.length > 3) {
            fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4)
            fluentNaming = false
        }

        // Let's see if it's a setter.
        val field = fields[fieldName] ?: return

        val setter = field.setter
        if (setter != null) {
            System.err.println("Found another setter: " + name + "." + setter.name)
        }
        field.setter = AccessorModel(methodName, fluentNaming, fluent, isPublic)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Reflection
    ////////////////////////////////////////////////////////////////////////////////////////////////

    protected fun registerFactory(factory: ConstructorModel) {
        if (factory.isValid()) {
            constructors.add(factory)
        }
    }

    fun registerCtor(ctor: Constructor<*>): ConstructorModel? {
        if (ctor.parameters == null || ctor.parameters.isEmpty()) {
            emptyCtor = ConstructorModel(ctor, emptyList())
            return emptyCtor
        }

        ctor.getAnnotation(ConstructorProperties::class.java)?.let {
            return ConstructorModel(
                ctor,
                it.value
                    .map { propName -> ConstructorParam(fields[propName]) }
                    .toList())
        }

        // Only proceed if this is a candidate to cover all fields.
        if (ctor.parameters.size != fields.size) {
            return null
        }

        val ctorFields = ArrayList<ConstructorParam>(ctor.parameters.size)

        val fields = this.fields.values.toList()
        for (i in 0 until ctor.parameters.size) {
            val parameter = ctor.parameters[i]
            // Field at same index.
            val field = fields[i]

            // Map to field at same index if types match.
            val parameterType = parameter.parameterizedType
            if (parameterType == field.type) {
                ctorFields.add(ConstructorParam(field))
                continue
            }

            val ptypeName = parameterType.toString()
            val typeName = field.type.toString()

            if (parameterType == field.type || parameterType.toString() == field.type.toString()) {
                ctorFields.add(ConstructorParam(field))
                continue
            }

            ctorFields.add(ConstructorParam(null))
        }

        val factory = ConstructorModel(ctor, ctorFields)
        registerFactory(factory)
        return factory
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Model
    ////////////////////////////////////////////////////////////////////////////////////////////////

    fun registerCtor(ctor: ExecutableElement): ConstructorModel? {
        if (ctor.parameters == null || ctor.parameters.size == 0) {
            emptyCtor = ConstructorModel(ctor, emptyList())
            return emptyCtor
        }

        // Handle constructor properties.
        ctor.getAnnotation(ConstructorProperties::class.java)?.let {
            return ConstructorModel(
                ctor,
                it.value
                    .map { propName -> ConstructorParam(fields[propName]) }
                    .toList())
        }

        if (ctor.parameters.size == fields.size) {
            val ctorFields = ArrayList<ConstructorParam>(ctor.parameters.size)

            val fields = this.fields.values.toList()
            for (i in 0 until ctor.parameters.size) {
                val parameter = ctor.parameters[i]
                // Field at same index.
                val field = fields[i]

                // Map to field at same index if types match.
                val parameterType = parameter.asType()
                if (parameterType == field.type) {
                    ctorFields.add(ConstructorParam(field))
                    continue
                }

                // Maybe toStrings match?
                val ptypeName = parameterType.toString()
                val typeName = field.type.toString()
                if (ptypeName == typeName) {
                    ctorFields.add(ConstructorParam(field))
                    continue
                }

                // We have a miss.
                ctorFields.add(ConstructorParam(null))
            }

            val factory = ConstructorModel(ctor, ctorFields)
            registerFactory(factory)
            return factory
        }

        return null
    }

    fun determineBuilder() {
        if (lombokBuilder) {
            builder = BuilderModel(
                "LOMBOK",
                this,
                fields
                    .values
                    .map { f -> BuilderField.of(f) }
                    .toList())
        }
    }

    companion object {
        fun ofElement(
            enclosing: EnclosingModel?,
            packageName: String,
            element: TypeElement,
            declaredType: DeclaredType,
            typeVars: List<TypeVarModel>,
            superType: MessageModel?,
            template: Boolean,
            impl: Boolean,
            wire: Wire?,
            kot: AnnotationMirror?,
            lombokData: AnnotationMirror?,
            lombokValue: AnnotationMirror?,
            lombokBuilder: AnnotationMirror?,
            autoValueAnnotation: AnnotationMirror?
        ): MessageModel {
            val simpleName = element.simpleName.toString()
            return MessageModel(
                enclosing,
                packageName,
                element.qualifiedName.toString(),
                simpleName,
                enclosing?.let { it.relativeName + "." + simpleName } ?: simpleName,
                element,
                declaredType,
                typeVars,
                superType,
                template,
                impl,
                wire,
                kot != null,
                lombokData != null,
                lombokValue != null,
                lombokBuilder != null,
                autoValueAnnotation != null
            )
        }

        fun ofClass(
            enclosing: EnclosingModel?,
            packageName: String,
            cls: Class<*>,
            declaredType: ParameterizedType?,
            typeVars: List<TypeVarModel>,
            superType: MessageModel?,
            template: Boolean,
            impl: Boolean,
            wire: Wire?
        ): MessageModel {
            return MessageModel(
                enclosing,
                packageName,
                cls.canonicalName,
                cls.simpleName,
                enclosing?.let { it.relativeName + "." + cls.simpleName } ?: cls.simpleName,
                cls,
                declaredType ?: cls,
                typeVars,
                superType,
                template,
                impl,
                wire,
                false,
                false,
                false,
                false,
                false
            )
        }
    }
}


class AccessorModel(
    val name: String,
    val isFluentNaming: Boolean,
    val isFluent: Boolean,
    val isPublic: Boolean) {
    companion object {

        fun setterOf(field: FieldModel, fluent: Boolean): AccessorModel {
            return AccessorModel(setterNameOf(field.name, fluent), fluent, fluent, true)
        }

        fun setterNameOf(fieldName: String, fluent: Boolean): String {
            return if (fluent) {
                fieldName
            } else "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1)
        }
    }
}


/**  */
data class FieldModel(
    // The field's name.
    val name: String,

    // The index.
//    val index: Int = 0

    // The flatbuffer table index.
//    var fbIndex: Int = 0

    // The name used within Json.
    val jsonName: String,

    // The javaType of data. Used the word javaType instead of Type to prevent the overuse of that
    // word.
    val model: WireModel,
    val type: Any? = null,

    // The enclosing message.
    val parent: MessageModel,

    // The original message the field was declared on.
    val declared: MessageModel,

    // The Protobuf getTag.
    val tag: Int = 0,
    val isPublic: Boolean = false,
    val isPackagePrivate: Boolean = false,
    val isFinal: Boolean = false,

    val packed: Boolean = true,

    val wire: Wire? = null,
    var getter: AccessorModel? = null,
    var setter: AccessorModel? = null
) {
//    var label: WireField.Label

    val isMutable: Boolean
        get() = !isFinal

    val isEnum: Boolean
        get() = model is EnumModel

    fun resolved(parent: MessageModel, kind: WireModel): FieldModel {
        return copy(parent = parent, model = kind)
    }
}

class FieldBuilder(val parent:MessageModel, val declared: MessageModel) {
    var name: String = ""
    var jsonName: String = ""
    var model: WireModel? = null
    var type: Any? = null
    var tag: Int = -1
    var isPublic: Boolean = false
    var isPackagePrivate: Boolean = false
    var isFinal: Boolean = false
    var wire: Wire? = null
    var getter: AccessorModel? = null
    var setter: AccessorModel? = null

    fun isValid() = model != null

    fun build(): FieldModel? {
        if (model == null) {
            return null
        }

        return FieldModel(
            parent = parent,
            declared = declared,
            name = name,
            jsonName = jsonName,
            model = model!!,
            type = type,
            tag = tag,
            isPublic = isPublic,
            isPackagePrivate = isPackagePrivate,
            isFinal = isFinal,
            wire = wire,
            getter = getter,
            setter = setter
        )
    }
}
