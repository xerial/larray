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
import scala.reflect.ClassTag;
import xerial.larray.LArray;
import xerial.larray.LIntArray;
import xerial.larray.LObjectArray;
import xerial.larray.util.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @author Taro L. Saito
 */
public class JLArrayTest {

    Logger _logger = new Logger(this.getClass());

    @Test
    public void constructor() {

        LIntArray l = LArrayJ.newLIntArray(5L);
        for (long i = 0; i < l.size(); ++i) l.update(i, (int) i * 2);
        _logger.debug(l.mkString(", "));
        for (long i = 0; i < l.size(); ++i) l.update(i, (int) (i * i));
        _logger.debug(l.mkString(", "));

        assertEquals(5L, l.size());

        l.free();
    }

    @Test
    public void testLObjectArray32() {

        ClassTag<String> classTag = scala.reflect.ClassTag$.MODULE$.apply(String.class);
        LArray<String> lObjectArray = LObjectArray.ofDim(10, classTag);

        lObjectArray.update(0, "FOO");
        lObjectArray.update(9, "BAR");

        assertEquals(lObjectArray.apply(0), "FOO");
        assertEquals(lObjectArray.apply(9), "BAR");
        assertEquals(lObjectArray.size(), 10);

    }

    @Test
    public void testLObjectArrayLarge() {

        ClassTag<String> classTag = scala.reflect.ClassTag$.MODULE$.apply(String.class);
        long oneMoreThanInt = (1L << 31) + 1; //2147483649L
        LArray<String> lObjectArray = LObjectArray.ofDim(oneMoreThanInt, classTag);

        lObjectArray.update(oneMoreThanInt - 2, "0,2147483644");
        lObjectArray.update(oneMoreThanInt - 1, "0,0");

        assertEquals("0,2147483644", lObjectArray.apply(oneMoreThanInt - 2));
        assertEquals("0,0", lObjectArray.apply(oneMoreThanInt - 1));

        assertEquals(oneMoreThanInt, lObjectArray.size());

    }
}
