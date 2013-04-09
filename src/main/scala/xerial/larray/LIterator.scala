//--------------------------------------
//
// LIterator.scala
// Since: 2013/03/15 14:40
//
//--------------------------------------

package xerial.larray

import collection.{Seq, mutable, AbstractIterator, Iterator}
import reflect.ClassTag
import scala.Some
import collection.Iterator._
import scala.Some
import scala.Seq
import collection.mutable.ArrayBuffer


/**
 * Iterator for LArray. It is a extension of `scala.collection.Iterable` and most of the code is
 * derived from its implementation except that the index type is Long instead of Int.
 * @author Taro L. Saito
 */
trait LIterator[+A] {
  self =>

  import LIterator._

  def hasNext : Boolean
  def next(): A

  /** Tests whether this iterator is empty.
    *
    *  @return   `true` if hasNext is false, `false` otherwise.
    */
  def isEmpty: Boolean = !hasNext


  def buffered: BufferedLIterator[A] = new AbstractLIterator[A] with BufferedLIterator[A] {
    private var hd: A = _
    private var hdDefined: Boolean = false

    def head: A = {
      if (!hdDefined) {
        hd = next()
        hdDefined = true
      }
      hd
    }

    def hasNext =
      hdDefined || self.hasNext

    def next() =
      if (hdDefined) {
        hdDefined = false
        hd
      } else self.next()

  }


  def collect[B](pf:PartialFunction[A, B]) : LIterator[B] = {
    val self = buffered
    new AbstractLIterator[B] {
      private def skip() { while (self.hasNext && !pf.isDefinedAt(self.head)) self.next() }
      def hasNext = { skip(); self.hasNext }
      def next() = { skip(); pf(self.next()) }
    }
  }

  def scanLeft[B](z: B)(op: (B, A) => B): LIterator[B] = new AbstractLIterator[B] {
    var hasNext = true
    var elem = z
    def next() = if (hasNext) {
      val res = elem
      if (self.hasNext) elem = op(elem, self.next())
      else hasNext = false
      res
    } else empty.next()
  }

//  def scanRight[B](z: B)(op: (A, B) => B): LIterator[B] = {
//    // TODO impl toBuffer
//    toBuffer.scanRight(z)(op).iterator
//  }


  def exists(p: A => Boolean) : Boolean = {
    var res = false
    while (!res && hasNext) res = p(next())
    res
  }
  def contains(elem: Any): Boolean = exists(_ == elem)

  def find(p: A => Boolean): Option[A] = {
    var res: Option[A] = None
    while (res.isEmpty && hasNext) {
      val e = next()
      if (p(e)) res = Some(e)
    }
    res
  }


  /** Returns the index of the first produced value satisfying a predicate, or -1.
    *
    *  @param  p the predicate to test values
    *  @return   the index of the first produced value satisfying `p`,
    *           or -1 if such an element does not exist until the end of the iterator is reached.
    */
  def indexWhere(p: A => Boolean): Long = {
    var i = 0L
    var found = false
    while (!found && hasNext) {
      if (p(next())) {
        found = true
      } else {
        i += 1
      }
    }
    if (found) i else -1L
  }

  /** Returns the index of the first occurrence of the specified
    *  object in this iterable object.
    *
    *  @param  elem  element to search for.
    *  @return the index of the first occurrence of `elem` in the values produced by this iterator,
    *          or -1 if such an element does not exist until the end of the iterator is reached.
    */
  def indexOf[B >: A](elem: B): Long = {
    var i = 0L
    var found = false
    while (!found && hasNext) {
      if (next() == elem) {
        found = true
      } else {
        i += 1
      }
    }
    if (found) i else -1L
  }



  def foreach[U](f: A => U) { while(hasNext) { f(next()) } }
  def forall(pred: A => Boolean) : Boolean = {
    var result = true
    while(result && hasNext) result = pred(next())
    result
  }

  def map[B](f:A=>B): LIterator[B] = new AbstractLIterator[B] {
    def next(): B = f(self.next())
    def hasNext: Boolean = self.hasNext
  }

  def flatMap[B](f: A => LIterator[B]) : LIterator[B] = new AbstractLIterator[B] {
    private var current : LIterator[B] = empty
    def hasNext: Boolean =
      current.hasNext || self.hasNext && { current = f(self.next()); hasNext }
    def next(): B = (if(hasNext) current else empty).next()
  }


  def reduceLeft[B >: A](op: (B, A) => B): B = {
    if (isEmpty)
      throw new UnsupportedOperationException("empty.reduceLeft")

    var first = true
    var acc: B = 0.asInstanceOf[B]

    for (x <- self) {
      if (first) {
        acc = x
        first = false
      }
      else acc = op(acc, x)
    }
    acc
  }


  def filter(pred:A=>Boolean) : LIterator[A] = new AbstractLIterator[A] {
    private var head : A = _
    private var headDefined : Boolean = false
    def next(): A = if(hasNext) { headDefined = false; head } else empty.next()
    def hasNext: Boolean = headDefined || {
      do {
        if(!self.hasNext) return false
        head = self.next()
      } while (!pred(head))
      headDefined = true
      true
    }
  }
  def filterNot(p: A => Boolean) : LIterator[A] = filter(!p(_))

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
    addString(new StringBuilder(), start, sep, end).toString()

  def mkString(sep: String): String = mkString("", sep, "")
  def mkString: String = mkString("")

  def withFilter(p: A => Boolean): LIterator[A] = filter(p)


  def sameElements(that:LIterator[_]) : Boolean = {
    while (hasNext && that.hasNext)
      if (next() != that.next())
        return false

    !hasNext && !that.hasNext
  }

  def slice(from:Long, until:Long) : LIterator[A] = {
    val lo = from max 0
    var toDrop = lo
    while (toDrop > 0 && self.hasNext) {
      self.next()
      toDrop -= 1
    }

    new AbstractLIterator[A] {
      private var remaining = until - lo
      def hasNext = remaining > 0L && self.hasNext
      def next(): A =
        if (remaining > 0) {
          remaining -= 1
          self.next()
        }
        else empty.next()
    }
  }

  def size : Long = {
    var count = 0L
    for(x <- self) count += 1
    count
  }


  /** Selects first ''n'' values of this iterator.
    *
    *  @param  n    the number of values to take
    *  @return an iterator producing only of the first `n` values of this iterator, or else the
    *          whole iterator, if it produces fewer than `n` values.
    */
  def take(n: Long): LIterator[A] = slice(0L, n)


  /** Takes longest prefix of values produced by this iterator that satisfy a predicate.
    *
    *  @param   p  The predicate used to test elements.
    *  @return  An iterator returning the values produced by this iterator, until
    *           this iterator produces a value that does not satisfy
    *           the predicate `p`.
    */
  def takeWhile(p: A => Boolean): LIterator[A] = new AbstractLIterator[A] {
    private var hd: A = _
    private var hdDefined: Boolean = false
    private var tail: LIterator[A] = self

    def hasNext = hdDefined || tail.hasNext && {
      hd = tail.next()
      if (p(hd)) hdDefined = true
      else tail = empty
      hdDefined
    }
    def next() = if (hasNext) { hdDefined = false; hd } else empty.next()
  }


  /** Partitions this iterator in two iterators according to a predicate.
    *
    *  @param p the predicate on which to partition
    *  @return  a pair of iterators: the iterator that satisfies the predicate
    *           `p` and the iterator that does not.
    *           The relative order of the elements in the resulting iterators
    *           is the same as in the original iterator.
    */
  def partition(p: A => Boolean): (LIterator[A], LIterator[A]) = {
    val self = buffered
    class PartitionIterator(p: A => Boolean) extends AbstractLIterator[A] {
      var other: PartitionIterator = _
      val lookahead = new mutable.Queue[A]
      def skip() {
        while (self.hasNext && !p(self.head)) {
          other.lookahead += self.next()
        }
      }
      def hasNext = !lookahead.isEmpty || { skip(); self.hasNext }
      def next() = if (!lookahead.isEmpty) lookahead.dequeue()
      else { skip(); self.next() }
    }
    val l = new PartitionIterator(p)
    val r = new PartitionIterator(!p(_))
    l.other = r
    r.other = l
    (l, r)
  }

  /** Splits this Iterator into a prefix/suffix pair according to a predicate.
    *
    *  @param p the test predicate
    *  @return  a pair of Iterators consisting of the longest prefix of this
    *           whose elements all satisfy `p`, and the rest of the Iterator.
    */
  def span(p: A => Boolean): (LIterator[A], LIterator[A]) = {
    val self = buffered

    /**
     * Giving a name to following iterator (as opposed to trailing) because
     * anonymous class is represented as a structural type that trailing
     * iterator is referring (the finish() method) and thus triggering
     * handling of structural calls. It's not what's intended here.
     */
    class Leading extends AbstractLIterator[A] {
      private var isDone = false
      val lookahead = new mutable.Queue[A]
      def advance() = {
        self.hasNext && p(self.head) && {
          lookahead += self.next()
          true
        }
      }
      def finish() {
        while (advance()) ()
        isDone = true
      }
      def hasNext = lookahead.nonEmpty || advance()
      def next() = {
        if (lookahead.isEmpty)
          advance()

        lookahead.dequeue()
      }
    }
    val leading = new Leading
    val trailing = new AbstractLIterator[A] {
      private lazy val it = {
        leading.finish()
        self
      }
      def hasNext = it.hasNext
      def next() = it.next()
      override def toString = "unknown-if-empty iterator"
    }

    (leading, trailing)
  }


  /** Advances this iterator past the first ''n'' elements, or the length of the iterator, whichever is smaller.
    *
    *  @param n the number of elements to drop
    *  @return  an iterator which produces all values of the current iterator, except
    *           it omits the first `n` values.
    */
  def drop(n: Long): LIterator[A] = slice(n, Long.MaxValue)


  def toArray[A1 >: A : ClassTag] : Array[A1] = {

    val b = Array.newBuilder[A1]
    foreach(b += _)
    b.result()
  }


  def zipAll[B, A1 >: A, B1 >: B](that: LIterator[B], thisElem: A1, thatElem: B1): LIterator[(A1, B1)] = new AbstractLIterator[(A1, B1)] {
    def hasNext = self.hasNext || that.hasNext
    def next(): (A1, B1) =
      if (self.hasNext) {
        if (that.hasNext) (self.next(), that.next())
        else (self.next(), thatElem)
      } else {
        if (that.hasNext) (thisElem, that.next())
        else empty.next()
      }
  }

  def zipWithIndex : LIterator[(A, Long)] = new AbstractLIterator[(A, Long)] {
    private var index = 0L
    def next(): (A, Long) = {
      val v = (self.next(), index)
      index += 1
      v
    }
    def hasNext: Boolean = self.hasNext
  }

  def zip[B](that: LIterator[B]): LIterator[(A, B)] = new AbstractLIterator[(A, B)] {
    def hasNext = self.hasNext && that.hasNext
    def next = (self.next, that.next)
  }


  def toLArray[B >: A : ClassTag] : LArray[B] = {
    val b = LArray.newBuilder[B]
    self.foreach(b += _)
    b.result
  }


}

object LIterator {
  val empty : LIterator[Nothing] = new AbstractLIterator[Nothing] {
    def hasNext: Boolean = false
    def next(): Nothing = throw new NoSuchElementException("next on empty iterator")
  }

}

trait BufferedLIterator[+A] extends LIterator[A] {
  def head: A
}


private[larray] abstract class AbstractLIterator[A] extends LIterator[A]
