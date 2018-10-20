package run.mojo.wire.type;

/** */
public class RpcInfo {

  public String path;
  public String[] paths;
  public boolean secured;

  public String[] paths() {
    return paths;
  }
}
