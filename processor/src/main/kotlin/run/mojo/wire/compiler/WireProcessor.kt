package run.mojo.wire.compiler

import com.google.auto.value.AutoValue
import run.mojo.wire.JavaKind
import run.mojo.wire.Wire
import run.mojo.wire.model.*
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.*
import javax.lang.model.element.ElementKind.INTERFACE
import javax.lang.model.type.*
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

class WireProcessor(
    internal val types: Types,
    internal val elements: Elements,
    internal val messager: Messager,
    internal val filer: Filer,
    internal val config: Config
) : ModelBuilder() {

    fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Mojo WireMessage Processor")
        config.actionLinkers.forEach { messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "ActionLinker: $it") }

        // Handle types.
        config.annotations.forEach { annotation -> }

        if (config.lombokData) {
            processAnnotatedWith(annotations, ModelBuilder.LOMBOK_DATA, roundEnv)
        }
//        processAnnotatedWith(annotations, ModelBuilder.LOMBOK_VALUE, roundEnv)
        processAnnotatedWith(annotations, Wire::class.java.canonicalName, roundEnv)

        // Find schema schemas.
//        roundEnv
//            .getElementsAnnotatedWith(ImportModels::class.java)
//            .stream()
//            .map { e ->
//                messager.printMessage(
//                    Diagnostic.Kind.WARNING, "FOUND @ImportModels: " + (e as Element).toString()
//                )
//                e
//            }
//            .map { el ->
//                (el as Element)
//                    .annotationMirrors
//                    .stream()
//                    .filter { a ->
//                        a.annotationType
//                            .toString() == ImportModels::class.java.canonicalName
//                    }
//                    .findFirst()
//                    .orElse(null)
//            }
//            .filter(Predicate<out AnnotationMirror> { Objects.nonNull(it) })
//            .map<DeclaredType>(Function<out AnnotationMirror, DeclaredType> { it.getAnnotationType() })
//            .filter(Predicate<DeclaredType> { Objects.nonNull(it) })
//            .forEach { values ->
//                val el = values.asElement()
//                if (el !is TypeElement) {
//                    return@roundEnv
//                        .getElementsAnnotatedWith(
//                            ImportModels.class)
//                                .stream()
//                                .map(
//                                    e -> {
//                        messager.printMessage(
//                            Diagnostic.Kind.WARNING, "FOUND @ImportModels: " + (Element e).toString()
//                        );
//                        return e;
//                    })
//                    .map(
//                        el ->
//                    (Element el)
//                        .getAnnotationMirrors()
//                        .stream()
//                        .filter(
//                            a ->
//                    a.getAnnotationType()
//                        .toString()
//                        .equals(ImportModels.class. getCanonicalName ()))
//                    .findFirst()
//                        .orElse(null))
//                    .filter(Objects::nonNull)
//                        .map(AnnotationMirror::getAnnotationType)
//                        .filter(Objects::nonNull)
//                        .forEach
//                }
//
//                val element = el as TypeElement
//
//                messager.printMessage(Diagnostic.Kind.WARNING, values.toString())
//                messager.printMessage(
//                    Diagnostic.Kind.WARNING,
//                    values.toString()
//                            + " : "
//                            + element.enclosedElements[0].enclosedElements.size
//                )
//            }

//        roundEnv
//            .getElementsAnnotatedWith(ImportAction::class.java)
//            .stream()
//            .forEach { element ->
//                messager.printMessage(
//                    Diagnostic.Kind.WARNING, "WIRE RPC! -> " + (element as Element).toString()
//                )
//                val rpc = element.getAnnotation(ImportAction::class.java)
//                //
//                //      Arrays.stream(rpc.request().getDeclaredFields()).forEach(field -> {
//                //        messager.printMessage(Kind.WARNING, "field: " + field.getName() + " : " +
//                // field.getType());
//                //      });
//                element
//                    .annotationMirrors
//                    .stream()
//                    .forEach { annotation ->
//                        val type = (annotation as AnnotationMirror).annotationType
//                        val el = type.asElement() as TypeElement
//                        if (el.qualifiedName
//                                .toString() != ImportAction::class.java.canonicalName
//                        ) {
//                            return @(Element element)
//                                .getAnnotationMirrors()
//                                .stream()
//                                .forEach
//                        }
//
//                        messager.printMessage(Diagnostic.Kind.WARNING, type.toString())
//                        annotation
//                            .elementValues
//                            .forEach { name, value ->
//                                val v = value as AnnotationValue
//
//                                if (v.value is DeclaredType) {
//                                    registerMirror(v.value as TypeMirror)
//                                }
//                            }
//                    }
//            }

        // Write
//        if (!roundEnv.rootElements.contains("example.HelloMojo")) {
//            val typeBuilder = TypeSpec.classBuilder("HelloMojo").addModifiers(Modifier.PUBLIC)
//
//            try {
//                JavaFile.builder("example", typeBuilder.build()).build().writeTo(filer)
//            } catch (e: Throwable) {
//                // Ignore.
//                messager.printMessage(
//                    Diagnostic.Kind.WARNING, "Failed to generate Source File: " + e.message
//                )
//            }
//
//        }
        return true
    }

    private fun processAnnotatedWith(
        annotations: Set<TypeElement>, name: String, roundEnv: RoundEnvironment
    ) {
        annotations
            .stream()
            .filter { f -> name == f.qualifiedName.toString() }
            .forEach { a ->
                roundEnv
                    .getElementsAnnotatedWith(a)
                    .filter { element -> element is TypeElement }
                    .map { element -> element as TypeElement }
                    .forEach { element ->
                        try {
                            registerMirror(element.asType())
                        } catch (e: Throwable) {
                            if (element.asType() == null) {
                                messager.printMessage(Diagnostic.Kind.ERROR, "NULLLLLLLLLLLL")
                            }
                            e.printStackTrace()
                            messager.printMessage(Diagnostic.Kind.ERROR, e.toString())
                            messager.printMessage(Diagnostic.Kind.ERROR, element.toString())
                            //                  e.printStackTrace();
                            //
                            // messager.printMessage(Kind.WARNING, e.getMessage());
                        }
                    }
            }
    }

    override fun printMessage(kind: Diagnostic.Kind, msg: CharSequence) {
        messager.printMessage(kind, msg)
    }

    override fun register(type: Any): WireModel? {
        if (type is TypeMirror) {
            return registerMirror(type)
        }
        return if (type is Element) {
            registerMirror(type.asType())
        } else null
    }

    private fun toMessage(type: DeclaredType, element: TypeElement?): EnclosingModel? {
        if (element == null) {
            return null
        }

        //      type.getEnclosedElements().stream().forEach(e -> printMessage(Kind.WARNING,
        // e.toString()));
        //          messager.printMessage(Kind.WARNING, "ANNOTATIONS: " +
        //       type.getAnnotationMirrors().size());
        //          type
        //              .getAnnotationMirrors()
        //              .forEach(
        //                  a -> {
        //                    messager.printMessage(Kind.WARNING, "       -> " + a.toString());
        //                  });

        val name = element.qualifiedName.toString()
        val existing = getEnclosing(name)
        // Enclosing and Template types must be recreated. Template types may have it's Type Args
        // specified.
        if (existing != null && !existing.isEnclosing && !existing.isTemplate) {
            return existing
        }

        val pkg = packageOf(elements.getPackageOf(element))

        val template: Boolean
        val impl: Boolean

        var typeVariables: List<TypeVarModel> = emptyList()
        if (type.typeArguments != null && !type.typeArguments.isEmpty()) {
            typeVariables = type.typeArguments.map { TypeVarModel.of(it) }.toList()
            template = true
            impl = typeVariables.count { !it.isVariable } == typeVariables.size
        } else if (element.modifiers.contains(Modifier.ABSTRACT)) {
            template = true
            impl = false
        } else {
            template = false
            impl = false
        }

        val superTypeElement = element.superclass
        var superType: MessageModel? = null
        if (superTypeElement.kind == TypeKind.DECLARED
            && element.superclass.toString() != JAVA_LANG_OBJECT
            && superTypeElement is DeclaredType
        ) {
            superType = toMessage(superTypeElement, superTypeElement.asElement() as TypeElement) as MessageModel?
        }

        val message = MessageModel.ofElement(
            enclosing = getEnclosing(pkg, element),
            packageName = pkg.name,
            element = element,
            declaredType = type,
            typeVars = typeVariables,
            superType = superType,
            template = template,
            impl = impl,

            // Annotations.
            wire = element.getAnnotation(Wire::class.java),

            kot = element.annotationMirrors
                .filter { a -> a.toString().contains(KOTLIN_METADATA) }
                .firstOrNull(),
            lombokData = element.annotationMirrors
                .filter { a -> a.toString().contains(LOMBOK_DATA) }
                .firstOrNull(),
            lombokValue = element.annotationMirrors
                .filter { a -> a.toString().contains(LOMBOK_VALUE) }
                .firstOrNull(),
            lombokBuilder = element.annotationMirrors
                .filter { a -> a.toString().contains(LOMBOK_BUILDER) }
                .firstOrNull(),
            autoValueAnnotation = element.annotationMirrors
                .filter { a -> a.toString().contains(AutoValue::class.java.canonicalName) }
                .firstOrNull()
        )

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

        // Model fields.
        element
            .enclosedElements
            .filter { el -> el is VariableElement }
            .map { el -> el as VariableElement }
            .filter { it != null }
            .filter { v -> !v.modifiers.contains(Modifier.STATIC) && !v.modifiers.contains(Modifier.NATIVE) }
            .forEach { variable -> message.registerField(this, variable) }

        // Model getters and setters.
        element
            .enclosedElements
            .filter { el -> el is ExecutableElement }
            .map { el -> el as ExecutableElement }
            // Ignore constructors.
            .filter { el -> !el.simpleName.toString().startsWith("<") }
            .forEach { executable -> message.matchAccessorElement(this, executable) }

        // Model constructors.
        element
            .enclosedElements
            .filter { e -> (e as Element).kind == ElementKind.CONSTRUCTOR }
            .map { e -> e as ExecutableElement }
            .map { ctor -> message.registerCtor(ctor) }
            .filter { ctor -> ctor != null }
            .forEach { }

        //      message
        //          .constructors
        //          .stream()
        //          .forEach(
        //              ctor -> {
        //                messager.printMessage(
        //                    Kind.WARNING,
        //                    message.type.toString() + " CTOR -> " + ctor.type.toString());
        //              });

        printMessage(Diagnostic.Kind.WARNING, "Finished processing type: " + message.toString())
        printMessage(Diagnostic.Kind.WARNING, "Impl: " + message.impl)
        printMessage(Diagnostic.Kind.WARNING, "Template: " + message.isTemplate)

        // Print out the fields if it's a concrete javaKind.
        //      if (!message.isTemplate()) {
        message
            .fields
            .values
            .forEach { field ->
                messager.printMessage(
                    Diagnostic.Kind.WARNING,
                    element.toString()
                            + "."
                            + field.name
                            + " = ["
                            + field.tag
                            + "] : "
                            + field.model
                )
            }
        //      }

        return message
    }

    fun registerMirror(type: TypeMirror?): WireModel? =
        when (type) {
            null -> null
            is TypeVariable -> TemplateModel((type as TypeVariable?)!!)
            is WildcardType -> WildcardModel(
                type,
                if (type.superBound == null)
                    emptyArray()
                else
                    registerMirror(type.superBound)?.let { arrayOf(it) } ?: emptyArray(),
                if (type.extendsBound == null)
                    emptyArray()
                else
                    registerMirror(type.extendsBound)?.let { arrayOf(it) } ?: emptyArray()
            )
            is ArrayType -> toArrayKind(type)
            is DeclaredType -> {
                when (type.toString()) {
                    JAVA_LANG_BOOLEAN -> ModelBuilder.BOOL_BOXED
                    JAVA_LANG_BYTE -> ModelBuilder.BYTE_BOXED
                    JAVA_LANG_SHORT -> ModelBuilder.SHORT_BOXED
                    JAVA_LANG_CHARACTER -> ModelBuilder.CHAR_BOXED
                    JAVA_LANG_INTEGER -> ModelBuilder.INT_BOXED
                    JAVA_LANG_LONG -> ModelBuilder.LONG_BOXED
                    JAVA_LANG_FLOAT -> ModelBuilder.FLOAT_BOXED
                    JAVA_LANG_DOUBLE -> ModelBuilder.DOUBLE_BOXED
                    JAVA_LANG_STRING -> ModelBuilder.STRING
                    else -> {
                        val element = type.asElement()
                        if (element is TypeElement) {
                            if (inherits(element, JAVA_UTIL_LIST_UNTYPED)) {
                                toListKind(JavaKind.LIST, type)
                            } else if (inherits(element, JAVA_UTIL_ARRAYLIST_UNTYPED)) {
                                toListKind(JavaKind.LIST, type)
                            } else if (inherits(element, JAVA_UTIL_SET_UNTYPED)) {
                                toListKind(JavaKind.SET, type)
                            } else if (inherits(element, JAVA_UTIL_MAP_UNTYPED)) {
                                toMapKind(type)
                            } else if (inherits(element, JAVA_UTIL_QUEUE_UNTYPED)) {
                                toListKind(JavaKind.QUEUE, type)
                            } else if (inherits(element, JAVA_LANG_ENUM)) {
                                toEnumKind(element)
                            } else if (element.kind == ElementKind.CLASS) {
                                toMessage(type, element)
                            } else {
                                null as WireModel?
                            }
                        } else {
                            null as WireModel?
                        }
                    }
                }
            }
            else -> when (type.kind) {
                TypeKind.BOOLEAN -> BOOL
                TypeKind.BYTE -> BYTE
                TypeKind.SHORT -> SHORT
                TypeKind.INT -> INT
                TypeKind.LONG -> LONG
                TypeKind.CHAR -> CHAR
                TypeKind.FLOAT -> FLOAT
                TypeKind.DOUBLE -> DOUBLE
                else -> null
//                TypeKind.VOID -> TODO()
//                TypeKind.NONE -> TODO()
//                TypeKind.NULL -> TODO()
//                TypeKind.ARRAY -> TODO()
//                TypeKind.DECLARED -> TODO()
//                TypeKind.ERROR -> TODO()
//                TypeKind.TYPEVAR -> TODO()
//                TypeKind.WILDCARD -> TODO()
//                TypeKind.PACKAGE -> TODO()
//                TypeKind.EXECUTABLE -> TODO()
//                TypeKind.OTHER -> TODO()
//                TypeKind.UNION -> TODO()
//                TypeKind.INTERSECTION -> TODO()
            }
        }

    /**
     * @param mirror
     * @return
     */
    private fun toArrayKind(mirror: TypeMirror): ListModel? {
        val arrayType = mirror as ArrayType
        val kind = registerMirror(arrayType.componentType)
        return if (kind == null) null else ListModel(JavaKind.ARRAY, kind)
    }

    /**
     * @param type
     * @param mirror
     * @return
     */
    private fun toListKind(type: JavaKind, mirror: TypeMirror): ListModel? {
        // Must be a DeclaredType.
        if (mirror !is DeclaredType) {
            messager.printMessage(
                Diagnostic.Kind.ERROR, "expected List javaKind to be a DeclaredType: $mirror"
            )
        }

        // Get first Type Arg.
        val typeArg = getTypeArg(mirror, 0)
        when (typeArg!!.kind) {
            TypeKind.TYPEVAR -> {
                // Template.
                val typeVar = typeArg as TypeVariable?
                return ListModel(type, TemplateModel(typeVar!!))
            }

            TypeKind.DECLARED ->
                // Impl.
                return ListModel(type, registerMirror(typeArg)!!)

            TypeKind.WILDCARD -> {
                // Wildcard
                return ListModel(type, registerMirror(typeArg)!!)
            }

            else -> messager.printMessage(
                Diagnostic.Kind.ERROR,
                "javaKind argument for '"
                        + mirror
                        + "' was not a TYPEVAR or DECLARED instead it was '"
                        + typeArg.kind
                        + "'"
            )
        }

        return null
    }

    /**
     * @param mirror
     * @return
     */
    private fun toMapKind(mirror: TypeMirror): MapModel? {
        if (mirror !is DeclaredType) {
            return null
        }
        val element = mirror.asElement() as? TypeElement ?: return null

        val keyType = getTypeArg(mirror, 0)
        val valueType = getTypeArg(mirror, 1)

        val key = registerMirror(keyType)
        val value = registerMirror(valueType)
        return if (key == null || value == null) {
            null
        } else MapModel(key, value)
        //    messager
        //        .printMessage(Kind.WARNING, "map value: " + key.toString() + " -> " +
        // value.toString());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Enum
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun toEnumKind(element: TypeElement): EnumModel {
        getEnum(element.qualifiedName.toString())?.let { return it }

        val packageElement = elements.getPackageOf(element)
        val pkg = packageOf(packageElement)

        val enclosing = getEnclosing(pkg, element)
        val relativeName =
            if (enclosing != null)
                enclosing.relativeName + "." + element.simpleName.toString()
            else
                element.simpleName.toString()

        var ordinal = 0
        var lastTag = 0
        val constants = mutableListOf<EnumConstant>()

        val relativeNameBase = relativeName.replace(".", "_")
        for (enclosed in element.enclosedElements) {
            //      messager.printMessage(Kind.WARNING,
            //          "ENUM TYPE: " + enclosed.toString() + "  -> " + enclosed.toString());

            if (enclosed.kind == ElementKind.ENUM_CONSTANT) {
                val name = enclosed.simpleName.toString()

                val constant = EnumConstant(
                    ordinal = ordinal++,
                    tag = enclosed.getAnnotation(Wire::class.java)?.let { it.tag } ?: lastTag+1,
                    name = name,
                    relativeName = relativeNameBase + "_" + name
                )

                constants += constant

                lastTag = constant.tag
            }
        }

        val model = EnumModel.ofElement(
            enclosing,
            packageElement?.qualifiedName?.toString() ?: "",
            relativeName,
            element,
            constants
        )

        // Register in schema.
        add(model)

        return model
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Enclosing Path
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun getEnclosing(pkg: PackageModel, element: Element): EnclosingModel? {
        val enclosing = element.enclosingElement
        if (enclosing == null || enclosing.kind != ElementKind.INTERFACE && enclosing.kind != ElementKind.CLASS) {
            return null
        }

        val enclosingType = enclosing as TypeElement
        var enclosingMirror: EnclosingModel? = getEnclosing(enclosingType.qualifiedName.toString())
        if (enclosingMirror == null) {
            val e = getEnclosing(pkg, enclosing)
            // Create an Enclosing holder.
            enclosingMirror = EnclosingModel(
                enclosing = getEnclosing(pkg, enclosing),
                packageName = pkg.name,
                name = enclosingType.qualifiedName.toString(),
                simpleName = enclosingType.simpleName.toString(),
                relativeName = e?.let {
                    it.relativeName + "." + enclosingType.simpleName.toString()
                } ?: enclosingType.simpleName.toString(),
                type = enclosingType
            )
            add(enclosingMirror)
        }

        return enclosingMirror
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Message
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    fun inherits(el: Element, name: String): Boolean {
        // Must be a TypeElement.
        if (el !is TypeElement) {
            return false
        }

        // Check the type sans generics.
        if (types.asElement(types.erasure(el.asType())).toString() == name) {
            return true
        }

        // Check the type.
        if (el.toString() == name) {
            return true
        }

        // Check superclass.
        val erasureSuper = types.erasure(el.superclass)
        if (erasureSuper != null) {
            val superName = erasureSuper.toString()
            if (superName == name) return true
            // Stop at java.lang.Object
            if (superName != JAVA_LANG_OBJECT) {
                val superElement = types.asElement(erasureSuper)

                if (superElement != null && inherits(superElement, name)) {
                    return true
                }
            }
        }

        // Search interfaces.
        val ifaces = el.interfaces
        if (ifaces != null && !ifaces.isEmpty()) {
            for (iface in ifaces) {
                if (inherits(types.asElement(types.erasure(iface)), name)) {
                    return true
                }
            }
        }

        return false
    }

    fun getTypeArg(typeMirror: TypeMirror, index: Int): TypeMirror? {
        if (index < 0) {
            return null
        }
        if (typeMirror !is DeclaredType) {
            return null
        }

        val typeArgs = typeMirror.typeArguments
        return if (typeArgs == null || index >= typeArgs.size) {
            null
        } else typeMirror.typeArguments[index]

    }

    /**
     * This returns the class name of the javaKind as one would use to reference in code. For most
     * cases, this is pretty straightforward. Inner messages are used with . notation, i.e., if class
     * Y is an inner class of class X, then class Y's class name should be X.Y.
     */
    fun getClassName(type: TypeElement, packageName: String): String {
        val packageLen = packageName.length + 1
        return type.qualifiedName.toString().substring(packageLen)
    }

    /**
     * This returns the prefix used to refer to the generated class. This is different because we
     * generate individual source files for each inner class. For instance, if we have class X with
     * inner messages Y and Z, then we generate three source files.
     *
     *
     * To make this work, we replace the normal dot notation between an outer class and an inner
     * class with a '_', i.e., the generated class for class X will be X_Y&lt;suffix&gt;.
     */
    fun getPrefixForGeneratedClass(type: TypeElement, packageName: String): String? {
        // Interfaces do not currently generate messages
        if (type.kind == INTERFACE) {
            return null
        }
        val packageLen = packageName.length + 1
        return type.qualifiedName.toString().substring(packageLen).replace('.', '_')
    }

    fun getPackageName(elements: Elements, type: TypeElement): String {
        return elements.getPackageOf(type).qualifiedName.toString()
    }

    /**
     * Returns a string with javaKind parameters replaced with wildcards. This is slightly different
     * from [Types.erasure], which removes all javaKind parameter data.
     *
     *
     * For instance, if there is a field with javaKind List&lt;String&gt;, this returns a string
     * List&lt;?&gt;.
     */
    private fun getCanonicalTypeName(declaredType: DeclaredType): String {
        val typeArguments = declaredType.typeArguments
        if (!typeArguments.isEmpty()) {
            val typeString = StringBuilder(declaredType.asElement().toString())
            typeString.append('<')
            for (i in typeArguments.indices) {
                if (i > 0) {
                    typeString.append(',')
                }
                typeString.append('?')
            }
            typeString.append('>')

            return typeString.toString()
        } else {
            return declaredType.toString()
        }
    }

    companion object {

        fun create(processingEnv: ProcessingEnvironment, config: Config): WireProcessor {
            val messager = processingEnv.messager
            val types = processingEnv.typeUtils
            val elements = processingEnv.elementUtils
            val filer = processingEnv.filer

            return WireProcessor(
                processingEnv.typeUtils,
                processingEnv.elementUtils,
                processingEnv.messager,
                processingEnv.filer,
                config
            )
        }
    }
}
