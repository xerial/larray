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
// UInt32ArrayTest.scala
// Since: 2013/03/18 4:08 PM
//
//--------------------------------------

package xerial.larray

import wvlet.airspec.AirSpec

/**
  * @author
  *   Taro L. Saito
  */
class UInt32ArrayTest extends AirSpec {

  test("UInt32Array") {

    test("record values larger than 2G") {

      val u       = new UInt32Array(10)
      val v: Long = Int.MaxValue.toLong + 10L
      u(0) = v
      u(0) shouldBe (v)

      u(1) = 3141341
      u(1) shouldBe (3141341L)

      val v2 = Integer.MAX_VALUE.toLong * 2L
      u(2) = v2
      u(2) shouldBe (v2)
    }

    test("have builder") {
      val b = UInt32Array.newBuilder
      b += 1
      b += Int.MaxValue.toLong + 3L
      b += 4
      val u = b.result()

      u.size shouldBe (3)
      u(0) shouldBe (1L)
      u(1) shouldBe (Int.MaxValue.toLong + 3L)
      u(2) shouldBe (4L)

    }

  }

}
