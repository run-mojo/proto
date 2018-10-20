package run.mojo.grpc;

/** */
class Native {

  static UnaryCall unary(
      CompletionQueue queue, UnaryMethod descriptor, long future, long message, int length) {

    return null;
  }

  static native boolean completeUnary(long future, long data, int length);

  static native boolean failUnary(long future, int status, byte[] message);

  static native boolean cancelUnary(long future);

  static void callCanceled(CompletionQueue queue, long pointer, int reason) {
    // Canceled
  }
}
