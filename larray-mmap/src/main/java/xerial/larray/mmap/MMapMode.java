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
package xerial.larray.mmap;

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