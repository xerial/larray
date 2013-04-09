package xerial.larray

import java.io.{FileFilter, File}
import xerial.core.log.Logger


object SharedMemoryTest {



}

trait Barrier extends Logger {

  val numJVMs : Int

  val jvmID = {
    val n = getClass.getSimpleName
    val p = "[0-9]+".r
    val id = p.findAllIn(n).toSeq.last.toInt
    info(s"jvm ID:$id")
    id
  }

  def enterBarrier(name:String) {
    debug(s"entering barrier:$name")
    val f = new File(s"target/${name}.barrier")
    f.deleteOnExit()
    val m = LArray.mmap(f, 0, numJVMs, MMapMode.READ_WRITE)
    m(jvmID -1) = 1.toByte
    m.flush

    def reached() : Boolean = {
      m.forall(_ == 1.toByte)
    }

    while(!reached()) {
      Thread.sleep(10)
    }
    debug(s"exit barrier: $name")
  }

}

trait SharedMemorySpec extends LArraySpec with Barrier {
  val numJVMs = 2

  "cleanup barriers" in {
    val barrierFiles = new File("target").listFiles(new FileFilter {
      def accept(pathname: File) = {
        pathname.getName.endsWith(".barrier")
      }
    })
    for(f <- barrierFiles)
      f.delete()
  }

}


class SharedMemoryMultiJvm1 extends SharedMemorySpec {

  "mmap" should {

    "be able to shared between processes" in {

      info("preparing mmap")

      //val d = new File("/dev/shm")
      val f = new File(new File("target"), "sharedmemory.mmap")
      //f.deleteOnExit
      val m = LArray.mmap(f, 0, 100, MMapMode.READ_WRITE)
      for(i <- 0 Until m.size)
        m(i) = i.toByte

      m.flush
      debug(m.mkString(","))

      for(i <- 0 Until m.size) {
        if((i % 64) == 0)
          m(i) = 1.toByte
        else
          m(i) = 0.toByte
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
