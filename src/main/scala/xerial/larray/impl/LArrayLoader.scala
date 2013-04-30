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
// LArrayLoader.scala
// Since: 2013/03/18 10:35 AM
//
//--------------------------------------

package xerial.larray.impl

import java.io._

import java.security.DigestInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.{UUID, Properties}


/**
 * <b>Internal only - Do not use this class.</b> This class loads a native
 * library of larray (larray.dll, liblarray.so, etc.) according to the
 * user platform (<i>os.name</i> and <i>os.arch</i>).

 * In default, no configuration is required to use larray, but you can load
 * your own native library created by 'make native' command.
 *
 * This LArrayLoader load a native library (larray.dll, larray.so, etc.) using the following procedure:
 * <ol>
 * <li>Extract one of the libraries embedded in larray-(version).jar into
 * (System property: <i>java.io.tempdir</i>). If
 * <i>xerial.larray.tempdir</i> is set, use this folder instead of
 * <i>java.io.tempdir</i>.
 * </ol>
 *
 * <p>
 * If you do not want to use folder <i>java.io.tempdir</i>, set the System
 * property <i>xerial.larray.tempdir</i>. For example, to use
 * <i>/tmp/leo</i> as a temporary folder to copy native libraries, use -D option
 * of JVM:
 *
 * <pre>
 * <code>
 * java -Dxerial.larray.tempdir="/tmp/leo" ...
 * </code>
 * </pre>
 *
 * </p>
 *
 * @author leo
 *
 */
object LArrayLoader {

  val KEY_LARRAY_TEMPDIR  = "xerial.larray.tempdir"

  private[larray] var isLoaded = false
  private[larray] var api : Any = null

  def setApi(nativeInstance:AnyRef) {
    api = nativeInstance
    isLoaded = true
  }

  def load {
    if(isLoaded)
      return

    synchronized {
      try {
        val libFile = findNativeLibrary.newCopy
        // Delete the extracted native library upon exit
        libFile.deleteOnExit()
        System.load(libFile.getAbsolutePath)
        isLoaded = true
      }
      catch {
        case e: Exception =>
          e.printStackTrace()
          sys.error(e.getMessage)
      }
    }
  }

  /**
   * Computes the MD5 value of the input stream
   *
   * @param input
   * @return
   * @throws IOException
   * @throws NoSuchAlgorithmException
   */
  def md5sum(input:InputStream) : String = {
    val in = new BufferedInputStream(input)
    try {
      val digest = java.security.MessageDigest.getInstance("MD5")
      val digestInputStream = new DigestInputStream(in, digest)
      while(digestInputStream.read() >= 0) {}
      val md5out = new ByteArrayOutputStream()
      md5out.write(digest.digest())
      md5out.toString
    }
    catch {
      case e:NoSuchAlgorithmException =>
        throw new IllegalStateException("MD5 algorithm is not available: " + e)
    }
    finally {
      in.close()
    }
  }


  private case class NativeLib(nativeLibFolder:String, libName:String) {
    /** Create a new unique copy of the native library **/
    def newCopy : File = {

      // Temporary library folder. Use the value of xerial.larray.tempdir or java.io.tmpdir
      val tempFolder = new File(System.getProperty(KEY_LARRAY_TEMPDIR, System.getProperty("java.io.tmpdir"))).getAbsolutePath()
      // Extract and load a native library inside the jar file
      extractLibraryFile(nativeLibFolder, libName, tempFolder)

    }

    /**
     * Extract the specified library file to the target folder
     *
     * @param libFolderForCurrentOS
     * @param libraryFileName
     * @param targetFolder
     * @return
     */
    private def extractLibraryFile(libFolderForCurrentOS:String, libraryFileName:String, targetFolder:String) : File =  {
      val nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName
      val suffix = UUID.randomUUID().toString
      val extractedLibFileName = s"larray-${getVersion}-${suffix}.lib"
      val extractedLibFile = new File(targetFolder, extractedLibFileName)


      if (extractedLibFile.exists()) {
        // test md5sum value
        val md5sum1 = md5sum(this.getClass.getResourceAsStream(nativeLibraryFilePath))
        val md5sum2 = md5sum(new FileInputStream(extractedLibFile))
        if (md5sum1.equals(md5sum2)) {
          return new File(targetFolder, extractedLibFileName)
        }
        else {
          // remove old native library file
          val deletionSucceeded = extractedLibFile.delete()
          if (!deletionSucceeded) {
            throw new IOException("failed to remove existing native library file: "
              + extractedLibFile.getAbsolutePath());
          }
        }
      }

      // Extract a native library file into the target directory
      val reader = this.getClass.getResourceAsStream(nativeLibraryFilePath)
      val writer = new FileOutputStream(extractedLibFile)
      val buffer = new Array[Byte](8192)
      var bytesRead = 0
      while ({bytesRead = reader.read(buffer); bytesRead != -1}) {
        writer.write(buffer, 0, bytesRead);
      }

      writer.close()
      reader.close()

      // Set executable (x) flag to enable Java to load the native library
      if (!System.getProperty("os.name").contains("Windows")) {
        try {
          Runtime.getRuntime().exec(Array("chhmod", "755", extractedLibFile.getAbsolutePath())).waitFor()
        }
        catch {
          case e:Throwable => // do nothing
        }
      }
      new File(targetFolder, extractedLibFileName)
    }

  }

  private def findNativeLibrary : NativeLib = {

    def hasResource(path:String) = this.getClass.getResource(path) != null

    // Try to load the library in xerial.larray.native  */
    // Resolve the library file name with a suffix (e.g., dll, .so, etc.)
    var nativeLibraryName = System.mapLibraryName("larray")
    // Load an OS-dependent native library inside a jar file
    val nativeLibraryPath = s"/xerial/larray/native/${OSInfo.getNativeLibFolderPathForCurrentOS}"
    var hasNativeLib = hasResource(s"$nativeLibraryPath/$nativeLibraryName")

    // Fix for openjdk7 for Mac
    if(!hasNativeLib) {
      if(OSInfo.getOSName().equals("Mac")) {
        val altName = "liblarray.jnilib";
        if(hasResource(nativeLibraryPath + "/" + altName)) {
          nativeLibraryName = altName
          hasNativeLib = true
        }
      }
    }

    if(!hasNativeLib) {
      val errorMessage = s"no native library is found for os.name=${OSInfo.getOSName} and os.arch=${OSInfo.getArchName}"
      sys.error(errorMessage)
    }

    NativeLib(nativeLibraryPath, nativeLibraryName)
  }


  /**
   * Get the LArray version by reading VERSION file embedded in jar.
   * This version data is used as a suffix of a dll file extracted from the
   * jar.
   *
   * @return the version string
   */
  def getVersion : String = {
    val versionFile = this.getClass.getResource("/xerial/larray/VERSION")
    var version = "unknown"
    try {
      if (versionFile != null) {
        val versionData = new Properties()
        versionData.load(versionFile.openStream())
        version = versionData.getProperty("version", version)
        if (version.equals("unknown"))
          version = versionData.getProperty("VERSION", version)
        version = version.trim().replaceAll("[^0-9\\.]", "")
      }
    }
    catch {
      case e:IOException => System.err.println(e)
    }
    version
  }

}