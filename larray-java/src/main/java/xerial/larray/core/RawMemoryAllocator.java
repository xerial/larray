package xerial.larray.core;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static xerial.larray.core.UnsafeUtil.unsafe;


interface Memory {

    /**
     * Allocated memory address
     * @return
     */
    long address();

    /**
     * data-part address
     * @return data address
     */
    long data();

    /**
     * Allocated memory size
     * @return
     */
    long size();

    /**
     * data-part size
     * @return
     */
    long dataSize();
}

/**
 * Stores |(memory size:long)| data ... |
 */
class RawMemory implements Memory {

    private final long _address;

    public static long HEADER_SIZE = 8L;

    public RawMemory(long address) {
        this._address = address;
    }

    public long address() {
        return _address;
    }
    public long size() {
        return (_address == 0) ? 0L : unsafe.getLong(_address);
    }

    public long data() {
        return _address + HEADER_SIZE;
    }

    public long dataSize() {
        return (_address == 0) ? 0L : unsafe.getLong(_address) - HEADER_SIZE;
    }

}


class MemoryReference extends PhantomReference<Memory> {
    public MemoryReference(Memory m, ReferenceQueue<Memory> queue) {
        super(m, queue);
    }
}


/**
 * @author Taro L. Saito
 */
public class RawMemoryAllocator {

    private Map<MemoryReference, Long> allocatedMemoryReferences = new ConcurrentHashMap<MemoryReference, Long>();
    private ReferenceQueue<Memory> queue = new ReferenceQueue<Memory>();

    {
        // Register a shutdown hook to deallocate memory
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized(this) {
                    for(long address : allocatedMemoryReferences.values()) {
                        // TODO Display warnings for unreleased memory addresses
                    }
                }
            }
        }));


        // Start RawMemory collector that releases the allocated memories when MemoryReference (phantom reference) is collected by GC.
        Thread collector = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        MemoryReference ref = MemoryReference.class.cast(queue.remove());
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
          return new RawMemory(0L);

        // Allocate memory of the given size + HEADER space
        long memorySize = size + RawMemory.HEADER_SIZE;
        Memory m = new RawMemory(unsafe.allocateMemory(memorySize));
        unsafe.putLong(m.address(), memorySize);

        // Register a memory reference that will be collected upon GC
        MemoryReference ref = new MemoryReference(m, queue);
        allocatedMemoryReferences.put(ref, m.address());
        totalAllocatedSize.getAndAdd(memorySize);
        return m;
    }

    public void release(MemoryReference ref) {
        if(allocatedMemoryReferences.containsKey(ref)) {
            long address = allocatedMemoryReferences.get(ref);
            RawMemory m = new RawMemory(address);
            long size = m.size();
            totalAllocatedSize.getAndAdd(-size);
            unsafe.freeMemory(address);
        }
    }



}
