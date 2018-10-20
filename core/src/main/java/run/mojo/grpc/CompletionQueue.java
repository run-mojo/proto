package run.mojo.grpc;

/** */
public class CompletionQueue {
  private Thread thread;
  private long pointer;

  public <ReqT, RespT> void onUnary(UnaryCall<ReqT, RespT> call) {}

  public <ReqT, RespT> RpcStatus onUnaryDelay(UnaryCall<ReqT, RespT> call) {
    return null;
  }
}
