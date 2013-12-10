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
// MemoryAllocator.scala
// Since: 2013/03/14 9:26
//
//--------------------------------------

package xerial.larray

import xerial.core.log.Logger
import java.lang.ref.{PhantomReference, ReferenceQueue}
import collection.mutable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import xerial.core.util.DataUnit
import xerial.larray.UnsafeUtil._


/**
 * Phantom reference to the allocated memory
 * @param m the allocated memory
 * @param queue the reference queue to which GCed reference of the Memory will be put
 * @param address the allocated memory address
 */
class MemoryReference(m:Memory, queue:ReferenceQueue[Memory], val address:Long) extends PhantomReference[Memory](m, queue) {
  def this(m:Memory, queue:ReferenceQueue[Memory]) = this(m, queue, m.address)

  def name : String = "off-heap"

  def release {
    unsafe.freeMemory(m.headerAddress)
  }

}


/**
 * Accessor to the allocated memory
 * @param address headerAddress + HEADER_SIZE
 */
abstract class Memory(val address: Long) {
  /**
   * Allocated memory address
   */
  def headerAddress : Long

  /**
   * Allocated memory size
   */
  def size : Long

  def toRef(queue:ReferenceQueue[Memory]) : MemoryReference
}

case class RawMemory(override val address:Long) extends Memory(address) {
  def headerAddress = address - MemoryAllocator.HEADER_SIZE

  def size = if(address == 0) 0L else UnsafeUtil.unsafe.getLong(headerAddress)

  def toRef(queue:ReferenceQueue[Memory]) : MemoryReference = new MemoryReference(this, queue)

}



object MemoryAllocator {

  val HEADER_SIZE = 8L

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
    trace(f"Found unreleased memory address:${addr}%x") //  of size ${ref.size}%,d")
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


  private[larray] def register(m:Memory) : MemoryReference


  /**
   * Release the memory allocated by [[xerial.larray.MemoryAllocator#allocate]]. This method is called after GC
   * @param ref the reference to the memory allocated by  [[xerial.larray.MemoryAllocator#allocate]]
   */
  private[larray] def release(ref: MemoryReference): Unit = release(RawMemory(ref.address), true)
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
      return RawMemory(0)
    val mSize = size + MemoryAllocator.HEADER_SIZE
    val m = RawMemory(unsafe.allocateMemory(mSize) + MemoryAllocator.HEADER_SIZE)
    unsafe.putLong(m.headerAddress, mSize)
    trace(f"allocated memory address:${m.headerAddress}%x, size:${DataUnit.toHumanReadableFormat(mSize)}")
    val ref = new MemoryReference(m, queue)
    allocatedMemoryReferences += ref.address -> ref
    totalAllocatedSize.getAndAdd(mSize)
    m
  }

  def register(m:Memory) = {
    val ref = m.toRef(queue)
    allocatedMemoryReferences += ref.address -> ref
    ref
  }

  protected def releaseInternal(m:Memory, isGC:Boolean) {
    if (allocatedMemoryReferences.contains(m.address)) {
      val ref = allocatedMemoryReferences(m.address)
      trace(f"${if(isGC) "[GC] " else ""}released ${ref.name} address:${m.address}%x, size:${DataUnit.toHumanReadableFormat(m.size)}")
      ref.release
      ref match {
        case r:MemoryReference =>
          totalAllocatedSize.getAndAdd(- m.size)
      }
      ref.clear()
      allocatedMemoryReferences.remove(m.address)
    }
    else {
      warn(f"unknown allocated address: ${m.headerAddress}%x")
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