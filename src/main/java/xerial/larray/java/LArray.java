package xerial.larray.java;

/**
 * Java interface of LArray
 * @author Taro L. Saito
 */
public interface LArray<A> {

//    /**
//     * Get an element at the given index
//     * @param i index
//     * @return element
//     */
//    public A get(long i);
//
//    /**
//     * Set the element at the specified index
//     * @param i index
//     * @param v value to set
//     * @return new value that is set
//     */
//    public A set(long i, A v);

    /**
     * Array size
     */
    public long size();

    /**
     * Release the memory resource held by this LArray
     */
    public void free();

    /**
     * Byte length of this LArray
     * @return
     */
    public long byteLength();
}
