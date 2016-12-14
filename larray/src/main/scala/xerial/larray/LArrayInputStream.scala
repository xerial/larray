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
// LArrayInputStream.scala
// Since: 2013/03/21 5:28 PM
//
//--------------------------------------

package xerial.larray

import java.io.InputStream

import wvlet.log.LogSupport

object LArrayInputStream {

  /**
    * Create a new InputStream from a given LArray
    *
    * @param array input
    * @tparam A element type
    * @return input stream
    */
  def apply[A](array: LArray[A]): InputStream = {
    array match {
      case r: RawByteArray[A] => new RawLArrayInputStream[A](r)
      case _ => sys.error(s"cannot create InputStream from this LArray class:${array.getClass}")
    }
  }

}

/**
  * InputStream implementation for LArrays that uses RawByteArray internally.
  *
  * @author Taro L. Saito
  */
private[larray] class RawLArrayInputStream[A](array: RawByteArray[A]) extends InputStream with LogSupport {

  private var cursor = 0L
  private var mark   = 0L

  def read() = {
    val v = array.getByte(cursor)
    cursor += 1
    v
  }

  override def read(b: Array[Byte], offset: Int, len: Int): Int = {
    if (cursor >= array.size) {
      -1
    }
    else {
      val readLen = math.min(len, array.byteLength - cursor).toInt
      array.writeToArray(cursor, b, offset, readLen)
      cursor += readLen
      readLen
    }
  }

  override def available = {
    val remaining = array.size - cursor
    math.min(Integer.MAX_VALUE, remaining).toInt
  }

  override def mark(readlimit: Int) {
    // read limit can be ignored since all data is in memory
    mark = cursor
  }

  override def reset() {
    cursor = mark
  }

  override def markSupported() = {
    true
  }
}