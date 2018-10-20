package run.mojo.wire.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import run.mojo.wire.JavaKind;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

/** */
public class FieldDesc {
  // Descriptor that implements the javaType.
  public TypeDesc typeDescriptor;

  // Kind of data.
  public JavaKind javaKind;

  // Contained by class.
  public Class containingClass;

  // Class of the actual javaKind.
  public Class dataClass;

  // Field name.
  public String name;

  // Json name.
  public String jsonName;

  // Protobuf getTag source.
  public TagSource tagSource = TagSource.AUTO;
  // Protobuf getTag value.
  public int tag;

  // Index relative to the concrete javaKind.
  public int index;

  // Native pointer offset.
  public long offset;

  // Flatbuffers name.
  public String fbName;
  // Flatbuffers index.
  public int fbIndex;

  // Field on class.
  public Field field;

  // The Parameterized javaKind for generic declarations.
  // e.g. List<String>
  // e.g. SomeBaseClass<String>
  // Can be collections or a generic javaKind.
  // Non-collections will generate an ObjectDescriptor.Template which
  // pumps out ObjectDescriptor.Impl
  public ParameterizedType generic;

  // Matched getter.
  public Method getter;
  // Matched setter.
  public Method setter;
  // Whether it's deprecated. Can be placed on the member, getter, or setter.
  public Deprecated deprecated;
  // Jackson annoation. Can be placed on the member, getter, or setter.
  public JsonProperty jsonProperty;
  // Jackson annoation to ignore. Can be placed on the member, getter, or setter.
  public boolean jsonIgnore;
  // Jackson annoation that describes custom serializer. Can be placed on the member, getter, or
  // setter.
  public JsonSerialize jsonSerialize;
  // Jackson annoation that describes custom deserializer. Can be placed on the member, getter, or
  // setter.
  public JsonDeserialize jsonDeserialize;

  public static boolean isPublic(Package p) {
    final String name = p.getName();
    if (name.startsWith("java") || name.startsWith("jdk") || name.isEmpty() || name.equals(".")) {
      return false;
    } else {
      return true;
    }
  }

  public Package[] packageDeps() {
    switch (javaKind) {
      case LIST:
        {
          ListDesc m = (ListDesc) typeDescriptor;
          Package key = m.component.pkg.model;

          if (isPublic(key)) {
            return new Package[] {key};
          }
          return null;
        }
      case SET:
        {
          ListDesc m = (ListDesc) typeDescriptor;
          Package key = m.component.pkg.model;

          if (isPublic(key)) {
            return new Package[] {key};
          }
          return null;
        }
      case MAP:
        {
          MapDesc m = (MapDesc) typeDescriptor;
          Package key = m.key.pkg.model;
          Package value = m.value.pkg.model;

          if (isPublic(key)) {
            if (isPublic(value)) {
              return new Package[] {key, value};
            }
            return new Package[] {key};
          } else if (isPublic(value)) {
            return new Package[] {value};
          }
          return null;
        }
      case ENUM:
      case OBJECT:
        return new Package[] {typeDescriptor.pkg.model};
      case ENCLOSING:
        break;
    }
    return null;
  }

  public boolean isGeneric() {
    return generic != null;
  }

  public int typeParameterCount() {
    if (generic == null
        || generic.getActualTypeArguments() == null
        || generic.getActualTypeArguments().length == 0) {
      return 0;
    } else {
      return generic.getActualTypeArguments().length;
    }
  }

  public boolean isTemplateType() {
    if (generic == null) {
      return false;
    }
    return javaKind == JavaKind.OBJECT;
  }

  public FieldDesc impl(Class cls) {
    final FieldDesc descriptor = new FieldDesc();
    descriptor.typeDescriptor = typeDescriptor;
    descriptor.javaKind = javaKind;
    descriptor.containingClass = containingClass;
    descriptor.dataClass = cls;
    descriptor.name = name;
    descriptor.jsonName = jsonName;
    descriptor.tag = tag;
    descriptor.index = index;
    descriptor.offset = offset;
    descriptor.fbName = fbName;
    descriptor.fbIndex = fbIndex;
    descriptor.field = field;
    descriptor.generic = generic;
    descriptor.getter = getter;
    descriptor.setter = setter;
    descriptor.deprecated = deprecated;
    descriptor.jsonProperty = jsonProperty;
    descriptor.jsonIgnore = jsonIgnore;
    descriptor.jsonSerialize = jsonSerialize;
    descriptor.jsonDeserialize = jsonDeserialize;
    return descriptor;
  }

  public enum TagSource {
    AUTO,
    JSON_PROPERTY,
    WIRE_FIELD,
    GOOGLE_PROTOBUF,
    OTHER,
  }

  //    @Override
  //    public int getTag() {
  //        return getTag;
  //    }
  //
  //    @Override
  //    public String name() {
  //        return name;
  //    }
  //
  //    @Override
  //    public String jsonName() {
  //        return jsonName;
  //    }
  //
  //    @Override
  //    public JavaKind javaKind() {
  //        return javaKind;
  //    }
  //
  //    @Override
  //    public ProtoType protoType() {
  //        return null;
  //    }
}
