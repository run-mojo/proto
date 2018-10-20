import com.squareup.wire.ProtoAdapter
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.stub.ServerCalls
import io.grpc.stub.StreamObserver
import run.mojo.wire.FunctionDescriptor
import java.io.ByteArrayInputStream
import java.io.InputStream

interface WireMarshallerListener {
    fun onEncodeException(value: Any, exception: Throwable)
    fun onDecodeException()
}


class WireMarshallerLocal {
    var encodedBytes: Long = 0
    var decodedBytes: Long = 0
    var unknownBytes: Long = 0

    companion object {

    }
}

class WireMarshaller<T>(val adapter: ProtoAdapter<T>) : MethodDescriptor.Marshaller<T> {
    val threadLocal = ThreadLocal<WireMarshallerLocal>()

    protected fun local(): WireMarshallerLocal {
        var l = threadLocal.get()
        if (l == null) {
            l = WireMarshallerLocal()
            threadLocal.set(l)
        }
        return l
    }

    override fun stream(value: T): InputStream {
        val encoded = adapter.encode(value)
        local().encodedBytes += encoded.size
        return ByteArrayInputStream(encoded)
    }

    override fun parse(stream: InputStream?): T {
        return adapter.decode(stream!!)
    }
}

class WireContext {
    var source: String = ""
}

class WireIsolate {
    val logger = ""

    fun get() {}

    fun <ReqT, RespT> call(
        descriptor: FunctionDescriptor<ReqT, RespT>,
        reqT: ReqT,
        responseObserver: StreamObserver<RespT>
    ) {

    }

    companion object {
        val THREAD_LOCAL = ThreadLocal<WireIsolate>()
    }
}

class WireServerCallHandlerFactory<ReqT, RespT>(
    val descriptor: FunctionDescriptor<ReqT, RespT>
) : ServerCalls.UnaryMethod<ReqT, RespT> {
//    val handler ServerCallHandler

    override fun invoke(request: ReqT, responseObserver: StreamObserver<RespT>?) {

    }
}

class WireServerCallHandler<ReqT, RespT>(
    val adapter: ProtoAdapter<ReqT>,
    val actual: ServerCallHandler<ReqT, RespT>,
    val handler: ServerCalls.UnaryMethod<ReqT, RespT>
) : ServerCallHandler<ReqT, RespT> {
    override fun startCall(
        call: ServerCall<ReqT, RespT>?,
        headers: Metadata?
    ): ServerCall.Listener<ReqT> {

        val listener = actual.startCall(call, headers)

        return listener
    }
}