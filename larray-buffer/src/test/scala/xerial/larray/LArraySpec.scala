package xerial.larray

import org.scalatest._
import xerial.core.io.Resource
import xerial.core.log.Logger
import xerial.core.util.Timer

import scala.language.implicitConversions

/**
  * @author Taro L. Saito
  */
trait LArraySpec extends WordSpec with Matchers with GivenWhenThen with OptionValues with Resource with Timer with Logger
  with BeforeAndAfterAll with BeforeAndAfter with BeforeAndAfterEach {

  implicit def toTag(t: String) = Tag(t)

}
