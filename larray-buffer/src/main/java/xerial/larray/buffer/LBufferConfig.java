package xerial.larray.buffer;

/**
 * Holding the default memory allocator
 * @author Taro L. Saito
 */
public class LBufferConfig {

    public static MemoryAllocator allocator = new DefaultMemoryAllocator();
}
