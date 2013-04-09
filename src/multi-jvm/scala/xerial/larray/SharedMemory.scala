package xerial.larray

import java.io.File
import akka.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks, MultiNodeConfig}
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.matchers.MustMatchers
import akka.testkit.ImplicitSender

object SharedMemoryTest {

}

trait STMultiNodeSpec extends MultiNodeSpecCallbacks with LArraySpec {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
}

object SharedMemorySpecConfig extends MultiNodeConfig {
  val node1 = role("node1")
  val node2 = role("node2")
}

class SharedMemorySpecMultiJvmNode1 extends SharedMemorySpec
class SharedMemorySpecMultiJvmNode2 extends SharedMemorySpec

class SharedMemorySpec extends MultiNodeSpec(SharedMemorySpecConfig)
 with STMultiNodeSpec with ImplicitSender {

  import SharedMemorySpecConfig._
  def initialParticipants = roles.size

  "wait for all nodes to enter a barrier" in {
    enterBarrier("startup")
  }

  "share memories between processes" in {
    runOn(node1) {
      info("preparing mmap")

      //val d = new File("/dev/shm")
      val f = new File(new File("target"), "sharedmemory.mmap")
      f.getParentFile.mkdirs()
      //f.deleteOnExit
      val m = LArray.mmap(f, 0, 100, MMapMode.READ_WRITE)
      for(i <- 0 Until m.size)
        m(i) = i.toByte

      m.flush

      for(i <- 0 Until m.size) {
        if((i % 64) == 0)
          m(i) = 1.toByte
        else
          m(i) = 0.toByte
      }

      enterBarrier("ready")
    }

    runOn(node2) {

      enterBarrier("ready")
      val f = new File(new File("target"), "sharedmemory.mmap")
      f.getParentFile.mkdirs()
      val m = LArray.mmap(f, 0, 100, MMapMode.READ_WRITE)

      debug(m.mkString(","))

      m(0) shouldBe (1.toByte)
      m(1) shouldBe (0.toByte)
      m(64) shouldBe (1.toByte)
    }

    enterBarrier("finished")
  }
}

