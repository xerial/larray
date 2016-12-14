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
// GenomeWeaverSpecverSpec.scala
// Since: 2013/03/01 10:55
//
//--------------------------------------

package xerial.larray

import org.scalatest._
import java.io.{ByteArrayOutputStream, PrintStream}

import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogSupport, Logger}
import wvlet.log.io.ResourceReader
import wvlet.log.io.Timer

import scala.language.implicitConversions


/**
 * @author leo
 */
trait LArraySpec extends WordSpec with Matchers with ResourceReader with Timer with LogSupport with BeforeAndAfterAll with BeforeAndAfter with GivenWhenThen with BeforeAndAfterEach {

  implicit def toTag(t:String) = Tag(t)

  /**
   * Captures the output stream and returns the printed messages as a String
   * @param body
   * @tparam U
   * @return
   */
  def captureOut[U](body: => U) : String = {
    val out = new ByteArrayOutputStream
    Console.withOut(out) {
      body
    }
    new String(out.toByteArray)
  }

  /**
   * Captures the output stream and returns the printed messages as a String
   * @param body
   * @tparam U
   * @return
   */
  def captureSystemOut[U](body: => U) : String = {
    val prev = System.out
    val b = new ByteArrayOutputStream
    val out = new PrintStream(b)
    try {
      System.setOut(out)
      body
      out.flush()
    }
    finally
      System.setOut(prev)
    new String(b.toByteArray)
  }

  def captureErr[U](body: => U) : String = {
    val out = new ByteArrayOutputStream
    Console.withErr(out) {
      body
    }
    new String(out.toByteArray)
  }

  Logger.setDefaultFormatter(SourceCodeLogFormatter)

  override protected def beforeAll(): Unit = {
    // Run LogLevel scanner (log-test.properties or log.properties in classpath) every 1 minute
    Logger.scheduleLogLevelScan
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    Logger.stopScheduledLogLevelScan
    super.afterAll()
  }

}
