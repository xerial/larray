package xerial.larray.buffer;


import org.xerial.util.log.Logger;

import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;



/**
 * A default implementation of MemoryAllocator that allocates off-heap memory and releases allocated memories in a background thread.
 *
 * @author Taro L. Saito
 */
public class DefaultMemoryAllocator implements MemoryAllocator {

    private Logger logger = Logger.getLogger(DefaultMemoryAllocator.class);


    // Table from address -> MemoryReference
    private Map<Long, MemoryReference> allocatedMemoryReferences = new ConcurrentHashMap<Long, MemoryReference>();
    private ReferenceQueue<Memory> queue = new ReferenceQueue<Memory>();

    {
        // Enable ANSI Color
        logger.enableColor(true);

        // Start OffHeapMemory collector that releases the allocated memory when the corresponding Memory object is collected by GC.
        Thread collector = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        MemoryReference ref = MemoryReference.class.cast(queue.remove());
                        if(logger.isTraceEnabled())
                            logger.trace(String.format("collected by GC. address:%x", ref.address));
                        release(ref);
                    }
                    catch(Exception e) {
                        e.printStackTrace(System.err);
                    }
                }
            }
        });
        collector.setDaemon(true);
        logger.trace("Start memory collector");
        collector.start();
    }

    private AtomicLong totalAllocatedSize = new AtomicLong(0L);

    /**
     * Get the total amount of allocated memory
     */
    public long allocatedSize() { return totalAllocatedSize.get(); }

    /**
     * Allocate a memory of the specified byte length. The allocated memory must be released via `release`
     * as in malloc() in C/C++.
     * @param size byte length of the memory
     * @return allocated memory information
     */
    public Memory allocate(long size) {
        if(size == 0L)
            return new OffHeapMemory();

        // Allocate memory of the given size + HEADER space
        long memorySize = size + OffHeapMemory.HEADER_SIZE;
        long address = UnsafeUtil.unsafe.allocateMemory(memorySize);
        Memory m = new OffHeapMemory(address, size);
        register(m);
        return m;
    }

    public void register(Memory m) {
        // Register a memory reference that will be collected upon GC
        MemoryReference ref = m.toRef(queue);
        allocatedMemoryReferences.put(ref.address, ref);
        totalAllocatedSize.getAndAdd(m.size());
    }



    /**
     * Release all memory addresses taken by this allocator.
     * Be careful in using this method, since all of the memory addresses become invalid.
     */
    public void releaseAll() {
        synchronized(this) {
            Object[] refSet = allocatedMemoryReferences.values().toArray();
            if(refSet.length != 0)
                logger.trace("Releasing allocated memory regions");
            for(Object ref : refSet) {
                release((MemoryReference) ref);
            }
        }
    }


    public void release(MemoryReference ref) {
        release(ref.toMemory());
    }

    public void release(Memory m) {
        synchronized(this) {
            long address = m.headerAddress();
            if(allocatedMemoryReferences.containsKey(address)) {
                if(logger.isTraceEnabled())
                    logger.trace(String.format("Released memory address:%x, size:%,d", address, m.dataSize()));
                totalAllocatedSize.getAndAdd(-m.size());
                allocatedMemoryReferences.remove(address);
                m.release();
            }
        }
    }


}
