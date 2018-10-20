package run.mojo.wire.type;

/** */
public class EnumDesc extends TypeDesc {
  public EnumConstant[] values;

  public String getSimpleName() {
    if (compiled != null) {
      return compiled.getSimpleName();
    }
    return "";
  }
}
