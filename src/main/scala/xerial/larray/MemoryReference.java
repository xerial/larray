package xerial.larray;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * @author Taro L. Saito
 */
public class MemoryReference extends PhantomReference<Memory> {
    public long address;

    public MemoryReference(Memory m, ReferenceQueue queue) {
        super(m, queue);
        address = m.address();
    }
}
