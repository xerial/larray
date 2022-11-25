package xerial.larray.buffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.WritableByteChannel;

import static xerial.larray.buffer.UnsafeUtil.unsafe;

/**
 * Defines common utility methods for accessing LBuffer
 *
 * @author Taro L. Saito
 */
public class LBufferAPI {

    public Memory m;

    public LBufferAPI() {
    }

    public LBufferAPI(Memory m) {
        this.m = m;
    }

    /**
     * Read a byte at offset
     *
     * @param offset
     * @return
     */
    public byte apply(int offset) {
        return getByte(offset);
    }

    /**
     * Set a byte at offset
     *
     * @param offset
     * @param value
     */
    public void update(int offset, byte value) {
        putByte(offset, value);
    }

    /**
     * Read a byte at offset
     *
     * @param offset
     * @return
     */
    public byte apply(long offset) {
        return getByte(offset);
    }

    /**
     * Set a byte at offset
     *
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
        LBufferConfig.allocator.release(m);
        m = null;
    }


    /**
     * Address of the data part of the allocated memory
     *
     * @return
     */
    public long address() {
        return m.address();
    }

    /**
     * Size of this buffer
     * @return
     */
    public long size() {
        return m.dataSize();
    }

    /**
     * Clear the buffer by filling with zeros
     */
    public void clear() {
        fill(0, size(), (byte) 0);
    }

    /**
     * Fill the buffer of the specified range with a given value
     * @param offset
     * @param length
     * @param value
     */
    public void fill(long offset, long length, byte value) {
        unsafe.setMemory(address() + offset, length, value);
    }

    public byte getByte(int offset) {
        return unsafe.getByte(address() + offset);
    }

    public char getChar(int offset) {
        return unsafe.getChar(address() + offset);
    }

    public short getShort(int offset) {
        return unsafe.getShort(address() + offset);
    }

    public int getInt(int offset) {
        return unsafe.getInt(address() + offset);
    }

    public float getFloat(int offset) {
        return unsafe.getFloat(address() + offset);
    }

    public long getLong(int offset) {
        return unsafe.getLong(address() + offset);
    }

    public double getDouble(int offset) {
        return unsafe.getDouble(address() + offset);
    }

    public void putByte(int offset, byte value) {
        unsafe.putByte(address() + offset, value);
    }

    public void putChar(int offset, char value) {
        unsafe.putChar(address() + offset, value);
    }

    public void putShort(int offset, short value) {
        unsafe.putShort(address() + offset, value);
    }

    public void putInt(int offset, int value) {
        unsafe.putInt(address() + offset, value);
    }

    public void putFloat(int offset, float value) {
        unsafe.putFloat(address() + offset, value);
    }

    public void putLong(int offset, long value) {
        unsafe.putLong(address() + offset, value);
    }

    public void putDouble(int offset, double value) {
        unsafe.putDouble(address() + offset, value);
    }

    public byte getByte(long offset) {
        return unsafe.getByte(address() + offset);
    }

    public char getChar(long offset) {
        return unsafe.getChar(address() + offset);
    }

    public short getShort(long offset) {
        return unsafe.getShort(address() + offset);
    }

    public int getInt(long offset) {
        return unsafe.getInt(address() + offset);
    }

    public float getFloat(long offset) {
        return unsafe.getFloat(address() + offset);
    }

    public long getLong(long offset) {
        return unsafe.getLong(address() + offset);
    }

    public double getDouble(long offset) {
        return unsafe.getDouble(address() + offset);
    }

    public void putByte(long offset, byte value) {
        unsafe.putByte(address() + offset, value);
    }

    public void putChar(long offset, char value) {
        unsafe.putChar(address() + offset, value);
    }

    public void putShort(long offset, short value) {
        unsafe.putShort(address() + offset, value);
    }

    public void putInt(long offset, int value) {
        unsafe.putInt(address() + offset, value);
    }

    public void putFloat(long offset, float value) {
        unsafe.putFloat(address() + offset, value);
    }

    public void putLong(long offset, long value) {
        unsafe.putLong(address() + offset, value);
    }

    public void putDouble(long offset, double value) {
        unsafe.putDouble(address() + offset, value);
    }


    /**
     * Copy the contents of this buffer begginning from the srcOffset to a destination byte array
     * @param srcOffset
     * @param destArray
     * @param destOffset
     * @param size
     */
    public void copyTo(int srcOffset, byte[] destArray, int destOffset, int size) {
        int cursor = destOffset;
        for (ByteBuffer bb : toDirectByteBuffers(srcOffset, size)) {
            int bbSize = bb.remaining();
            if ((cursor + bbSize) > destArray.length)
                throw new ArrayIndexOutOfBoundsException(String.format("cursor + bbSize = %,d", cursor + bbSize));
            bb.get(destArray, cursor, bbSize);
            cursor += bbSize;
        }
    }

    /**
     * Copy the contents of this buffer to the destination LBuffer
     * @param srcOffset
     * @param dest
     * @param destOffset
     * @param size
     */
    public void copyTo(long srcOffset, LBufferAPI dest, long destOffset, long size) {
        unsafe.copyMemory(address() + srcOffset, dest.address() + destOffset, size);
    }

    /**
     * Extract a slice [from, to) of this buffer. This methods creates a copy of the specified region.
     * @param from
     * @param to
     * @return
     */
    public LBuffer slice(long from, long to) {
        if(from > to)
            throw new IllegalArgumentException(String.format("invalid range %,d to %,d", from, to));

        long size = to - from;
        LBuffer b = new LBuffer(size);
        copyTo(from, b, 0, size);
        return b;
    }

    /**
     * Create a view of the range [from, to) of this buffer. Unlike slice(from, to), the generated view
     * is a reference to this buffer.
     * @param from
     * @param to
     * @return
     */
    public WrappedLBuffer view(long from, long to) {
        if(from > to)
            throw new IllegalArgumentException(String.format("invalid range %,d to %,d", from, to));

        return new WrappedLBuffer(m, from + offset(), to - from);
    }

    /**
     * Convert this buffer to a java array.
     * @return
     */
    public byte[] toArray() {
        if (size() > Integer.MAX_VALUE)
            throw new IllegalStateException("Cannot create byte array of more than 2GB");

        int len = (int) size();
        ByteBuffer bb = toDirectByteBuffer(0L, len);
        byte[] b = new byte[len];
        // Copy data to the array
        bb.get(b, 0, len);
        return b;
    }

    /**
     * Write the buffer contents to the given file channel. This method just
     * calls channel.write(this.toDirectByteBuffers());
     * @param channel
     * @throws IOException
     */
    public void writeTo(FileChannel channel) throws IOException {
        channel.write(toDirectByteBuffers());
    }

    /**
     * Writes the buffer contents to the given byte channel.
     *
     * @param channel
     * @throws IOException
     */
    public void writeTo(GatheringByteChannel channel) throws IOException {
        channel.write(toDirectByteBuffers());
    }

    /**
     * Writes the buffer contents to the given byte channel.
     *
     * @param channel
     * @throws IOException
     */
    public void writeTo(WritableByteChannel channel) throws IOException {
        for (ByteBuffer buffer : toDirectByteBuffers()) {
            channel.write(buffer);
        }
    }

    /**
     * Dump the buffer contents to a file
     * @param file
     * @throws IOException
     */
    public void writeTo(File file) throws IOException {
        FileChannel channel = new FileOutputStream(file).getChannel();
        try {
            writeTo(channel);
        } finally {
            channel.close();
        }
    }

    /**
     * Read the given source byte array, then overwrite this buffer's contents
     *
     * @param src source byte array
     * @param destOffset offset in this buffer to read to
     * @return the number of bytes read
     */
    public int readFrom(byte[] src, long destOffset) {
        return readFrom(src, 0, destOffset, src.length);
    }

    /**
     * Read the given source byte array, then overwrite this buffer's contents
     *
     * @param src source byte array
     * @param srcOffset offset in source byte array to read from
     * @param destOffset offset in this buffer to read to
     * @param length max number of bytes to read
     * @return the number of bytes read
     */
    public int readFrom(byte[] src, int srcOffset, long destOffset, int length) {
        int readLen = (int) Math.min(src.length - srcOffset, Math.min(size() - destOffset, length));
        ByteBuffer b = toDirectByteBuffer(destOffset, readLen);
        b.position(0);
        b.put(src, srcOffset, readLen);
        return readLen;
    }

    /**
     * Reads the given source byte buffer into this buffer at the given offset
     * @param src source byte buffer
     * @param destOffset offset in this buffer to read to
     * @return the number of bytes read
     */
    public int readFrom(ByteBuffer src, long destOffset) {
        if (src.remaining() + destOffset > size())
            throw new BufferOverflowException();
        int readLen = src.remaining();
        ByteBuffer b = toDirectByteBuffer(destOffset, readLen);
        b.position(0);
        b.put(src);
        return readLen;
    }

    /**
     * Create an LBuffer from a given file.
     * @param file
     * @return
     * @throws IOException
     */
    public static LBuffer loadFrom(File file) throws IOException {
        FileChannel fin = new FileInputStream(file).getChannel();
        long fileSize = fin.size();
        if (fileSize > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Cannot load from file more than 2GB: " + file);
        LBuffer b = new LBuffer((int) fileSize);
        long pos = 0L;
        WritableChannelWrap ch = new WritableChannelWrap(b);
        while (pos < fileSize) {
            pos += fin.transferTo(0, fileSize, ch);
        }
        return b;
    }


    /**
     * Gives an sequence of ByteBuffers. Writing to these ByteBuffers modifies the contents of this LBuffer.
     * @return
     */
    public ByteBuffer[] toDirectByteBuffers() {
        return toDirectByteBuffers(0, size());
    }

    /**
     * Gives an sequence of ByteBuffers of a specified range. Writing to these ByteBuffers modifies the contents of this LBuffer.
     * @param offset
     * @param size
     * @return
     */
    public ByteBuffer[] toDirectByteBuffers(long offset, long size) {
        long pos = offset;
        long blockSize = Integer.MAX_VALUE;
        long limit = offset + size;
        int numBuffers = (int) ((size + (blockSize - 1)) / blockSize);
        ByteBuffer[] result = new ByteBuffer[numBuffers];
        int index = 0;
        while (pos < limit) {
            long blockLength = Math.min(limit - pos, blockSize);
            result[index++] = UnsafeUtil.newDirectByteBuffer(address() + pos, (int) blockLength, this)
                    .order(ByteOrder.nativeOrder());
            pos += blockLength;
        }
        return result;

    }

    /**
     * Gives a ByteBuffer view of the specified range. Writing to the returned ByteBuffer modifies the contenets of this LByteBuffer
     * @param offset
     * @param size
     * @return
     */
    public ByteBuffer toDirectByteBuffer(long offset, int size) {
        return UnsafeUtil.newDirectByteBuffer(address() + offset, size, this);
    }

    protected long offset() {
        return 0;
    }
}
