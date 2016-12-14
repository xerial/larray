package xerial.larray

import org.scalatest._
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogSupport, Logger}
import wvlet.log.io.{ResourceReader, StopWatch, Timer}

import scala.language.implicitConversions

/**
 * @author Taro L. Saito
 */
trait LArraySpec
  extends WordSpec
    with Matchers
    with ResourceReader
    with Timer
    with LogSupport
    with BeforeAndAfterAll
    with BeforeAndAfter
    with BeforeAndAfterEach
    with GivenWhenThen {

  implicit def toTag(t:String) = Tag(t)

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
