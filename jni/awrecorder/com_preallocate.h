#include <jni.h>
/* Header for class com_xinzhihui_mydvr_FileUtil */

#ifndef _Included_com_xinzhihui_mydvr_FileUtil
#define _Included_com_xinzhihui_mydvr_FileUtil
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     Java_com_xinzhihui_mydvr_FileUtil
 * Method:    _ftruncate
 * Signature: (Ljava/lang/String;J)I
 */
JNIEXPORT jint JNICALL Java_com_xinzhihui_mydvr_FileUtil__1ftruncate
  (JNIEnv *, jobject, jstring, jlong);

/*
 * Class:     Java_com_xinzhihui_mydvr_FileUtil
 * Method:    _fallocate
 * Signature: (Ljava/lang/String;J)I
 */
JNIEXPORT jint JNICALL Java_com_xinzhihui_mydvr_FileUtil__1fallocate
  (JNIEnv *, jobject, jstring, jlong);


  /*
   * Class: 	Java_com_xinzhihui_mydvr_FileUtil
   * Method:	_test
   * Signature:   ()Ljava/lang/String;
   */

JNIEXPORT jstring JNICALL Java_com_xinzhihui_mydvr_FileUtil__1test
  	(JNIEnv * env, jobject);


#ifdef __cplusplus
}
#endif
#endif

