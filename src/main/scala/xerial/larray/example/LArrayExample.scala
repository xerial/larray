//--------------------------------------
//
// LArrayExample.scala
// Since: 2013/03/23 23:16
//
//--------------------------------------

package xerial.larray.example

/**
 * Example of LArray Usages
 * @author Taro L. Saito
 */
class LArrayExample {

  import xerial.larray._

  // Create a new LArray of Int type
  val l = LArray.of[Int](5)

  // Create an LArray with initial values
  val ll = LArray(3, 5, 9, 10)

  // Set elements
  for(i <- 0 until l.size.toInt)
    l(i) = i

  // Read elements
  val e0 = l(0)
  val e1 = l(1)

  // Print the elements
  println(l.mkString(", ")) // 0, 1, 2, 3, 4

  // Traverse the elements with their indexes
  for((e, i) <- l.zipWithIndex)
    println(s"l($i) = $e") // l(0) = 0, l(1) = 1, ...

  // Manipulate LArray
  val l2 = l.map(_ * 10) // LArray(0, 10, 20, 30, 40)
  val f = l.filter(_ % 2 == 0) // LArray(0, 2, 4)
  val s = l.slice(2) // LArray(2, 3, 4)
  l.foreach(println(_))

  // Build LArray
  val b = LArray.newBuilder[Int]
  for(i <- 0 until (10, step=3))
    b += i
  val lb = b.result // LArray(0, 3, 6, 9)

  // Convert to Scala Array
  val arr = l.toArray
  print(arr.mkString(", ")) // 0, 1, 2, 3, 4

  // Save to a file
  import java.io.File
  val file = l.saveTo(new File("larray.tmp"))

  // Load from a file
  val l3 = LArray.loadFrom[Int](file) // LArray(0, 1, 2, 3, 4)

  // Initialize the array
  l.clear()
  println(l.mkString(", ")) // 0, 0, 0, 0, 0


  // Release the memory contents.
  l.free
  l3.free

  // You can omit calling free, because GC collects unused LArrays
}