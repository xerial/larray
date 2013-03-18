//--------------------------------------
//
// LArrayTest.scala
// Since: 2013/03/13 13:46
//
//--------------------------------------

package xerial.larray
import util.Random

/**
 * @author Taro L. Saito
 */
class LArrayTest extends LArraySpec {

  val G: Long = 1024L * 1024 * 1024

  override def afterEach {
//    MemoryAllocator.default.releaseAll
//    System.gc()
  }

  "LArray" should {
    "have constructor" in {

      val l = LArray(1, 2, 3)
      val l0 = LArray()
      val l1 = LArray(1)

      try {
        l.size should be(3)
        l(0) should be(1)
        l(1) should be(2)
        l(2) should be(3)

        l0.size should be(0)
        try {
          l0.apply(3)
          fail("cannot reach here")
        }
        catch {
          case e: Exception => // "OK"
        }
      }
      finally {
        l.free
        l0.free
        l1.free
      }
    }

    "have map/flatMap" in {
      val l = LArray(1, 3, 5)
      def mul(v:Int) = v * 2

      val m = l.map(mul).toArray
      m.size should be (3)
      for(i <- 0L until l.size)
        m(i.toInt) should be (mul(l(i)))
    }

    "read/write values correctly" in {
      info("read/write test")

      val step = 1L
      val l = new LIntArray((0.1 * G).toLong)
      try {
        def v(i: Long) = (i * 2).toInt
        for(i <- 0L until(l.size, step)) l(i) = v(i)
        def loop(i: Long): Boolean = {
          if (i >= l.size)
            true
          else
            l(i) == v(i) && loop(i + step)
        }

        loop(0) should be(true)
      }
      finally {
        l.free
      }
    }

    "read/write data to Array[Byte]" taggedAs("rw") in {
      val l = LArray(1, 3)
      val b = new Array[Byte](l.byteLength.toInt)

      debug(s"LArray: [${l.mkString(", ")}]")
      debug(s"Array[Byte]: [${b.mkString(", ")}]")
      l.write(0, b, 0, l.byteLength.toInt)

      debug(s"Array[Byte]: [${b.mkString(", ")}]")
      val l2 = LArray(0, 0)
      l2.read(b, 0, 0, b.length)

      debug(s"LArray2: [${l2.mkString(", ")}]")

      l.sameElements(l2) should be (true)
    }




    "compare its random access performance with native Scala array and its wrapper" in {
      //val N = 1 * 1024 * 1024 * 1024
      val N = 1 * 1024 * 1024
      info("benchmark has started..")
      val arr1 = new Array[Int](N)
      val arr2 = new LIntArray(N)
      val arr3 = new LIntArraySimple(N)
      val arr4 = new MatrixBasedLIntArray(N)

      try {
        val r = new Random(0)
        val indexes = {
          val M = N / 10
          val a = new Array[Int](M)
          var i = 0
          while (i < M) {
            a(i) = r.nextInt(N);
            i += 1
          }
          a
        }
        val R = 5
        time("random access performance", repeat = 2) {
          block("scala array", repeat=R) {
            for (i <- indexes)
              arr1(i) = 1
          }

          block("LIntArray", repeat=R) {
            for (i <- indexes)
              arr2(i) = 1

          }

          block("LIntArraySimple", repeat=R) {
            for (i <- indexes)
              arr3(i) = 1
          }

          block("MatrixBasedLIntArray", repeat=R) {
            for (i <- indexes)
              arr4(i) = 1
          }
        }
      }
      finally {
        arr2.free
        arr3.free
      }
    }

    "compare sequential access performance" in {

      //val N = 1 * 1024 * 1024 * 1024
      val N = 64 * 1024 * 1024
      info("benchmark has started..")
      val arr1 = new Array[Int](N)
      val arr2 = new LIntArray(N)
      val arr3 = new LIntArraySimple(N)
      val arr4 = new MatrixBasedLIntArray(N)

      try {
        val range = (0 until (N / 10)).map(_.toLong).toSeq
        time("sequential read performance", repeat = 5) {
          block("scala array") {
            for (i <- range)
              arr1(i.toInt)
          }

          block("LIntArray") {
            for (i <- range)
              arr2(i)

          }

          block("LIntArraySimple") {
            for (i <- range)
              arr3(i)
          }

          block("MatrixBasedLIntArray") {
            for (i <- range)
              arr4(i)
          }

        }
      }
      finally {
        arr2.free
        arr3.free
      }
    }


    "create large array" taggedAs ("la") in {
      for (i <- 0 until 10) {
        val arr = new LIntArray((2.1 * G).toLong)
        try {
          arr(arr.size - 1) = 134
        }
        finally {
          arr.free
        }
      }
    }

  }



  "LByteArray" should {

    "have constructor" in {
      val a = Array[Byte](1, 2, 3)
      val b = LArray[Byte](1, 5, 34)

      b(0) should be (1.toByte)
      b(1) should be (5.toByte)
      b(2) should be (34.toByte)
    }

    "compare performance" taggedAs("bp") in {

      val N = (0.01*G).toLong
      val a = new Array[Byte](N.toInt)
      val b = new LByteArray(N)
      info("LByteArray performance test has started")
      time("LByteArray random access & sort", repeat=5) {
        block("native array")  {
          val r= new Random(0)
          for(i <- 0L until N) {
            val index = (i / 4) * 4
            a((index + (i % 4L)).toInt) = r.nextInt.toByte
          }
          java.util.Arrays.sort(a)
        }

        block("LByteArray")  {
          val r= new Random(0)
          for(i <- 0L until N) {
            val index = (i / 4) * 4
            b((index + (i % 4L))) = r.nextInt.toByte
          }
          b.sort
        }

      }

      info("sequential access test")
      time("LByteArray sequential write", repeat=5) {
        block("native array")  {
          val r= new Random(0)
          for(i <- 0L until N) {
            a(i.toInt) = r.nextInt.toByte
          }
        }

        block("LByteArray")  {
          val r= new Random(0)
          for(i <- 0L until N) {
            b(i) = r.nextInt.toByte
          }
        }
      }

      b.free
    }


  }
}