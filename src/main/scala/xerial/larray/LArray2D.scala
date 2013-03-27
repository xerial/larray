//--------------------------------------
//
// LArray2D.scala
// Since: 2013/03/26 13:58
//
//--------------------------------------

package xerial.larray

import reflect.ClassTag

object LArray2D {

  /**
   * Create a new 2D array
   * @param rowSize the row size
   * @param colSize the column size
   * @tparam A the element type
   * @return a new 2D array
   */
  def of[A : ClassTag](rowSize:Long, colSize:Long) = new LArray2D[A](rowSize, colSize)

}

/**
 * LArray2D is a wrapper of LArray to emulate 2-dimensional array using a single array.
 * @author Taro L. Saito
 */
class LArray2D[A : ClassTag](val rowSize:Long, val colSize:Long) {
  private val arr : LArray[A] = LArray.of[A](rowSize * colSize)

  @inline def pos(i:Long, j:Long) = i * rowSize + j

  def apply(i:Long, j:Long) : A = arr(pos(i, j))
  def update(i:Long, j:Long, v:A) : A = arr.update(pos(i,j), v)

  def free { arr.free }

  /**
   * Get a view of a row
   * @param row the row index
   * @return LArrayView[A] of the specified row
   */
  def row(row:Long) = {
    val p = pos(row, 0)
    arr.view(p, p+colSize)
  }

  /**
   * Clear all of the elements in the array
   */
  def clear() = arr.clear()

  /**
   * Get a raw LArray representation of this 2D array
   * @return
   */
  def rawArray : LArray[A] = arr

}