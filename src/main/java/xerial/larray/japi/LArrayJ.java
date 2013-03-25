package xerial.larray.japi;

import scala.reflect.ClassTag$;
import xerial.larray.*;

import java.io.File;

/**
 * Java interface of LArray
 * @author Taro L. Saito
 */
public class LArrayJ {

    static xerial.larray.MemoryAllocator defaultAllocator = new ConcurrentMemoryAllocator();


    public static LByteArray newLByteArray(long size) {
        return new LByteArray(size, defaultAllocator);
    }

    public static LCharArray newLCharArray(long size) {
        return new LCharArray(size, defaultAllocator);
    }

    public static LShortArray newLShortArray(long size) {
        return new LShortArray(size, defaultAllocator);
    }

    public static LIntArray newLIntArray(long size) {
        return new LIntArray(size, defaultAllocator);
    }

    public static LFloatArray newLFloatArray(long size) {
        return new LFloatArray(size, defaultAllocator);
    }

    public static LDoubleArray newLDoubleArray(long size) {
        return new LDoubleArray(size, defaultAllocator);
    }

    public static LLongArray newLLongArray(long size) {
        return new LLongArray(size, defaultAllocator);
    }

    public static LBitArray newLBitArray(long size) {
        return new LBitArray(size);
    }

    public static LIntArray loadLIntArrayFrom(File file) {
        return (LIntArray) LArray$.MODULE$.loadFrom(file, ClassTag$.MODULE$.Int());
    }

    public static LByteArray loadLByteArrayFrom(File file) {
        return (LByteArray) LArray$.MODULE$.loadFrom(file, ClassTag$.MODULE$.Byte());
    }

    public static LShortArray loadLShortArrayFrom(File file) {
        return (LShortArray) LArray$.MODULE$.loadFrom(file, ClassTag$.MODULE$.Short());
    }

    public static LCharArray loadLCharArrayFrom(File file) {
        return (LCharArray) LArray$.MODULE$.loadFrom(file, ClassTag$.MODULE$.Char());
    }

    public static LFloatArray loadLFloatArrayFrom(File file) {
        return (LFloatArray) LArray$.MODULE$.loadFrom(file, ClassTag$.MODULE$.Float());
    }

    public static LDoubleArray loadLDoubleArrayFrom(File file) {
        return (LDoubleArray) LArray$.MODULE$.loadFrom(file, ClassTag$.MODULE$.Double());
    }

    public static LLongArray loadLLongArrayFrom(File file) {
        return (LLongArray) LArray$.MODULE$.loadFrom(file, ClassTag$.MODULE$.Long());
    }

}
