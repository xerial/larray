package xerial.larray.buffer;

import java.lang.ref.ReferenceQueue;

/**
 * @author Taro L. Saito
 */
public class OffHeapMemoryReference extends MemoryReference {

    /**
     * Create a phantom reference
     * @param m the allocated memory
     * @param queue the reference queue to which GCed reference of the Memory will be inserted
     */
    public OffHeapMemoryReference(Memory m, ReferenceQueue<Memory> queue) {
        super(m, queue);
    }

    public Memory toMemory() {
        if(address != 0)
            return new OffHeapMemory(address);
        else
            return new OffHeapMemory();
    }

    public String name() { return "off-heap"; }

}
