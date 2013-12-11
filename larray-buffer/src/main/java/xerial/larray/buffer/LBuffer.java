package xerial.larray.buffer;

/**
 * Off-heap memory buffer of int and long type indexes.
 * LBuffer is used as a backend of LArray.
 *
 * @author Taro L. Saito
 */
public class LBuffer extends LBufferAPI {

    /**
     * Allocate a memory of the specified byte size
     *
     * @param size byte size of the array
     */
    public LBuffer(long size) {
        super(OffHeapMemory.allocate(size));
    }


}


