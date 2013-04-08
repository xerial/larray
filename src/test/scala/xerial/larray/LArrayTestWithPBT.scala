package xerial.larray

import org.scalatest.prop.PropertyChecks
import org.scalatest.{WordSpec, ShouldMatchers}
import scala.util.Random
import org.scalacheck.Gen

/**
 * Created with IntelliJ IDEA.
 * User: hayato
 * Date: 13/03/27
 * Time: 15:06
 */
class LArrayTestWithPBT extends PropertyChecks with LArraySpec with LArrayBehaviour
{
  val maxNumberOfTests = 25
  val maxSizeOfList = 10000
  val minSizeOfList = 1

  forAll("array", minSuccessful(maxNumberOfTests), maxSize(maxSizeOfList), minSize(1))
  {
    (input: Array[Int]) =>
      "int test with length " + input.take(10).toString should
        {
          behave like validArray(input)
          behave like validIntArray(input)
        }
  }

  forAll("array", minSuccessful(maxNumberOfTests), maxSize(maxSizeOfList), minSize(1))
  {
    (input: Array[Long]) =>
      "long test with length " + input.take(10).toString should
        {
          behave like validArray(input)
          behave like validLongArray(input)
        }
  }

  forAll("array", minSuccessful(maxNumberOfTests), maxSize(maxSizeOfList), minSize(1))
  {
    (input: Array[Float]) =>
      "float test with length " + input.take(10).toString should
        {
          behave like validArray(input)
          behave like validFloatArray(input)
        }

  }

  forAll("array", minSuccessful(maxNumberOfTests), maxSize(maxSizeOfList), minSize(1))
  {
    (input: Array[Double]) =>
      "double test with length " + input.take(10).toString should
        {
          behave like validArray(input)
          behave like validDoubleArray(input)
        }
  }

  "empty test" should
    {
      val input = Seq.empty[Int]
      behave like validArray(input)
      behave like validIntArray(input)
    }
}
