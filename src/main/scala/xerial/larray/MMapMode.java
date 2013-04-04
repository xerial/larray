package xerial.larray;

/**
 * Modes allowed in memory mapped file
 * @author Taro L. Saito
 */
public enum MMapMode {

    READ_ONLY(0, "r"),
    READ_WRITE(1, "rw"),
    PRIVATE(2, "rw");

    public final int code;
    public final String mode;

    private MMapMode(int code, String fileOpenMode) {
        this.code = code;
        this.mode = fileOpenMode;
    }

}
