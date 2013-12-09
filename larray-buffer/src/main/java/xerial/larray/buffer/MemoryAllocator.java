package xerial.larray.buffer;

/**
 * Memory allocator interface
 * @author Taro L. Saito
 */
public interface MemoryAllocator {

    Memory allocate(long size);
    void release(Memory m);
    void release(MemoryReference ref);

}
