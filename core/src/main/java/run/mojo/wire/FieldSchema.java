package run.mojo.wire;

import com.squareup.moshi.JsonAdapter;
import com.squareup.wire.ProtoAdapter;

/** */
public class FieldSchema {

  public final String name;
  public final String jsonName;
  public final int tag;
  public final JavaKind java;
  public final ProtoKind proto;
  public final JsonKind json;
  public final ProtoAdapter protoAdapter;
  public final JsonAdapter jsonAdapter;

  public FieldSchema(
      String name,
      String jsonName,
      int tag,
      JavaKind java,
      ProtoKind proto,
      JsonKind json,
      ProtoAdapter protoAdapter,
      JsonAdapter jsonAdapter) {
    this.name = name;
    this.jsonName = jsonName;
    this.tag = tag;
    this.java = java;
    this.proto = proto;
    this.json = json;
    this.protoAdapter = protoAdapter;
    this.jsonAdapter = jsonAdapter;
  }
}
