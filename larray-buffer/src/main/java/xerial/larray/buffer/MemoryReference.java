package xerial.larray.buffer;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * Phantom reference to the allocated memory that will be queued to the ReferenceQueue upon GC time
 */
public abstract class MemoryReference extends PhantomReference<Memory> {
    public final long address;

    /**
     * Create a phantom reference
     * @param m the allocated memory
     * @param queue the reference queue to which GCed reference of the Memory will be put
     */
    public MemoryReference(Memory m, ReferenceQueue<Memory> queue) {
        super(m, queue);
        this.address = m.headerAddress();
    }

    abstract public Memory toMemory();
    abstract public String name();
}
