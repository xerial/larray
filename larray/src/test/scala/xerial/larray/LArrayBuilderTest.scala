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
// LArrayBuilderTest.scala
// Since: 2013/03/19 10:37 AM
//
//--------------------------------------

package xerial.larray

import wvlet.airspec.AirSpec

import scala.util.Random

/**
  * @author
  *   Taro L. Saito
  */
class LArrayBuilderTest extends AirSpec {

  test("LArrayBuilder") {

    test("build LArray") {

      val b = LArray.newBuilder[Int]

      def elem(i: Long) = math.toDegrees(math.sin(i / 15.0)).toInt

      for (i <- 0L until 100L)
        b += elem(i)

      val l = b.result
      debug(l.mkString(", "))
      l.size shouldBe (100)
      l.zipWithIndex.forall { case (v, i) => v == elem(i) } shouldBe (true)
    }

    test("build large LArray") {

      val b = LArray.newBuilder[Byte]
      val N = 3L * 1024 * 1024
      debug("Creating large array")
      val r = new Random(0)
      var i = 0L
      while (i < N) {
        b += r.nextInt(255).toByte
        i += 1
      }
      val l = b.result
      l.size shouldBe (N)

      debug("Checking the elements")
      val r2 = new Random(0)
      l.forall(v => v == r2.nextInt(255).toByte) shouldBe (true)
    }

  }
}
