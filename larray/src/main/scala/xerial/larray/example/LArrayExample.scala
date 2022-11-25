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
// LArrayExample.scala
// Since: 2013/03/23 23:16
//
//--------------------------------------

package xerial.larray.example

/**
  * Example of LArray Usages
  * @author
  *   Taro L. Saito
  */
class LArrayExample {

  import xerial.larray._

  // Create a new LArray of Int type of length = 5
  val l = LArray.of[Int](5)

  // Create an LArray with initial values
  val ll = LArray(3, 5, 9, 10)

  // Set elements. To specify a range of LArray, use 'Until' (for Long range) instead of 'until' (for Int range).
  for (i <- 0 Until l.size)
    l(i) = i.toInt

  // Read elements
  val e0 = l(0)
  val e1 = l(1)

  // Print the elements
  println(l.mkString(", ")) // 0, 1, 2, 3, 4

  // Traverse the elements with their indexes
  for ((e, i) <- l.zipWithIndex)
    println(s"l($i) = $e") // l(0) = 0, l(1) = 1, ...

  // Manipulate LArray
  val l2 = l.map(_ * 10)        // LArray(0, 10, 20, 30, 40)
  val f  = l.filter(_ % 2 == 0) // LArray(0, 2, 4)
  val s  = l.slice(2)           // LArray(2, 3, 4)
  l.foreach(println(_))

  // Build LArray
  val b = LArray.newBuilder[Int]
  for (i <- 0 until (10, step = 3))
    b += i
  val lb = b.result // LArray(0, 3, 6, 9)

  // Convert to Scala Array
  val arr = l.toArray
  println(arr.mkString(", ")) // 0, 1, 2, 3, 4

  // Convert Scala Array to LArray
  val arr2 = Array(1, 3, 5)
  val la   = arr2.toLArray

  // Save to a file
  import java.io.File
  val file = l.saveTo(new File("target/larray.tmp"))
  file.deleteOnExit()
  // Load from a file
  val l3 = LArray.loadFrom[Int](file) // LArray(0, 1, 2, 3, 4)

  // Initialize the array
  l.clear()
  println(l.mkString(", ")) // 0, 0, 0, 0, 0

  // Release the memory contents.
  l.free
  l3.free

  // You can omit calling free, because GC collects unused LArrays

  println("done.")
}
