package run.mojo.wire.type;

import com.squareup.wire.schema.ProtoType;
import run.mojo.wire.JavaKind;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

/** */
public class MessageDesc extends TypeDesc {
  public LinkedHashMap<String, FieldDesc> props = new LinkedHashMap<>();

  public Stream<FieldDesc> fields() {
    return props.values().stream();
  }

  @Override
  public JavaKind getJavaKind() {
    return JavaKind.OBJECT;
  }

  @Override
  public ProtoType getProtoType() {
    return ProtoType.get(getName());
  }

  public static class Template extends MessageDesc {
    public ParameterizedType generic;
    public ArrayList<Impl> impls = new ArrayList<>();

    public Impl impl(String name, String simpleName, Class declaringClass) {
      final Impl impl = new Impl();

      impl.template = this;
      impl.implName = name;
      impl.declaringClass = declaringClass;
      impl.compiled = compiled;
      impl.javaType = javaType;
      impl.simpleName = simpleName;
      impl.jsonSerialize = jsonSerialize;
      impl.jsonDeserialize = jsonDeserialize;

      impls.add(impl);
      return impl;
    }
  }

  public static class Impl extends MessageDesc {
    public Template template;
    public String implName;
    public Class declaringClass;

    @Override
    public String getPackageName() {
      return declaringClass.getPackage().getName();
    }

    @Override
    public String getName() {
      return implName;
    }

    @Override
    public String getSimpleName() {
      return simpleName;
    }
  }
}
