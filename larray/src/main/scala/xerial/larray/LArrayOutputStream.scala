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
// LArrayOutputStream.scala
// Since: 2013/03/21 5:54 PM
//
//--------------------------------------

package xerial.larray

import java.io.OutputStream
import reflect.ClassTag

/**
 * Create LArray using `java.io.OutputStream` interface
 *
 * @author Taro L. Saito
 */
class LArrayOutputStream[A : ClassTag] extends OutputStream {

  private val buf = new LByteArrayBuilder

  def write(v: Int) {
    buf += v.toByte
  }

  override def write(b: Array[Byte], off: Int, len: Int) {
    buf.append(b, off, len)
  }

  def result : LArray[A] = {
    val arr = buf.result.asInstanceOf[LByteArray]
    LArray.wrap[A](arr.size, arr.m)
  }

}