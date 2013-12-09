package xerial.larray.buffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import static xerial.larray.buffer.UnsafeUtil.unsafe;


/**
 * Off-heap memory buffer of int indexes.
 *
 * @author Taro L. Saito
 */
public class Buffer {

    final Memory m;

    /**
     * Allocate a memory of the specified byte size
     * @param size byte size of the array
     */
    public Buffer(int size) {
        this.m = LArrayBuffer.allocator.allocate(size);
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
        LArrayBuffer.allocator.release(m);
    }


    /**
     * Address of the data part of the allocated memory
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
        ByteBuffer b = toDirectByteBuffer(srcOffset, size);
        b.get(destArray, destOffset, size);
    }

    public void copyTo(int srcOffset, Buffer dest, int destOffset, int size) {
        unsafe.copyMemory(m.data() + srcOffset, dest.data() + destOffset, size);
    }

    public byte[] toArray() {
        byte[] b = new byte[(int) m.dataSize()];
        toDirectByteBuffer().get(b);
        return b;
    }

    public void writeTo(FileChannel channel) throws IOException {
        channel.write(toDirectByteBuffer());
    }

    public void writeTo(File file) throws IOException {
        FileChannel channel = new FileOutputStream(file).getChannel();
        try {
            writeTo(channel);
        }
        finally {
            channel.close();
        }
    }

    public int readFrom(byte[] src, int srcOffset, int destOffset, int length) {
        int readLen = (int) Math.min(src.length - srcOffset, Math.min(size() - destOffset, length));
        ByteBuffer b = UnsafeUtil.newDirectByteBuffer(m.data(), size());
        b.position(destOffset);
        b.put(src, srcOffset, readLen);
        return readLen;
    }


    public static Buffer loadFrom(File file) throws IOException {
        FileChannel fin = new FileInputStream(file).getChannel();
        long fileSize = fin.size();
        if(fileSize > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Cannot load from file more than 2GB: " + file);
        Buffer b = new Buffer((int) fileSize);
        long pos = 0L;
        WritableChannelWrap ch = new WritableChannelWrap(b);
        while(pos < fileSize) {
            pos += fin.transferTo(0, fileSize, ch);
        }
        return b;
    }


    public ByteBuffer toDirectByteBuffer() {
        ByteBuffer b = UnsafeUtil.newDirectByteBuffer(m.data(), (int) m.dataSize());
        return b.order(ByteOrder.nativeOrder());
    }

    public ByteBuffer toDirectByteBuffer(int offset, int size) {
        ByteBuffer b = UnsafeUtil.newDirectByteBuffer(m.data() + offset, size);
        return b.order(ByteOrder.nativeOrder());
    }

}


