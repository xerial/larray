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
package xerial.larray

import org.scalactic.anyvals.PosInt
import org.scalatest.Matchers
import org.scalatest.prop.PropertyChecks

/**
  * Created with IntelliJ IDEA.
  * User: hayato
  * Date: 13/03/27
  * Time: 15:06
  */
class LArrayTestWithPBT extends LArraySpec with Matchers with LArrayBehaviour with PropertyChecks {
  val maxNumberOfTests = PosInt(3)
  val maxSizeOfList = PosInt(10000)
  val minSizeOfList = PosInt(1)

  forAll("array", minSuccessful(maxNumberOfTests), maxSize(maxSizeOfList), minSize(minSizeOfList)) {
    (input: Array[Int]) =>
      "int test with length " + input.take(10).toString should {
        behave like validArray(input)
        behave like validIntArray(input)
      }
  }

  forAll("array", minSuccessful(maxNumberOfTests), maxSize(maxSizeOfList), minSize(minSizeOfList)) {
    (input: Array[Long]) =>
      "long test with length " + input.take(10).toString should {
        behave like validArray(input)
        behave like validLongArray(input)
      }
  }

  forAll("array", minSuccessful(maxNumberOfTests), maxSize(maxSizeOfList), minSize(minSizeOfList)) {
    (input: Array[Float]) =>
      "float test with length " + input.take(10).toString should {
        behave like validArray(input)
        behave like validFloatArray(input)
      }

  }

  forAll("array", minSuccessful(maxNumberOfTests), maxSize(maxSizeOfList), minSize(minSizeOfList)) {
    (input: Array[Double]) =>
      "double test with length " + input.take(10).toString should {
        behave like validArray(input)
        behave like validDoubleArray(input)
      }
  }

  "empty test" should {
    val input = Seq.empty[Int]
    behave like validArray(input)
    behave like validIntArray(input)
  }
}
