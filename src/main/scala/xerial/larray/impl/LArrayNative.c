#include "LArrayNative.h"
#include <string.h>
#include <sys/mman.h>

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
  (JNIEnv *env, jclass cls, jlong addr, jlong size, jint prot, jint flags, jint fd, jlong offset) {

#if _WIN64 | _WIN32
  // TODO Windows specific implementation
  return (long) -1L;
#else
  void* maddr = NULL;
  void* r;
  if(addr != -1)
    maddr = addr;

  r = mmap(maddr, (size_t) size, (int) prot, (int) flags, (int) fd, (off_t) offset);

  return (long) r;
#endif
}
