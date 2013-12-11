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

import org.junit.Test;
import org.xerial.snappy.Snappy;
import xerial.larray.LByteArray;
import xerial.larray.LIntArray;
import xerial.larray.MappedLByteArray;
import xerial.larray.mmap.MMapMode;
import xerial.larray.util.Logger;

import java.io.File;

public class JSnappyCompressTest {

    private Logger _logger = new Logger(this.getClass());

    @Test
    public void testCompress() throws Exception {

        long N = 100000000L;
        LIntArray l = LArrayJ.newLIntArray(N);
        for (int i = 0; i < N; i++) {
            l.update(i, (int) (Math.toDegrees(Math.sin(i / 360))));
        }
        for (int iter = 0; iter < 10; iter++) {
            long sum = 0;
            long start = System.currentTimeMillis();
            for (int i = 0; i < l.length(); i++) {
                sum += l.apply(i);
            }
            long end = System.currentTimeMillis();
            _logger.debug("time:" + (end - start) + " sum:" + sum);
        }
        _logger.debug("compressing the data");
        int maxLen = Snappy.maxCompressedLength((int) l.byteLength());
        LByteArray compressedBuf = LArrayJ.newLByteArray(maxLen);
        long compressedLen = Snappy.rawCompress(l.address(), l.byteLength(),
                compressedBuf.address());
        LByteArray compressed = (LByteArray) compressedBuf.slice(0,
                compressedLen);
        File f = File.createTempFile("snappy", ".dat", new File("target"));
        f.deleteOnExit();
        compressed.saveTo(f);

        _logger.debug("decompressing the data");
        long T1 = System.currentTimeMillis();
        MappedLByteArray b = LArrayJ.mmap(f, 0, f.length(), MMapMode.READ_ONLY);
        long T2 = System.currentTimeMillis();

        long len = Snappy.uncompressedLength(b.address(), b.length());
        LIntArray decompressed = LArrayJ.newLIntArray(len / 4);
        Snappy.rawUncompress(b.address(), b.length(), decompressed.address());
        long T3 = System.currentTimeMillis();
        _logger.debug("Map time:" + (T2 - T1));
        _logger.debug("decompress time:" + (T3 - T2));


        _logger.debug("Summing decompressed array");
        for (int iter = 0; iter < 10; iter++) {
            long sum = 0;
            long start = System.currentTimeMillis();
            for (int i = 0; i < decompressed.length(); i++) {
                sum += decompressed.apply(i);
            }
            long end = System.currentTimeMillis();
            _logger.debug("time:" + (end - start) + " sum:" + sum);
        }
    }
}