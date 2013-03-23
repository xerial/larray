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

  // Set elements
  for(i <- 0 until l.size.toInt)
    l(i) = i

  // Read elements
  val e0 = l(0)
  val e1 = l(1)

  // Print the contents
  println(l.mkString(", ")) // 0, 1, 2, 3, 4

  for((e, i) <- l.zipWithIndex)
    println(s"l($i) = $e") // l(0) = 0, l(1) = 1, ...

  // Manipulate LArray
  val l2 = l.map(_ * 10) // LArray(0, 10, 20, 30, 40)
  val f = l.filter(_ % 2 == 0) // LArray(0, 2, 4)
  val s = l.slice(2) // LArray(2, 3, 4)

  // Build LArray
  val b = LArray.newBuilder[Int]
  for(i <- 0 until 10 step 3)
    b += i
  val lb = b.result // LArray(0, 3, 6, 9)

  // Convert to Scala Array
  val arr = l.toArray
  print(arr.mkString(", ")) // 0, 1, 2, 3, 4

  // Save to a file
  import java.io.File
  val file = l.saveTo(new File("larray.tmp"))

  // Read from a file
  val l3 = LArray.loadFrom[Int](file) // LArray(0, 1, 2, 3, 4)

  // Initialize the array
  l.clear()
  println(l.mkString(", ")) // 0, 0, 0, 0, 0


  // Release the memory contents
  l.free
  l3.free

}