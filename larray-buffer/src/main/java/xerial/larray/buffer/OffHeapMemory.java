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

    /**
     * Allocate a memory of the specified byte length. The allocated memory must be released via `release`
     * as in malloc() in C/C++.
     * @param size byte length of the memory
     * @return allocated memory information
     */
    public static Memory allocate(long size) {
        if(size == 0L)
            return new OffHeapMemory();

        // Allocate memory of the given size + HEADER space
        long memorySize = size + OffHeapMemory.HEADER_SIZE;
        long address = unsafe.allocateMemory(memorySize);
        Memory m = new OffHeapMemory(address, size);
        LBufferConfig.allocator.register(m);
        return m;
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


