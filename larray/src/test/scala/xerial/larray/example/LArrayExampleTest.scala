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
// LArrayExampleTest.scala
// Since: 2013/03/26 15:04
//
//--------------------------------------

package xerial.larray.example

import xerial.larray.LArraySpec

/**
  * @author
  *   Taro L. Saito
  */
class LArrayExampleTest extends LArraySpec {

  "LArrayExample" should {
    "run Scala API" in {
      val out = captureOut(new LArrayExample)
      out should (include("done."))
    }

    "run Java API" in {
      val out = captureSystemOut(LArrayJavaExample.main(Array.empty[String]))
      out should (include("done."))
    }

  }
}
