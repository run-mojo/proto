package run.mojo.wire

import io.grpc.MethodDescriptor

data class GrpcDescriptor<ReqT, RespT>(val method: MethodDescriptor<ReqT, RespT>)


/**
 *
 */
data class FunctionDescriptor<REQ, RESP>(
    val fullName: String,
    val request: MessageDescriptor<REQ>,
    val response: MessageDescriptor<RESP>,
    val requestStreaming: Boolean,
    val responseStreaming: Boolean,
    val restPaths: List<String> = listOf()
)

interface UnaryAdapter {

}

interface GrpcUnaryAdapter {

}

/**
 * A wire function may support different mechanisms and systems of dispatching.
 */
interface UnaryFunctionAdapter<ReqT, RespT> {
    val descriptor: FunctionDescriptor<ReqT, RespT>
}

interface UnaryBrokerAdapter {

}

interface HttpRequest {}

interface HttpResponse {}

/**
 *
 */
interface Channel {

}

/**
 *
 */
interface ResponseFuture<T> {

}

interface RequestStream<T> {

}

interface ResponseStream<T> {

}

abstract class ChannelAcceptor {

}

/**
 *
 */
abstract class ChannelHandler {
    abstract fun start()

    abstract fun unary(pointer: UnaryHandle): UnaryHandler<*, *>

    abstract fun requestStream(): RequestStreamHandler<*, *>

    abstract fun responseStream(): RequestStreamHandler<*, *>

    abstract fun duplexStream(): RequestStreamHandler<*, *>
}

data class UnaryHandle(val pointer: Long)

data class DuplexHandle(val pointer: Long)


/**
 * Handles a single unary request.
 */
abstract class UnaryHandler<REQ, RESP>(val descriptor: FunctionDescriptor<REQ, RESP>) {
    val requestProto = descriptor.request.adapters.proto
    val pointer: Long = 0
    val processors = mutableListOf<UnaryProcessor<REQ, RESP>>()
    val inflight = mutableSetOf<UnaryProcessor<REQ, RESP>>()

    fun handle(handle: UnaryHandle) {
        // Deserialize request.
        // Create response future.
        // Get or create compiler instance.
        // Process it.
    }

    abstract fun process(request: REQ): ResponseFuture<RESP>
}

/**
 *
 */
abstract class UnaryProcessor<REQ, RESP> {
    abstract fun process(request: REQ): ResponseFuture<RESP>
}

/**
 *
 */
abstract class RequestStreamHandler<REQ, RESP>(val descriptor: FunctionDescriptor<REQ, RESP>) {

    abstract fun handle(request: RequestStream<REQ>): ResponseFuture<RESP>
}

/**
 *
 */
abstract class ResponseStreamHandler<REQ, RESP>(val descriptor: FunctionDescriptor<REQ, RESP>) {

    abstract fun handle(request: REQ): ResponseStream<RESP>
}

/**
 *
 */
abstract class DuplexHandler<REQ, RESP>(val descriptor: FunctionDescriptor<REQ, RESP>) {

    abstract fun handle(request: RequestStream<REQ>): ResponseStream<RESP>
}