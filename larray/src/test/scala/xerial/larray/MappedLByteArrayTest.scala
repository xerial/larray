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
// MappedLByteArrayTest.scala
// Since: 2013/04/04 1:58 PM
//
//--------------------------------------

package xerial.larray

import java.io.File

/**
  * @author Taro L. Saito
  */
class MappedLByteArrayTest extends LArraySpec {

  "MappedLByteArray" should {

    "create memory mapped file" taggedAs ("simple") in {

      import xerial.larray._

      val f = File.createTempFile("mmap", ".larray", new File("target"))
      f.deleteOnExit()

      val L = 100L
      val m = new MappedLByteArray(f, 0, L)
      m.size shouldBe L
      for (i <- 0 Until m.size) {
        m(i) = i.toByte
      }

      trace(m.mkString(", "))
      val mc = m.slice(0)
      m.close()

      val m2 = LArray.loadFrom[Byte](f)
      mc.sameElements(m2) should be(true)

      val mOffset = new MappedLByteArray(f, 3, L - 3)
      trace(mOffset.mkString(", "))

      mc.slice(3).sameElements(mOffset) should be(true)

      //mOffset.flush
      //m.free
      //m2.free
    }

    "create large memory mapped file more than 2GB" taggedAs ("large") in {

      val f = File.createTempFile("mmap", ".larray", new File("target"))
      f.deleteOnExit()

      val G = 1024L * 1024 * 1024
      val m = new MappedLByteArray(f, 0, 2L * G + 1024)
      val offset = 100

      val v = 34.toByte
      m(2L * G + offset) = v
      m.close()

      f.length() shouldBe m.size

      val view = new MappedLByteArray(f, 2L * G, 1024)
      view(offset) shouldBe v
      view.close()

      f.delete()
    }

  }
}