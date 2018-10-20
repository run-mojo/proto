package move.proto.api

import com.squareup.wire.ProtoAdapter
import io.movemedical.server.essentials.intraop.ActionIntraop
import io.movemedical.server.essentials.intraop.QueueIntraop
import okio.Buffer
import run.mojo.wire.FunctionDescriptor
import java.util.concurrent.atomic.AtomicLong

interface ActionHandler<REQ, RESP> {
    val descriptor: FunctionDescriptor<REQ, RESP>

    fun requestProto(): ProtoAdapter<REQ> = descriptor.request.adapters.proto!!
}



class UnaryRequest() : AtomicLong(0L) {
    override fun toByte(): Byte {
        return get().toByte()
    }

    override fun toChar(): Char {
        return get().toChar()
    }

    override fun toShort(): Short {
        return get().toShort()
    }

    fun lock(): Boolean {
        val pointer = get()
        if (pointer == 0L) {
            return false
        }

        return false
    }
}

/**
 *
 */
class QueueDispatcher<T>(
    override val descriptor: FunctionDescriptor<T, Unit>,
    val intraop: QueueIntraop
) : ActionHandler<T, Unit> {

    fun handle(raw: Buffer) {

    }

    fun handleRequest(request: T) {

    }
}

class ActionDispatcher<REQ, RESP>(
    val descriptor: FunctionDescriptor<REQ, RESP>,
    val action: ActionIntraop
) {

    fun handle(buffer: Buffer) {

    }
}