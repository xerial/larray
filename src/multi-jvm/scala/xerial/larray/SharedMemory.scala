package xerial.larray

import java.io.File


class SharedMemoryMultiJvm1 extends LArraySpec {
  "mmap" should {

    "be able to shared between processes" in {

      info("preparing mmap")

      val f = new File(new File("target"), "sharedmemory.mmap")
      val m = LArray.mmap(f, 0, 1024, MMapMode.READ_WRITE)
      m.clear

      debug(m.mkString(","))

      for(i <- 0 Until m.size) {
        if((i % 64) == 0)
          m(i) = 1.toByte
        else
          m(i) = 0.toByte
      }


      info("done.")
    }
  }
}


class SharedMemoryMultiJvm2 extends LArraySpec {
  "mmap" should {


    "be able to shared between processes" in {

      Thread.sleep(1000)

      val f = new File(new File("target"), "sharedmemory.mmap")
      val m = LArray.mmap(f, 0, 1024, MMapMode.READ_WRITE)

      debug(m.mkString(","))
    }
  }
}
