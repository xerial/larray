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
// LArrayTest.scala
// Since: 2013/03/13 13:46
//
//--------------------------------------

package xerial.larray

import wvlet.airspec.AirSpec
import wvlet.log.io.Timer

import scala.util.Random
import java.io.{File, FileInputStream, FileOutputStream}

/**
  * @author
  *   Taro L. Saito
  */
class LArrayTest extends AirSpec with Timer {

  val G: Long = 1024L * 1024L * 1024L

  test("LArray") {
    test("have constructor") {

      val l  = LArray(1, 2, 3)
      val l0 = LArray()
      val l1 = LArray(1)

      try {
        l.size shouldBe (3)
        l(0) shouldBe (1)
        l(1) shouldBe (2)
        l(2) shouldBe (3)

        l0.size shouldBe (0)
        try {
          l0.apply(3)
          fail("cannot reach here")
        } catch {
          case e: Exception => // "OK"
        }
      } finally {
        l.free
        l0.free
        l1.free
      }
    }

    test("have map/flatMap") {
      val l           = LArray(1, 3, 5)
      def mul(v: Int) = v * 2

      val m = l.map(mul).toArray
      m.size shouldBe (3)
      for (i <- 0L until l.size)
        m(i.toInt) shouldBe (mul(l(i)))
    }

    test("read/write values correctly") {
      info("read/write test")

      val step = 1L
      val l    = new LIntArray((0.1 * G).toLong)
      try {
        def v(i: Long) = (i * 2).toInt
        for (i <- 0L until (l.size, step)) l(i) = v(i)
        def loop(i: Long): Boolean = {
          if (i >= l.size)
            true
          else
            l(i) == v(i) && loop(i + step)
        }

        loop(0) shouldBe (true)
      } finally {
        l.free
      }
    }

    test("read/write data to Array[Byte]") {
      val l = LArray(1, 3)
      val b = new Array[Byte](l.byteLength.toInt)

      l match {
        case l: RawByteArray[_] =>
          debug(s"LArray: [${l.mkString(", ")}]")
          debug(s"Array[Byte]: [${b.mkString(", ")}]")
          l.writeToArray(0, b, 0, l.byteLength.toInt)
        case _ => fail("cannot reach here")
      }

      debug(s"Array[Byte]: [${b.mkString(", ")}]")
      val l2 = LArray(0, 0)
      l2 match {
        case l2: RawByteArray[_] =>
          l2.readFromArray(b, 0, 0, b.length)
          debug(s"LArray2: [${l2.mkString(", ")}]")
          l.sameElements(l2) shouldBe (true)
        case _ => fail("cannot reach here")
      }

    }

    test("read/write data through ByteBuffer") {
      val l = LArray(1, 3, 5, 134, 34, -3, 2)
      debug(l.mkString(", "))
      val bb = l.toDirectByteBuffer

      val tmp = File.createTempFile("larray-dump", ".dat", new File("target"))
      debug(s"Write LArray to file: $tmp")
      tmp.deleteOnExit()
      val out = new FileOutputStream(tmp).getChannel
      out.write(bb)
      out.close()

      // read LArray from file
      {
        val in       = new FileInputStream(tmp).getChannel
        val fileSize = in.size()
        val newArray = new LByteArray(fileSize)

        var pos = 0L
        while (pos < fileSize) {
          pos += in.transferTo(pos, fileSize - pos, newArray)
        }
        val l2 = LArray.wrap[Int](newArray.byteLength, newArray.m)
        debug(l2.mkString(", "))

        l.sameElements(l2) shouldBe (true)

        in.close
      }

      // read LArray from File using LArrayBuilder
      {
        val ib       = LArray.newBuilder[Int]
        val in       = new FileInputStream(tmp).getChannel
        val fileSize = in.size()
        var pos      = 0L
        while (pos < fileSize) {
          pos += in.transferTo(pos, fileSize - pos, ib)
        }
        val l2 = ib.result()
        debug(l2.mkString(", "))
        l.sameElements(l2) shouldBe (true)

        in.close
      }

    }

    test("provide initializer") {
      {
        val l = new LIntArray(1000)
        l(40) = 34
        l.clear()
        l.forall(_ == 0) shouldBe (true)
      }

      {
        val l = new LByteArray(1000)
        l(340) = 34.toByte
        l.clear()
        l.forall(_ == 0) shouldBe (true)
      }
    }

    test("compare its random access performance with native Scala array and its wrapper") {
      // val N = 1 * 1024 * 1024 * 1024
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
            a(i) = r.nextInt(N)
            i += 1
          }
          a
        }
        val R = 5
        time("random access performance", repeat = 2, blockRepeat = R) {
          block("scala array") {
            for (i <- indexes)
              arr1(i) = 1
          }

          block("LIntArray") {
            for (i <- indexes)
              arr2(i) = 1

          }

          block("LIntArraySimple") {
            for (i <- indexes)
              arr3(i) = 1
          }

          block("MatrixBasedLIntArray") {
            for (i <- indexes)
              arr4(i) = 1
          }
        }
      } finally {
        arr2.free
        arr3.free
      }
    }

    test("compare sequential access performance") {

      // val N = 1 * 1024 * 1024 * 1024
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
      } finally {
        arr2.free
        arr3.free
      }
    }

    test("create large array") {
      info("large memory allocation test")
      for (i <- 0 until 10) {
        val arr = new LByteArray(2L * G)
        try {
          arr(arr.size - 1) = 134.toByte
        } finally {
          arr.free
        }
      }
    }

    test("create view") {
      val l = LArray(1, 13, 4, 5)
      val v = l.view(1, 3)
      v.mkString(", ") shouldBe "13, 4"
      val b = v.toDirectByteBuffer(0)
      b.getInt() shouldBe 13
      b.getInt() shouldBe 4
    }

  }

  test("LByteArray") {

    test("have constructor") {
      val a = Array[Byte](1, 2, 3)

      val b = LArray[Byte](1, 5, 34)

      b(0) shouldBe (1.toByte)
      b(1) shouldBe (5.toByte)
      b(2) shouldBe (34.toByte)
    }

    test("compare performance") {

      val N = (0.01 * G).toLong
      val a = new Array[Byte](N.toInt)
      val b = new LByteArray(N)
      info("LByteArray performance test has started")
      time("LByteArray random access & sort", repeat = 5) {
        block("native array") {
          val r = new Random(0)
          for (i <- 0L until N) {
            val index = (i / 4) * 4
            a((index + (i % 4L)).toInt) = r.nextInt.toByte
          }
          java.util.Arrays.sort(a)
        }

        block("LByteArray") {
          val r = new Random(0)
          for (i <- 0L until N) {
            val index = (i / 4) * 4
            b((index + (i % 4L))) = r.nextInt.toByte
          }
          b.sort
        }

      }

      info("sequential access test")
      time("LByteArray sequential write", repeat = 5) {
        block("native array") {
          val r = new Random(0)
          for (i <- 0L until N) {
            a(i.toInt) = r.nextInt.toByte
          }
        }

        block("LByteArray") {
          val r = new Random(0)
          for (i <- 0L until N) {
            b(i) = r.nextInt.toByte
          }
        }
      }

      b.free
    }

  }
}
