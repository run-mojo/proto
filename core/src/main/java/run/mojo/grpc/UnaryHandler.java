package run.mojo.grpc;

/** */
public class UnaryHandler<ReqT, RespT> {
  public final CompletionQueue queue;

  public UnaryHandler(CompletionQueue queue) {
    this.queue = queue;
  }

  public void start(UnaryCall<ReqT, RespT> call) {
    call.complete(null);
  }

  public RpcStatus onDelay(UnaryCall<ReqT, RespT> call, boolean deadline) {
    return null;
  }

  public void onDeadline(UnaryCall<ReqT, RespT> call, boolean deadline) {}
}
