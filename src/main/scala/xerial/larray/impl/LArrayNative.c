#include "LArrayNative.h"
#include <string.h>

/*
 * Class:     xerial_larray_impl_LArrayNative
 * Method:    copyToArray
 * Signature: (JLjava/lang/Object;II)I
 */
JNIEXPORT jint JNICALL Java_xerial_larray_impl_LArrayNative_copyToArray
  (JNIEnv *env, jobject obj, jlong srcAddr, jobject destArr, jint destOffset, jint len) {

  char* ptr = (char*) (*env)->GetPrimitiveArrayCritical(env, (jarray) destArr, 0);
  memcpy((void*) srcAddr, ptr + destOffset, (size_t) len);
  (*env)->ReleasePrimitiveArrayCritical(env, (jarray) destArr, ptr, 0);

  return len;
}

/*
 * Class:     xerial_larray_impl_LArrayNative
 * Method:    copyFromArray
 * Signature: (Ljava/lang/Object;IJI)I
 */
JNIEXPORT jint JNICALL Java_xerial_larray_impl_LArrayNative_copyFromArray
  (JNIEnv *env, jobject obj, jobject srcArr, jint srcOffset, jlong destAddr, jint len) {

  char* ptr = (char*) (*env)->GetPrimitiveArrayCritical(env, (jarray) srcArr, 0);
  memcpy((void*) (ptr + srcOffset), (void*) destAddr, (size_t) len);
  (*env)->ReleasePrimitiveArrayCritical(env, (jarray) srcArr, ptr, 0);

  return len;
}

