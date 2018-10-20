package run.mojo.wire;

/** A protocol buffer label. */
public enum Label {
  REQUIRED,

  OPTIONAL,

  REPEATED,

  ONE_OF,

  /** Implies {@link #REPEATED}. */
  PACKED;

  public boolean isRepeated() {
    return this == REPEATED || this == PACKED;
  }

  public boolean isPacked() {
    return this == PACKED;
  }

  public boolean isOneOf() {
    return this == ONE_OF;
  }
}
