//--------------------------------------
//
// Logger.scala
// Since: 2013/03/22 2:32
//
//--------------------------------------

package xerial.larray.util

/**
 * Logger wrapper for using `xerial.core.log.Logger` in Java
 * @author Taro L. Saito
 */
class Logger(cl:Class[_]) {
  private val _logger = xerial.core.log.LoggerFactory(cl)

  def trace(m:String) { _logger.trace(m) }
  def debug(m:String) { _logger.debug(m) }
  def info(m:String) { _logger.info(m) }
  def warn(m:String) { _logger.warn(m) }
  def error(m:String) { _logger.error(m) }
  def fatal(m:String) { _logger.fatal(m) }

}