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
// IOPerfTest.scala
// Since: 2013/03/26 23:27
//
//--------------------------------------

package xerial.larray

import java.io._

import wvlet.log.io.IOUtil

import scala.util.Random

/**
  * @author Taro L. Saito
  */
class IOPerfTest extends LArraySpec {

  def createSampleFile: File = {
    val file = File.createTempFile("sample", ".larray", new File("target"))
    file.deleteOnExit()
    val b = new Array[Byte](1024 * 1024)
    //val P = 1024 * 1024
    val P = 64
    val f = new FileOutputStream(file)
    for (i <- 0 until P) {
      Random.nextBytes(b)
      f.write(b)
    }
    f.close
    file
  }

  "LArray" should {
    "compare I/O performance" in {
      val f1 = createSampleFile
      time("read", repeat = 10) {
        block("LArray.loadFrom") {
          trace("Loading to LArray")
          val l = LArray.loadFrom[Byte](f1)
          l.free
        }
        block("FileOutputStream") {
          trace("Loading to Array")
          IOUtil.readFully(new BufferedInputStream(new FileInputStream(f1))) {buf =>
            // do nothing
          }
        }
      }
    }
  }
}