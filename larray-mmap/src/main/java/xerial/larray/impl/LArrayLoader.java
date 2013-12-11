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

package xerial.larray.impl;


import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;


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
class LArrayLoader {

    public static String KEY_LARRAY_TEMPDIR  = "xerial.larray.tempdir";

    private static boolean isLoaded = false;
    private static Object api = null;

    public static void setApi(Object nativeInstance) {
        api = nativeInstance;
        isLoaded = true;
    }

    public static synchronized void load() throws Exception {
        if(isLoaded)
            return;

        try {
            File libFile = findNativeLibrary().newCopy();
            // Delete the extracted native library upon exit
            libFile.deleteOnExit();
            System.load(libFile.getAbsolutePath());
            isLoaded = true;
        }
        catch(Exception e) {
            System.err.println(e.getMessage());
            throw e;
        }
    }

    /**
     * Computes the MD5 value of the input stream
     *
     * @param input
     * @return
     * @throws IOException
     * @throws IllegalStateException
     */
    public static String md5sum(InputStream input) throws IOException {
        InputStream in = new BufferedInputStream(input);
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            DigestInputStream digestInputStream = new DigestInputStream(in, digest);
            while(digestInputStream.read() >= 0) {
            }
            OutputStream md5out = new ByteArrayOutputStream();
            md5out.write(digest.digest());
            return md5out.toString();
        }
        catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is not available: " + e.getMessage());
        }
        finally {
            in.close();
        }
    }


    static class NativeLib{

        private final String nativeLibFolder;
        private final String libName;

        public NativeLib(String nativeLibFolder, String libName) {
            this.nativeLibFolder = nativeLibFolder;
            this.libName = libName;
        }

        /** Create a new unique copy of the native library **/
        public File newCopy() throws IOException {
            // Temporary library folder. Use the value of xerial.larray.tempdir or java.io.tmpdir
            String tempFolder = new File(System.getProperty(KEY_LARRAY_TEMPDIR, System.getProperty("java.io.tmpdir"))).getAbsolutePath();
            // Extract and load a native library inside the jar file
            return extractLibraryFile(nativeLibFolder, libName, tempFolder);
        }


        private boolean contentsEquals(InputStream in1, InputStream in2) throws IOException {
            if(!(in1 instanceof  BufferedInputStream)) {
                in1 = new BufferedInputStream(in1);
            }
            if(!(in2 instanceof BufferedInputStream)) {
                in2 = new BufferedInputStream(in2);
            }

            int ch = in1.read();
            while(ch != -1) {
                int ch2 = in2.read();
                if(ch != ch2)
                    return false;
                ch = in1.read();
            }
            int ch2 = in2.read();
            return ch2 == -1;
        }

        /**
         * Extract the specified library file to the target folder
         *
         * @param libFolderForCurrentOS
         * @param libraryFileName
         * @param targetFolder
         * @return
         */
        private File extractLibraryFile(String libFolderForCurrentOS, String libraryFileName, String targetFolder) throws IOException {
            String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            String extractedLibFileName = String.format("larray-%s.lib", suffix);
            File extractedLibFile = new File(targetFolder, extractedLibFileName);


            // Extract a native library file into the target directory
            InputStream reader = this.getClass().getResourceAsStream(nativeLibraryFilePath);
            OutputStream writer = new FileOutputStream(extractedLibFile);
            try {
                byte[] buffer = new byte[8192];
                int bytesRead = 0;
                while ((bytesRead = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, bytesRead);
                }
            }
            finally {
                extractedLibFile.deleteOnExit();
                if(writer != null)
                    writer.close();
                if(reader != null)
                    reader.close();
            }

            // Set executable (x) flag to enable Java to load the native library
            extractedLibFile.setReadable(true);
            extractedLibFile.setWritable(true, true);
            extractedLibFile.setExecutable(true);


            // Check whether the contents are properly copied from the resource folder
            {
                InputStream nativeIn = LArrayLoader.class.getResourceAsStream(nativeLibraryFilePath);
                InputStream extractedLibIn = new FileInputStream(extractedLibFile);
                try {
                    if(!contentsEquals(nativeIn, extractedLibIn))
                        throw new IOException(String.format("Failed to write a native library file at %s", extractedLibFile));
                }
                finally {
                    if(nativeIn != null)
                        nativeIn.close();
                    if(extractedLibIn != null)
                        extractedLibIn.close();
                }
            }


            return new File(targetFolder, extractedLibFileName);
        }

    }

    private static boolean hasResource(String path) {
        return LArrayLoader.class.getResource(path) != null;
    }

    private static NativeLib findNativeLibrary() {

        // Try to load the library in xerial.larray.native  */
        // Resolve the library file name with a suffix (e.g., dll, .so, etc.)
        String nativeLibraryName = System.mapLibraryName("larray");
        // Load an OS-dependent native library inside a jar file
        String nativeLibraryPath = "/xerial/larray/native/" + OSInfo.getNativeLibFolderPathForCurrentOS();
        boolean hasNativeLib = hasResource(nativeLibraryPath + "/" + nativeLibraryName);

        // Fix for openjdk7 for Mac
        if(!hasNativeLib) {
            if(OSInfo.getOSName().equals("Mac")) {
                String altName = "liblarray.jnilib";
                if(hasResource(nativeLibraryPath + "/" + altName)) {
                    nativeLibraryName = altName;
                    hasNativeLib = true;
                }
            }
        }

        if(!hasNativeLib) {
            String errorMessage = String.format("no native library is found for os.name=%s and os.arch=%s", OSInfo.getOSName(), OSInfo.getArchName());
            System.err.println(errorMessage);
        }

        return new NativeLib(nativeLibraryPath, nativeLibraryName);
    }


}