package xerial.larray.buffer;

/**
 * Memory collector interface
 * @author Taro L. Saito
 */
public interface MemoryCollector {

    /**
     * Register a memory
     * @param m
     */
    void register(Memory m);

    /**
     * Release a memory
     */
    void release(Memory m);

    /**
     * Release a memory, referenced by ref
     */
    void release(MemoryReference ref);

}
