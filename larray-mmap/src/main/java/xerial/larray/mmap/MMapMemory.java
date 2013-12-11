package xerial.larray.mmap;

import xerial.larray.buffer.Memory;
import xerial.larray.impl.LArrayNative;

import java.lang.ref.ReferenceQueue;

/**
 * @author Taro L. Saito
 */
public class MMapMemory implements Memory {
    public long address;
    public long size;

    public MMapMemory(long address, long size) {
        this.address = address;
        this.size = size;
    }

    public long address() {
        return address;
    }

    public long size() {
        return size;
    }

    public long headerAddress() { return address; }

    public MMapMemoryReference toRef(ReferenceQueue<Memory> queue) {
        return new MMapMemoryReference(this, queue);
    }

    public long dataSize() { return size; }

    public void release() {
        LArrayNative.munmap(address, size);
    }
}
