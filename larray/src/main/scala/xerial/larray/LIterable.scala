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
// LIterable.scala
// Since: 2013/03/15 16:25
//
//--------------------------------------

package xerial.larray

import reflect.ClassTag
import annotation.tailrec
import collection.{AbstractIterator, Iterator}
import xerial.core.log.Logger
import scala.inline


/**
 * Iterable interface for LArray.
 *
 * @author Taro L. Saito
 */
trait LIterable[A] { self : LSeq[A] =>

  type Repr = LArray[A]

  /**
   * Create a new array that concatenates two arrays
   * @param other
   * @return
   */
  def concat(other:LSeq[A]) : Repr = {
    val b = newBuilder
    b.sizeHint(this.size + other.size)
    b.append(self)
    b.append(other)
    b.result()
  }

  /**
   * Create a new array that concatenates two arrays
   * @param other
   * @return
   */
  def ++(other:LSeq[A]) : Repr = concat(other)

  /**
   * fold left
   * @param z the start value
   * @param op the binary operator
   * @tparam B the result type of the binary operator
   * @return the result of inserting op between consecutive elements of this array, going left to right with the start value z on the left:
   *         {{{ op(...op(op(z, x1), x2), ..., xn))) }}}
   */
  def /:[B](z:B)(op:(B, A) => B) : B = foldLeft(z)(op)

  /**
   * fold right
   * @param z the start value
   * @param op the binary operator
   * @tparam B the result type of the binary operator
   * @return the result of inserting op between consecutive elements of this array, going right to left with the start value z on the right:
   *         {{{ op(x1, op(x2, ..., op(xn, z)...)) }}}
   *
   */
  def :\[B](z:B)(op:(A, B) => B) : B = foldRight(z)(op)

  /**
   * Copy of this array with an element appended.
   * @param elem the appended element
   * @return a new array consisting of all elements of this array follwed by the new elem
   */
  def :+(elem:A) : Repr = {
    val b = newBuilder
    b.sizeHint(size + 1)
    b.append(self)
    b.append(elem)
    b.result()
  }

  /**
   * Copy of thie array with an element prepended.
   * @param elem the prepended element.
   * @return a new array consisting ofall elements of this array preceded by the new elem.
   */
  def +:(elem:A) : Repr = {
    val b = newBuilder
    b.sizeHint(size + 1)
    b.append(elem)
    b.append(self)
    b.result()
  }

  /**
   * Provides the Iterable interface for Java
   * @return
   */
  def ji : java.lang.Iterable[A] = new java.lang.Iterable[A] {
    def iterator(): java.util.Iterator[A] = new java.util.Iterator[A] {
      private var index = 0L
      def hasNext: Boolean = index < size
      def next(): A = {
        val v = self(index)
        index += 1
        v
      }
      override def remove() { throw new UnsupportedOperationException("remove") }
    }
  }

  protected[this] def newBuilder : LBuilder[A, Repr]

  /**
   * Creates a new iterator over all elements contained in this collection
   */
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

  /**
   * Creates a copy of this array in the form of the standard Scala Array
   * @tparam A1
   * @return
   */
  def toArray[A1 >: A : ClassTag] : Array[A1] = {
    val b = Array.newBuilder[A1]
    foreach(b += _)
    b.result()
  }

  /**
   * Tests whether this sequence is empty
   * @return
   */
  def isEmpty : Boolean = { size == 0L }

  /**
   * Builds a new collection by applying a partial function to all elments of this array on which the function is defined.
   */
  def collect[B](pf:PartialFunction[A, B]) : LIterator[B] = iterator.collect(pf)

  /**
   * Finds the first element of this array on which the given partial function is defined, and applies the partial function to it.
   * @param pf partial function
   * @tparam B return type
   * @return an option value containing pf applied to the first value for which the function is defined, or None if not exists.
   */
  def collectFirst[B](pf:PartialFunction[A, B]) : Option[B] = {
    for (x <- self.toIterator) { // make sure to use an iterator or `seq`
      if (pf isDefinedAt x)
        return Some(pf(x))
    }
    None
  }

  def contains(elem: A): Boolean = exists(_ == elem)
  def exists(p: A => Boolean) : Boolean = iterator.exists(p)

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
    while (i < len && p(self(i))) i += 1
    i - from
  }

  def length : Long = size

  def withFilter(p: A=>Boolean) : LIterator[A] = iterator.filter(p)

  def map[B](f:A=>B): LIterator[B] = iterator.map(f)
  def flatMap[B](f: A => LIterator[B]) : LIterator[B] = iterator.flatMap(f)

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

  def slice(from:Long) : LArray[A] = slice(from, size)

  def slice(from:Long, until:Long) : LArray[A] = {
    val lo    = math.max(from, 0L)
    val hi    = math.min(math.max(until, 0L), length)
    val elems = math.max(hi - lo, 0L)
    // Supply array size to reduce the number of memory allocation
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
  def span(p:A=>Boolean) : (Repr, Repr) = {
    val l, r = newBuilder
    var toLeft = true
    for (x <- this) {
      toLeft = toLeft && p(x)
      (if (toLeft) l else r) += x
    }
    (l.result, r.result)
  }

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

  def zipAll[B, A1 >: A, B1 >: B](that: LIterable[B], thisElem: A1, thatElem: B1): LIterator[(A1, B1)] =
    iterator.zipAll(that.toIterator, thisElem, thatElem)

  def zipWithIndex : LIterator[(A, Long)] = iterator.zipWithIndex

  def zip[B](that: LIterable[B]): LIterator[(A, B)] = iterator.zip(that.toIterator)

  def addString(b: StringBuilder, start: String, sep: String, end: String): StringBuilder =
    iterator.addString(b, start, sep, end)
  def mkString(start: String, sep: String, end: String): String =
    addString(new StringBuilder(), start, sep, end).toString()
  def mkString(sep: String): String = mkString("", sep, "")
  def mkString: String = mkString("")


  class SlidingIterator(size:Long, step:Long) extends AbstractLIterator[Repr] {
    require(size > 0 && step > 0, s"size:$size and step:$step must be greater than 0")
    private var cursor = 0L

    def hasNext = cursor < self.size

    def next() = {
      val begin = cursor
      val end = math.min(begin + size, self.size)
      val b = newBuilder
      b.sizeHint(end-begin)
      var i = begin
      while(i < end) {
        b += self.apply(i)
        i += 1
      }
      cursor += step
      b.result
    }
  }


  /**
   * Groups elements in fixed size blocks by passing a 'sliding window' over them
   * @param size the number of elements per group
   * @return An iterator producing group of elements.
   */
  def sliding(size:Int) : LIterator[Repr] = sliding(size, 1)

  /**
   * Groups elemnts in fixed size blocks by passing a 'sliding window' over them.
   * @param size the number of elements per group
   * @param step the distance between the first elements of successive groups
   * @return An itertor producing group of elements.
   */
  def sliding(size:Long, step:Long) : LIterator[Repr] = new SlidingIterator(size, step)

}