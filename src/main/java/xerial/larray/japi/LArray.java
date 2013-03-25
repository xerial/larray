package xerial.larray.japi;

import xerial.larray.*;

/**
 * Java interface of LArray
 * @author Taro L. Saito
 */
public class LArray {

    static xerial.larray.MemoryAllocator defaultAllocator = new ConcurrentMemoryAllocator();


    public static LByteArray newLByteArray(long size) {
        return new LByteArray(size, defaultAllocator);
    }

    public static LCharArray newLCharArray(long size) {
        return new LCharArray(size, defaultAllocator);
    }

    public static LShortArray newLShortArray(long size) {
        return new LShortArray(size, defaultAllocator);
    }

    public static LIntArray newLIntArray(long size) {
        return new LIntArray(size, defaultAllocator);
    }

    public static LFloatArray newLFloatArray(long size) {
        return new LFloatArray(size, defaultAllocator);
    }

    public static LDoubleArray newLDoubleArray(long size) {
        return new LDoubleArray(size, defaultAllocator);
    }

    public static LLongArray newLLongArray(long size) {
        return new LLongArray(size, defaultAllocator);
    }

    public static LBitArray newLBitArray(long size) {
        return new LBitArray(size);
    }

}
