package run.mojo.wire.type;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.squareup.wire.schema.ProtoType;
import run.mojo.wire.JavaKind;

import java.util.TreeMap;

/** */
public class TypeDesc {

  public final TreeMap<String, TypeDesc> nested = new TreeMap<>();
  public PackageDesc pkg;
  public ProtoType protoType;
  public JavaKind javaType;
  public TypeDesc enclosing;
  public Class compiled;
  public String simpleName;
  public JsonSerialize jsonSerialize;
  public JsonDeserialize jsonDeserialize;
  public RpcInfo rpc;
  String relativeName;

  public String getPackageName() {
    if (pkg == null) {
      return ".";
    }
    return pkg.name();
  }

  public String getName() {
    return compiled.getCanonicalName();
  }

  public String getSimpleName() {
    if (simpleName != null) {
      return simpleName;
    }
    if (compiled != null) {
      return compiled.getSimpleName();
    }
    return null;
  }

  public String getRelativeName() {
    if (relativeName != null) {
      return relativeName;
    }

    if (enclosing == null) {
      relativeName = compiled.getSimpleName();
      return relativeName;
    }

    relativeName = enclosing.getRelativeName() + "." + compiled.getSimpleName();
    return relativeName;
  }

  public boolean isEnum() {
    return compiled.isEnum();
  }

  public JavaKind getJavaKind() {
    return javaType;
  }

  public ProtoType getProtoType() {
    return null;
  }
}
