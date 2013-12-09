package xerial.larray.buffer;

/**
 * @author Taro L. Saito
 */
interface Memory {

    /**
     * Allocated memory address
     * @return
     */
    long address();

    /**
     * data-part address
     * @return data address
     */
    long data();

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
