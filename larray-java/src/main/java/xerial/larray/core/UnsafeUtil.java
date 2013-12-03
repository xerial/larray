package xerial.larray.core;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author Taro L. Saito
 */
public class UnsafeUtil {

    static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return Unsafe.class.cast(f.get(null));
        }
        catch(NoSuchFieldException e) {
            throw new IllegalStateException("sun.misc.Unsafe is not available in this JVM");
        }
        catch(IllegalAccessException e) {
            throw new IllegalStateException("sun.misc.Unsafe is not available in this JVM");
        }
    }

    static Unsafe unsafe = getUnsafe();

}
