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
// LArrayLoaderTest.scala
// Since: 2013/03/29 9:56
//
//--------------------------------------

package xerial.larray.impl

import java.net.{URL, URLClassLoader}
import java.io.File
import xerial.larray.LArraySpec


/**
 * @author Taro L. Saito
 */
class LArrayLoaderTest extends LArraySpec {

  "LArrayLoader" should {

    "use a different library name for each class loader" in {

      val larrayNativeCls = "xerial.larray.impl.LArrayNative"

      val cl = Thread.currentThread().getContextClassLoader
      val parentCl = cl.getParent.getParent // Fix for sbt-0.13
      debug(s"context cl: ${cl}, parent cl: $parentCl")
      val classPath = Array(new File("larray-mmap/target/classes").toURI.toURL)
      val cl1 = new URLClassLoader(classPath, parentCl)
      val cl2 = new URLClassLoader(classPath, parentCl)

      import java.{lang=>jl}

      val nativeCls1 = cl1.loadClass(larrayNativeCls)
      val ni1 = nativeCls1.newInstance()
      val arr1 = Array.ofDim[Byte](100)
      val m1 = nativeCls1.getDeclaredMethod("copyToArray", jl.Long.TYPE, classOf[AnyRef], jl.Integer.TYPE, jl.Integer.TYPE)
      m1.invoke(ni1, Seq.apply[AnyRef](new jl.Long(0L), arr1, new jl.Integer(0), new jl.Integer(0)):_*)
      val nativeCls1_2 = cl1.loadClass(larrayNativeCls)

      val nativeCls2 = cl2.loadClass(larrayNativeCls)
      val ni2 = nativeCls2.newInstance()
      val arr2 = Array.ofDim[Byte](100)
      val m2 = nativeCls2.getDeclaredMethod("copyToArray", jl.Long.TYPE, classOf[AnyRef], jl.Integer.TYPE, jl.Integer.TYPE)
      m2.invoke(ni1, Seq.apply[AnyRef](new jl.Long(0L), arr2, new jl.Integer(0), new jl.Integer(0)):_*)

      nativeCls1 should not be (nativeCls2)
      nativeCls1 should be (nativeCls1_2)

      val arr3 = Array.ofDim[Byte](100)
      LArrayNative.copyToArray(0, arr3, 0, 0)
    }


  }

}