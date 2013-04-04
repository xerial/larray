#include "LArrayNative.h"
#include <string.h>

#if defined(_WIN32) || defined(_WIN64)
//
#else
#include <sys/mman.h>
#endif

/*
 * Class:     xerial_larray_impl_LArrayNative
 * Method:    copyToArray
 * Signature: (JLjava/lang/Object;II)I
 */
JNIEXPORT jint JNICALL Java_xerial_larray_impl_LArrayNative_copyToArray
  (JNIEnv *env, jclass cls, jlong srcAddr, jobject destArr, jint destOffset, jint len) {

  char* ptr = (char*) (*env)->GetPrimitiveArrayCritical(env, (jarray) destArr, 0);
  memcpy(ptr + destOffset, (void*) srcAddr, (size_t) len);
  (*env)->ReleasePrimitiveArrayCritical(env, (jarray) destArr, ptr, 0);

  return len;
}

/*
 * Class:     xerial_larray_impl_LArrayNative
 * Method:    copyFromArray
 * Signature: (Ljava/lang/Object;IJI)I
 */
JNIEXPORT jint JNICALL Java_xerial_larray_impl_LArrayNative_copyFromArray
  (JNIEnv *env, jclass cls, jobject srcArr, jint srcOffset, jlong destAddr, jint len) {

  char* ptr = (char*) (*env)->GetPrimitiveArrayCritical(env, (jarray) srcArr, 0);
  memcpy((void*) destAddr, (void*) (ptr + srcOffset), (size_t) len);
  (*env)->ReleasePrimitiveArrayCritical(env, (jarray) srcArr, ptr, 0);

  return len;
}



JNIEXPORT jlong JNICALL Java_xerial_larray_impl_LArrayNative_mmap
  (JNIEnv *env, jclass cls, jint fd, jint mode, jlong offset, jlong size) {

   void *addr;
   int prot = 0;
   int flags = 0;
   if(mode == 0) {
      prot = PROT_READ;
      flags = MAP_SHARED;
   } else if(mode == 1) {
      prot = PROT_READ | PROT_WRITE;
      flags = MAP_SHARED;
   } else if(mode == 2) {
     prot = PROT_READ | PROT_WRITE;
     flags = MAP_PRIVATE;
   }

   addr = mmap(0, size, prot, flags, fd, offset);

   return (jlong) addr;
}



JNIEXPORT void JNICALL Java_xerial_larray_impl_LArrayNative_munmap
  (JNIEnv *env, jclass cls, jlong addr, jlong size) {

    munmap((void *) addr, (size_t) size);
}


JNIEXPORT void JNICALL Java_xerial_larray_impl_LArrayNative_msync
  (JNIEnv *env, jclass cls, jlong addr, jlong size) {

    msync((void *) addr, (size_t) size, MS_SYNC);
}

