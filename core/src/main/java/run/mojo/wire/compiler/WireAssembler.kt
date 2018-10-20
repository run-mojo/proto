package run.mojo.wire.compiler

import com.squareup.javapoet.*
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.schema.ProtoType
import okio.ByteString
import run.mojo.wire.compiler.WireAssembler.Pkg
import run.mojo.wire.model.*

import javax.lang.model.element.Modifier
import java.io.IOException
import java.util.*
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.Stream

/** Assembles the completed Wire object model with all the supporting components.  */
class WireAssembler(val nameAllocator: NameAllocator, val packageMap: TreeMap<String, Pkg>) {

    internal fun protoFieldAdapterVar(name: String?): String {
        var name = name
        if (name == null) {
            name = ""
        }
        return PROTO_ADAPTER_FIELD_VAR_PREFIX + name
    }

    fun build() {
        packageMap.values.forEach { pkg -> pkg.build() }
    }

    internal fun toWireOuterName(prefix: String, enclosing: ClassName?, declared: DeclaredModel): ClassName {
        return if (enclosing == null) {
            ClassName.get(declared.packageName, prefix + declared.relativeName)
        } else {
            enclosing.nestedClass(declared.simpleName)
        }
    }

    /**  */
    class Pkg(val spec: PackageModel) {
        val nested: List<Declared<*>>

        init {

            this.nested = spec.nested.values
                .map { NestedMapper(this, null, ClassName.bestGuess(spec.name)) }
                .toList()
        }

        fun build() {
            nested.forEach { n -> n.buildWire() }
        }
    }

    /**  */
    class Impl(
        val impl: ImplModel, pkg: Pkg, enclosing: Enclosing<*>, model: MessageModel, wireOuter: ClassName
    ) : Message(pkg, enclosing, impl.message, wireOuter)

    abstract class Declared<T : DeclaredModel>(
        val pkg: Pkg,
        val enclosing: Enclosing<*>?,
        val name: ClassName,
        val model: T,
        val wireOuter: ClassName,
        nested: Stream<DeclaredModel>?
    ) {
        val nested: List<Declared<*>> = if (nested == null)
            emptyList()
        else
            nested
                .map(NestedMapper(pkg, enclosing, wireOuter))
                .collect(Collectors.toList()).map { it as Declared<*> }.toList()

        open fun buildWire(): TypeSpec? {
            return null
        }
    }

    private class NestedMapper(
        internal val pkg: Pkg,
        internal val enclosing: Enclosing<*>?,
        internal val wireOuter: ClassName
    ) : Function<DeclaredModel, Declared<*>?> {

        override fun apply(n: DeclaredModel): Declared<*>? {
            return if (n is MessageModel) {
                Message(
                    pkg, enclosing, n, wireOuter.nestedClass(n.simpleName)
                )
            } else if (n is EnumModel) {
                Enum(
                    pkg, enclosing, n, wireOuter.nestedClass(n.simpleName)
                )
            } else if (n is EnclosingModel) {
                Enclosing(
                    pkg, enclosing, n, wireOuter.nestedClass(n.simpleName)
                )
            } else if (n is ImplModel) {
                Impl(
                    n, pkg, enclosing, n.message, wireOuter.nestedClass(n.name)
                )
            } else {
                null
            }
        }
    }

    /** @param <T>
    </T> */
    open class Enclosing<T : EnclosingModel>(
        pkg: Pkg,
        enclosing: Enclosing<*>?,
        model: T,
        wireOuter: ClassName) : Declared<T>(
        pkg,
        enclosing,
        ClassName.bestGuess(model.name),
        model,
        wireOuter,
        model.nested.values.stream()) {

        fun simpleName(): String {
            return model.simpleName
        }

        override fun buildWire(): TypeSpec? {
            val outer = TypeSpec.interfaceBuilder(wireOuter)

            //      nested.stream().forEach(nested -> outer.addType(nested.buildWire()));

            return outer.build()
        }
    }

    /**  */
    open class Message(pkg: Pkg, enclosing: Enclosing<*>?, model: MessageModel, wireOuter: ClassName) :
        Enclosing<MessageModel>(pkg, enclosing, model, wireOuter) {

        val protoAdapterName: ClassName
        val jsonAdapterName: ClassName
        val protoAdapterVar: ProtoAssignment
        val fields: List<Field>
        var builder: MessageBuilder? = null
        internal var unknownField: Field? = null

        init {

            this.protoAdapterName = wireOuter.nestedClass(PROTO_ADAPTER_NAME)
            this.jsonAdapterName = wireOuter.nestedClass(JSON_ADAPTER_NAME)

            // Wire_MyType.PROTO
            this.protoAdapterVar = toProtoAdapter(model)

            // Create fields.
            fields = model
                .fields
                .values
                .stream()
                .map { f ->
                    Field(
                        f,
                        f.tag,
                        f.name,
                        toProtoAdapter(protoAdapterName, f.name, f.model)
                    )
                }
                .collect<List<Field>, Any>(Collectors.toList())
        }

        fun buildBuilder(): TypeSpec? {
            //      final TypeSpec.MessageBuilder builder = TypeSpec.classBuilder()
            return null
        }

        override fun buildWire(): TypeSpec? {
            val outer = TypeSpec.classBuilder(wireOuter)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)

            // WireMessage Adapter Var.
            outer.addField(
                FieldSpec.builder(
                    protoAdapterName,
                    ADAPTER_VAR_NAME,
                    Modifier.PUBLIC,
                    Modifier.STATIC,
                    Modifier.FINAL
                )
                    .initializer("new \$T()", protoAdapterName)
                    .build()
            )

            // Create WireMessage adapter.
            val protoAdapter = TypeSpec.classBuilder(protoAdapterName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .superclass(ParameterizedTypeName.get(PROTO_ADAPTER, name))
                .addMethod(
                    MethodSpec.constructorBuilder()
                        .addStatement("super(\$T.FIELD_DELIMITED, \$T.class)", FIELD_ENCODING, name)
                        .build()
                )

            // Declare adapter fields.
            for (field in fields) {
                val adapterField = field.proto.build()
                // Does this field need to cache a local adapter instance?
                if (adapterField != null) {
                    protoAdapter.addField(adapterField)
                }
            }

            //////////////////////////////////////////////////////////////////////
            // encodedSize
            //////////////////////////////////////////////////////////////////////

            run {
                // encodedSize
                val encodedSize = MethodSpec.methodBuilder("encodedSize")
                    .addAnnotation(Override::class.java)
                    .returns(TypeName.INT)
                    .addParameter(ParameterSpec.builder(name, "value").build())

                encodedSize.addStatement("int size = 0")
                for (field in fields) {
                    val nullable = !field.spec.model.javaKind.isPrimitive
                    if (nullable) {
                        encodedSize.beginControlFlow("if (value.\$L != null)", field.getterExpression())
                    }
                    encodedSize.addStatement(
                        "size += \$T.\$L.encodeSizeWithTag(writer, \$L, \$L)",
                        field.proto.varClass,
                        field.proto.varName,
                        field.tag,
                        field.getterExpression()
                    )
                    if (nullable) {
                        encodedSize.endControlFlow()
                    }
                }
                encodedSize.addStatement("return size")
                protoAdapter.addMethod(encodedSize.build())
            }

            //////////////////////////////////////////////////////////////////////
            // encode
            //////////////////////////////////////////////////////////////////////

            run {
                val encode = MethodSpec.methodBuilder("encode")
                    .addAnnotation(Override::class.java)
                    .returns(TypeName.VOID)
                    .addParameter(ParameterSpec.builder(PROTO_WRITER, "writer").build())
                    .addParameter(ParameterSpec.builder(name, "value").build())
                    .addException(IO_EXCEPTION)

                for (field in fields) {
                    val nullable = !field.spec.model.javaKind.isPrimitive

                    if (nullable) {
                        // Wrap in a "!= null" statement.
                        encode.beginControlFlow("if (value.\$L != null)", field.getterExpression())
                    }
                    encode.addStatement(
                        "\$T.\$L.encodeWithTag(writer, \$L, \$L)",
                        field.proto.varClass,
                        field.proto.varName,
                        field.tag,
                        field.getterExpression()
                    )
                    if (nullable) {
                        encode.endControlFlow()
                    }
                }
                protoAdapter.addMethod(encode.build())
            }

            //////////////////////////////////////////////////////////////////////
            // decode
            //////////////////////////////////////////////////////////////////////

            run {
                // decode
                val decode = MethodSpec.methodBuilder("decode")
                    .addAnnotation(Override::class.java)
                    .returns(name)
                    .addParameter(ParameterSpec.builder(PROTO_READER, "reader").build())
                    .addException(IO_EXCEPTION)

                // Create local vars.
                for (field in fields) {
                    //
                }

                decode.addStatement("\$T builder = new \$T()", name, name)
                decode.addStatement("long token = reader.beginMessage()")

                // Begin for loop.
                decode.beginControlFlow("for (int getTag; (getTag = reader.nextTag()) != -1;)")

                // Begin switch.
                decode.beginControlFlow("switch (getTag)")

                for (field in fields) {
                    val nullable = !field.spec.model.javaKind.isPrimitive

                    decode.beginControlFlow("case \$L:", field.tag)

                    decode.addStatement(
                        "\$T.\$L.encodeWithTag(writer, \$L, \$L)",
                        field.proto.varClass,
                        field.proto.varName,
                        field.tag,
                        field.getterExpression()
                    )

                    decode.endControlFlow("break")
                }

                // Unknown handling.
                decode.beginControlFlow("default:")

                decode.addStatement("\$T fieldEncoding = reader.peekFieldEncoding()", FIELD_ENCODING)
                decode.addStatement("Object value = fieldEncoding.rawProtoAdapter().decode(reader)")
                if (unknownField != null) {
                    // Add to unknown field.
                }

                decode.endControlFlow("break")

                // End switch.
                decode.endControlFlow()

                // End for loop.
                decode.endControlFlow()

                decode.addStatement("reader.endMessage(token)")
                decode.addStatement("return builder")
                protoAdapter.addMethod(decode.build())
            }

            outer.addType(protoAdapter.build())

            //      nested.forEach(nested -> outer.addType(nested.buildWire()));

            return outer.build()
        }
    }

    class MessageBuilder(val message: Message, val name: ClassName)

    /**  */
    class Field(
        val spec: FieldModel, val tag: Int, val name: String, // ProtoAdapter static expression.
        val proto: ProtoAssignment
    ) {

        fun getterExpression(): String {
            return ""
        }
    }

    /**  */
    class Enum(pkg: Pkg, enclosing: Enclosing<*>, model: EnumModel, wireOuter: ClassName) :
        Declared<EnumModel>(pkg, enclosing, ClassName.bestGuess(model.name), model, wireOuter, Stream.empty()) {

        // {package}.Proto_
        val protoAdapterName: ClassName
        val jsonAdapterName: ClassName
        val constants: List<EnumConstant>

        var processed: Long = 0

        init {
            this.protoAdapterName = wireOuter.nestedClass(PROTO_ADAPTER_NAME)
            this.jsonAdapterName = wireOuter.nestedClass(JSON_ADAPTER_NAME)
            this.constants = model.constants
        }

        fun build(): TypeSpec? {
            return null
        }
    }

    companion object {

        val PROTO_PREFIX = "Proto_"
        val JSON_PREFIX = "Json_"
        val WIRE_PREFIX = "Wire_"
        val WIRE_SUFFIX = "Wire_"
        val PROTO_ADAPTER_NAME = "_Proto"
        val JSON_ADAPTER_NAME = "_Json"
        val PROTO_VAR_NAME = "PROTO"
        val PROTO_ADAPTER_FIELD_VAR_PREFIX = "__"

        val ADAPTER_VAR_NAME = "ADAPTER"
        internal val PROTO_ADAPTER = ClassName.get(ProtoAdapter<*>::class.java)
        internal val FIELD_ENCODING = ClassName.get(FieldEncoding::class.java)
        internal val PROTO_WRITER = ClassName.get(ProtoWriter::class.java)
        internal val PROTO_READER = ClassName.get(ProtoReader::class.java)
        internal val IO_EXCEPTION = ClassName.get(IOException::class.java)

        // WireMessage adapter types.
        internal val BOOL = toBuiltinProtoAdapter(Boolean::class.java, "BOOL")
        internal val INT32 = toBuiltinProtoAdapter(Int::class.java, "INT32")
        internal val UINT32 = toBuiltinProtoAdapter(Int::class.java, "UINT32")
        internal val SINT32 = toBuiltinProtoAdapter(Int::class.java, "SINT32")
        internal val FIXED32 = toBuiltinProtoAdapter(Int::class.java, "FIXED32")
        internal val SFIXED32 = toBuiltinProtoAdapter(Int::class.java, "SFIXED32")
        internal val INT64 = toBuiltinProtoAdapter(Long::class.java, "INT64")
        internal val UINT64 = toBuiltinProtoAdapter(Long::class.java, "UINT64")
        internal val SINT64 = toBuiltinProtoAdapter(Long::class.java, "SINT64")
        internal val FIXED64 = toBuiltinProtoAdapter(Long::class.java, "FIXED64")
        internal val SFIXED64 = toBuiltinProtoAdapter(Long::class.java, "SFIXED64")
        internal val FLOAT = toBuiltinProtoAdapter(Float::class.java, "FLOAT")
        internal val DOUBLE = toBuiltinProtoAdapter(Double::class.java, "DOUBLE")
        internal val STRING = toBuiltinProtoAdapter(String::class.java, "STRING")
        internal val BYTES = toBuiltinProtoAdapter(ByteString::class.java, "BYTES")

        fun create(processor: ModelBuilder): WireAssembler {
            return create(processor.packages.values)
        }

        fun create(packages: Collection<PackageModel>): WireAssembler {

            val pkgs = packages.stream().map { Pkg(it) }.collect<List<Pkg>, Any>(Collectors.toList())
            val nameAllocator = NameAllocator()

            // Resolve.

            return WireAssembler(
                nameAllocator,
                pkgs.stream()
                    .collect(
                        Collectors.toMap(
                            { k -> k.spec.name },
                            { it },
                            { p1, p2 -> p2 },
                            { TreeMap<String, Pkg>() })
                    )
            )
        }

        /**
         * @param declared
         * @return
         */
        internal fun toWireOuter(declared: DeclaredModel): ClassName {
            val packageName = declared.packageName
            var name = declared.name

            if (!name.startsWith(packageName) || packageName.length == name.length) {
                throw IllegalStateException(
                    "EnclosingSpec '$name' is not inside package '$packageName'"
                )
            }

            // Remove package and '.' prefix
            name = name.substring(packageName.length + 1)

            // Return the 'Wire_' outer class.
            return ClassName.get(packageName, WIRE_PREFIX + name)
        }

        internal fun toBuiltinProtoAdapter(type: Class<*>, varName: String): ProtoAssignment {
            return ProtoAssignment(
                ParameterizedTypeName.get(PROTO_ADAPTER, ClassName.get(type)), PROTO_ADAPTER, varName
            )
        }

        internal fun toProtoAdapter(spec: WireModel): ProtoAssignment {
            return toProtoAdapter(null, null, spec)
        }

        internal fun toProtoAdapter(
            protoAdapterName: ClassName?, fieldName: String?, spec: WireModel
        ): ProtoAssignment {
            val protoType = spec.protoType
            if (protoType.isScalar) {
                when (protoType.simpleName().toLowerCase()) {
                    "bool" -> return BOOL
                    "bytes" -> return BYTES
                    "double" -> return DOUBLE
                    "float" -> return FLOAT
                    "fixed32" -> return FIXED32
                    "fixed64" -> return FIXED64
                    "int32" -> return INT32
                    "int64" -> return INT64
                    "sfixed32" -> return SFIXED32
                    "sfixed64" -> return SFIXED64
                    "sint32" -> return SINT32
                    "sint64" -> return SINT64
                    "string" -> return STRING
                    "uint32" -> return UINT32
                    "uint64" -> return UINT64

                    else -> throw IllegalStateException(
                        "Scalar ProtoType not recognized: " + protoType.simpleName().toLowerCase()
                    )
                }
            }

            if (spec.isList) {
                val listSpec = spec as ListModel

                val component = toProtoAdapter(listSpec.component)
                return ProtoListAssignment(
                    ParameterizedTypeName.get(
                        ClassName.get(ProtoAdapter::class.java), listSpec.component.toTypeName(true)
                    ),
                    protoAdapterName,
                    fieldName,
                    component,
                    spec.isPacked
                )
            }

            if (spec.isMap) {
                val mapSpec = spec as MapModel
                val keyName = mapSpec.key.toTypeName(true)
                val valueName = mapSpec.key.toTypeName(true)

                val key = toProtoAdapter(mapSpec.key)
                val value = toProtoAdapter(mapSpec.value)

                return ProtoMapAssignment(
                    ParameterizedTypeName.get(
                        ClassName.get(ProtoAdapter::class.java),
                        ParameterizedTypeName.get(ClassName.get(Map::class.java), keyName, valueName)
                    ),
                    protoAdapterName,
                    fieldName,
                    key,
                    value
                )
            }

            if (spec !is DeclaredModel) {
                throw IllegalStateException(
                    "WireSpec: '" + spec.toString() + "' toProtoAdapter() cannot determine type"
                )
            }

            val wireOuter = toWireOuter(spec as DeclaredModel)

            return ProtoAssignment(
                ParameterizedTypeName.get(PROTO_ADAPTER, (spec as WireModel).toTypeName(true)),
                wireOuter,
                PROTO_VAR_NAME
            )
        }
    }
}


internal enum class NamingStrategy {
    PREFIX,
    SUFFIX,
    PREFIX_AND_SUFFIX
}

enum class Mutability {
    /** The message is also the builder. Mutable.  */
    MUTABLE,

    /** No builder is used and the message is immutable.  */
    IMMUTABLE,

    /**
     * A separate object is used for building a message. The final message is usually immutable but
     * this pattern may apply to mutable messages.
     */
    BUILDER
}

class StaticVar(val signature: TypeName, val location: ClassName, val name: String)

/**
 *
 */
internal open class ProtoAssignment(
    val signature: ParameterizedTypeName,
    val varClass: ClassName,
    val varName: String
) {

    open fun cache(): Boolean {
        return false
    }

    open fun initializer(): CodeBlock {
        return CodeBlock.of("\$T.\$L", varClass, varName)
    }

    open fun build(): FieldSpec? {
        return null
    }
}

/**
 *
 */
internal class ProtoListAssignment(
    signature: ParameterizedTypeName,
    varClass: ClassName,
    varName: String,
    val component: ProtoAssignment,
    val packed: Boolean
) : ProtoAssignment(signature, varClass, varName) {

    override fun cache(): Boolean {
        return true
    }

    override fun initializer(): CodeBlock {
        return CodeBlock.builder()
            .add(component.initializer())
            .add(if (packed) CodeBlock.of("asPacked()") else CodeBlock.of("asRepeated()"))
            .build()
    }

    override fun build(): FieldSpec? {
        return FieldSpec.builder(signature, varName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer(initializer())
            .build()
    }
}

/**
 *
 */
internal class ProtoMapAssignment(
    signature: ParameterizedTypeName,
    varClass: ClassName,
    varName: String,
    val key: ProtoAssignment,
    val value: ProtoAssignment
) : ProtoAssignment(signature, varClass, varName) {

    override fun cache(): Boolean {
        return true
    }

    override fun initializer(): CodeBlock {
        return CodeBlock.builder()
            .add(CodeBlock.of("\$T.newMapAdapter("))
            .add(key.initializer())
            .add(", ")
            .add(value.initializer())
            .add(")")
            .build()
    }

    override fun build(): FieldSpec? {
        return FieldSpec.builder(signature, varName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer(
                "\$T.newMapAdapter(\$T.\$L, \$T.\$L)",
                ClassName.get(ProtoAdapter<*>::class.java),
                key.varClass,
                key.varName,
                value.varClass,
                value.varName
            )
            .build()
    }
}

internal class JsonAssignment

internal class JsonListAssignment

internal class JsonMapAssignment