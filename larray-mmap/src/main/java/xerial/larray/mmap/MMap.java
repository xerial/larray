package xerial.larray.mmap;

import xerial.larray.MappedLByteArray;
import xerial.larray.buffer.BufferConfig;

import java.io.File;

/**
 * @author Taro L. Saito
 */
public class MMap {

    /**
     * Create an LArray[Byte] of a memory mapped file
     * @param f file
     * @param offset offset in file
     * @param size region byte size
     * @param mode open mode.
     */
    public static MappedLByteArray open(File f, long offset, long size, MMapMode mode) {
        return new MappedLByteArray(f, offset, size, mode, BufferConfig.allocator);
    }

    /**
     * Create an LArray[Byte] of a memory mapped file. The size of this array is determined
     * by the file size.
     * @param f
     * @param mode
     * @return
     */
    public static MappedLByteArray open(File f, MMapMode mode) {
        return new MappedLByteArray(f, 0, f.length(), mode, BufferConfig.allocator);
    }



}

