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

import java.io.{File, FileFilter}

import wvlet.log.LogSupport
import xerial.larray.mmap.MMapMode

object SharedMemoryTest {

}

trait Barrier extends LogSupport {
  this: LArraySpec =>

  val numJVMs: Int

  val jvmID = {
    val n = getClass.getSimpleName
    val p = "[0-9]+".r
    val id = p.findAllIn(n).toSeq.last.toInt
    info(s"jvm ID:$id")
    id
  }

  def enterBarrier(name: String) {
    debug(s"entering barrier:$name")
    val f = new File(s"target/${name}.barrier")
    f.deleteOnExit()
    val m = LArray.mmap(f, 0, numJVMs, MMapMode.READ_WRITE)
    m(jvmID - 1) = 1.toByte
    m.flush

    def reached(): Boolean = {
      m.forall(_ == 1.toByte)
    }

    while (!reached()) {
      Thread.sleep(10)
    }
    debug(s"exit barrier: $name")
  }

  before {
    if (jvmID == 1) {
      val lockFile = Option(new File("target").listFiles(new FileFilter {
        def accept(pathname: File) = pathname.getName.endsWith(s".barrier")
      })) getOrElse (Array.empty[File])

      while (lockFile.exists(_.exists())) {
        lockFile.filter(_.exists()) map (_.delete())
        Thread.sleep(50)
      }
    }
    else {
      Thread.sleep(1000)
    }
  }

}

trait SharedMemorySpec extends LArraySpec with Barrier {
  val numJVMs = 2

}

class SharedMemoryMultiJvm1 extends SharedMemorySpec {

  "mmap" should {

    "be able to shared between processes" in {

      info("preparing mmap")

      //val d = new File("/dev/shm")
      val f = new File(new File("target"), "sharedmemory.mmap")
      //f.deleteOnExit
      val m = LArray.mmap(f, 0, 100, MMapMode.READ_WRITE)
      for (i <- 0 Until m.size) {
        m(i) = i.toByte
      }

      m.flush
      debug(m.mkString(","))

      for (i <- 0 Until m.size) {
        if ((i % 64) == 0) {
          m(i) = 1.toByte
        }
        else {
          m(i) = 0.toByte
        }
      }

      enterBarrier("prepare")

      //m.close
      info("done.")
    }
  }
}

class SharedMemoryMultiJvm2 extends SharedMemorySpec {
  "mmap" should {
    "be able to shared between processes" in {

      enterBarrier("prepare")

      val f = new File(new File("target"), "sharedmemory.mmap")
      val m = LArray.mmap(f, 0, 100, MMapMode.READ_WRITE)

      debug(m.mkString(","))

      m(0) shouldBe (1.toByte)
      m(1) shouldBe (0.toByte)
      m(64) shouldBe (1.toByte)
      //m.close
    }
  }
}
