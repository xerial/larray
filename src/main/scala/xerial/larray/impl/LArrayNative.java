package xerial.larray.impl;

import java.lang.reflect.Method;

/**
 * LArray native code interface
 * @author Taro L. Saito
 */
public class LArrayNative {

    static {
        try {
            // Use reflection to make easier jni-header compilation in Makefile
            Method m = Class.forName("xerial.larray.impl.LArrayLoader").getDeclaredMethod("load");
            m.invoke(null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static native int copyToArray(long srcAddress, Object destArray, int destOffset, int length);
    public static native int copyFromArray(Object srcArray, int srcOffset, long destAddress, int length);

    public static native long mmap(long addr, long length, int prot, int flags, int fd, long offset);
}
