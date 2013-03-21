package xerial.larray.japi;

import xerial.larray.DefaultAllocator;
import xerial.larray.LByteArray;
import xerial.larray.LIntArray;

/**
 * Java interface of LArray
 * @author Taro L. Saito
 */
public class LArray {

    static xerial.larray.MemoryAllocator defaultAllocator = new DefaultAllocator();

    public static LIntArray newLIntArray(long size) {
        return new LIntArray(size, defaultAllocator);
    }

    public static LByteArray newLByteArray(long size) {
        return new LByteArray(size, defaultAllocator);
    }


}
