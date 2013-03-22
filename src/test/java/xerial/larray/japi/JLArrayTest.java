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

        LIntArray l = LArray.newLIntArray(5L);
        for (long i = 0; i < l.size(); ++i) l.update(i, (int) i * 2);
        _logger.debug(l.mkString(", "));
        for (long i = 0; i < l.size(); ++i) l.update(i, (int) (i * i));
        _logger.debug(l.mkString(", "));

        Assert.assertEquals(5L, l.size());

        l.free();
    }
}
