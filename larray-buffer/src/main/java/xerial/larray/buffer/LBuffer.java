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
 * Off-heap memory of long indexes.
 *
 * @author Taro L. Saito
 */
public class LBuffer {

    final Memory m;

    /**
     * Allocate a memory of the specified byte size
     * @param size byte size of the array
     */
    public LBuffer(long size) {
        this.m = LArrayBuffer.allocator.allocate(size);
    }

    /**
     * Read a byte at offset
     * @param offset
     * @return
     */
    public byte apply(long offset) {
        return getByte(offset);
    }

    /**
     * Set a byte at offset
     * @param offset
     * @param value
     */
    public void update(long offset, byte value) {
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

    public long size() {
        return m.dataSize();
    }

    public void clear() {
        fill(0, size(), (byte) 0);
    }

    public void fill(long offset, long size, byte value) {
        unsafe.setMemory(m.data() + offset, size, value);
    }

    public byte getByte(long offset) {
        return unsafe.getByte(m.data() + offset);
    }

    public char getChar(long offset) {
        return unsafe.getChar(m.data() + offset);
    }

    public short getShort(long offset) {
        return unsafe.getShort(m.data() + offset);
    }

    public int getInt(long offset) {
        return unsafe.getInt(m.data() + offset);
    }

    public float getFloat(long offset) {
        return unsafe.getFloat(m.data() + offset);
    }

    public long getLong(long offset) {
        return unsafe.getLong(m.data() + offset);
    }

    public double getDouble(long offset) {
        return unsafe.getDouble(m.data() + offset);
    }

    public void putByte(long offset, byte value) {
        unsafe.putByte(m.data() + offset, value);
    }

    public void putChar(long offset, char value) {
        unsafe.putChar(m.data() + offset, value);
    }

    public void putShort(long offset, short value) {
        unsafe.putShort(m.data() + offset, value);
    }

    public void putInt(long offset, int value) {
        unsafe.putInt(m.data() + offset, value);
    }
    public void putFloat(long offset, float value) {
        unsafe.putFloat(m.data() + offset, value);
    }

    public void putLong(long offset, long value) {
        unsafe.putLong(m.data()+ offset, value);
    }

    public void putDouble(long offset, double value) {
        unsafe.putDouble(m.data() + offset, value);
    }

    public void copyTo(int srcOffset, byte[] destArray, int destOffset, int size) {
        int cursor = destOffset;
        for(ByteBuffer bb : toDirectByteBuffers(srcOffset, size)) {
            int bbSize = bb.remaining();
            if((cursor + bbSize) > destArray.length)
                throw new ArrayIndexOutOfBoundsException(String.format("cursor + bbSize = %,d", cursor+bbSize));
            bb.get(destArray, cursor, bbSize);
            cursor += bbSize;
        }
    }

    public void copyTo(int srcOffset, Buffer dest, int destOffset, int size) {
        unsafe.copyMemory(m.data() + srcOffset, dest.data() + destOffset, size);
    }

    public byte[] toArray() {
        if(size() > Integer.MAX_VALUE)
            throw new IllegalStateException("Cannot create byte array of more than 2GB");

        int len = (int) size();
        ByteBuffer bb = toDirectByteBuffer(0L, len);
        byte[] b = new byte[len];
        // Copy to array
        bb.get(b, 0, len);
        return b;
    }

    public void writeTo(FileChannel channel) throws IOException {
        channel.write(toDirectByteBuffers());
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

    public int readFrom(byte[] src, int srcOffset, long destOffset, int length) {
        int readLen = (int) Math.min(src.length - srcOffset, Math.min(size() - destOffset, length));
        ByteBuffer b = toDirectByteBuffer(destOffset, readLen);
        b.position(0);
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


    public ByteBuffer[] toDirectByteBuffers() {
        return toDirectByteBuffers(0, size());
    }

    public ByteBuffer[] toDirectByteBuffers(long offset, long size) {
        long pos = offset;
        long blockSize = Integer.MAX_VALUE;
        long limit = offset + size;
        int numBuffers = (int) ((size + (blockSize - 1)) / blockSize);
        ByteBuffer[] result = new ByteBuffer[numBuffers];
        int index = 0;
        while(pos < limit) {
            long blockLength = Math.min(limit - pos, blockSize);
            result[index++] = UnsafeUtil.newDirectByteBuffer(m.data() + pos, (int) blockLength).order(ByteOrder.nativeOrder());
            pos += blockLength;
        }
        return result;

    }

    public ByteBuffer toDirectByteBuffer(long offset, int size) {
        return UnsafeUtil.newDirectByteBuffer(m.data() + offset, size);
    }

}
