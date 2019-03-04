package xerial.larray.mmap;


import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import sun.misc.SharedSecrets;
import xerial.larray.buffer.LBufferAPI;
import xerial.larray.buffer.LBufferConfig;
import xerial.larray.buffer.UnsafeUtil;
import xerial.larray.impl.LArrayNative;
import xerial.larray.impl.OSInfo;

/**
 * Memory-mapped file buffer
 *
 * @author Taro L. Saito
 */
public class MMapBuffer extends LBufferAPI {

    private final RandomAccessFile raf;
    private final FileChannel fc;
    private final long fd;
    private final int pagePosition;

    private final long address;
    private long winHandle = -1;

    @Override
    public long address()  { return address; }

    /**
     * Open an memory mapped file.
     * @param f
     * @param mode
     * @throws IOException
     */
    public MMapBuffer(File f, MMapMode mode) throws IOException {
        this(f, 0L, f.length(), mode);
    }

    /**
     * Open an memory mapped file.
     * @param f
     * @param offset
     * @param size
     * @param mode
     * @throws IOException
     */
    public MMapBuffer(File f, long offset, long size, MMapMode mode) throws IOException {
        super();
        this.raf = new RandomAccessFile(f, mode.mode);
        this.fc = raf.getChannel();
        // Retrieve file descriptor
        FileDescriptor rawfd = raf.getFD();
        try {
            if(!OSInfo.isWindows()) {
                Field idf = rawfd.getClass().getDeclaredField("fd");
                idf.setAccessible(true);
                this.fd = idf.getInt(rawfd);
            }
            else {
                // In Windows, fd is stored as 'handle'
                Field idf = rawfd.getClass().getDeclaredField("handle");
                idf.setAccessible(true);
                this.fd = idf.getLong(rawfd);
            }
        }
        catch(Exception e) {
            throw new IOException("Failed to retrieve file descriptor of " + f.getPath() + ": " + e.getMessage());
        }

        long allocationGranule = UnsafeUtil.unsafe.pageSize();
        this.pagePosition = (int) (offset % allocationGranule);

        // Compute mmap address
        if(!fc.isOpen())
            throw new IOException("closed " + f.getPath());

        long fileSize = fc.size();
        if(fileSize < offset + size) {
            // If file size is smaller than the specified size, extend the file size
            raf.seek(offset + size - 1);
            raf.write(0);
            //logger.trace(s"extend file size to ${fc.size}")
        }
        long mapPosition = offset - pagePosition;
        long mapSize = size + pagePosition;
        // A workaround for the error when calling fc.map(MapMode.READ_WRITE, offset, size) with size more than 2GB

        long rawAddr = LArrayNative.mmap(fd, mode.code, mapPosition, mapSize);
        //trace(f"mmap addr:$rawAddr%x, start address:${rawAddr+pagePosition}%x")

        if(OSInfo.isWindows()) {
            sun.misc.JavaIOFileDescriptorAccess a = SharedSecrets.getJavaIOFileDescriptorAccess();
            winHandle = LArrayNative.duplicateHandle(a.getHandle(raf.getFD()));
            //debug(f"win handle: $winHandle%x")
        }

        this.m = new MMapMemory(rawAddr, mapSize);
        LBufferConfig.allocator.register(m);

        this.address = rawAddr + pagePosition;
    }

    /**
     * Forces any changes made to this buffer to be written to the file
     */
    public void flush() {
        LArrayNative.msync(winHandle, m.headerAddress(), m.size());
    }

    /**
     * Close the memory mapped file. To ensure the written data is saved in the file, call flush before closing.
     */
    public void close() throws IOException {
        release();
        fc.close();
    }

    /**
     * Attempts to prefetch the specified range in memory so that followup
     * accesses do not incur I/O. This method is best-effort: there are no
     * guarantees that the JVM and the OS will honor this request, and even
     * if they do there are no guarantees for how long the data in the range
     * will remain in memory before being paged out. When this method
     * returns the prefetch operation may be still ongoing.
     * You should only attempt to prefetch data you are going to access soon,
     * and most likely length should be significantly smaller than the size of
     * the page cache.
     * This method returns false if the prefetch request could not be issued
     * to the OS; true otherwise. Note that the best-effort behavior described
     * above applies even if prefetch returns true.
     */
    public bool prefetch(long offset, long length) {
        return LArrayNative.prefetch(m.headerAddress()+offset, length);
    }

    protected long offset() {
        return pagePosition;
    }

}
