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

import junit.framework.Assert;
import org.junit.Test;
import xerial.larray.LIntArray;
import xerial.larray.util.Logger;

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

        Assert.assertEquals(5L, l.size());

        l.free();
    }
}
