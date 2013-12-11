package xerial.larray.buffer;

import java.lang.ref.ReferenceQueue;

import static xerial.larray.buffer.UnsafeUtil.unsafe;

/**
 * Stores |(memory size:long)| data ... |
 */
public class OffHeapMemory implements Memory {

    private final long _data;

    public static long HEADER_SIZE = 8L;

    /**
     * Create an empty memory
     */
    public OffHeapMemory() {
        this._data = 0L;
    }

    public OffHeapMemory(long address) {
        if(address != 0L)
            this._data = address + HEADER_SIZE;
        else
            this._data = 0L;
    }

    public OffHeapMemory(long address, long size) {
        if(address != 0L) {
            this._data = address + HEADER_SIZE;
            unsafe.putLong(address, size);
        }
        else {
            this._data = 0L;
        }
    }



    public long headerAddress() {
        return _data - HEADER_SIZE;
    }
    public long size() {
        return (_data == 0) ? 0L : unsafe.getLong(headerAddress()) + HEADER_SIZE;
    }

    public long address() {
        return _data;
    }

    public long dataSize() {
        return (_data == 0) ? 0L : unsafe.getLong(headerAddress());
    }

    public MemoryReference toRef(ReferenceQueue<Memory> queue) {
        return new OffHeapMemoryReference(this, queue);
    }

    public void release() {
        if(_data != 0)
            UnsafeUtil.unsafe.freeMemory(headerAddress());
    }
}


