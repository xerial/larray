//--------------------------------------
//
// LArrayFunctionTest.scala
// Since: 2013/03/26 15:41
//
//--------------------------------------

package xerial.larray

import reflect.ClassTag

/**
 * @author Taro L. Saito
 */
class LArrayFunctionTest extends LArraySpec {

  def test[A:ClassTag, B:ClassTag](input:Array[A], answer:Array[A] => Seq[B], target:LArray[A] => LSeq[B]) : Boolean = {
    val l = input.toLArray
    val o1 = answer(input)
    val o2 = target(l)
    (o1, o2) match {
      case (oi:Iterable[B], o2:LIterable[B]) => oi.toLArray.sameElements(o2)
      case _ => warn(s"cannot compare the result: $o1 and $o2"); false
    }
  }

  abstract class LMatcher[A](left:LSeq[A]) {
    def ===[A:ClassTag](answer:Seq[A]) {
      val l = left.mkString(", ")
      val a = answer.mkString(", ")
      debug(s"target:$l, answer:$a")
      l should be (a)
    }
  }


  implicit class LIterableMatcher[A:ClassTag](leftIt:LIterator[A]) extends LMatcher[A](leftIt.toLArray[A])
  implicit class LArrayMatcher[A:ClassTag](left:LSeq[A]) extends LMatcher[A](left)



  "LArray" should {

    /**
     * Sample input data
     */
    trait Input1 {
      val a = Seq(0, 1, 2, 3, 4)
      val l = a.toLArray
    }

    trait Input2 {

    }

    "have iterator" in new Input1 {
      l.iterator.mkString(", ") should be (a.mkString(", "))
    }

    "map elements" in new Input1 {
      l.map(_*2) === a.map(_*2)
    }

    "flatMap nested elements" in new Input1 {
      l.flatMap(x => (0 Until x).map(x=>x)) === a.flatMap(x => (0 until x).map(x => x))
    }

    "filter elements" in new Input1 {
      l.filter(_ % 2 == 1) === a.filter(_ % 2 == 1)
      l.filterNot(_ % 2 == 1) === a.filterNot(_ % 2 == 1)
    }

    "reverse elements" in new Input1 {
      l.reverse === a.reverse
    }

    "find an element" in new Input1 {
      l.find(_ == 4) shouldBe a.find(_ == 4)
      l.find(_ == 10) shouldBe a.find(_ == 10)

      l.contains(3) should be (a.contains(3))
      l.exists(_ == 1) should be (a.exists(_ == 1))
    }

    "be written in for-comprehension" in new Input1 {
      l.map(x => x) === (for(e <- a) yield e)
      for(e <- l) yield e === (for(e <- a) yield e)
    }

    "slice elements" in new Input1 {
      l.slice(1, 3) === a.slice(1, 3)
      l.slice(2) === a.slice(2, a.length)
    }

    "drop elements" taggedAs("drop") in new Input1 {
      val da = a.drop(4)
      debug(s"drop(4): ${da.mkString(", ")}")
      l.drop(4) === a.drop(4)
    }

    "retrieve elements" in new Input1 {
      l.head shouldBe a.head
      l.tail === a.tail

      l.take(4) === a.take(4)
      l.takeRight(3) === a.takeRight(3)
      l.takeWhile(_ < 3) === a.takeWhile(_ < 3)
      //l.splitAt(2) === a.splitAt(2)
      //l.splitAt(4) === a.splitAt(4)
      l.dropWhile(_ < 4) === a.dropWhile(_ < 4)
      def f(x:Int) = x<=2
      (l.span(f), a.span(f)) match {
        case ((l1, a1), (l2, a2)) =>
          //l1 === (a1.toArray[Int])
      }
    }

    "fold elements" in new Input1 {
      l.foldLeft(0)(_ + _) shouldBe a.foldLeft(0)(_ + _)
      (0 /: l)(_ + _) shouldBe ((0 /: a)(_ + _))
      l.foldRight(0)(_ + _) shouldBe a.foldRight(0)(_ + _)
      (l :\ 0)(_ + _) shouldBe (a :\ 0)(_ + _)
    }

    "reduce elements" in new Input1 {
      def sum(a:Int, b:Int) : Int = a + b
      l.reduce(sum) shouldBe a.reduce(sum)
      l.aggregate(100)(sum, sum) shouldBe a.aggregate(100)(sum, sum)
    }

    "scan elements" in new Input1 {
      l.scanLeft(100)(_ * _) === a.scanLeft(100)(_ * _)
    }

    "concatenate elements" in new Input1 {
      l ++ l === a ++ a
      l :+ 10 === a :+ 10
      7 +: l === 7 +: a
    }

    "collect elements" in new Input1 {
      def f : PartialFunction[Int, Int] = { case i : Int if i % 2 == 0 => i }
      l.collect(f) === a.collect(f)
    }




  }
}