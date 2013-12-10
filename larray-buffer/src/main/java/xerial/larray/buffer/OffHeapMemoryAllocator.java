package xerial.larray.buffer;

import java.lang.ref.ReferenceQueue;
import java.util.Collection;
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

    public long headerAddress() {
        return _data - HEADER_SIZE;
    }
    public long size() {
        return (_data == 0) ? 0L : unsafe.getLong(headerAddress());
    }

    public long address() {
        return _data;
    }

    public long dataSize() {
        return (_data == 0) ? 0L : unsafe.getLong(headerAddress()) - HEADER_SIZE;
    }

    public MemoryReference toRef(ReferenceQueue<Memory> queue) {
        return new OffHeapMemoryReference(this, queue);
    }

    public void release() {
        if(_data != 0)
            UnsafeUtil.unsafe.freeMemory(headerAddress());
    }
}

class OffHeapMemoryReference extends MemoryReference {

    /**
     * Create a phantom reference
     * @param m the allocated memory
     * @param queue the reference queue to which GCed reference of the Memory will be put
     */
    public OffHeapMemoryReference(Memory m, ReferenceQueue<Memory> queue) {
        super(m, queue);
    }

    public Memory toMemory() {
        return new OffHeapMemory(address);
    }

    public String name() { return "off-heap"; }

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

        // Start OffHeapMemory collector that releases the allocated memory when the corresponding Memory object is collected by GC.
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

    /**
     * Get the total amount of allocated memory
     */
    public long allocatedSize() { return totalAllocatedSize.get(); }

    public Memory allocate(long size) {
        if(size == 0L)
          return new OffHeapMemory(0L);

        // Allocate memory of the given size + HEADER space
        long memorySize = size + OffHeapMemory.HEADER_SIZE;
        Memory m = new OffHeapMemory(unsafe.allocateMemory(memorySize));
        register(m);
        return m;
    }

    public void register(Memory m) {
        unsafe.putLong(m.headerAddress(), m.size());
        // Register a memory reference that will be collected upon GC
        MemoryReference ref = m.toRef(queue);
        allocatedMemoryReferences.put(ref.address, ref);
        totalAllocatedSize.getAndAdd(m.size());
    }


    public void release(MemoryReference ref) {
        if(allocatedMemoryReferences.containsKey(ref.address)) {
            release(ref.toMemory());
        }
    }

    /**
     * Release all memory addresses taken by this allocator.
     * Be careful in using this method, since all of the memory addresses become invalid.
     */
    public void releaseAll() {
        synchronized(this) {
            Collection<MemoryReference> refSet = allocatedMemoryReferences.values();
            for(MemoryReference ref : refSet) {
                release(ref);
            }
        }
    }


    public void release(Memory m) {
        long address = m.headerAddress();
        if(allocatedMemoryReferences.containsKey(address)) {
            long size = m.size();
            totalAllocatedSize.getAndAdd(-size);
            m.release();
            allocatedMemoryReferences.remove(address);
        }
    }


}
