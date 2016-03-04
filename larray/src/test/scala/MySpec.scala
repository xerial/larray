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
package scalamin

import org.scalatest._
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
trait MySpec extends WordSpec with ShouldMatchers with BeforeAndAfter with BeforeAndAfterAll with Resource with Timer with Logger {

  implicit def toTag(s:String) = Tag(s)

}