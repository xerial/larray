//--------------------------------------
//
// LByteBuffer.scala
// Since: 2013/03/26 14:36
//
//--------------------------------------

package xerial.larray

import java.nio.{InvalidMarkException, Buffer}

/**
 * ByteBuffer interface of [[xerial.larray.LArray]]
 * @author Taro L. Saito
 */
class LByteBuffer(buf:LByteArray, private var cursor:Long, private var bufLimit:Long) {
  private var _mark : Long = -1L

  def this(buf:LByteArray) = this(buf, 0L, buf.byteLength)

  /**
   * Return the LArray representation of this buffer
   */
  def array : LByteArray = buf

  def position : Long = cursor
  def position(newPos:Long) : this.type = { cursor = newPos; this }
  def limit : Long = buf.byteLength
  def limit(newLimit:Long) : this.type = { bufLimit = newLimit; this }

  def flip : this.type = {
    bufLimit = cursor
    cursor = 0L
    _mark = -1L
    this
  }

  def rewind : this.type = {
    cursor = 0L
    _mark = -1L
    this
  }

  def remaining : Long = bufLimit - cursor
  def hasRemaining : Boolean = cursor < bufLimit

  def clear() : this.type = {
    cursor = 0L
    bufLimit = buf.length
    _mark = -1L
    this
  }

  def mark : this.type = {
    _mark = cursor
    this
  }

  def reset : this.type = {
    val m = _mark
    if (m < 0L) throw new InvalidMarkException
    cursor = m
    this
  }

  def put(b:Array[Byte]) : this.type = put(b, 0, b.length)
  def put(b:Array[Byte], offset:Int, len:Int) : this.type = {
    buf.readFromArray(b, offset, cursor, len)
    cursor += len
    this
  }
  def put[A](b:LArray[A], offset:Long, len:Long) : this.type = {
    b.copyTo(offset, buf, cursor, len)
    cursor += len
    this
  }

  def putBoolean(v:Boolean) : this.type = { buf.putByte(cursor, if(v) 1 else 0); cursor += 1; this }
  def putByte(v:Byte) : this.type = { buf.putByte(cursor, v); cursor += 1; this }
  def putChar(v:Char) : this.type = { buf.putChar(cursor, v); cursor += 2; this }
  def putShort(v:Short) : this.type = { buf.putShort(cursor, v); cursor += 2; this }
  def putInt(v:Int) : this.type = { buf.putInt(cursor, v); cursor += 4; this }
  def putFloat(v:Float) : this.type = { buf.putFloat(cursor, v); cursor += 4; this }
  def putLong(v:Long) : this.type = { buf.putLong(cursor, v); cursor += 8; this }
  def putDouble(v:Double) : this.type = { buf.putDouble(cursor, v); cursor += 8; this }

  def get(b:Array[Byte]) : this.type = get(b, 0, b.length)
  def get(b:Array[Byte], offset:Int, len:Int) : this.type = {
    buf.writeToArray(cursor, b, offset, len)
    this
  }
  def get[A](b:RawByteArray[A], offset:Long, len:Long) : this.type = {
    buf.copyTo(cursor, b, offset, len)
    cursor += len
    this
  }
  def getBoolean: Boolean = { val v = if(buf.getByte(cursor) == 0) false else true; cursor += 1; v}
  def getByte : Byte = { val v = buf.getByte(cursor); cursor += 1; v }
  def getChar : Char = { val v = buf.getChar(cursor); cursor += 2; v }
  def getShort : Short = { val v = buf.getShort(cursor); cursor += 2; v }
  def getInt : Int = { val v = buf.getInt(cursor); cursor += 4; v }
  def getFloat : Float = { val v = buf.getFloat(cursor); cursor += 4; v }
  def getLong : Long = { val v = buf.getLong(cursor); cursor += 8; v }
  def getDouble : Double = { val v = buf.getDouble(cursor); cursor += 8; v }


}