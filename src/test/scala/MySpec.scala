package scalamin

import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import org.scalatest.{Tag, OptionValues, GivenWhenThen, WordSpec}
import xerial.core.io.Resource
import xerial.core.log.Logger
import xerial.core.util.Timer
import scala.language.implicitConversions

//--------------------------------------
//
// MySpec.scala
// Since: 2012/11/20 12:57 PM
// 
//--------------------------------------

/**
 * Helper trait for writing test codes. Extend this trait in your test classes
 */
trait MySpec extends WordSpec with ShouldMatchers with MustMatchers with GivenWhenThen with OptionValues with Resource with Timer with Logger {

  implicit def toTag(s:String) = Tag(s)

}