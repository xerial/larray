package xerial.larray.buffer;

/**
 * Allocated memory information
 * @author Taro L. Saito
 */
public interface Memory {

    /**
     * Allocated memory address
     * @return
     */
    long headerAddress();

    /**
     * data-part address
     * @return data address
     */
    long address();

    /**
     * Allocated memory size
     * @return
     */
    long size();

    /**
     * data-part size
     * @return
     */
    long dataSize();
}
