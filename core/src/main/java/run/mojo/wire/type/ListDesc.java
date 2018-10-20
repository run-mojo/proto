package run.mojo.wire.type;

import run.mojo.wire.JavaKind;

/** */
public class ListDesc extends TypeDesc {
  public Class componentClass;
  public TypeDesc component;

  public ListDesc(JavaKind jkind) {
    this.javaType = jkind;
  }
}
