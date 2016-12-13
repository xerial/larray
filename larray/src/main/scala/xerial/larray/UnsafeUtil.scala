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
// UnsafeUtil.scala
// Since: 2013/04/04 5:36 PM
//
//--------------------------------------

package xerial.larray

import java.nio.ByteBuffer

import sun.misc.Unsafe
import wvlet.log.LogSupport

/**
  * Utilities for accessing sun.misc.Unsafe
  *
  * @author Taro L. Saito
  */
object UnsafeUtil extends LogSupport {
  val unsafe = {
    val f = classOf[Unsafe].getDeclaredField("theUnsafe")
    f.setAccessible(true)
    f.get(null).asInstanceOf[Unsafe]
  }

  private val dbbCC = Class.forName("java.nio.DirectByteBuffer").getDeclaredConstructor(classOf[Long], classOf[Int])

  def newDirectByteBuffer(addr: Long, size: Int): ByteBuffer = {
    dbbCC.setAccessible(true)
    val b = dbbCC.newInstance(new java.lang.Long(addr), new java.lang.Integer(size))
    b.asInstanceOf[ByteBuffer]
  }

  val byteArrayOffset   = unsafe.arrayBaseOffset(classOf[Array[Byte]]).toLong
  val objectArrayOffset = unsafe.arrayBaseOffset(classOf[Array[AnyRef]]).toLong
  val objectArrayScale  = unsafe.arrayIndexScale(classOf[Array[AnyRef]]).toLong
  val addressBandWidth  = System.getProperty("sun.arch.data.model", "64").toInt
  private val addressFactor = if (addressBandWidth == 64) {
    8L
  }
  else {
    1L
  }
  val addressSize = unsafe.addressSize()

  /**
    * @param obj
    * @return
    *
    */
  @deprecated(message = "Deprecated because this method does not return correct object addresses in some platform", since = "0.1")
  def getObjectAddr(obj: AnyRef): Long = {
    trace(f"address factor:$addressFactor%d, addressSize:$addressSize, objectArrayOffset:$objectArrayOffset, objectArrayScale:$objectArrayScale")

    val o = new Array[AnyRef](1)
    o(0) = obj
    objectArrayScale match {
      case 4 => (unsafe.getInt(o, objectArrayOffset) & 0xFFFFFFFFL) * addressFactor
      case 8 => (unsafe.getLong(o, objectArrayOffset) & 0xFFFFFFFFFFFFFFFFL) * addressFactor
    }
  }
}
