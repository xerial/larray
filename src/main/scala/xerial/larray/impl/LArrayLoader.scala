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
import java.security.ProtectionDomain;
import java.util.Properties
;



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

    private def getRootClassLoader : ClassLoader = {
      var cl = Thread.currentThread().getContextClassLoader()
      while (cl.getParent() != null) {
        cl = cl.getParent()
      }
      cl
    }


    private def getByteCode(resourcePath:String) : Array[Byte] = {
      val in = this.getClass.getResourceAsStream(resourcePath)
      if (in == null)
        throw new IOException(resourcePath + " is not found")
      val buf = new Array[Byte](1024)
      val byteCodeBuf = new ByteArrayOutputStream()
      var readLength = 0
      while({readLength = in.read(buf); readLength != -1}) {
        byteCodeBuf.write(buf, 0, readLength)
      }
      in.close()
      byteCodeBuf.toByteArray()
    }

    def isNativeLibraryLoaded() = isLoaded

    private def hasInjectedNativeLoader : Boolean = {
        try {
            val nativeLoaderClassName = "xerial.larray.LArrayNativeLoader"
            val cl = Class.forName(nativeLoaderClassName)
            // If this native loader class is already defined, it means that another class loader already loaded the native library
            true
        }
        catch {
          case e:ClassNotFoundException => {
            // do loading somewhere else
            false
          }
        }
    }

    /**
     * Load LArrayNative and its JNI native implementation using the root class
     * loader. This hack is for avoiding the JNI multi-loading issue when the
     * same JNI library is loaded by different class loaders.
     * 
     * In order to load native code in the root class loader, this method first
     * inject LArrayNativeLoader class into the root class loader, because
     * {@link System#load(String)} method uses the class loader of the caller
     * class when loading native libraries.
     * 
     * <pre>
     * (root class loader) -> [LArrayNativeLoader (load JNI code), LArrayNative (has native methods), LArrayNativeAPI]  (injected by this method)
     *    |
     *    |
     * (child class loader) -> Sees the above classes loaded by the root class loader.
     *   Then creates LArrayNativeAPI implementation by instantiating LArrayNaitive class.
     * </pre>
     * 
     * 
     * <pre>
     * (root class loader) -> [LArrayNativeLoader, LArrayNative ...]  -> native code is loaded by once in this class loader
     *   |   \
     *   |    (child2 class loader)      
     * (child1 class loader)
     * 
     * child1 and child2 share the same LArrayNative code loaded by the root class loader.
     * </pre>
     * 
     * Note that Java's class loader first delegates the class lookup to its
     * parent class loader. So once LArrayNativeLoader is loaded by the root
     * class loader, no child class loader initialize LArrayNativeLoader again.
     * 
     * @return
     */
    def load : Any = {
        if (api != null)
            return api;

      /**
       * Inject NativeLoader class to the root class loader
       *
       * @return native code loader class initialized in the root class loader
       */
      def injectNativeLoader : Class[_] = {

        try {
          // Use parent class loader to load LArrayNative, since Tomcat, which uses different class loaders for each webapps, cannot load JNI interface twice

          val nativeLoaderClassName = "xerial.larray.LArrayNativeLoader"
          val rootClassLoader = getRootClassLoader
          // Load a byte code
          val byteCode = getByteCode("/xerial/larray/LArrayNativeLoader.bytecode")
          // In addition, we need to load the other dependent classes (e.g., LArrayNativeAPI and LArrayNative) using the system class loader
          val classesToPreload = Array("xerial.larray.impl.LArrayNativeAPI", "xerial.larray.impl.LArrayNative")
          val preloadClassByteCode = for (each <- classesToPreload) yield getByteCode(String.format("/%s.class", each.replaceAll("\\.", "/")))

          val classLoader = Class.forName("java.lang.ClassLoader")
          val defineClass = classLoader.getDeclaredMethod("defineClass", classOf[String], classOf[Array[Byte]], java.lang.Integer.TYPE, java.lang.Integer.TYPE, classOf[ProtectionDomain])
          val pd = classOf[System].getProtectionDomain
          // ClassLoader.defineClass is a protected method, so we have to make it accessible
          defineClass.setAccessible(true);
          try {
            // Create a new class using a ClassLoader#defineClass
            defineClass.invoke(rootClassLoader, nativeLoaderClassName, byteCode, new java.lang.Integer(0), new java.lang.Integer(byteCode.length), pd)

            // And also define dependent classes in the root class loader
            var i = 0
            while(i<classesToPreload.size) {
              val b = preloadClassByteCode(i)
              defineClass.invoke(rootClassLoader, classesToPreload(i), b, new java.lang.Integer(0), new java.lang.Integer(b.length), pd)
              i += 1
            }
          }
          finally {
            // Reset the accessibility to defineClass method
            defineClass.setAccessible(false)
          }

          // Load the LArrayNativeLoader class
          rootClassLoader.loadClass(nativeLoaderClassName)
        }
        catch {
          case e: Exception =>
            e.printStackTrace(System.err);
            sys.error(s"failed to load native code: ${e.getMessage}")
        }
      }




      synchronized {
        try {
          if (!hasInjectedNativeLoader) {
            // Inject LArrayNativeLoader (src/main/resources/xerial/larray/LArrayNativeLoader.bytecode) to the root class loader
            val nativeLoader = injectNativeLoader
            // Load the JNI code using the injected loader
            loadNativeLibrary(nativeLoader);
          }

          isLoaded = true;
          // Look up LArrayNative, injected to the root classloder, using reflection in order to avoid the initialization of LArrayNative class in this context class loader.
          val nativeCode : Any = Class.forName("xerial.larray.impl.LArrayNative").newInstance()
          api = nativeCode
          api
        }
        catch {
          case e: Exception =>
            e.printStackTrace()
            sys.error(e.getMessage)
        }
      }
    }

    /**
     * Load LArray's native code using the load method of the
     * LArrayNativeLoader class injected to the root class loader.
     * 
     * @param loaderClass
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private def loadNativeLibrary(loaderClass:Class[_]) {
        if (loaderClass == null)
            sys.error("missing native loader class")

      findNativeLibrary match {
        case Some(nativeLib) =>
            // Load extracted or specified larray native library.
            val loadMethod = loaderClass.getDeclaredMethod("load", classOf[String])
            loadMethod.invoke(null, nativeLib.getAbsolutePath().asInstanceOf[AnyRef])
        case None =>
          // Load preinstalled larray (in the path -Djava.library.path)
          val loadMethod = loaderClass.getDeclaredMethod("loadLibrary", classOf[String])
          loadMethod.invoke(null, "larray");
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

    /**
     * Extract the specified library file to the target folder
     * 
     * @param libFolderForCurrentOS
     * @param libraryFileName
     * @param targetFolder
     * @return
     */
    private def extractLibraryFile(libFolderForCurrentOS:String, libraryFileName:String, targetFolder:String) : Option[File] =  {
        val nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName
        val prefix = "larray-" + getVersion + "-";
        val extractedLibFileName = prefix + libraryFileName
        val extractedLibFile = new File(targetFolder, extractedLibFileName)

        try {
            if (extractedLibFile.exists()) {
                // test md5sum value
                val md5sum1 = md5sum(this.getClass.getResourceAsStream(nativeLibraryFilePath))
                val md5sum2 = md5sum(new FileInputStream(extractedLibFile))
                if (md5sum1.equals(md5sum2)) {
                  return Some(new File(targetFolder, extractedLibFileName))
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

            writer.close();
            reader.close();

            // Set executable (x) flag to enable Java to load the native library
            if (!System.getProperty("os.name").contains("Windows")) {
              try {
                Runtime.getRuntime().exec(Array("chhmod", "755", extractedLibFile.getAbsolutePath())).waitFor()
              }
              catch {
                case e:Throwable => // do nothing
              }
            }
            Some(new File(targetFolder, extractedLibFileName))
        }
        catch {
          case e:IOException =>
            e.printStackTrace(System.err);
            None
        }
    }

    private def findNativeLibrary : Option[File] = {

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

      // Temporary library folder. Use the value of xerial.larray.tempdir or java.io.tmpdir
      val tempFolder = new File(System.getProperty(KEY_LARRAY_TEMPDIR, System.getProperty("java.io.tmpdir"))).getAbsolutePath()
      // Extract and load a native library inside the jar file
      extractLibraryFile(nativeLibraryPath, nativeLibraryName, tempFolder)
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
        version;
    }

}