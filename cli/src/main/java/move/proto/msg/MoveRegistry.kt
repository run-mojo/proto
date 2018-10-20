package move.proto.msg

import com.google.common.collect.Sets
import com.google.common.reflect.ClassPath
import com.movemedical.server.essentials.docreport.MoveDocReportGeneratorVerticle
import com.squareup.wire.ProtoAdapter
import io.grpc.*
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.stub.ServerCalls
import io.grpc.stub.StreamObserver
import io.movemedical.server.essentials.verticle.*
import kotlinx.coroutines.experimental.*
import run.mojo.wire.type.ReflectionSchema
import run.mojo.wire.type.ReflectionSchema.RpcEntry
import run.mojo.wire.type.RpcInfo
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.ParameterizedType
import java.util.function.Function

/**
 * Move's reflection registry.
 */
@Suppress("WARNINGS")
object MoveRegistry {

    @Throws(IOException::class)
    fun create(): ReflectionSchema {
        //    log.warn("Searching classpath for descriptors and actions...");

//        val r = CreateDeliveryScheduleProfileApi.Request()
//        r.name = "hi proto"
//        r.implantDeliveryLeadTimeDays = 10
//
//        val buffer = Buffer()
//        val reader = ProtoReader(buffer)
//        val writer = ProtoWriter(buffer)
//
//        ProtoAdapter.STRING.encodeWithTag(writer, 1, r.name)
//        ProtoAdapter.INT32.encodeWithTag(writer, 2, r.implantDeliveryLeadTimeDays)
//        buffer.flush()
//
//        val builder = CreateDeliveryScheduleProfileApi.Request()
//        val token = reader.beginMessage()
//
//        var getTag = reader.nextTag()
//        while (getTag != -1) {
//            when (getTag) {
//                1 -> builder.name = ProtoAdapter.STRING.decode(reader)
//                2 -> builder.implantDeliveryLeadTimeDays = ProtoAdapter.INT32.decode(reader)
//                else -> {
//                    val fieldEncoding = reader.peekFieldEncoding()
//                    val value = fieldEncoding.rawProtoAdapter().decode(reader)
//                    //                    builder.addUnknownField(getTag, fieldEncoding, value);
//                }
//            }
//            getTag = reader.nextTag()
//        }
//        reader.endMessage(token)

        return ReflectionSchema.create(
            // Filter candidates.
            ReflectionSchema.all().allClasses.stream().filter {
                it.name.startsWith("com.movemedical")
                        || it.name.startsWith("io.movemedical")
            },
            Adapter(),
            // Whitelist.
            Function { cls: Class<*> -> true },
            // Blacklist.
            Function { cls -> false },
            // RPC adapter.
            Function { cls ->
                try {
                    val a = cls.getAnnotation(Rest::class.java)
                    if (a == null || a !is Rest) {
                        return@Function null
                    }
                    val info = RpcInfo()
                    info.paths = a.paths
                    info.secured = a.secured
                } catch (e: Throwable) {
                    // Ignore.
                }

                null
            }
        )
    }

    fun classpath(): ClassPath {
        try {
            return ClassPath.from(Thread.currentThread().contextClassLoader)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }


    data class Message(val id: Int)



    class WireMarshaller<T>(val adapter: ProtoAdapter<T>) : MethodDescriptor.Marshaller<T> {
        override fun stream(value: T): InputStream {
            return ByteArrayInputStream(adapter.encode(value))
        }

        override fun parse(stream: InputStream?): T {
            return adapter.decode(stream!!)
        }
    }



    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val method = MethodDescriptor.newBuilder<String, Int>()
            .setRequestMarshaller(WireMarshaller(ProtoAdapter.STRING))
            .setResponseMarshaller(WireMarshaller(ProtoAdapter.INT32))
            .setSchemaDescriptor(Object())
            .setFullMethodName("/some.name/Method")
            .setIdempotent(true)
            .setType(MethodDescriptor.MethodType.UNARY)
            .build()

        val service = ServiceDescriptor.newBuilder("")
            .addMethod(method).build()

        NettyServerBuilder.forPort(9002)
            .addService(
                ServerServiceDefinition.builder(service)
                    .addMethod(ServerMethodDefinition.create(method, ServerCalls.asyncUnaryCall(object:
                        ServerCalls.UnaryMethod<String, Int> {
                        override fun invoke(request: String?, responseObserver: StreamObserver<Int>?) {

                            TODO("not implemented")
                        }
                    })))
                    .build()
            ).build()

        val channel = kotlinx.coroutines.experimental.channels.Channel<Message>()

        val channelRunner = async(Dispatchers.Default) {
            var started = System.currentTimeMillis()

            while (!channel.isClosedForReceive) {
                val code = channel.receive()
                if (code.id % 1000000 == 0) {
                    println(System.currentTimeMillis() - started)
                    started = System.currentTimeMillis()
                }
//                println(channel.receive())
            }
        }

        val job = launch(Dispatchers.IO) {
            println("Launching")
            var i = 0
            for (i in 0..100000000) {
//                delay(1000)
//                println(i)
                channel.send(Message(i))
            }
        }

        runBlocking {
            job.join()
        }
        System.`in`.read()
        //        WireCompiler.main("--proto_path=.proto/java",
        ////                "--java_out=.proto",
        //                "--kotlin_out=.proto/java",
        //                "com_movemedical_server_app_action_admin.proto",
        //                "com_movemedical_server_sql_datatype.proto",
        //                "com_movemedical_server_app_action_inventory_verify.proto",
        //                "com_movemedical_server_app_model_api.proto"
        //        );
        ////                "admin.proto", "dir.proto");

        val registry = create()
        println(registry)
        //    ProtobufSchema pb = ProtobufSchema
        //        .create(new File(".proto/java"), type, ProtobufSchema.java());
        //    pb.run();

    }

    fun generateImports() {

    }

    class Adapter : Function<ClassPath.ClassInfo, ReflectionSchema.Entry?> {

        override fun apply(ci: ClassPath.ClassInfo): ReflectionSchema.Entry? {
            val pkg = ci.packageName

            if (CLS_BLACK_LIST.contains(ci.simpleName) ||
                ci.simpleName.isEmpty() ||
                !pkg.startsWith("com.movemedical")
            ) {
                return null
            }

            if (pkg.startsWith("com.movemedical.server.sql.datatype") &&
                !ci.simpleName.endsWith("Converter") &&
                !ci.name.contains("$")
            ) {
                return ReflectionSchema.TypeEntry.create(ci.load())
            } else if (pkg.startsWith("com.movemedical.server.app.typeDescriptor.api")) {
                return ReflectionSchema.TypeEntry.create(ci.load())
            } else if (pkg.startsWith("com.movemedical.server.app.action") && ci.simpleName.endsWith("Verticle")) {
                return actionFor(ci.load())
            } else if (pkg.startsWith("com.movemedical.server.app.docreport") && ci.simpleName.endsWith("Verticle")) {
                return actionFor(ci.load())
            }

            return null
        }

        fun actionFor(klass: Class<*>): RpcEntry? {
            // Generic superType.
            val genericClass = klass.genericSuperclass as ParameterizedType

            // Actual javaKind typeArgs.
            val typeArguments = genericClass.actualTypeArguments

            when {
                MoveActionVerticle::class.java.isAssignableFrom(klass) -> return RpcEntry.create(
                    klass,
                    typeArguments[0] as Class<*>,
                    typeArguments[1] as Class<*>,
                    typeArguments[2] as Class<*>
                )
                MoveAction::class.java.isAssignableFrom(klass) -> return RpcEntry.create(
                    null,
                    klass,
                    typeArguments[0] as Class<*>,
                    typeArguments[1] as Class<*>
                )
                MoveDocReportGeneratorVerticle::class.java.isAssignableFrom(klass) -> return RpcEntry.create(
                    null,
                    klass,
                    typeArguments[0] as Class<*>, null
                )
                MoveQueueVerticle::class.java.isAssignableFrom(klass) -> return RpcEntry.create(
                    null,
                    typeArguments[1] as Class<*>,
                    typeArguments[0] as Class<*>, null
                )
                MoveQueue::class.java.isAssignableFrom(klass) -> return RpcEntry.create(
                    null,
                    klass,
                    typeArguments[0] as Class<*>, null
                )
                else -> return null
            }
        }

        companion object {
            private val CLS_BLACK_LIST = Sets.newHashSet(
                "PaginatedListResponse",
                "BaseResponse",
                "OrderByParams"
            )
        }
    }
}
