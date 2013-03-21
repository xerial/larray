package xerial.larray.java;

import xerial.larray.Memory;

/**
 * @author Taro L. Saito
 */
public class LByteArray implements LArray<Byte> {

    private final long size;
    private final Memory m;
    
    public LByteArray(long size) {
        this(size, MemoryAllocator.defaultAllocator.allocate(size));
    }
    
    LByteArray(long size, Memory m) {
        this.size = size;
        this.m = m;
    }
    
    public byte get(long i) {
        return m.getByte(i);
    }

    public byte set(long i, byte v) {
        m.putByte(i, v);
        return v;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public void free() {
        m.free();
    }

    @Override
    public long byteLength() {
        return size;
    }
}
