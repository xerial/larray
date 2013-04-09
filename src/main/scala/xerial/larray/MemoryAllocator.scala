//--------------------------------------
//
// MemoryAllocator.scala
// Since: 2013/03/14 9:26
//
//--------------------------------------

package xerial.larray

import impl.LArrayNative
import xerial.core.log.Logger
import java.lang.ref.{PhantomReference, ReferenceQueue}
import collection.mutable
import java.util.concurrent.ConcurrentHashMap
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


class MMapMemoryReference(m:Memory, queue:ReferenceQueue[Memory], override val address:Long, override val size:Long) extends MemoryReference(m, queue, address, size) {
  def this(m:Memory, queue:ReferenceQueue[Memory]) = this(m, queue, m.address, m.size)
}

/**
 * Accessor to the allocated memory
 * @param address
 */
case class Memory(address: Long, size: Long)



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


  def registerMMapMemory(m:Memory) : MemoryReference


  /**
   * Release the memory allocated by [[xerial.larray.MemoryAllocator#allocate]]. This method is called after GC
   * @param ref the reference to the memory allocated by  [[xerial.larray.MemoryAllocator#allocate]]
   */
  private[larray] def release(ref: MemoryReference): Unit = release(Memory(ref.address, ref.size), true)
  def release(m:Memory, isGC:Boolean = false) : Unit

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
            release(ref)
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


  def release(m:Memory, isGC:Boolean) {
    synchronized {
      releaseInternal(m, isGC)
    }
  }

  protected def allocateInternal(size: Long): Memory = {
    if (size == 0L)
      return Memory(0, 0)
    val m = Memory(unsafe.allocateMemory(size), size)
    trace(f"allocated memory address:${m.address}%x, size:${DataUnit.toHumanReadableFormat(size)}")
    val ref = new MemoryReference(m, queue)
    allocatedMemoryReferences += ref.address -> ref
    totalAllocatedSize.getAndAdd(size)
    m
  }


  def registerMMapMemory(m:Memory) = {
    val ref = new MMapMemoryReference(m, queue)
    allocatedMemoryReferences += ref.address -> ref
    ref
  }

  protected def releaseInternal(m:Memory, isGC:Boolean) {
    if (allocatedMemoryReferences.contains(m.address)) {
      val ref = allocatedMemoryReferences(m.address)
      ref match {
        case r:MMapMemoryReference =>
          trace(f"${if(isGC) "[GC] " else ""}released mmap   address:${m.address}%x, size:${DataUnit.toHumanReadableFormat(m.size)}")
          LArrayNative.munmap(m.address, m.size)
          //MappedLArray.unmap0.invoke(null, m.address.asInstanceOf[AnyRef], m.size.asInstanceOf[AnyRef])
        case r:MemoryReference =>
          trace(f"${if(isGC) "[GC] " else ""}released memory address:${m.address}%x, size:${DataUnit.toHumanReadableFormat(m.size)}")
          unsafe.freeMemory(ref.address)
          totalAllocatedSize.getAndAdd(-m.size)
      }
      ref.clear()
      allocatedMemoryReferences.remove(m.address)
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

  override def release(m:Memory, isGC:Boolean): Unit = releaseInternal(m, isGC)

}