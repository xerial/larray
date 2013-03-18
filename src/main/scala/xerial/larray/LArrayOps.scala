//--------------------------------------
//
// LArrayOps.scala
// Since: 2013/03/15 14:39
//
//--------------------------------------

package xerial.larray

/**
 * Operations for LArray
 *
 * @author Taro L. Saito
 */
trait LArrayOps[A] {

  /**
   * Write the contents of this array to the destination buffer
   * @param srcOffset byte offset
   * @param dest destination array
   * @param destOffset offset in the destination array
   * @param length the byte length to write
   * @return byte length to write
   */
  def write(srcOffset:Long, dest:Array[Byte], destOffset:Int, length:Int) : Int

  /**
   * Read the contents from a given source buffer
   * @param src
   * @param srcOffset
   * @param destOffset
   * @param length
   */
  def read(src:Array[Byte], srcOffset:Int, destOffset:Long, length:Int) : Int


}

