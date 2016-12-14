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
// LArrayInputStreamTest.scala
// Since: 2013/03/21 23:38
//
//--------------------------------------

package xerial.larray

import wvlet.log.io.IOUtil

class LArrayInputStreamTest extends LArraySpec {

  "LArrayInputStream" should {
    "be created from LArray[A]" in {
      val l = LArray(1, 3, 4, 5)
      debug(s"input ${l.mkString(", ")}")
      val in = LArrayInputStream(l)
      IOUtil.readFully(in) {buf =>
        debug(s"buf length: ${buf.length}")
        val out = new LArrayOutputStream[Int]
        out.write(buf)
        val r = out.result
        debug(s"output ${r.mkString(", ")}")
        l.sameElements(r) should be(true)
      }
    }
  }
}