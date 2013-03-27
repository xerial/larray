package xerial.larray

import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, ShouldMatchers}

/**
 * Created with IntelliJ IDEA.
 * User: hayato
 * Date: 13/03/27
 * Time: 15:06
 */
class LArrayTestWithPBT extends PropertyChecks with FlatSpec with ShouldMatchers
{
  "LArray" should
      "return the value it stores" in
        {
          forAll
          {
            (a: Int, b: Int) =>
              whenever(a > 0 && b > 0)
              {
                val lArray = LArray(a, b)
                val array = Array(a, b)

                lArray(0) should be(array(0))
                lArray(1) should be(array(1))
              }
          }
        }
}
