package xerial.larray.core;

import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * @author Taro L. Saito
 */
public class UnsafeUtil {

    public static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return Unsafe.class.cast(f.get(null));
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("sun.misc.Unsafe is not available in this JVM");
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("sun.misc.Unsafe is not available in this JVM");
        }
    }

    public static Unsafe unsafe = getUnsafe();

    private static Constructor<?> findDirectByteBufferConstructor() {
        try {
          return Class.forName("java.nio.DirectByteBuffer").getDeclaredConstructor(Long.TYPE, Integer.TYPE);
        }
        catch(ClassNotFoundException e) {
            throw new IllegalStateException(String.format("Failed to find java.nio.DirectByteBuffer: $s", e.getMessage()));
        }
        catch(NoSuchMethodException e) {
            throw new IllegalStateException(String.format("Failed to find constructor f java.nio.DirectByteBuffer: $s", e.getMessage()));
        }
    }

    private static Constructor<?> dbbCC = findDirectByteBufferConstructor();

    public static ByteBuffer newDirectByteBuffer(long addr, int size)
    {
        dbbCC.setAccessible(true);
        Object b = null;
        try {
            b = dbbCC.newInstance(new Long(addr), new Integer(size));
            return ByteBuffer.class.cast(b);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to create DirectByteBuffer: %s", e.getMessage()));
        }
    }


}
