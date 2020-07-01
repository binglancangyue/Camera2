#define LOG_NDEBUG 0
#define LOG_TAG "MediaRecorderJNI"
#include <utils/Log.h>

#include <gui/Surface.h>
#include <camera/ICameraService.h>
#include <camera/Camera.h>
#include "./AWRecorder.h"
#include <stdio.h>
#include <assert.h>
#include <limits.h>
#include <unistd.h>
#include <fcntl.h>
#include <utils/threads.h>

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"

#include <system/audio.h>
#include <android_runtime/android_view_Surface.h>

#include "com_preallocate.h"


// ----------------------------------------------------------------------------

using namespace android;

// ----------------------------------------------------------------------------

// helper function to extract a native Camera object from a Camera Java object
extern sp<Camera> get_native_camera(JNIEnv *env, jobject thiz, struct JNICameraContext** context);

struct fields_t {
    jfieldID    context;
    jfieldID    surface;

    jmethodID   post_event;
};
static fields_t fields;

static Mutex sLock;

// ----------------------------------------------------------------------------
// ref-counted object for callbacks
class JNIAWRecorderListener: public AWRecorderListener
{
public:
	JNIAWRecorderListener(JNIEnv* env, jobject thiz, jobject weak_thiz);
    ~JNIAWRecorderListener();
    void notify(int msg, int ext1, int ext2);
private:
    JNIAWRecorderListener();
    jclass      mClass;     // Reference to AWRecorder class
    jobject     mObject;    // Weak ref to AWRecorder Java object to call on
};

JNIAWRecorderListener::JNIAWRecorderListener(JNIEnv* env, jobject thiz, jobject weak_thiz)
{

    // Hold onto the AWRecorder class for use in calling the static method
    // that posts events to the application thread.
    jclass clazz = env->GetObjectClass(thiz);
    if (clazz == NULL) {
        ALOGE("Can't find com/softwinner/recorder/AWRecorder");
        jniThrowException(env, "java/lang/Exception", NULL);
        return;
    }
    mClass = (jclass)env->NewGlobalRef(clazz);

    // We use a weak reference so the AWRecorder object can be garbage collected.
    // The reference is only used as a proxy for callbacks.
    mObject  = env->NewGlobalRef(weak_thiz);
}

JNIAWRecorderListener::~JNIAWRecorderListener()
{
    // remove global references
    JNIEnv *env = AndroidRuntime::getJNIEnv();
    env->DeleteGlobalRef(mObject);
    env->DeleteGlobalRef(mClass);
}

void JNIAWRecorderListener::notify(int msg, int ext1, int ext2)
{
    ALOGV("JNIMediaRecorderListener::notify");

    JNIEnv *env = AndroidRuntime::getJNIEnv();
    env->CallStaticVoidMethod(mClass, fields.post_event, mObject, msg, ext1, ext2, 0);
}

// ----------------------------------------------------------------------------

static sp<Surface> get_surface(JNIEnv* env, jobject clazz)
{
    ALOGV("get_surface");
    return android_view_Surface_getSurface(env, clazz);
}

// Returns true if it throws an exception.
static bool process_awrecorder_call(JNIEnv *env, status_t opStatus, const char* exception, const char* message)
{
    ALOGV("process_media_recorder_call");
    if (opStatus == (status_t)INVALID_OPERATION) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return true;
    } else if (opStatus != (status_t)OK) {
        jniThrowException(env, exception, message);
        return true;
    }
    return false;
}

static sp<AWRecorder> getAWRecorder(JNIEnv* env, jobject thiz)
{
    Mutex::Autolock l(sLock);
    AWRecorder* const p = (AWRecorder*)env->GetIntField(thiz, fields.context);
    return sp<AWRecorder>(p);
}

static sp<AWRecorder> setAWRecorder(JNIEnv* env, jobject thiz, const sp<AWRecorder>& recorder)
{
    Mutex::Autolock l(sLock);
    sp<AWRecorder> old = (AWRecorder*)env->GetIntField(thiz, fields.context);
    if (recorder.get()) {
        recorder->incStrong(thiz);
    }
    if (old != 0) {
        old->decStrong(thiz);
    }
    env->SetIntField(thiz, fields.context, (int)recorder.get());
    return old;
}


static void AWRecorder_setCamera(JNIEnv* env, jobject thiz, jobject camera, jint cameraId)
{
    // we should not pass a null camera to get_native_camera() call.
    if (camera == NULL) {
        jniThrowNullPointerException(env, "camera object is a NULL pointer");
        return;
    }
    sp<Camera> c = get_native_camera(env, camera, NULL);
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    process_awrecorder_call(env, mr->setCamera(c->remote(), c->getRecordingProxy(), cameraId),
            "java/lang/RuntimeException", "setCamera failed.");
}

static void AWRecorder_setVideoSource(JNIEnv *env, jobject thiz, jint vs)
{
    ALOGV("setVideoSource(%d)", vs);
    if (vs < VIDEO_SOURCE_DEFAULT || vs >= VIDEO_SOURCE_LIST_END) {
        jniThrowException(env, "java/lang/IllegalArgumentException", "Invalid video source");
        return;
    }
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    process_awrecorder_call(env, mr->setVideoSource(vs), "java/lang/RuntimeException", "setVideoSource failed.");
}

static void AWRecorder_setAudioSource(JNIEnv *env, jobject thiz, jint as)
{
    ALOGV("setAudioSource(%d)", as);
    if (as < AUDIO_SOURCE_DEFAULT || as >= AUDIO_SOURCE_CNT) {
        jniThrowException(env, "java/lang/IllegalArgumentException", "Invalid audio source");
        return;
    }

    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    process_awrecorder_call(env, mr->setAudioSource(as), "java/lang/RuntimeException", "setAudioSource failed.");
}

static void AWRecorder_setOutputFormat(JNIEnv *env, jobject thiz, jint of)
{
    ALOGV("setOutputFormat(%d)", of);
    if (of < OUTPUT_FORMAT_DEFAULT || of >= OUTPUT_FORMAT_LIST_END) {
        jniThrowException(env, "java/lang/IllegalArgumentException", "Invalid output format");
        return;
    }
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    process_awrecorder_call(env, mr->setOutputFormat(of), "java/lang/RuntimeException", "setOutputFormat failed.");
}

static void AWRecorder_setVideoEncoder(JNIEnv *env, jobject thiz, jint ve)
{
    ALOGV("setVideoEncoder(%d)", ve);
    if (ve < VIDEO_ENCODER_DEFAULT || ve >= VIDEO_ENCODER_LIST_END) {
        jniThrowException(env, "java/lang/IllegalArgumentException", "Invalid video encoder");
        return;
    }
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    process_awrecorder_call(env, mr->setVideoEncoder(ve), "java/lang/RuntimeException", "setVideoEncoder failed.");
}

static void AWRecorder_setAudioEncoder(JNIEnv *env, jobject thiz, jint ae)
{
    ALOGV("setAudioEncoder(%d)", ae);
    if (ae < AUDIO_ENCODER_DEFAULT || ae >= AUDIO_ENCODER_LIST_END) {
        jniThrowException(env, "java/lang/IllegalArgumentException", "Invalid audio encoder");
        return;
    }
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    process_awrecorder_call(env, mr->setAudioEncoder(ae), "java/lang/RuntimeException", "setAudioEncoder failed.");
}

static void AWRecorder_setParameter(JNIEnv *env, jobject thiz, jstring params)
{
    ALOGV("setParameter()");
    if (params == NULL)
    {
        ALOGE("Invalid or empty params string.  This parameter will be ignored.");
        return;
    }

    sp<AWRecorder> mr = getAWRecorder(env, thiz);

    const char* params8 = env->GetStringUTFChars(params, NULL);
    if (params8 == NULL)
    {
        ALOGE("Failed to covert jstring to String8.  This parameter will be ignored.");
        return;
    }

    process_awrecorder_call(env, mr->setParameters(String8(params8)), "java/lang/RuntimeException", "setParameter failed.");
    env->ReleaseStringUTFChars(params,params8);
}

static void AWRecorder_setOutputFileFD(JNIEnv *env, jobject thiz, jobject fileDescriptor, jlong offset, jlong length)
{
    ALOGV("setOutputFile");
    if (fileDescriptor == NULL) {
        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return;
    }
    int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    status_t opStatus = mr->setOutputFile(fd, offset, length);
    process_awrecorder_call(env, opStatus, "java/io/IOException", "setOutputFile failed.");
}

static void AWRecorder_setVideoSize(JNIEnv *env, jobject thiz, jint width, jint height, jint out_width, jint out_height)
{
    ALOGV("setVideoSize(%d, %d, %d, %d)", width, height, out_width, out_height);
    sp<AWRecorder> mr = getAWRecorder(env, thiz);

    if (width <= 0 || height <= 0 || out_width < 0 || out_height < 0) {
        jniThrowException(env, "java/lang/IllegalArgumentException", "invalid video size");
        return;
    }
    process_awrecorder_call(env, mr->setVideoSize(width, height, out_width, out_height), "java/lang/RuntimeException", "setVideoSize failed.");
}

static void AWRecorder_setVideoFrameRate(JNIEnv *env, jobject thiz, jint rate)
{
    ALOGV("setVideoFrameRate(%d)", rate);
    if (rate <= 0) {
        jniThrowException(env, "java/lang/IllegalArgumentException", "invalid frame rate");
        return;
    }
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    process_awrecorder_call(env, mr->setVideoFrameRate(rate), "java/lang/RuntimeException", "setVideoFrameRate failed.");
}

static void AWRecorder_setMaxDuration(JNIEnv *env, jobject thiz, jint max_duration_ms)
{
    ALOGV("setMaxDuration(%d)", max_duration_ms);
    sp<AWRecorder> mr = getAWRecorder(env, thiz);

    char params[64];
    sprintf(params, "max-duration=%d", max_duration_ms);

    process_awrecorder_call(env, mr->setParameters(String8(params)), "java/lang/RuntimeException", "setMaxDuration failed.");
}

static void AWRecorder_setMaxFileSize(JNIEnv *env, jobject thiz, jlong max_filesize_bytes)
{
    ALOGV("setMaxFileSize(%lld)", max_filesize_bytes);
    sp<AWRecorder> mr = getAWRecorder(env, thiz);

    char params[64];
    sprintf(params, "max-filesize=%lld", max_filesize_bytes);

    process_awrecorder_call(env, mr->setParameters(String8(params)), "java/lang/RuntimeException", "setMaxFileSize failed.");
}

static void AWRecorder_setNextOutputFileFD(JNIEnv *env, jobject thiz, jobject fileDescriptor, jlong offset, jlong length)
{
    ALOGV("setNextOutputFileFD");
    if (fileDescriptor == NULL) {
        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return;
    }
    int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    status_t opStatus = mr->setNextOutputFile(fd, offset, length);
    process_awrecorder_call(env, opStatus, "java/io/IOException", "setNextOutputFileFD failed.");
}

static void AWRecorder_outputToNextFileStartFD(JNIEnv *env, jobject thiz, jobject fileDescriptor, jlong offset, jlong length)
{
    ALOGV("outputToNextFileStartFD");
    if (fileDescriptor == NULL) {
        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return;
    }
    int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    status_t opStatus = mr->outputToNextFileStart(fd, offset, length);
    process_awrecorder_call(env, opStatus, "java/io/IOException", "outputToNextFileStartFD failed.");
}

static void AWRecorder_outputToNextFileStart(JNIEnv *env, jobject thiz)
{
    ALOGV("outputToNextFileStart");
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    status_t opStatus = mr->outputToNextFileStart();
    process_awrecorder_call(env, opStatus, "java/lang/IllegalArgumentException", "outputToNextFileStart failed.");
}

static void AWRecorder_outputToNextFileStartPath(JNIEnv *env, jobject thiz, jstring path)
{
    ALOGV("outputToNextFileStartPath()");
    if (path == NULL)
    {
        ALOGE("Invalid or empty params string.  This parameter will be ignored.");
        return;
    }
    sp<AWRecorder> mr = getAWRecorder(env, thiz);

    const char* path8 = env->GetStringUTFChars(path, NULL);
    if (path8 == NULL)
    {
        ALOGE("Failed to covert jstring to String8.  This parameter will be ignored.");
        return;
    }

    process_awrecorder_call(env, mr->outputToNextFileStart(String8(path8)),"java/lang/IllegalArgumentException", "outputToNextFileStartPath failed.");
    env->ReleaseStringUTFChars(path,path8);
}

static void AWRecorder_setMicMute(JNIEnv *env, jobject thiz, jint micStatu)
{
    ALOGV("setMicMute(%d)", micStatu);

    sp<AWRecorder> mr = getAWRecorder(env, thiz);

    process_awrecorder_call(env, mr->setMicMute(micStatu), "java/lang/RuntimeException", "setMicMute failed.");

}

static void AWRecorder_prepare(JNIEnv *env, jobject thiz)
{
    ALOGV("prepare");
    sp<AWRecorder> mr = getAWRecorder(env, thiz);

    jobject surface = env->GetObjectField(thiz, fields.surface);
    if (surface != NULL) {
        const sp<Surface> native_surface = get_surface(env, surface);

        // The application may misbehave and
        // the preview surface becomes unavailable
        if (native_surface.get() == 0) {
            ALOGE("Application lost the surface");
            jniThrowException(env, "java/io/IOException", "invalid preview surface");
            return;
        }

        ALOGI("prepare: surface=%p", native_surface.get());
        if (process_awrecorder_call(env, mr->setPreviewSurface(native_surface->getIGraphicBufferProducer()), "java/lang/RuntimeException", "setPreviewSurface failed.")) {
            return;
        }
    }
    process_awrecorder_call(env, mr->prepare(), "java/io/IOException", "prepare failed.");
}

static int AWRecorder_native_getMaxAmplitude(JNIEnv *env, jobject thiz)
{
    ALOGV("getMaxAmplitude");
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    int result = 0;
    process_awrecorder_call(env, mr->getMaxAmplitude(&result), "java/lang/RuntimeException", "getMaxAmplitude failed.");
    return result;
}

static void AWRecorder_start(JNIEnv *env, jobject thiz)
{
    ALOGV("start");
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    process_awrecorder_call(env, mr->start(), "java/lang/RuntimeException", "start failed.");
}

static void AWRecorder_stop(JNIEnv *env, jobject thiz)
{
    ALOGV("stop");
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    process_awrecorder_call(env, mr->stop(), "java/lang/RuntimeException", "stop failed.");
}

static void AWRecorder_native_reset(JNIEnv *env, jobject thiz)
{
    ALOGV("native_reset");
    sp<AWRecorder> mr = getAWRecorder(env, thiz);
    process_awrecorder_call(env, mr->reset(), "java/lang/RuntimeException", "native_reset failed.");
}

static void AWRecorder_release(JNIEnv *env, jobject thiz)
{
    ALOGV("release");
    sp<AWRecorder> mr = setAWRecorder(env, thiz, 0);
    if (mr != NULL) {
        mr->setListener(NULL);
        mr->release();
    }
}

// This function gets some field IDs, which in turn causes class initialization.
// It is called from a static block in AWRecorder, which won't run until the
// first time an instance of this class is used.
static void AWRecorder_native_init(JNIEnv *env)
{
    jclass clazz;

    clazz = env->FindClass("com/softwinner/recorder/AWRecorder");
    if (clazz == NULL) {
        return;
    }

    fields.context = env->GetFieldID(clazz, "mNativeContext", "I");
    if (fields.context == NULL) {
        return;
    }

    fields.surface = env->GetFieldID(clazz, "mSurface", "Landroid/view/Surface;");
    if (fields.surface == NULL) {
        return;
    }

    jclass surface = env->FindClass("android/view/Surface");
    if (surface == NULL) {
        return;
    }

    fields.post_event = env->GetStaticMethodID(clazz, "postEventFromNative",
                                               "(Ljava/lang/Object;IIILjava/lang/Object;)V");
    if (fields.post_event == NULL) {
        return;
    }
}


static void AWRecorder_native_setup(JNIEnv *env, jobject thiz, jobject weak_this,
                                         jstring packageName)
{
    ALOGV("setup");

    sp<AWRecorder> mr = new AWRecorder();
    if (mr == NULL) {
        jniThrowException(env, "java/lang/RuntimeException", "Out of memory");
        return;
    }
    if (mr->initCheck() != NO_ERROR) {
        jniThrowException(env, "java/lang/RuntimeException", "Unable to initialize media recorder");
        return;
    }

    // create new listener and give it to AWRecorder
    sp<JNIAWRecorderListener> listener = new JNIAWRecorderListener(env, thiz, weak_this);
    mr->setListener(listener);

   // Convert client name jstring to String16
    const char16_t *rawClientName = env->GetStringChars(packageName, NULL);
    jsize rawClientNameLen = env->GetStringLength(packageName);
    String16 clientName(rawClientName, rawClientNameLen);
    env->ReleaseStringChars(packageName, rawClientName);

    // pass client package name for permissions tracking
    mr->setClientName(clientName);

    setAWRecorder(env, thiz, mr);
}

static void AWRecorder_native_finalize(JNIEnv *env, jobject thiz)
{
    ALOGV("finalize");
    AWRecorder_release(env, thiz);
}


#if 1 // zgx add
/* Native interface, it will be call in java code */
/*
 * Class:     com_android_camera_v66_FileUtil
 * Method:    _ftruncate
 * Signature: (Ljava/lang/String;J)I
 */
JNIEXPORT jint JNICALL Java_com_xinzhihui_mydvr_FileUtil__1ftruncate(JNIEnv * env, jobject, jstring name, jlong size) {
	int fd,ret = -1;
	
	const char* fileName = env->GetStringUTFChars(name, NULL);
	ALOGI("Java_com_xinzhihui_mydvr_FileUtil__1ftruncate                 file_name =%s\n",fileName);

	//fd = open(fileName, O_CREAT | O_RDWR, 0777);
	ret = truncate(fileName, size);
	//close(fd);
	return ret;
}

/*
 * Class:     com_xinzhihui_mydvr_FileUtil
 * Method:    _fallocate
 * Signature: (Ljava/lang/String;J)I
 */
JNIEXPORT jint JNICALL Java_com_xinzhihui_mydvr_FileUtil__1fallocate(JNIEnv * env, jobject, jstring name, jlong size) {
	int fd,ret,alloc_ret,alloc_fd;
	
	ALOGI("zgx ---- Java_com_xinzhihui_mydvr_FileUtil__1fallocate                 ----");   
	
	const char* fileName = env->GetStringUTFChars(name, NULL);
	fd = open(fileName, O_CREAT | O_RDWR, 0777);
/*
	alloc_fd = open("/mnt/extsd/DCIM/zgx_Pre_file2", O_CREAT | O_RDWR, 0644);
    if(alloc_fd <= 0)  
	{  
		ALOGI("zgx --- open /mnt/extsd/DCIM/zgx_Pre_file2: %s  faild\n");
        return -1;  
	}
	else{
		ALOGI(" open /mnt/extsd/DCIM/zgx_Pre_file2 success\n");
		alloc_ret =    fallocate(alloc_fd, 0, 0, size);	
		ALOGI(" fallocate ret = %d\n",alloc_ret);
		
	    write(alloc_fd,"zgx fallocate file\n",25);		
		ALOGI(" open file success\n");
		close(alloc_fd);
	
	}
*/
	ret = fallocate(fd, 0x01, 0, size);
	close(fd);
	return ret;
}


/*
 * Class:     com_xinzhihui_mydvr_FileUtil
 * Method:    _test
 * Signature:   ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_xinzhihui_mydvr_FileUtil__1test(JNIEnv * env, jobject) {
    //ALOGI("zgx---- FileUtil JNI Test");
	return (*env).NewStringUTF("JNI cpp FileUtil JNI Test  success!!!");
}


#endif




// ---------------------------------------------------------------------------------------

static JNINativeMethod gMethods[] = {
    {"setCamera",            "(Landroid/hardware/Camera;I)V",    (void *)AWRecorder_setCamera},
    {"setVideoSource",       "(I)V",                            (void *)AWRecorder_setVideoSource},
    {"setAudioSource",       "(I)V",                            (void *)AWRecorder_setAudioSource},
    {"setOutputFormat",      "(I)V",                            (void *)AWRecorder_setOutputFormat},
    {"setVideoEncoder",      "(I)V",                            (void *)AWRecorder_setVideoEncoder},
    {"setAudioEncoder",      "(I)V",                            (void *)AWRecorder_setAudioEncoder},
    {"setParameter",         "(Ljava/lang/String;)V",           (void *)AWRecorder_setParameter},
    {"_setOutputFile",       "(Ljava/io/FileDescriptor;JJ)V",   (void *)AWRecorder_setOutputFileFD},
    {"_setVideoSize",        "(IIII)V",                         (void *)AWRecorder_setVideoSize},
    {"setVideoFrameRate",    "(I)V",                            (void *)AWRecorder_setVideoFrameRate},
    {"setMaxDuration",       "(I)V",                            (void *)AWRecorder_setMaxDuration},
    {"setMaxFileSize",       "(J)V",                            (void *)AWRecorder_setMaxFileSize},
    {"_prepare",             "()V",                             (void *)AWRecorder_prepare},
    {"getMaxAmplitude",      "()I",                             (void *)AWRecorder_native_getMaxAmplitude},
    {"start",                "()V",                             (void *)AWRecorder_start},
    {"stop",                 "()V",                             (void *)AWRecorder_stop},
    {"native_reset",         "()V",                             (void *)AWRecorder_native_reset},
    {"release",              "()V",                             (void *)AWRecorder_release},
    {"native_init",          "()V",                             (void *)AWRecorder_native_init},
    {"native_setup",         "(Ljava/lang/Object;Ljava/lang/String;)V", (void *)AWRecorder_native_setup},
    {"native_finalize",      "()V",                             (void *)AWRecorder_native_finalize},
    {"outputToNextFileStart","()V",                            (void *)AWRecorder_outputToNextFileStart},
    {"outputToNextFileStart","(Ljava/lang/String;)V",  (void *)AWRecorder_outputToNextFileStartPath},
    {"_outputToNextFileStart", "(Ljava/io/FileDescriptor;JJ)V",  (void *)AWRecorder_outputToNextFileStartFD},
    {"_setNextOutputFile",   "(Ljava/io/FileDescriptor;JJ)V",   (void *)AWRecorder_setNextOutputFileFD},
	{"setMicMute",	         "(I)V",                            (void *)AWRecorder_setMicMute},

	
	{"qc_ftruncate", "(Ljava/lang/String;J)I", (void*)Java_com_xinzhihui_mydvr_FileUtil__1ftruncate},
    {"qc_fallocate", "(Ljava/lang/String;J)I", (void*)Java_com_xinzhihui_mydvr_FileUtil__1fallocate},
	{"qc_test", "()Ljava/lang/String;", (void*)Java_com_xinzhihui_mydvr_FileUtil__1test},
};

static const char* const kClassPathName = "com/softwinner/recorder/AWRecorder";

int register_AWRecorder(JNIEnv *env)
{
    return AndroidRuntime::registerNativeMethods(env,"com/softwinner/recorder/AWRecorder", gMethods, NELEM(gMethods));
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    if (register_AWRecorder(env) < 0) {
        ALOGE("ERROR: AWRecorder native registration failed\n");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}
