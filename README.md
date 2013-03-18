LArray
=== 
A library for managing large arrays that can hold more than 2G (2^31) entries in Java and Scala.

## Features 
 * 2^31 (2GB) entries is the limitation of the default Java/Scala arrays. LArray uses Long (2^63) type indexes and for example the entire human genome (3GB) data can be stored in LArray. 
 * LArray can be released from the main memory at any time. 
   * The default arrays in Scala(Java) consumes JVM heaps heavily and often causes OutOfMemory error when using large amount of genomic data. LArray uses memory space outside of the default JVM heap, and can be releaseed immedeately.

