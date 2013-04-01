package xerial.larray

import org.scalatest.prop.PropertyChecks
import org.scalatest.ShouldMatchers
import scala.util.Random
import org.scalacheck.Gen

/**
 * Created with IntelliJ IDEA.
 * User: hayato
 * Date: 13/03/27
 * Time: 15:06
 */
class LArrayTestWithPBT extends PropertyChecks with ShouldMatchers with LArraySpec with LArrayBehaviour
{
  val validLength = for (l <- Gen.choose[Int](1, 10000)) yield l
  val validSeed = for (s <- Gen.choose[Int](1, 10000)) yield s

  forAll((validLength, "length"), (validSeed, "seed"))
  {
    (length: Int, seed: Int) =>
      whenever(length > 0 && seed > 0)
      {
        "int test with length " + length + " with seed " + seed should
          {
            val rand = new Random(seed)
            val input = Seq.fill(length)(rand.nextInt)
            behave like validArray(input)
            behave like validIntArray(input)
          }
      }
  }

  forAll((validLength, "length"), (validSeed, "seed"))
  {
    (length: Int, seed: Int) =>
      whenever(length > 0 && seed > 0)
      {
        "long test with length " + length + " with seed " + seed should
          {
            val rand = new Random(seed)
            val input = Seq.fill(length)(rand.nextLong)
            behave like validArray(input)
            behave like validLongArray(input)
          }
      }
  }

  forAll((validLength, "length"), (validSeed, "seed"))
  {
    (length: Int, seed: Int) =>
      whenever(length > 0 && seed > 0)
      {
        "float test with length " + length + " with seed " + seed should
          {
            val rand = new Random(seed)
            val input = Seq.fill(length)(rand.nextFloat)
            behave like validArray(input)
            behave like validFloatArray(input)
          }
      }
  }

  forAll((validLength, "length"), (validSeed, "seed"))
  {
    (length: Int, seed: Int) =>
      whenever(length > 0 && seed > 0)
      {
        "double test with length " + length + " with seed " + seed should
          {
            val rand = new Random(seed)
            val input = Seq.fill(length)(rand.nextDouble)
            behave like validArray(input)
            behave like validDoubleArray(input)
          }
      }
  }

  "empty test" should
    {
      val input = Seq.empty[Int]
      behave like validArray(input)
      behave like validIntArray(input)
    }
}
