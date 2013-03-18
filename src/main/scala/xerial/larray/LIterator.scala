//--------------------------------------
//
// LIterator.scala
// Since: 2013/03/15 14:40
//
//--------------------------------------

package xerial.larray

import collection.{AbstractIterator, Iterator}
import scala.Iterator
import collection.Iterator._
import reflect.ClassTag


/**
 * Iterator for LArray. It is a extension of [[scala.collection.Iterable]] and most of the code is
 * derived from its implementation except that the index type is Long instead of Int.
 * @author Taro L. Saito
 */
trait LIterator[+A] {
  self =>

  import LIterator._

  def hasNext : Boolean
  def next: A

  def buffered: BufferedLIterator[A] = new AbstractLIterator[A] with BufferedLIterator[A] {
    private var hd: A = _
    private var hdDefined: Boolean = false

    def head: A = {
      if (!hdDefined) {
        hd = next
        hdDefined = true
      }
      hd
    }

    def hasNext =
      hdDefined || self.hasNext

    def next =
      if (hdDefined) {
        hdDefined = false
        hd
      } else self.next

  }


  def collect[B](pf:PartialFunction[A, B]) : LIterator[B] = {
    val self = buffered
    new AbstractLIterator[B] {
      private def skip() = while (self.hasNext && !pf.isDefinedAt(self.head)) self.next
      def hasNext = { skip(); self.hasNext }
      def next = { skip(); pf(self.next) }
    }
  }



  def exists(p: A => Boolean) : Boolean = {
    var res = false
    while (!res && hasNext) res = p(next)
    res
  }
  def contains(elem: Any): Boolean = exists(_ == elem)

  def find(p: A => Boolean): Option[A] = {
    var res: Option[A] = None
    while (res.isEmpty && hasNext) {
      val e = next
      if (p(e)) res = Some(e)
    }
    res
  }


  def foreach[U](f: A => U) : Unit = while(hasNext) { f(next) }
  def forall(pred: A => Boolean) : Boolean = {
    var result = true
    while(result && hasNext) result = pred(next)
    result
  }

  def map[B](f:A=>B): LIterator[B] = new AbstractLIterator[B] {
    def next: B = f(self.next)
    def hasNext: Boolean = self.hasNext
  }

  def flatMap[B](f: A => LIterable[B]) : LIterator[B] = new AbstractLIterator[B] {
    private var current : LIterator[B] = empty
    def hasNext: Boolean =
      current.hasNext || self.hasNext && { current = f(self.next).toIterator; hasNext }
    def next: B = (if(hasNext) current else empty).next
  }

  def filter(pred:A=>Boolean) : LIterator[A] = new AbstractLIterator[A] {
    private var head : A = _
    private var headDefined : Boolean = false
    def next: A = if(hasNext) { headDefined = false; head } else empty.next
    def hasNext: Boolean = {
      do {
        if(!self.hasNext) return false
        head = self.next
      } while (!pred(head))
      headDefined = true
      true
    }
  }

  def addString(b: StringBuilder, start: String, sep: String, end: String): StringBuilder = {
    var first = true

    b append start
    for (x <- self) {
      if (first) {
        b append x
        first = false
      }
      else {
        b append sep
        b append x
      }
    }
    b append end

    b
  }

  def mkString(start: String, sep: String, end: String): String =
    addString(new StringBuilder(), start, sep, end).toString

  def mkString(sep: String): String = mkString("", sep, "")
  def mkString: String = mkString("")

  def withFilter(p: A => Boolean): LIterator[A] = filter(p)


  def sameElements(that:LIterator[_]) : Boolean = {
    while (hasNext && that.hasNext)
      if (next != that.next)
        return false

    !hasNext && !that.hasNext
  }

  def size : Long = {
    var count = 0L
    for(x <- self) count += 1
    count
  }

  def toArray[A1 >: A : ClassTag] : Array[A1] = {
    val b = Array.newBuilder[A1]
    foreach(b += _)
    b.result
  }


  def zipAll[B, A1 >: A, B1 >: B](that: LIterator[B], thisElem: A1, thatElem: B1): LIterator[(A1, B1)] = new AbstractLIterator[(A1, B1)] {
    def hasNext = self.hasNext || that.hasNext
    def next(): (A1, B1) =
      if (self.hasNext) {
        if (that.hasNext) (self.next, that.next)
        else (self.next, thatElem)
      } else {
        if (that.hasNext) (thisElem, that.next)
        else empty.next
      }
  }

  def zipWithIndex : LIterator[(A, Long)] = new AbstractLIterator[(A, Long)] {
    private var index = 0L
    def next: (A, Long) = {
      val v = (self.next, index)
      index += 1
      v
    }
    def hasNext: Boolean = self.hasNext
  }
}

object LIterator {
  val empty : LIterator[Nothing] = new AbstractLIterator[Nothing] {
    def hasNext: Boolean = false
    def next: Nothing = throw new NoSuchElementException("next on empty iterator")
  }

}

trait BufferedLIterator[+A] extends LIterator[A] {
  def head: A
}


private[array] abstract class AbstractLIterator[A] extends LIterator[A]
