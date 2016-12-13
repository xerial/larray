package xerial.larray

import org.scalatest._
import xerial.core.io.Resource
import xerial.core.util.Timer
import xerial.core.log.Logger
import scala.language.implicitConversions

/**
 * @author Taro L. Saito
 */
trait LArraySpec extends WordSpec with Matchers with Resource with Timer with Logger
with BeforeAndAfterAll with BeforeAndAfter with BeforeAndAfterEach with GivenWhenThen {

  implicit def toTag(t:String) = Tag(t)

}
