package run.mojo.wire;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

/** JVM based data type. */
public enum JavaKind {
  /** */
  BOOL,
  BOXED_BOOL,
  BYTE,
  BOXED_BYTE,
  SHORT,
  BOXED_SHORT,
  CHAR,
  BOXED_CHAR,
  INT,
  BOXED_INT,

  LONG,
  BOXED_LONG,
  FLOAT,
  BOXED_FLOAT,
  DOUBLE,
  BOXED_DOUBLE,

  BIG_DECIMAL,
  DURATION,

  /** */
  DATE,
  LOCAL_DATE,
  ZONED_DATE,

  BYTES,
  ARRAY,
  LIST,
  SET,
  QUEUE,
  MAP,
  ENUM,
  STRING,
  OBJECT,

  TEMPLATE,
  ENCLOSING,

  VOID,
  INVALID,

  EXTENDED,
  ;

  //        public final FieldAccessor accessor;

  public static JavaKind of(Class cls) {
    if (cls.equals(boolean.class)) {
      return JavaKind.BOOL;
    }
    if (cls.equals(Boolean.class)) {
      return JavaKind.BOXED_BOOL;
    }
    if (cls.equals(byte.class)) {
      return JavaKind.BYTE;
    }
    if (cls.equals(Byte.class)) {
      return JavaKind.BOXED_BYTE;
    }
    if (cls.equals(short.class)) {
      return JavaKind.SHORT;
    }
    if (cls.equals(Short.class)) {
      return JavaKind.BOXED_SHORT;
    }
    if (cls.equals(char.class)) {
      return JavaKind.CHAR;
    }
    if (cls.equals(Character.class)) {
      return JavaKind.BOXED_CHAR;
    }
    if (cls.equals(int.class)) {
      return JavaKind.INT;
    }
    if (cls.equals(Integer.class)) {
      return JavaKind.BOXED_INT;
    }
    if (cls.equals(long.class)) {
      return JavaKind.LONG;
    }
    if (cls.equals(Long.class)) {
      return JavaKind.BOXED_LONG;
    }
    if (cls.equals(float.class)) {
      return JavaKind.FLOAT;
    }
    if (cls.equals(Float.class)) {
      return JavaKind.BOXED_FLOAT;
    }
    if (cls.equals(double.class)) {
      return JavaKind.DOUBLE;
    }
    if (cls.equals(Double.class)) {
      return JavaKind.BOXED_DOUBLE;
    }
    if (cls.equals(BigDecimal.class)) {
      return JavaKind.BIG_DECIMAL;
    }
    if (cls.equals(Duration.class)) {
      return JavaKind.DURATION;
    }
    if (cls.equals(LocalDate.class)) {
      return JavaKind.DATE;
    }
    if (cls.equals(LocalDateTime.class)) {
      return JavaKind.DATE;
    }
    //        if (klass.equals(ZonedDate.class)) {
    //            return JavaType.DATE;
    //        }
    if (cls.equals(ZonedDateTime.class)) {
      return JavaKind.DATE;
    }
    if (cls.isEnum()) {
      return JavaKind.ENUM;
    }
    if (cls.isArray() && cls.getComponentType().equals(byte.class)) {
      return JavaKind.BYTES;
    }
    if (ByteBuffer.class.isAssignableFrom(cls)) {
      return JavaKind.BYTES;
    }
    if (String.class.isAssignableFrom(cls)) {
      return JavaKind.STRING;
    }
    if (Set.class.isAssignableFrom(cls)) {
      return JavaKind.SET;
    }
    if (Map.class.isAssignableFrom(cls)
        || HashMap.class.isAssignableFrom(cls)
        || LinkedHashMap.class.isAssignableFrom(cls)) {
      return JavaKind.MAP;
    }
    if (cls.isArray()) {
      return JavaKind.ARRAY;
    }
    if (List.class.isAssignableFrom(cls) || Collection.class.isAssignableFrom(cls)) {
      return JavaKind.LIST;
    }

    return JavaKind.OBJECT;
  }

  public Class asClass(boolean boxed) {
    switch (this) {
      case BOOL:
        return boxed ? Boolean.class : boolean.class;
      case BOXED_BOOL:
        return Boolean.class;
      case BYTE:
        return boxed ? Byte.class : byte.class;
      case BOXED_BYTE:
        return Byte.class;
      case SHORT:
        return boxed ? Short.class : short.class;
      case BOXED_SHORT:
        return Short.class;
      case CHAR:
        return boxed ? Character.class : char.class;
      case BOXED_CHAR:
        return Character.class;
      case INT:
        return boxed ? Integer.class : int.class;
      case BOXED_INT:
        return Integer.class;
      case LONG:
        return boxed ? Long.class : long.class;
      case BOXED_LONG:
        return Long.class;
      case FLOAT:
        return boxed ? Float.class : float.class;
      case BOXED_FLOAT:
        return Float.class;
      case DOUBLE:
        return boxed ? Double.class : double.class;
      case BOXED_DOUBLE:
        return Double.class;
      case BIG_DECIMAL:
        return BigDecimal.class;
      case DURATION:
        return Duration.class;
      case DATE:
        return Date.class;
      case LOCAL_DATE:
        return LocalDate.class;
      case ZONED_DATE:
        return ZonedDateTime.class;
      case BYTES:
        return byte[].class;
      case ARRAY:
        return Object[].class;
      case LIST:
        return List.class;
      case SET:
        return Set.class;
      case QUEUE:
        return Queue.class;
      case MAP:
        return Map.class;
      case ENUM:
        return Enum.class;
      case STRING:
        return String.class;
      case OBJECT:
        return Object.class;
      case TEMPLATE:
        return Object.class;
      case EXTENDED:
        return Object.class;
      case ENCLOSING:
        return Object.class;
    }
    return Object.class;
  }

  public boolean isPrimitive() {
    switch (this) {
      case BOOL:
      case BYTE:
      case SHORT:
      case CHAR:
      case INT:
      case LONG:
      case FLOAT:
      case DOUBLE:
        return true;

      default:
        return false;
    }
  }

  public boolean isBoxedPrimitive() {
    switch (this) {
      case BOXED_BOOL:
      case BOXED_BYTE:
      case BOXED_SHORT:
      case BOXED_CHAR:
      case BOXED_INT:
      case BOXED_LONG:
      case BOXED_FLOAT:
      case BOXED_DOUBLE:
        return true;

      default:
        return false;
    }
  }

  public boolean isBoxed() {
    switch (this) {
      case BOXED_BOOL:
      case BOXED_BYTE:
      case BOXED_SHORT:
      case BOXED_CHAR:
      case BOXED_INT:
      case BOXED_LONG:
      case BOXED_FLOAT:
      case BOXED_DOUBLE:
      case ENUM:
      case STRING:
      case LIST:
      case SET:
      case MAP:
      case OBJECT:
        return true;

      default:
        return false;
    }
  }
}
