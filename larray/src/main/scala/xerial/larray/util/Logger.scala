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
// Logger.scala
// Since: 2013/03/22 2:32
//
//--------------------------------------

package xerial.larray.util

/**
  * Logger wrapper for using `wvlet.log.Logger` in Java
  *
  * @author
  *   Taro L. Saito
  */
class Logger(cl: Class[_]) {
  private val _logger = wvlet.log.Logger.apply(cl.getName)

  def trace(m: String) { _logger.trace(m) }
  def debug(m: String) { _logger.debug(m) }
  def info(m: String) { _logger.info(m) }
  def warn(m: String) { _logger.warn(m) }
  def error(m: String) { _logger.error(m) }
  def fatal(m: String) { _logger.error(m) }

}
