package xerial.larray.core;

import sun.nio.ch.DirectBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

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
        ByteBuffer b = toDirectByteBuffer(srcOffset, size);
        b.get(destArray, destOffset, size);
    }

    public void copyTo(int srcOffset, RawByteArray dest, int destOffset, int size) {
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

    public int readFrom(byte[] src, int srcOffset, long destOffset, int length) {
        int readLen = (int) Math.min(src.length - srcOffset, Math.min(size() - destOffset, length));
        // TODO Fix this since copy memory cannot access byte[] addresses
        unsafe.copyMemory(src, srcOffset, null, m.data() + destOffset, readLen);
        return readLen;
    }


    public static RawByteArray loadFrom(File file) throws IOException {
        FileChannel fin = new FileInputStream(file).getChannel();
        long fileSize = fin.size();
        if(fileSize > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Cannot load from file more than 2GB: " + file);
        RawByteArray b = new RawByteArray((int) fileSize);
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


class WritableChannelWrap implements WritableByteChannel {

    private final RawByteArray b;
    long cursor = 0L;

    WritableChannelWrap(RawByteArray b) {
        this.b = b;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int len = (int) Math.max(src.limit() - src.position(), 0);
        int writeLen = 0;
        if(src.getClass().isAssignableFrom(DirectBuffer.class)) {
            DirectBuffer d = (DirectBuffer) src;
            unsafe.copyMemory(d.address() + src.position(), cursor, len);
            writeLen = len;
        }
        else if(src.hasArray()) {
            writeLen = b.readFrom(src.array(), src.position(), cursor, len);
        }
        else {
            for(long i=0; i<len; ++i)
                unsafe.putByte(b.data() + i, src.get((int) (src.position() + i)));
            writeLen = len;
        }
        cursor += writeLen;
        src.position(src.position() + writeLen);
        return writeLen;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }
}


