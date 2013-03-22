//--------------------------------------
//
// LIterable.scala
// Since: 2013/03/15 16:25
//
//--------------------------------------

package xerial.larray

import reflect.ClassTag
import annotation.tailrec
import collection.{AbstractIterator, Iterator}
import scala.Iterator


/**
 * Iterable interface for LArray.
 *
 * @author Taro L. Saito
 */
trait LIterable[A] { self : LArray[A] =>

  type Repr = LArray[A]

  protected[this] def newBuilder : LBuilder[A, LArray[A]]

  def iterator : LIterator[A] = new AbstractLIterator[A] {
    private var index = 0L
    override def size = self.size
    def hasNext: Boolean = index < size
    def next(): A = {
      val v = self(index)
      index += 1
      v
    }
  }

  def reverseIterator: LIterator[A] = new AbstractLIterator[A] {
    private var i = self.size
    def hasNext: Boolean = 0L < i
    def next(): A =
      if (0L < i) {
        i -= 1L
        self(i)
      } else LIterator.empty.next()
  }

  def toIterator : LIterator[A] = iterator
  def toArray[A1 >: A : ClassTag] : Array[A1] = {
    val b = Array.newBuilder[A1]
    foreach(b += _)
    b.result()
  }

  def isEmpty : Boolean = { size == 0L }

  def collect[B](pf:PartialFunction[A, B]) : LIterator[B] = iterator.collect(pf)
  def contains(elem: A): Boolean = exists(_ == elem)
  def exists(p: A => Boolean) : Boolean = prefixLength(p(_)) != length

  def find(p: A => Boolean): Option[A] = {
    val i = prefixLength(!p(_))
    if (i < length) Some(this(i)) else None
  }

  def filter(pred:A=>Boolean) : LIterator[A] = iterator.filter(pred)
  def filterNot(pred:A=>Boolean) : LIterator[A] = iterator.filterNot(pred)
  def forall(p: A => Boolean) : Boolean = prefixLength(p(_)) == length
  def foreach[U](f: A => U) {
    var i = 0L
    val len = size
    while(i < len) { f(self(i)); i += 1}
  }
  def prefixLength(p: A => Boolean): Long = segmentLength(p, 0L)
  def segmentLength(p: A => Boolean, from: Long): Long = {
    val len = length
    var i = from
    while (i < len && p(this(i))) i += 1
    i - from
  }

  def length : Long = size

  def withFilter(p: A=>Boolean) : LIterator[A] = iterator.filter(p)

  def map[B](f:A=>B): LIterator[B] = iterator.map(f)
  def flatMap[B](f: A => LIterable[B]) : LIterator[B] = iterator.flatMap(f)

  def reverse[A]: Repr = {
    val b = newBuilder
    b.sizeHint(size)
    var i = length
    while(0L < i) {
      i -= 1
      b += self(i)
    }
    b.result()
  }


  @tailrec
  private def foldl[B](start: Long, end: Long, z: B, op: (B, A) => B): B =
    if (start == end) z
    else foldl(start + 1L, end, op(z, this(start)), op)

  @tailrec
  private def foldr[B](start: Long, end: Long, z: B, op: (A, B) => B): B =
    if (start == end) z
    else foldr(start, end - 1L, op(this(end - 1L), z), op)

  def foldLeft[B](z: B)(op: (B, A) => B): B =
    foldl(0L, length, z, op)

  def foldRight[B](z: B)(op: (A, B) => B): B =
    foldr(0L, length, z, op)

  def reduceLeft[B >: A](op: (B, A) => B): B =
    if (length > 0L)
      foldl(1L, length, this(0), op)
    else
      throw new UnsupportedOperationException("empry.reduceLeft")

  def reduceRight[B >: A](op: (A, B) => B): B =
    if (length > 0L)
      foldr(0L, length - 1, this(length - 1), op)
    else
      throw new UnsupportedOperationException("empry.reduceRight")


  def reduceLeftOption[B >: A](op: (B, A) => B): Option[B] =
    if (isEmpty) None else Some(reduceLeft(op))

  def reduceRightOption[B >: A](op: (A, B) => B): Option[B] =
    if (isEmpty) None else Some(reduceRight(op))

  def reduce[A1 >: A](op: (A1, A1) => A1): A1 = reduceLeft(op)

  def reduceOption[A1 >: A](op: (A1, A1) => A1): Option[A1] = reduceLeftOption(op)

  def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): A1 = foldLeft(z)(op)

  def aggregate[B](z: B)(seqop: (B, A) => B, combop: (B, B) => B): B = foldLeft(z)(seqop)




  def scanLeft[B](z:B)(op:(B, A) => B) : LIterator[B] = iterator.scanLeft(z)(op)

  private def negLength(n: Long) = if (n >= length) -1L else n

  def indexWhere(p: A => Boolean, from: Long): Long = {
    val start = from max 0
    negLength(start + segmentLength(!p(_), start))
  }
  def lastIndexWhere(p: A => Boolean, end: Int): Int = {
    var i = end
    while (i >= 0 && !p(this(i))) i -= 1
    i
  }
  def indexOf[B>:A](elem:B) : Long = iterator.indexOf(elem)


  def slice(from:Long, until:Long) : LArray[A] = {
    val lo    = math.max(from, 0L)
    val hi    = math.min(math.max(until, 0L), length)
    val elems = math.max(hi - lo, 0L)
    // Supply array size to recuce the number of memory allocation
    val b     = newBuilder
    b.sizeHint(elems)

    var i = lo
    while (i < hi) {
      b += self(i)
      i += 1
    }
    b.result()
  }

  def head : A = if(isEmpty) throw new NoSuchElementException else this(0)
  def tail : LArray[A] = if(isEmpty) throw new NoSuchElementException else slice(1L, size)
  def last : A = if(length > 0) self(length - 1) else throw new NoSuchElementException

  def drop(n:Long) : Repr = slice(n, length)
  def init: Repr = if (length > 0L) slice(0L, length - 1L) else throw new UnsupportedOperationException("empty.init")
  def take(n:Long) : Repr = slice(0L, n)
  def takeRight(n: Long) : Repr = slice(length - n, length)
  def takeWhile(p: A => Boolean) : Repr = take(prefixLength(p))
  def splitAt(n: Long) : (Repr, Repr) = (take(n), drop(n))
  def dropWhile(p: A => Boolean) : Repr = drop(prefixLength(p))
  def partition(p : A=>Boolean) = iterator.partition(p)
  def span(p:A=>Boolean) : (Repr, Repr) = splitAt(prefixLength(p))

  def copyToArray[B >: A](xs: Array[B], start: Int, len: Int) {
    var i = 0L
    var j = start
    val end = length min len min (xs.length - start)
    while (i < end) {
      xs(j) = this(i)
      i += 1
      j += 1
    }
  }
  def copyToArray[B >: A](xs: LArray[B], start: Long, len: Long) {
    var i = 0L
    var j = start
    val end = length min len min (xs.length - start)
    while (i < end) {
      xs(j) = this(i)
      i += 1
      j += 1
    }
  }


  def sameElements[B >: A](that: LIterable[B]): Boolean = iterator.sameElements(that.toIterator)

  def zipAll[B, A1 >: A, B1 >: B](that: LIterator[B], thisElem: A1, thatElem: B1): LIterator[(A1, B1)] =
    iterator.zipAll(that, thisElem, thatElem)

  def zipWithIndex : LIterator[(A, Long)] = iterator.zipWithIndex


  def addString(b: StringBuilder, start: String, sep: String, end: String): StringBuilder =
    iterator.addString(b, start, sep, end)
  def mkString(start: String, sep: String, end: String): String =
    addString(new StringBuilder(), start, sep, end).toString()
  def mkString(sep: String): String = mkString("", sep, "")
  def mkString: String = mkString("")
}