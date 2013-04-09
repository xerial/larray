package xerial.larray

import java.io.File


object SharedMemoryTest {

}

class SharedMemoryMultiJvm1 extends LArraySpec {
  "mmap" should {

    "be able to shared between processes" in {

      info("preparing mmap")

      val d = new File("/dev/shm")
      val f = new File(if(d.exists) d else new File("target"), "sharedmemory.mmap")
      //f.deleteOnExit
      val m = LArray.mmap(f, 0, 1024 * 1024, MMapMode.READ_WRITE)
      for(i <- 0 Until m.size)
        m(i) = i.toByte

      m.flush

      //debug(m.mkString(","))

      for(i <- 0 Until m.size) {
        if((i % 64) == 0)
          m(i) = 1.toByte
        else
          m(i) = 0.toByte
      }

      //m.close
      info("done.")
    }
  }
}


class SharedMemoryMultiJvm2 extends LArraySpec {
  "mmap" should {


    "be able to shared between processes" in {

      Thread.sleep(1000)

      val f = new File(new File("target"), "sharedmemory.mmap")
      val m = LArray.mmap(f, 0, 1024 * 1024, MMapMode.READ_WRITE)

      //debug(m.mkString(","))

      m(0) shouldBe (1.toByte)
      m(1) shouldBe (0.toByte)
      m(64) shouldBe (1.toByte)
      //m.close
    }
  }
}
