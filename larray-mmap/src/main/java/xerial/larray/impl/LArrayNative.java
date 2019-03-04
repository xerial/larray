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
package xerial.larray.impl;

/**
 * LArray native code interface
 * @author Taro L. Saito
 */
public class LArrayNative {

    static {
        try {
            LArrayLoader.load();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static native int copyToArray(long srcAddress, Object destArray, int destOffset, int length);
    public static native int copyFromArray(Object srcArray, int srcOffset, long destAddress, int length);

    public static native long mmap(long fd, int mode, long offset, long size);
    public static native void munmap(long address, long size);
    public static native void msync(long handle, long address, long size);
    public static native long duplicateHandle(long handle);
    public static native boolean prefetch(long address, long size);
}
