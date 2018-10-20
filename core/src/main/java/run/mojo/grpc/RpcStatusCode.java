package run.mojo.grpc;

/** */
public enum RpcStatusCode {
  OK(0),
  CANCELLED(1),
  UNKNOWN(2),
  INVALID_ARGUMENT(3),
  DEADLINE_EXCEEDED(4),
  NOT_FOUND(5),
  ALREADY_EXISTS(6),
  PERMISSION_DENIED(7),
  UNAUTHENTICATED(16),
  RESOURCE_EXHAUSTED(8),
  FAILED_PRECONDITION(9),
  ABORTED(10),
  OUT_OF_RANGE(11),
  UNIMPLEMENTED(12),
  INTERNAL(13),
  UNAVAILABLE(14),
  DATA_LOSS(15),
  ;

  public final int code;

  RpcStatusCode(int code) {
    this.code = code;
  }

  public static RpcStatusCode from(int code) {
    switch (code) {
      case 0:
        return RpcStatusCode.OK;
      case 1:
        return RpcStatusCode.CANCELLED;
      case 3:
        return RpcStatusCode.INVALID_ARGUMENT;
      case 4:
        return RpcStatusCode.DEADLINE_EXCEEDED;
      case 5:
        return RpcStatusCode.NOT_FOUND;
      case 6:
        return RpcStatusCode.ALREADY_EXISTS;
      case 7:
        return RpcStatusCode.PERMISSION_DENIED;
      case 16:
        return RpcStatusCode.UNAUTHENTICATED;
      case 8:
        return RpcStatusCode.RESOURCE_EXHAUSTED;
      case 9:
        return RpcStatusCode.FAILED_PRECONDITION;
      case 10:
        return RpcStatusCode.ABORTED;
      case 11:
        return RpcStatusCode.OUT_OF_RANGE;
      case 12:
        return RpcStatusCode.UNIMPLEMENTED;
      case 13:
        return RpcStatusCode.INTERNAL;
      case 14:
        return RpcStatusCode.UNAVAILABLE;
      case 15:
        return RpcStatusCode.DATA_LOSS;
      default:
        return RpcStatusCode.UNKNOWN;
    }
  }
}
