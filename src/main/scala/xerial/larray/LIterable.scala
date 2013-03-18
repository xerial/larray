//--------------------------------------
//
// LIterable.scala
// Since: 2013/03/15 16:25
//
//--------------------------------------

package xerial.larray

import reflect.ClassTag


/**
 * Iterable interface for LArray
 *
 * @author Taro L. Saito
 */
trait LIterable[A] { self : LArray[A] =>

  def iterator : LIterator[A] = new AbstractLIterator[A] {
    private var index = 0L
    override def size = self.size
    def hasNext: Boolean = index < size
    def next: A = {
      val v = self(index)
      index += 1
      v
    }
  }
  def toIterator : LIterator[A] = iterator
  def toArray[A1 >: A : ClassTag] : Array[A1] = {
    val b = Array.newBuilder[A1]
    foreach(b += _)
    b.result
  }

  def collect[B](pf:PartialFunction[A, B]) : LIterator[B] = iterator.collect(pf)
  def contains(elem: A): Boolean = exists(_ == elem)
  def exists(p: A => Boolean) : Boolean = iterator.exists(p)
  def find(p: A => Boolean): Option[A] = iterator.find(p)

  def filter(pred:A=>Boolean) : LIterator[A] = iterator.filter(pred)
  def forall(pred: A => Boolean) : Boolean = iterator.forall(pred)
  def foreach[U](f: A => U) : Unit = iterator.foreach(f)

  def withFilter(p: A=>Boolean) : LIterator[A] = iterator.filter(p)

  def map[B](f:A=>B): LIterator[B] = iterator.map(f)
  def flatMap[B](f: A => LIterable[B]) : LIterator[B] = iterator.flatMap(f)


  def sameElements[B >: A](that: LIterable[B]): Boolean = iterator.sameElements(that.toIterator)


  def zipWithIndex : LIterator[(A, Long)] = iterator.zipWithIndex



  def addString(b: StringBuilder, start: String, sep: String, end: String): StringBuilder =
    iterator.addString(b, start, sep, end)
  def mkString(start: String, sep: String, end: String): String =
    addString(new StringBuilder(), start, sep, end).toString
  def mkString(sep: String): String = mkString("", sep, "")
  def mkString: String = mkString("")
}