package xerial.larray.impl;

/**
 * LArray native code interface
 * @author Taro L. Saito
 */
public class LArrayNative {

    static {
        LArrayLoader.load();
    }

    public static native int copyToArray(long srcAddress, Object destArray, int destOffset, int length);
    public static native int copyFromArray(Object srcArray, int srcOffset, long destAddress, int length);

}
