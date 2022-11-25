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
// LBitArrayTest.scala
// Since: 2013/03/25 12:33
//
//--------------------------------------

package xerial.larray

import scala.util.Random

/**
  * @author
  *   Taro L. Saito
  */
class LBitArrayTest extends LArraySpec with LArrayBehaviour {
  "LBitArray" should {

    "have constructor" in {
      val b = new LBitArray(6)
      b.size shouldBe (6)

      b.clear
      b.on(1)
      b.toString shouldBe ("010000")

      b.on(5)
      b.toString shouldBe ("010001")
    }

    "set bits" in {
      val N = 100
      val b = new LBitArray(N)
      b.size shouldBe (N)
      debug(b)
      b.clear
      debug(b)
      b.forall(_ == false) shouldBe (true)
      for (i <- 0L until b.length) {
        b.on(i)
      }
      b.forall(_ == true) shouldBe (true)
    }

    "on and off specific bits" in {
      val b = new LBitArray(10000)
      b.fill
      b.forall(_ == true) shouldBe (true)

      for (pos <- Seq(91, 34, 5093, 443, 4)) {
        b.off(pos)
        b(pos) shouldBe (false)
      }
    }

    "have builder" in {

      val b  = LArray.newBuilder[Boolean]
      val in = Seq(true, false, false, true, true, false, false, true, true)
      in.foreach(b += _)
      val l = b.result()

      debug(l)

      l.toString shouldBe (in.map(v => if (v) "1" else "0").mkString)

    }

    "behave like valid LArray" should {
      val input = Seq(true, false, true, false, false, false, true, true)
      behave like validArray(input)
    }

    "behave like valid LArray for large input" should {
      val input2 = (for (i <- 0 until 150) yield { Random.nextBoolean }).toArray.toSeq
      behave like validArray(input2)
    }

  }
}
