/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//--------------------------------------
//
// DataUnit.scala
// Since: 2012/12/07 1:38 PM
//
//--------------------------------------

package xerial.larray

/**
  * Translators of data sizes
  *
  * @author
  *   Taro L. Saito
  */
object DataUnit {

  /**
    * Convert the given byte size into human readable format like 1024 -> 1K
    * @param byteSize
    * @return
    *   string representation of the byte size
    */
  def toHumanReadableFormat(byteSize: Long): String = {
    // kilo, mega, giga, tera, peta, exa, zetta, yotta
    val unitName = Seq("", "K", "M", "G", "T", "P", "E", "Z", "Y")

    def loop(index: Int, v: Long): (Long, String) = {
      if (index >= unitName.length)
        (byteSize, "")
      val next = v >> 10L
      if (next == 0L)
        (v, unitName(index))
      else
        loop(index + 1, next)
    }

    val (prefix, unit) =
      if (byteSize > 0)
        loop(0, byteSize)
      else
        loop(0, -byteSize) match { case (p, u) => (-p, u) }
    s"$prefix$unit"
  }

//  implicit class DataSize(size:Long) {
//    def K = size * KB
//    def M = size * MB
//    def G = size * GB
//    def P = size * PB
//  }

  // data size unit
  val KB = 1L << 10
  val MB = 1L << 20
  val GB = 1L << 30
  val PB = 1L << 40

}
