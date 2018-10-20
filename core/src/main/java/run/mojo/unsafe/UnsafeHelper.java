package run.mojo.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/** */
public class UnsafeHelper {
  public static final Unsafe UNSAFE;

  static {
    try {
      Field f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      UNSAFE = (Unsafe) f.get(null);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public static long objectFieldOffset(Field field) {
    return UNSAFE.objectFieldOffset(field);
  }

  public static boolean getBoolean(Object obj, long offset) {
    return UNSAFE.getBoolean(obj, offset);
  }

  public static void setBoolean(Object obj, long offset, boolean value) {
    UNSAFE.putBoolean(obj, offset, value);
  }

  public static byte getByte(Object obj, long offset) {
    return UNSAFE.getByte(obj, offset);
  }

  public static void setByte(Object obj, long offset, byte value) {
    UNSAFE.putByte(obj, offset, value);
  }

  public static short getShort(Object obj, long offset) {
    return UNSAFE.getShort(obj, offset);
  }

  public static void setShort(Object obj, long offset, short value) {
    UNSAFE.putShort(obj, offset, value);
  }

  public static char getChar(Object obj, long offset) {
    return UNSAFE.getChar(obj, offset);
  }

  public static void setChar(Object obj, long offset, char value) {
    UNSAFE.putChar(obj, offset, value);
  }

  public static int getInt(Object obj, long offset) {
    return UNSAFE.getInt(obj, offset);
  }

  public static void setInt(Object obj, long offset, int value) {
    UNSAFE.putInt(obj, offset, value);
  }

  public static long getLong(Object obj, long offset) {
    return UNSAFE.getLong(obj, offset);
  }

  public static void setLong(Object obj, long offset, long value) {
    UNSAFE.putLong(obj, offset, value);
  }

  public static float getFloat(Object obj, long offset) {
    return UNSAFE.getFloat(obj, offset);
  }

  public static void setFloat(Object obj, long offset, float value) {
    UNSAFE.putFloat(obj, offset, value);
  }

  public static double getDouble(Object obj, long offset) {
    return UNSAFE.getDouble(obj, offset);
  }

  public static void setDouble(Object obj, long offset, double value) {
    UNSAFE.putDouble(obj, offset, value);
  }

  public static Object getObject(Object obj, long offset) {
    return UNSAFE.getObject(obj, offset);
  }

  public static void setObject(Object obj, long offset, Object value) {
    UNSAFE.putObject(obj, offset, value);
  }

  //  public static long objectFieldOffset(Class compiled, String fieldName) {
  //    Field field = null;
  //    try {
  ////      UNSAFE.objectFieldOffset();
  //      field.setAccessible(true);
  //      return UNSAFE.objectFieldOffset(field);
  //    } catch (NoSuchFieldException e) {
  //      throw new RuntimeException(e);
  //    }
  //  }
}
