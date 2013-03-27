package xerial.larray;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * Use java class to extend PhantomReference so as not to create local variables holding Memory objects.
 * @author Taro L. Saito
 */
public class MemoryReference extends PhantomReference<Memory> {
    public long address;
    public long size;

    public MemoryReference(Memory m, ReferenceQueue queue) {
        super(m, queue);
        address = m.address();
        size = m.size();
    }
}
