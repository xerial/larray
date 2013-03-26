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
      trace(s"target:$l, answer:$a")
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
    }


    "slice elements" in new Input1 {
      l.slice(1, 3) === a.slice(1, 3)
      l.slice(2) === a.slice(2, a.length)
    }


  }
}