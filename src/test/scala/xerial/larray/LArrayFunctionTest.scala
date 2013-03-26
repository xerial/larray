//--------------------------------------
//
// LArrayFunctionTest.scala
// Since: 2013/03/26 15:41
//
//--------------------------------------

package xerial.larray

import reflect.ClassTag
import org.scalatest.{WordSpec, FlatSpec}
import org.scalatest.matchers.ShouldMatchers
import xerial.core.log.Logger



object LArrayFunctionTest extends Logger with ShouldMatchers {

  abstract class LMatcher[A](left: LSeq[A]) {
    def ===[A: ClassTag](answer: Seq[A]) {
      val l = left.mkString(", ")
      val a = answer.mkString(", ")
      debug(s"target:$l, answer:$a")
      l should be(a)
    }
  }

  implicit class LIterableMatcher[A: ClassTag](leftIt: LIterator[A]) extends LMatcher[A](leftIt.toLArray[A])

  implicit class LArrayMatcher[A: ClassTag](left: LSeq[A]) extends LMatcher[A](left)

  implicit class LArrayTupleMatcher[A: ClassTag](left: (LSeq[A], LSeq[A])) {

    def ===[A: ClassTag](answer: (Seq[A], Seq[A])) {
      (left, answer) match {
        case ((l1, l2), (a1, a2)) =>
          l1 === a1
          l2 === a2
      }
    }
  }

}

trait LIntArrayBehaviour { this: LArraySpec =>

  import LArrayFunctionTest._

  def validArray(arr:Seq[Int]) {
    val l: LArray[Int] = arr.toLArray

    When(s"input is ${arr.mkString(", ")}")

    "have iterator" in {
      l.iterator === arr
    }

    "map elements" in {
      l.map(_ * 2) === arr.map(_ * 2)
      l.map(_.toFloat) === arr.map(_.toFloat)
    }

    "flatMap nested elements" in {
      l.flatMap(x => (0 Until x).map(x => x)) === arr.flatMap(x => (0 until x).map(x => x))
    }

    "filter elements" in {
      l.filter(_ % 2 == 1) === arr.filter(_ % 2 == 1)
      l.filterNot(_ % 2 == 1) === arr.filterNot(_ % 2 == 1)
    }

    "reverse elements" in {
      l.reverse === arr.reverse
      l.reverseIterator.toLArray === arr.reverse
    }

    "find an element" in {
      l.find(_ == 4) shouldBe arr.find(_ == 4)
      l.find(_ == 10) shouldBe arr.find(_ == 10)

      l.contains(3) should be(arr.contains(3))
      l.exists(_ == 1) should be(arr.exists(_ == 1))
    }

    "be used in for-comprehension" in {
      l.map(x => x) === (for (e <- arr) yield e)
      for (e <- l) yield e === (for (e <- arr) yield e)

      for (e <- l if e > 3) yield e === (for (e <- arr if e > 3) yield e)
    }

    "slice elements" in {
      l.slice(1, 3) === arr.slice(1, 3)
      l.slice(2) === arr.slice(2, arr.length)
    }

    "drop elements" taggedAs ("drop") in {
      val da = arr.drop(4)
      debug(s"drop(4): ${da.mkString(", ")}")
      l.drop(4) === arr.drop(4)
    }

    "retrieve elements" in {
      l.head shouldBe arr.head
      l.tail === arr.tail

      l.take(4) === arr.take(4)
      l.takeRight(3) === arr.takeRight(3)
      l.takeWhile(_ < 3) === arr.takeWhile(_ < 3)
      l.dropWhile(_ < 4) === arr.dropWhile(_ < 4)
    }

    "split elements" in {
      l.splitAt(2) === arr.splitAt(2)
      l.splitAt(4) === arr.splitAt(4)
      def f(x: Int) = x <= 2
      l.span(f) === arr.span(f)
      l.partition(_ % 3 == 0) === arr.partition(_ % 3 == 0)
    }

    "fold elements" in {
      l.foldLeft(0)(_ + _) shouldBe arr.foldLeft(0)(_ + _)
      (0 /: l)(_ + _) shouldBe ((0 /: arr)(_ + _))
      l.foldRight(0)(_ + _) shouldBe arr.foldRight(0)(_ + _)
      (l :\ 0)(_ + _) shouldBe (arr :\ 0)(_ + _)
    }

    "reduce elements" in {
      def sum(a: Int, b: Int): Int = a + b
      l.reduce(sum) shouldBe arr.reduce(sum)
      l.aggregate(100)(sum, sum) shouldBe arr.aggregate(100)(sum, sum)
    }

    "scan elements" in {
      l.scanLeft(100)(_ * _) === arr.scanLeft(100)(_ * _)
    }

    "concatenate elements" in {
      l ++ l === arr ++ arr
      l.concat(l) === arr ++ arr
      l :+ 10 === arr :+ 10
      7 +: l === 7 +: arr
    }

    "collect elements" in {
      def f: PartialFunction[Int, Int] = {
        case i: Int if i % 2 == 0 => i
      }
      l.collect(f) === arr.collect(f)
      l.collectFirst(f) shouldBe arr.collectFirst(f)
    }

    "transform to array" in {
      l.toArray shouldBe arr.toArray
    }

    "copy to array" in {
      val b = new Array[Int](l.length.toInt)
      l.copyToArray(b, 0, b.length)
      b shouldBe arr.toArray
    }

    "zip with elements" in {
      l.zipWithIndex === arr.zipWithIndex
      l.zipAll(l.drop(3), -1, 255) === arr.zipAll(arr.drop(3), -1, 255)
      l.zip(l.takeRight(4)) === arr.zip(arr.takeRight(4))
    }

    "make strings" in {
      l.mkString("--") shouldBe arr.mkString("--")
      l.mkString("(", ",", ")") shouldBe arr.mkString("(", ",", ")")
    }
  }
}

/**
 * @author Taro L. Saito
 */
class LArrayFunctionTest extends LArraySpec with LIntArrayBehaviour {

  "test1" should {
    behave like validArray(Seq(0, 1, 2, 3, 4, 5))
  }

  "test2" should {
    behave like validArray(Seq(4, 3, 1, 10, 3, 5, 3, 9))
  }
}