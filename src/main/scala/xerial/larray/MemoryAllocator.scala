//--------------------------------------
//
// MemoryAllocator.scala
// Since: 2013/03/14 9:26
//
//--------------------------------------

package xerial.larray

import sun.misc.Unsafe
import xerial.core.log.Logger

object MemoryAllocator {

  /**
   * Provides default memory allocator
   */
  implicit val default : MemoryAllocator = new UnsafeAllocator

}


/**
 * Memory allocator interface
 *
 * @author Taro L. Saito
 */
trait MemoryAllocator extends Logger {

  private val allocatedMemoryAddr = collection.mutable.Set[Long]()

  // Register a shutdown hook to deallocate memory regions
  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    def run() {
      releaseAll
    }
  }))

  private var hasDisplayedMemoryWarning = false

  /**
   * Release all memory addresses taken by this allocator.
   * Be careful in using this method, since all the memory addresses in LArray will be invalid.
   */
  def releaseAll {
    synchronized {
      val addrSet = Set() ++ allocatedMemoryAddr // take a copy of the set
      if(!addrSet.isEmpty)
        trace("Releasing allocated memory regions")
      for(addr <- addrSet) {
        warn(f"Found unreleased address:$addr%x")
        if(!hasDisplayedMemoryWarning) {
          warn("Probably LArray.free is not called properly. You can check when this memory is allocated by setting -Dloglevel=trace in JVM option")
          hasDisplayedMemoryWarning = true
        }
        release(addr)
      }
    }
  }

  protected def allocateInternal(size:Long) : Long
  protected def releaseInternal(addr:Long) : Unit


  /**
   * Allocate a memory of the specified byte length. The allocated memory must be released via [[xerial.larray.MemoryAllocator#release]]
   * as in malloc() in C/C++.
   * @param size byte length of the memory
   * @return adress of the allocated mmoery.
   */
  def allocate(size:Long) : Long = {
    synchronized {
      val addr = allocateInternal(size)
      trace(f"allocated memory:$addr%x")
      allocatedMemoryAddr += addr
      addr
    }
  }

  /**
   * Release the memory allocated by [[xerial.larray.MemoryAllocator#allocate]]
   * @param addr the address returned by  [[xerial.larray.MemoryAllocator#allocate]]
   */
  def release(addr:Long) : Unit = {
    synchronized {
      if(addr != 0 && allocatedMemoryAddr.contains(addr)) {
        trace(f"release memory:$addr%x")
        releaseInternal(addr)
        allocatedMemoryAddr -= addr
      }
    }
  }
}

object UnsafeUtil extends Logger {
  val unsafe = {
    val f = classOf[Unsafe].getDeclaredField("theUnsafe")
    f.setAccessible(true)
    f.get(null).asInstanceOf[Unsafe]
  }

  val byteArrayOffset = unsafe.arrayBaseOffset(classOf[Array[Byte]]).toLong
  val objectArrayOffset = unsafe.arrayBaseOffset(classOf[Array[AnyRef]]).toLong
  val objectArrayScale = unsafe.arrayIndexScale(classOf[Array[AnyRef]]).toLong
  val addressBandWidth = System.getProperty("sun.arch.data.model", "64").toInt
  private val addressFactor = if(addressBandWidth == 64) 8L else 1L
  val addressSize = unsafe.addressSize()

  /**
   * @param obj
   * @return
   *
   */
  @deprecated(message="Deprecated because this method does not return correct object addresses in some platform", since="0.1")
  def getObjectAddr(obj:AnyRef) : Long = {
    trace(f"address factor:$addressFactor%d, addressSize:$addressSize, objectArrayOffset:$objectArrayOffset, objectArrayScale:$objectArrayScale")

    val o = new Array[AnyRef](1)
    o(0) = obj
    objectArrayScale match {
      case 4 => (unsafe.getInt(o, objectArrayOffset) & 0xFFFFFFFFL) * addressFactor
      case 8 => (unsafe.getLong(o, objectArrayOffset) & 0xFFFFFFFFFFFFFFFFL) * addressFactor
    }
  }
}


/**
 * Allocate memory using [[sun.misc.Unsafe]]. OpenJDK (and probably Oracle JDK) implements allocateMemory and freeMemory functions using malloc() and free() in C.
 */
class UnsafeAllocator extends MemoryAllocator with Logger {

  import UnsafeUtil.unsafe

  protected def allocateInternal(size: Long): Long = unsafe.allocateMemory(size)
  protected def releaseInternal(addr: Long) = unsafe.freeMemory(addr)
}

///**
// * Allocate memory using Numa API
// */
//class NumaAllocator extends MemoryAllocator {
//
//  import xerial.jnuma.Numa
//
//  private val metaInfoSpace = 8L
//
//  def allocateInternal(size: Long): Long = {
//    val s = metaInfoSpace + size
//    val addr = Numa.allocMemory(s)
//    // Write the buffer length as meta info
//    UnsafeUtil.unsafe.putLong(addr, s)
//    // Return the address after the meta info
//    addr + metaInfoSpace
//  }
//
//  def releaseInternal(addr: Long) {
//    val start = addr - metaInfoSpace
//    val size = UnsafeUtil.unsafe.getLong(start)
//    Numa.free(start,size)
//  }
//}