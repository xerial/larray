package xerial.larray.buffer;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static xerial.larray.buffer.UnsafeUtil.unsafe;


/**
 * Stores |(memory size:long)| data ... |
 */
class OffHeapMemory implements Memory {

    private final long _data;

    public static long HEADER_SIZE = 8L;

    public OffHeapMemory(long address) {
        this._data = address + HEADER_SIZE;
    }

    public long address() {
        return _data - HEADER_SIZE;
    }
    public long size() {
        return (_data == 0) ? 0L : unsafe.getLong(address());
    }

    public long data() {
        return _data;
    }

    public long dataSize() {
        return (_data == 0) ? 0L : unsafe.getLong(address()) - HEADER_SIZE;
    }

}


class MemoryReference extends PhantomReference<Memory> {
    public final Long address;
    public MemoryReference(Memory m, ReferenceQueue<Memory> queue) {
        super(m, queue);
        this.address = m.address();
    }
}


/**
 * Allocating off-heap memory
 *
 * @author Taro L. Saito
 */
public class OffHeapMemoryAllocator implements MemoryAllocator {

    // Table from address -> MemoryReference
    private Map<Long, MemoryReference> allocatedMemoryReferences = new ConcurrentHashMap<Long, MemoryReference>();
    private ReferenceQueue<Memory> queue = new ReferenceQueue<Memory>();

    {
        // Register a shutdown hook to deallocate memory
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized(this) {
                    for(long address : allocatedMemoryReferences.keySet()) {
                        // TODO Display warnings for unreleased memory addresses
                    }
                }
            }
        }));

        // Start OffHeapMemory collector that releases the allocated memories when MemoryReference (phantom reference) is collected by GC.
        Thread collector = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        MemoryReference ref = MemoryReference.class.cast(queue.remove());
                        //System.err.println(String.format("collected by GC. address:%x", ref.address.longValue()));
                        release(ref);
                    }
                    catch(Exception e) {
                        e.printStackTrace(System.err);
                    }
                }
            }
        });
        collector.setDaemon(true);
        collector.start();
    }

    private AtomicLong totalAllocatedSize = new AtomicLong(0L);

    public long allocatedSize() { return totalAllocatedSize.get(); }

    public Memory allocate(long size) {
        if(size == 0L)
          return new OffHeapMemory(0L);

        // Allocate memory of the given size + HEADER space
        long memorySize = size + OffHeapMemory.HEADER_SIZE;
        Memory m = new OffHeapMemory(unsafe.allocateMemory(memorySize));
        unsafe.putLong(m.address(), memorySize);

        // Register a memory reference that will be collected upon GC
        MemoryReference ref = new MemoryReference(m, queue);
        allocatedMemoryReferences.put(ref.address, ref);
        totalAllocatedSize.getAndAdd(memorySize);
        return m;
    }

    public void release(MemoryReference ref) {
        if(allocatedMemoryReferences.containsKey(ref.address)) {
            release(new OffHeapMemory(ref.address));
        }
    }

    public void release(Memory m) {
        long address = m.address();
        if(allocatedMemoryReferences.containsKey(address)) {
            long size = m.size();
            totalAllocatedSize.getAndAdd(-size);
            unsafe.freeMemory(address);
            allocatedMemoryReferences.remove(address);
        }
    }


}
