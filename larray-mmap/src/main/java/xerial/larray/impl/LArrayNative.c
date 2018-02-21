#include "LArrayNative.h"
#include <string.h>
#include <stdio.h>

#if defined(_WIN32 )|| defined(_WIN64)
#include <windows.h>
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
  (JNIEnv *env, jclass cls, jlong fd, jint mode, jlong offset, jlong size)
{
#if defined(_WIN32) || defined(_WIN64)
  void *mapAddress = 0;
  jlong maxSize = offset + size;
  jint lowLen = (jint) (maxSize);
  jint highLen = (jint) (maxSize >> 32);
  jint lowOffset = (jint) offset;
  jint highOffset = (jint) (offset >> 32);
  HANDLE fileHandle = (HANDLE) fd;
  HANDLE mapping;
  DWORD mapAccess = FILE_MAP_READ;
  DWORD fileProtect = PAGE_READONLY;
  BOOL result;
  if (mode == 0) {
    fileProtect = PAGE_READONLY;
    mapAccess = FILE_MAP_READ;
  } else if (mode == 1) {
    fileProtect = PAGE_READWRITE;
    mapAccess = FILE_MAP_WRITE;
  } else if (mode == 2) {
    fileProtect = PAGE_WRITECOPY;
    mapAccess = FILE_MAP_COPY;
  }

  mapping = CreateFileMapping(fileHandle, NULL, fileProtect, highLen, lowLen, NULL);
  mapAddress = MapViewOfFile(mapping, mapAccess, highOffset, lowOffset, (size_t) size);

  result = CloseHandle(mapping);
  return (jlong) mapAddress;

#else 
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

   addr = mmap(0, size, prot, flags, (int) fd, offset);

   return (jlong) addr;
#endif
}



JNIEXPORT void JNICALL Java_xerial_larray_impl_LArrayNative_munmap
  (JNIEnv *env, jclass cls, jlong addr, jlong size) {
#if defined(_WIN32) || defined(_WIN64)
  void *a = (void *) addr;
  BOOL result;
  result = UnmapViewOfFile(a);
#else
  munmap((void *) addr, (size_t) size);
#endif

}


JNIEXPORT void JNICALL Java_xerial_larray_impl_LArrayNative_msync
(JNIEnv *env, jclass cls, jlong fd, jlong addr, jlong size) {

#if defined(_WIN32) || defined(_WIN64)
  void *a = (void *) addr;
  BOOL result;
  int retry;

  /*
   * FlushViewOfFile can fail with ERROR_LOCK_VIOLATION if the memory
   * system is writing dirty pages to disk. As there is no way to
   * synchronize the flushing then we retry a limited number of times.
   */
  retry = 0;
  do {
    result = FlushViewOfFile(a, (DWORD)size);
    if ((result != 0) || (GetLastError() != ERROR_LOCK_VIOLATION))
      break;
    retry++;
  } while (retry < 3);

  if(fd != 0 && result != 0)  {
    result = FlushFileBuffers((HANDLE)fd);
    if(result == 0 && GetLastError() == ERROR_ACCESS_DENIED) {
      // read-only mapping
      result = 1;
    }
  }
  if(result == 0)
    printf("failed to sync\n");

#else
    msync((void *) addr, (size_t) size, MS_SYNC);
#endif
}

JNIEXPORT jlong JNICALL Java_xerial_larray_impl_LArrayNative_duplicateHandle
  (JNIEnv *env, jclass cls, jlong handle) {

#if defined(_WIN32) || defined(_WIN64)
  HANDLE hProcess = GetCurrentProcess();
  HANDLE hFile = (HANDLE) handle;
  HANDLE hResult;
  BOOL res = DuplicateHandle(hProcess, hFile, hProcess, &hResult, 0, FALSE, DUPLICATE_SAME_ACCESS);
  if(res == 0)
     return -1;
  else
     return (jlong) hResult;
#else
  return -1;
#endif

  }
