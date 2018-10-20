package run.mojo.grpc;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/** */
public class ServerBuilder {

  public final ArrayList<Bind> binds = new ArrayList<>();
  public final LinkedHashMap<String, Method> methods = new LinkedHashMap<>();
  public int threads;

  public <ReqT, RespT> ServerBuilder addUnary(
      UnaryMethod method, UnaryHandler<ReqT, RespT> handler) {

    return this;
  }

  public static class Bind {
    public String host;
    public int port;

    public Bind host(final String host) {
      this.host = host;
      return this;
    }

    public Bind port(final int port) {
      this.port = port;
      return this;
    }
  }
}
