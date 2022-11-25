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
//--------------------------------------
//
// LArrayFunctionTest.scala
// Since: 2013/03/26 15:41
//
//--------------------------------------

package xerial.larray

import java.io.File

import org.scalatest._
import wvlet.log.LogSupport

import scala.reflect.ClassTag

object LArrayFunctionTest extends LogSupport with Matchers {

  def stringRepr[A: ClassTag](l: LSeq[A]): String = {
    val tag = implicitly[ClassTag[A]]
    val isBoolean = (tag.runtimeClass == java.lang.Boolean.TYPE)
    if (isBoolean) {
      l.toString
    }
    else {
      l.mkString(", ")
    }
  }
  def stringRepr[A: ClassTag](l: Seq[A]): String = {
    val tag = implicitly[ClassTag[A]]
    val isBoolean = (tag.runtimeClass == java.lang.Boolean.TYPE)
    if (isBoolean) {
      l.map(v => if (v.asInstanceOf[Boolean]) {
        "1"
      }
      else {
        "0"
      }).mkString
    }
    else {
      l.mkString(", ")
    }
  }

  abstract class LMatcher[A: ClassTag](left: LSeq[A]) extends LogSupport {
    def ===[A: ClassTag](answer: Seq[A]) {
      val l = stringRepr(left)
      val a = stringRepr(answer)
      trace(s"target:$l, answer:$a")
      l shouldBe(a)
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

trait LArrayBehaviour {
  this: LArraySpec =>

  import LArrayFunctionTest._

  def validArray[A: ClassTag](arr: Seq[A]) = {
    val l: LArray[A] = arr.toLArray

    When(s"input is (${stringRepr(arr).take(100)})")

    "have iterator" in {
      l.iterator === arr
    }

    "check status" in {
      l.isEmpty shouldBe(arr.isEmpty)
      l.size shouldBe arr.size
    }

    "reverse elements" in {
      l.reverse === arr.reverse
      l.reverseIterator.toLArray === arr.reverse
    }

    "flatMap nested elements" in {
      l.flatMap(x => (0 Until 3).map(x => x)) === arr.flatMap(x => (0 until 3).map(x => x))
    }

    "be used in for-comprehension" in {
      l.map(x => x) === (for (e <- arr) yield e)
      for (e <- l) yield e === (for (e <- arr) yield e)
    }

    "slice elements" in {
      l.slice(1, 3) === arr.slice(1, 3)
      l.slice(2) === arr.slice(2, arr.length)
    }

    "drop elements" taggedAs ("drop") in {
      val da = arr.drop(4)
      l.drop(4) === arr.drop(4)
    }

    "report head/tail" in {
      if (!l.isEmpty) {
        l.head shouldBe(arr.head)
        l.tail === arr.tail
      }
    }

    "retrieve elements" in {
      if (!l.isEmpty) {
        l.init === arr.init
      }
      l.take(4) === arr.take(4)
      l.takeRight(3) === arr.takeRight(3)
    }

    "split elements" in {
      l.splitAt(2) === arr.splitAt(2)
      l.splitAt(4) === arr.splitAt(4)
    }

    "transform to array" in {
      stringRepr(l.toArray) shouldBe stringRepr(arr.toArray)
    }

    "copy to array" in {
      val b = new Array[A](l.length.toInt)
      l.copyToArray(b, 0, b.length)
      stringRepr(b) shouldBe stringRepr(arr.toArray)
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

    "read from/write to file" in {
      val file = l.saveTo(File.createTempFile("sample", ".larray", new File("target")))
      file.deleteOnExit()
      LArray.loadFrom[A](file) === arr
    }

    "have sliding iterator" in {
      val w = math.max(l.length, (l.length / 10)).toInt
      if (w > 0) {
        val ls = l.sliding(w, w).toArray
        val as = arr.sliding(w, w).toArray
        for ((l1, a1) <- ls.zip(as)) {
          l1 === a1
        }
      }
    }
  }

  def validDoubleArray(arr: Seq[Double]) = {
    val l: LArray[Double] = arr.toLArray

    When(s"input is (${arr.mkString(", ").take(100)})")

    "map elements" in {
      l.map(_ * 2) === arr.map(_ * 2)
      l.map(_.toInt) === arr.map(_.toInt)
    }

    "filter elements" in {
      l.filter(_ % 2 == 1) === arr.filter(_ % 2 == 1)
      l.filterNot(_ % 2 == 1) === arr.filterNot(_ % 2 == 1)
    }

    "be used in for-comprehension with if statment" in {
      for (e <- l if e > 3) yield e === (for (e <- arr if e > 3) yield e)
    }

    "find an element" taggedAs ("fel") in {
      l.find(_ == 4d) shouldBe arr.find(_ == 4d)
      l.find(_ == 10d) shouldBe arr.find(_ == 10d)
      l.contains(3d) shouldBe(arr.contains(3d))
      l.exists(_ == 1d) shouldBe(arr.exists(_ == 1d))
    }

    "take/drop while a condition is satisfied" in {
      l.takeWhile(_ < 3) === arr.takeWhile(_ < 3)
      l.dropWhile(_ < 4) === arr.dropWhile(_ < 4)
    }

    "partition elements" in {
      def f(x: Double) = x <= 2.0
      l.span(f) === arr.span(f)
      l.partition(_ % 3 == 0) === arr.partition(_ % 3 == 0)
    }

    "fold elements" in {
      if (arr.length <= 1000) {
        l.foldLeft(0d)(_ + _) shouldBe arr.foldLeft(0d)(_ + _)
        (0d /: l) (_ + _) shouldBe ((0d /: arr) (_ + _))
        l.foldRight(0d)(_ + _) shouldBe arr.foldRight(0d)(_ + _)
        (l :\ 0d) (_ + _) shouldBe (arr :\ 0d) (_ + _)
      }
    }

    "reduce elements" in {
      def sum(a: Double, b: Double): Double = a + b
      if (!l.isEmpty) {
        l.reduce(sum) shouldBe arr.reduce(sum)
        l.aggregate(100d)(sum, sum) shouldBe arr.aggregate(100d)(sum, sum)
      }
    }

    "scan elements" in {
      l.scanLeft(100d)(_ * _) === arr.scanLeft(100d)(_ * _)
    }

    "concatenate elements" in {
      l ++ l === arr ++ arr
      l.concat(l) === arr ++ arr
      l :+ 10d === arr :+ 10d
      7d +: l === 7d +: arr
    }

    "collect elements" in {
      def f: PartialFunction[Double, Double] = {
        case i: Double if i % 2 == 0 => i
      }
      l.collect(f) === arr.collect(f)
      l.collectFirst(f) shouldBe arr.collectFirst(f)
    }

  }

  def validFloatArray(arr: Seq[Float]) = {
    val l: LArray[Float] = arr.toLArray

    When(s"input is (${arr.mkString(", ").take(100)})")

    "map elements" in {
      l.map(_ * 2) === arr.map(_ * 2)
      l.map(_.toInt) === arr.map(_.toInt)
    }

    "filter elements" in {
      l.filter(_ % 2 == 1) === arr.filter(_ % 2 == 1)
      l.filterNot(_ % 2 == 1) === arr.filterNot(_ % 2 == 1)
    }

    "find an element" taggedAs ("fel") in {
      l.find(_ == 4f) shouldBe arr.find(_ == 4f)
      l.find(_ == 10f) shouldBe arr.find(_ == 10f)
      l.contains(3f) shouldBe(arr.contains(3f))
      l.exists(_ == 1f) shouldBe(arr.exists(_ == 1f))
    }

    "be used in for-comprehension with if statements" in {
      for (e <- l if e > 3) yield e === (for (e <- arr if e > 3) yield e)
    }

    "take/drop elements while a condition is satisfied" in {
      l.takeWhile(_ < 3) === arr.takeWhile(_ < 3)
      l.dropWhile(_ < 4) === arr.dropWhile(_ < 4)
    }

    "partition elements" in {
      def f(x: Float) = x <= 2.0
      l.span(f) === arr.span(f)
      l.partition(_ % 3 == 0) === arr.partition(_ % 3 == 0)
    }

    "fold elements" in {
      if (arr.length <= 1000) {
        l.foldLeft(0f)(_ + _) shouldBe arr.foldLeft(0f)(_ + _)
        (0f /: l) (_ + _) shouldBe ((0f /: arr) (_ + _))
        l.foldRight(0f)(_ + _) shouldBe arr.foldRight(0f)(_ + _)
        (l :\ 0f) (_ + _) shouldBe (arr :\ 0f) (_ + _)
      }
    }

    "reduce elements" in {
      def sum(a: Float, b: Float): Float = a + b
      if (!l.isEmpty) {
        l.reduce(sum) shouldBe arr.reduce(sum)
        l.aggregate(100f)(sum, sum) shouldBe arr.aggregate(100f)(sum, sum)
      }
    }

    "scan elements" in {
      l.scanLeft(100f)(_ * _) === arr.scanLeft(100f)(_ * _)
    }

    "concatenate elements" in {
      l ++ l === arr ++ arr
      l.concat(l) === arr ++ arr
      l :+ 10f === arr :+ 10f
      7f +: l === 7f +: arr
    }

    "collect elements" in {
      def f: PartialFunction[Float, Float] = {
        case i: Float if i % 2 == 0 => i
      }
      l.collect(f) === arr.collect(f)
      l.collectFirst(f) shouldBe arr.collectFirst(f)
    }

  }

  def validIntArray(arr: Seq[Int]) = {
    val l: LArray[Int] = arr.toLArray

    When(s"input is (${arr.mkString(", ").take(100)})")

    "map elements" in {
      l.map(_ * 2) === arr.map(_ * 2)
      l.map(_.toFloat) === arr.map(_.toFloat)
    }

    "filter elements" in {
      l.filter(_ % 2 == 1) === arr.filter(_ % 2 == 1)
      l.filterNot(_ % 2 == 1) === arr.filterNot(_ % 2 == 1)
    }

    "find an element" in {
      l.find(_ == 4) shouldBe arr.find(_ == 4)
      l.find(_ == 10) shouldBe arr.find(_ == 10)

      l.contains(3) shouldBe(arr.contains(3))
      l.exists(_ == 1) shouldBe(arr.exists(_ == 1))
    }

    "be used in for-comprehension with if statments" in {
      for (e <- l if e > 3) yield e === (for (e <- arr if e > 3) yield e)
    }

    "take/drop elements while some condition is satisfied" in {
      l.takeWhile(_ < 3) === arr.takeWhile(_ < 3)
      l.dropWhile(_ < 4) === arr.dropWhile(_ < 4)
    }

    "partition elements" in {
      def f(x: Int) = x <= 2
      l.span(f) === arr.span(f)
      l.partition(_ % 3 == 0) === arr.partition(_ % 3 == 0)
    }

    "fold elements" in {
      if (arr.length <= 1000) {
        l.foldLeft(0)(_ + _) shouldBe arr.foldLeft(0)(_ + _)
        (0 /: l) (_ + _) shouldBe ((0 /: arr) (_ + _))
        l.foldRight(0)(_ + _) shouldBe arr.foldRight(0)(_ + _)
        (l :\ 0) (_ + _) shouldBe (arr :\ 0) (_ + _)
      }
    }

    "reduce elements" in {
      def sum(a: Int, b: Int): Int = a + b
      if (!l.isEmpty) {
        l.reduce(sum) shouldBe arr.reduce(sum)
        l.aggregate(100)(sum, sum) shouldBe arr.aggregate(100)(sum, sum)
      }
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

  }

  def validLongArray(arr: Seq[Long]) = {
    val l: LArray[Long] = arr.toLArray

    When(s"input is (${arr.mkString(", ").take(100)})")

    "map elements" in {
      l.map(_ * 2) === arr.map(_ * 2)
      l.map(_.toFloat) === arr.map(_.toFloat)
    }

    "filter elements" in {
      l.filter(_ % 2 == 1) === arr.filter(_ % 2 == 1)
      l.filterNot(_ % 2 == 1) === arr.filterNot(_ % 2 == 1)
    }

    "find an element" in {
      l.find(_ == 4) shouldBe arr.find(_ == 4)
      l.find(_ == 10) shouldBe arr.find(_ == 10)

      l.contains(3) shouldBe(arr.contains(3))
      l.exists(_ == 1) shouldBe(arr.exists(_ == 1))
    }

    "be used in for-comprehension with if statements" in {
      for (e <- l if e > 3) yield e === (for (e <- arr if e > 3) yield e)
    }

    "take/drop elements while some condition is satisfied" in {
      l.takeWhile(_ < 3) === arr.takeWhile(_ < 3)
      l.dropWhile(_ < 4) === arr.dropWhile(_ < 4)
    }

    "partition elements" in {
      def f(x: Long) = x <= 2
      l.span(f) === arr.span(f)
      l.partition(_ % 3 == 0) === arr.partition(_ % 3 == 0)
    }

    "fold elements" in {
      if (arr.length <= 1000) {
        l.foldLeft(0L)(_ + _) shouldBe arr.foldLeft(0L)(_ + _)
        (0L /: l) (_ + _) shouldBe ((0L /: arr) (_ + _))
        l.foldRight(0L)(_ + _) shouldBe arr.foldRight(0L)(_ + _)
        (l :\ 0L) (_ + _) shouldBe (arr :\ 0L) (_ + _)
      }
    }

    "reduce elements" in {
      def sum(a: Long, b: Long): Long = a + b
      if (!l.isEmpty) {
        l.reduce(sum) shouldBe arr.reduce(sum)
        l.aggregate(100L)(sum, sum) shouldBe arr.aggregate(100L)(sum, sum)
      }
    }

    "scan elements" in {
      l.scanLeft(100L)(_ * _) === arr.scanLeft(100L)(_ * _)
    }

    "concatenate elements" in {
      l ++ l === arr ++ arr
      l.concat(l) === arr ++ arr
      l :+ 10L === arr :+ 10L
      7L +: l === 7L +: arr
    }

    "collect elements" in {
      def f: PartialFunction[Long, Long] = {
        case i: Long if i % 2 == 0 => i
      }
      l.collect(f) === arr.collect(f)
      l.collectFirst(f) shouldBe arr.collectFirst(f)
    }

  }

}

/**
  * @author Taro L. Saito
  */
class LArrayFunctionTest extends LArraySpec with LArrayBehaviour {

  "int test1" should {
    val input = Seq(0, 1, 2, 3, 4, 5)
    behave like validArray(input)
    behave like validIntArray(input)
  }

  "int test2" should {
    val input = Seq(4, 3, 1, 10, 3, 5, 3, 9)
    behave like validArray(input)
    behave like validIntArray(input)
  }

  "long test1" should {
    val input = Seq(4L, 3L, 1L, 10L, 3L, 5L, 3L, 9L)
    behave like validArray(input)
    behave like validLongArray(input)
  }

  "empty test" should {
    val input = Seq.empty[Int]
    behave like validArray(input)
    behave like validIntArray(input)
  }

  "float test1" should {
    val input = Seq(0f, 1f, 2f, 3f, 4f, 5f)
    behave like validArray(input)
    behave like validFloatArray(input)
  }

  "float test2" should {
    val input = Seq(3.5f, 1.2f, 2.9f, 0.3f, 3.8f, 7f)
    behave like validArray(input)
    behave like validFloatArray(input)
  }

  "double test2" should {
    val input = Seq(3.5d, 1.2d, 2.0d, 0.3d, 3.8d, 4d)
    behave like validArray(input)
    behave like validDoubleArray(input)
  }

}

