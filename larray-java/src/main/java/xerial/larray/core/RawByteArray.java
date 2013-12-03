package xerial.larray.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static xerial.larray.core.UnsafeUtil.unsafe;


/**
 * Raw off-heap array of int indexes.
 *
 * @author Taro L. Saito
 */
public class RawByteArray {

    final Memory m;

    public static MemoryAllocator alloc = new RawMemoryAllocator();

    /**
     * Allocate a memory of the specified byte size
     * @param size byte size of the array
     */
    public RawByteArray(int size) {
        this.m = alloc.allocate(size);
    }

    /**
     * Read a byte at offset
     * @param offset
     * @return
     */
    public byte apply(int offset) {
        return getByte(offset);
    }

    /**
     * Set a byte at offset
     * @param offset
     * @param value
     */
    public void update(int offset, byte value) {
        putByte(offset, value);
    }

    /**
     * Release the memory content. After this method invocation, the behaviour of
     * getXXX and putXXX methods becomes undefined.
     */
    public void release() {
        alloc.release(m);
    }


    /**
     * Address of the
     * @return
     */
    public long data() {
        return m.data();
    }

    public int size() {
        return (int) m.dataSize();
    }

    public void clear() {
        fill(0, size(), (byte) 0);
    }

    public void fill(int offset, int size, byte value) {
        unsafe.setMemory(m.data() + offset, size, value);
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

    public void copyTo(int srcOffset, byte[] destArray, int destOffset, int size) {
        unsafe.copyMemory(null, m.data() + srcOffset, destArray, destOffset, size);
    }

    public void copyTo(int srcOffset, RawByteArray dest, int destOffset, int size) {
        unsafe.copyMemory(m.data() + srcOffset, dest.data() + destOffset, size);
    }

    public byte[] toArray() {
        byte[] b = new byte[(int) m.dataSize()];
        unsafe.copyMemory(m.data(), 0L, b, 0, m.dataSize());
        return b;
    }

    public ByteBuffer toDirectByteBuffer() {
        ByteBuffer b = UnsafeUtil.newDirectByteBuffer(m.data(), (int) m.dataSize());
        return b.order(ByteOrder.nativeOrder());
    }
}


