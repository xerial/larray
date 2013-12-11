package xerial.larray.buffer;

/**
 * A subrange of memory
 *
 * @author Taro L. Saito
 */
public class WrappedLBuffer extends LBufferAPI {

    private final long offset;
    private final long size;

    public WrappedLBuffer(Memory m, long offset, long size) {
        super(m);
        this.offset = offset;
        this.size = size;
    }

    @Override
    public long address() {
        return m.address() + offset;
    }

    @Override
    public long size() {
        return size;
    }
}
