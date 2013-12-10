package xerial.larray.mmap;

import xerial.larray.buffer.Memory;
import xerial.larray.buffer.MemoryReference;

import java.lang.ref.ReferenceQueue;

/**
 * @author Taro L. Saito
 */
public class MMapMemoryReference extends MemoryReference {

    public final long size;

    public MMapMemoryReference(Memory m, ReferenceQueue<Memory> queue) {
        super(m, queue);
        this.size = m.size();
    }

    public String name() { return "mmap"; }

    public MMapMemory toMemory() {
        return new MMapMemory(address, size);
    }

}

