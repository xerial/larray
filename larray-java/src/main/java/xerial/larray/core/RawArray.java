package xerial.larray.core;

import static xerial.larray.core.UnsafeUtil.*;




/**
 * Raw off-heap array of int indexes.
 *
 *
 * @author Taro L. Saito
 */
public class RawArray {

    final Memory m;

    public static RawMemoryAllocator alloc = new RawMemoryAllocator();

    public RawArray(int size) {
        this.m = alloc.allocate(size);
    }


    /**
     * Release the memory content. After this method invocation, the behaiour of
     * getXXX and putXXX methods are undefined.
     */
    public void release() {
        alloc.release(m);
    }

    public long data() {
        return m.data();
    }

    public void clear() {
        unsafe.setMemory(m.data(), m.dataSize(), (byte) 0);
    }

    public byte getByte(int offset) {
        return unsafe.getByte(m.data() + offset);
    }

    public char getChar(int offset) {
        return unsafe.getChar(m.data() + offset);
    }

    public short getShort(int offset) {
        return unsafe.getShort(m.data() + offset);
    }

    public int getInt(int offset) {
        return unsafe.getInt(m.data() + offset);
    }

    public float getFloat(int offset) {
        return unsafe.getFloat(m.data() + offset);
    }

    public long getLong(int offset) {
        return unsafe.getLong(m.data() + offset);
    }

    public double getDouble(int offset) {
        return unsafe.getDouble(m.data() + offset);
    }

    public void putByte(int offset, byte value) {
        unsafe.putByte(m.data() + offset, value);
    }

    public void putChar(int offset, char value) {
        unsafe.putChar(m.data() + offset, value);
    }

    public void putShort(int offset, short value) {
        unsafe.putShort(m.data() + offset, value);
    }

    public void putInt(int offset, int value) {
        unsafe.putInt(m.data() + offset, value);
    }

    public void putFloat(int offset, float value) {
        unsafe.putFloat(m.data() + offset, value);
    }

    public void putLong(int offset, long value) {
        unsafe.putLong(m.data()+ offset, value);
    }

    public void putDouble(int offset, double value) {
        unsafe.putDouble(m.data() + offset, value);
    }

    public void copyTo(int offset, int size, Object destArray, int destOffset) {
        unsafe.copyMemory(null, offset, destArray, destOffset, size);
    }

    public void copyTo(int srcOffset, RawArray dest, int destOffset, int size) {
        unsafe.copyMemory(m.data() + srcOffset, dest.data() + destOffset, size);
    }

}


