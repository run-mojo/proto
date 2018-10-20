package run.mojo.grpc;

import java.util.concurrent.atomic.AtomicLong;

/** */
public class ServerCall extends AtomicLong {
  // associated completion queue
  private final CompletionQueue queue;
  private volatile int status;

  public ServerCall(CompletionQueue queue) {
    this.queue = queue;
  }

  public enum Status {
    EMPTY,
  }

  public static class UnaryCall extends ServerCall {
    private Object descriptor;

    public UnaryCall(CompletionQueue queue, Object descriptor) {
      super(queue);
      this.descriptor = descriptor;
    }
  }
}
