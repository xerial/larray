//--------------------------------------
//
// MemoryAllocator.scala
// Since: 2013/03/14 9:26
//
//--------------------------------------

package xerial.larray

import sun.misc.Unsafe
import xerial.core.log.Logger
import java.lang.ref.{PhantomReference, ReferenceQueue}
import collection.mutable
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong
import xerial.core.util.DataUnit


/**
 * Phantom reference to the allocated memory
 * @param m the allocated memory
 * @param queue the reference queue to which GCed reference of the Memory will be put
 * @param address the allocated memory address
 * @param size the size of the allocated memory
 */
class MemoryReference(m:Memory, queue:ReferenceQueue[Memory], val address:Long, val size:Long) extends PhantomReference[Memory](m, queue) {
  def this(m:Memory, queue:ReferenceQueue[Memory]) = this(m, queue, m.address, m.size)
}

/**
 * Accessor to the allocated memory
 * @param address
 */
case class Memory(address: Long, size: Long, alloc: MemoryAllocator) {

  import UnsafeUtil.unsafe

  @inline def getByte(offset: Long): Byte = unsafe.getByte(address + offset)
  @inline def getChar(offset: Long): Char = unsafe.getChar(address + offset)
  @inline def getShort(offset: Long): Short = unsafe.getShort(address + offset)
  @inline def getInt(offset: Long): Int = unsafe.getInt(address + offset)
  @inline def getFloat(offset: Long): Float = unsafe.getFloat(address + offset)
  @inline def getLong(offset: Long): Long = unsafe.getLong(address + offset)
  @inline def getDouble(offset: Long): Double = unsafe.getDouble(address + offset)
  @inline def putByte(offset: Long, v: Byte): Unit = unsafe.putByte(address + offset, v)
  @inline def putChar(offset: Long, v: Char): Unit = unsafe.putChar(address + offset, v)
  @inline def putShort(offset: Long, v: Short): Unit = unsafe.putShort(address + offset, v)
  @inline def putInt(offset: Long, v: Int): Unit = unsafe.putInt(address + offset, v)
  @inline def putFloat(offset: Long, v: Float): Unit = unsafe.putFloat(address + offset, v)
  @inline def putLong(offset: Long, v: Long): Unit = unsafe.putLong(address + offset, v)
  @inline def putDouble(offset: Long, v: Double): Unit = unsafe.putDouble(address + offset, v)

  def free = alloc.release(address, size, isGC=false)
}


object MemoryAllocator {

  /**
   * Provides a default memory allocator
   */
  implicit val default: MemoryAllocator = new ConcurrentMemoryAllocator

}


/**
 * Memory allocator interface
 *
 * @author Taro L. Saito
 */
trait MemoryAllocator extends Logger {


  // Register a shutdown hook to deallocate memory regions
  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    def run() {
      synchronized {
        allocatedAddresses foreach (checkAddr(_, false))
      }
    }
  }))

  private def checkAddr(ref: MemoryReference, doRelease: Boolean) {
    val addr = ref.address
    trace(f"Found unreleased memory address:${addr}%x of size ${ref.size}%,d")
    if (!hasDisplayedMemoryWarning) {
      debug("Some instances of LArray are not freed nor collected by GC. You can check when this memory is allocated by setting -Dloglevel=trace in JVM option")
      debug(f"The total amount of unreleased memory: ${DataUnit.toHumanReadableFormat(allocatedSize)}")
      hasDisplayedMemoryWarning = true
    }
    if (doRelease)
      release(ref)
  }

  private var hasDisplayedMemoryWarning = false

  /**
   * Release all memory addresses taken by this allocator.
   * Be careful in using this method, since all the memory addresses in LArray will be invalid.
   */
  def releaseAll {
    synchronized {
      val addrSet = allocatedAddresses
      if (!addrSet.isEmpty)
        trace("Releasing allocated memory regions")
      allocatedAddresses foreach (checkAddr(_, true))
    }
  }

  protected def allocatedAddresses: Seq[MemoryReference]

  /**
   * Get the total amount of allocated memory
   */
  def allocatedSize : Long

  /**
   * Allocate a memory of the specified byte length. The allocated memory must be released via `release`
   * as in malloc() in C/C++.
   * @param size byte length of the memory
   * @return adress of the allocated mmoery.
   */
  def allocate(size: Long): Memory

  /**
   * Release the memory allocated by [[xerial.larray.MemoryAllocator#allocate]]
   * @param ref the reference to the memory allocated by  [[xerial.larray.MemoryAllocator#allocate]]
   */
  def release(ref: MemoryReference, isGC:Boolean=false): Unit = release(ref.address, ref.size, isGC)

  def release(addr: Long, size: Long, isGC:Boolean): Unit
}

object UnsafeUtil extends Logger {
  val unsafe = {
    val f = classOf[Unsafe].getDeclaredField("theUnsafe")
    f.setAccessible(true)
    f.get(null).asInstanceOf[Unsafe]
  }

  private val dbbCC = Class.forName("java.nio.DirectByteBuffer").getDeclaredConstructor(classOf[Long], classOf[Int])

  def newDirectByteBuffer(addr: Long, size: Int): ByteBuffer = {
    dbbCC.setAccessible(true)
    val b = dbbCC.newInstance(new java.lang.Long(addr), new java.lang.Integer(size))
    b.asInstanceOf[ByteBuffer]
  }


  val byteArrayOffset = unsafe.arrayBaseOffset(classOf[Array[Byte]]).toLong
  val objectArrayOffset = unsafe.arrayBaseOffset(classOf[Array[AnyRef]]).toLong
  val objectArrayScale = unsafe.arrayIndexScale(classOf[Array[AnyRef]]).toLong
  val addressBandWidth = System.getProperty("sun.arch.data.model", "64").toInt
  private val addressFactor = if (addressBandWidth == 64) 8L else 1L
  val addressSize = unsafe.addressSize()

  /**
   * @param obj
   * @return
   *
   */
  @deprecated(message = "Deprecated because this method does not return correct object addresses in some platform", since = "0.1")
  def getObjectAddr(obj: AnyRef): Long = {
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
 * Allocate memory using `sun.misc.Unsafe`. OpenJDK (and probably Oracle JDK) implements allocateMemory and freeMemory functions using malloc() and free() in C.
 */
class DefaultAllocator(allocatedMemoryReferences: mutable.Map[Long, MemoryReference]) extends MemoryAllocator with Logger {

  def this() = this(collection.mutable.Map[Long, MemoryReference]())

  /**
   * When Memory is garbage-collected, the reference to the Memory is pushed into this queue.
   */
  private val queue = new ReferenceQueue[Memory]

  {
    // Receives garbage-collected Memory
    val worker = new Thread(new Runnable {
      def run() {
        while (true) {
          try {
            val ref = queue.remove.asInstanceOf[MemoryReference]
            release(ref, isGC=true)
          }
          catch {
            case e: Exception => warn(e)
          }
        }
      }
    })
    worker.setDaemon(true)
    trace("Started memory collector")
    worker.start
  }

  private val totalAllocatedSize = new AtomicLong(0L)

  def allocatedSize = totalAllocatedSize.get()

  import UnsafeUtil.unsafe

  protected def allocatedAddresses: Seq[MemoryReference] = synchronized {
    Seq() ++ allocatedMemoryReferences.values
  } // take a copy of the set

  def allocate(size: Long): Memory = synchronized {
    allocateInternal(size)
  }

  def release(addr: Long, size: Long, isGC:Boolean) {
    synchronized {
      releaseInternal(addr, size, isGC)
    }
  }

  protected def allocateInternal(size: Long): Memory = {
    if (size == 0L)
      return Memory(0, 0, this)
    val m = Memory(unsafe.allocateMemory(size), size, this)
    trace(f"allocated memory address:${m.address}%x, size:${DataUnit.toHumanReadableFormat(size)}")
    val ref = new MemoryReference(m, queue)
    allocatedMemoryReferences += m.address -> ref
    totalAllocatedSize.getAndAdd(size)
    m
  }

  protected def releaseInternal(addr: Long, size: Long, isGC:Boolean) {
    if (allocatedMemoryReferences.contains(addr)) {
      trace(f"${if(isGC) "[GC] " else ""}released memory  address:${addr}%x, size:${DataUnit.toHumanReadableFormat(size)}")
      val ref = allocatedMemoryReferences(addr)
      unsafe.freeMemory(ref.address)
      ref.clear()
      totalAllocatedSize.getAndAdd(-size)
      allocatedMemoryReferences.remove(addr)
    }
  }
}

import collection.JavaConversions._

/**
 * This class uses ConcurrentHashMap to improve the memory allocation performance
 */
class ConcurrentMemoryAllocator(memMap: collection.mutable.Map[Long, MemoryReference]) extends DefaultAllocator(memMap) {
  def this() = this(new ConcurrentHashMap[Long, MemoryReference]())

  override def allocate(size: Long): Memory = allocateInternal(size)

  override def release(addr: Long, size: Long, isGC:Boolean): Unit = releaseInternal(addr, size, isGC)

}