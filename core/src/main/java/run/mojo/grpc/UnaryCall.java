package run.mojo.grpc;

import java.util.concurrent.atomic.AtomicLong;

/** */
public class UnaryCall<ReqT, RespT> extends AtomicLong {

  private long future;

  public void complete(RespT response) {
    final long future = get();
    if (future > 0L) {
      if (compareAndSet(future, 0L)) {}
    }
  }

  public void fail(RpcStatus status) {
    final long future = get();
    if (future > 0L) {
      if (compareAndSet(future, 0L)) {}
    }
  }
}
