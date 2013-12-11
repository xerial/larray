/*--------------------------------------------------------------------------
 *  Copyright 2013 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
package xerial.larray.japi;

import scala.reflect.ClassTag$;
import xerial.larray.*;
import xerial.larray.buffer.LBufferConfig;
import xerial.larray.buffer.MemoryCollector;
import xerial.larray.mmap.MMapMode;

import java.io.File;

/**
 * Java interface of LArray
 * @author Taro L. Saito
 */
public class LArrayJ {

    static MemoryCollector defaultAllocator() { return  LBufferConfig.allocator; }

    public static MappedLByteArray mmap(File f, MMapMode mode) {
        return new MappedLByteArray(f, 0L, f.length(), mode, defaultAllocator());
    }

    public static MappedLByteArray mmap(File f, long offset, long size, MMapMode mode) {
        return new MappedLByteArray(f, offset, size, mode, defaultAllocator());
    }


    public static LByteArray newLByteArray(long size) {
        return new LByteArray(size, defaultAllocator());
    }

    public static LCharArray newLCharArray(long size) {
        return new LCharArray(size, defaultAllocator());
    }

    public static LShortArray newLShortArray(long size) {
        return new LShortArray(size, defaultAllocator());
    }

    public static LIntArray newLIntArray(long size) {
        return new LIntArray(size, defaultAllocator());
    }

    public static LFloatArray newLFloatArray(long size) {
        return new LFloatArray(size, defaultAllocator());
    }

    public static LDoubleArray newLDoubleArray(long size) {
        return new LDoubleArray(size, defaultAllocator());
    }

    public static LLongArray newLLongArray(long size) {
        return new LLongArray(size, defaultAllocator());
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
