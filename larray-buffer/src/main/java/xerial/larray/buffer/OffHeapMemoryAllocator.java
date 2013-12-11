package xerial.larray.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Create an empty memory
     */
    public OffHeapMemory() {
        this._data = 0L;
    }

    public OffHeapMemory(long address) {
        if(address != 0L)
            this._data = address + HEADER_SIZE;
        else
            this._data = 0L;
    }

    public OffHeapMemory(long address, long size) {
        if(address != 0L) {
            this._data = address + HEADER_SIZE;
            unsafe.putLong(address, size);
        }
        else {
            this._data = 0L;
        }
    }

    public long headerAddress() {
        return _data - HEADER_SIZE;
    }
    public long size() {
        return (_data == 0) ? 0L : unsafe.getLong(headerAddress()) + HEADER_SIZE;
    }

    public long address() {
        return _data;
    }

    public long dataSize() {
        return (_data == 0) ? 0L : unsafe.getLong(headerAddress());
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



/**
 * Allocating off-heap memory
 *
 * @author Taro L. Saito
 */
public class OffHeapMemoryAllocator implements MemoryAllocator {

    private Logger logger = LoggerFactory.getLogger(OffHeapMemoryAllocator.class);

    // Table from address -> MemoryReference
    private Map<Long, MemoryReference> allocatedMemoryReferences = new ConcurrentHashMap<Long, MemoryReference>();
    private ReferenceQueue<Memory> queue = new ReferenceQueue<Memory>();

    {
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

    public Memory allocate(long size) {
        if(size == 0L)
          return new OffHeapMemory();

        // Allocate memory of the given size + HEADER space
        long memorySize = size + OffHeapMemory.HEADER_SIZE;
        long address = unsafe.allocateMemory(memorySize);
        if(logger.isTraceEnabled())
            logger.trace(String.format("Allocated memory address:%x, size:%,d", address, size));
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
        long address = m.headerAddress();
        if(allocatedMemoryReferences.containsKey(address)) {
            long size = m.size();
            if(logger.isTraceEnabled())
                logger.trace(String.format("Released memory address:%x, size:%,d", address, size));
            totalAllocatedSize.getAndAdd(-size);
            m.release();
            allocatedMemoryReferences.remove(address);
        }
    }


}
