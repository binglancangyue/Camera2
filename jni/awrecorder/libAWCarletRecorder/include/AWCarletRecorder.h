#ifndef AWCARLETRECORDER_H_
#define AWCARLETRECORDER_H_
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include <fcntl.h>
#include <signal.h>
#include <getopt.h>
#include <sys/wait.h>
#include <utils/Thread.h>
#include <pthread.h>
#include <sys/types.h>
#include <ctype.h>

#include <utils/Log.h>

#include <binder/IPCThreadState.h>
#include <utils/Errors.h>
#include <utils/Thread.h>
#include <utils/Timers.h>

#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>
#include <gui/ISurfaceComposer.h>
#include <ui/DisplayInfo.h>
#include <media/openmax/OMX_IVCommon.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/MediaCodec.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaMuxer.h>
#include <media/ICrypto.h>
//#include <media/mediarecorder.h>
#include <media/stagefright/AudioSource.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/Utils.h>
#include <media/stagefright/CameraSource.h>

//#include "RecorderThread.h"

namespace android {
using android::status_t;

class AWCarletRecorder{

public:
    /* Constructs AWCarletRecorder instance. */
    AWCarletRecorder();

    /* Destructs AWCarletRecorder instance. */
    ~AWCarletRecorder();

protected:


    class PutVideoInputBufferThread : public Thread {
        sp<MediaCodec> mEncoder;
        sp<CameraSource> mSource;
        AWCarletRecorder* mAWCarletRecorder;
        bool mRequestExit;

    public:
        PutVideoInputBufferThread( AWCarletRecorder* recorder):
            Thread(false),
            mAWCarletRecorder(recorder),
            mRequestExit(false) {
        }

        virtual ~PutVideoInputBufferThread() {
        }

        void startThread() {
            run("PutVideoInputBufferThread", PRIORITY_URGENT_DISPLAY);
        }

        void stopThread() {
            mRequestExit = true;
        }

        virtual bool threadLoop() {
            if (mRequestExit) {
                return false;
            }
            return mAWCarletRecorder->prepareVideoInputBuffersThread();
        }
    };


    class PutAudioInputBufferThread : public Thread {
        sp<MediaCodec> mEncoder;
        sp<AudioSource> mSource;
        AWCarletRecorder* mAWCarletRecorder;
        bool mRequestExit;

    public:
        PutAudioInputBufferThread( AWCarletRecorder* recorder):
            Thread(false),
            mAWCarletRecorder(recorder),
            mRequestExit(false) {
        }

        virtual ~PutAudioInputBufferThread() {
        }

        void startThread() {
            run("PutAudioInputBufferThread", PRIORITY_URGENT_DISPLAY);
        }

        void stopThread() {
            mRequestExit = true;
        }

        virtual bool threadLoop() {
            if (mRequestExit) {
                return false;
            }
            return mAWCarletRecorder->prepareAudioInputBuffersThread();
        }
    };


    class GetVideoStreamToMuxerThread : public Thread {
        AWCarletRecorder* mAWCarletRecorder;
        bool mRequestExit;

    public:
        GetVideoStreamToMuxerThread(AWCarletRecorder* recorder):
            Thread(false),
            mAWCarletRecorder(recorder),
            mRequestExit(false){
        }

        virtual ~GetVideoStreamToMuxerThread() {
        }

        void startThread() {
            run("GetVideoStreamToMuxerThread", PRIORITY_AUDIO );
        }

        void stopThread() {
            mRequestExit = true;
        }

        virtual bool threadLoop() {
            if (mRequestExit) {
                return false;
            }
            return mAWCarletRecorder->getAndWriteVideoStreamToMuxerThread();
        }
    };

    class GetAudioStreamToMuxerThread : public Thread {
        AWCarletRecorder* mAWCarletRecorder;
        bool mRequestExit;

    public:
        GetAudioStreamToMuxerThread(AWCarletRecorder* recorder):
            Thread(false),
            mAWCarletRecorder(recorder),
            mRequestExit(false){
        }

        virtual ~GetAudioStreamToMuxerThread() {
        }

        void startThread() {
            run("GetAudioStreamToMuxerThread", PRIORITY_AUDIO );
        }

        void stopThread() {
            mRequestExit = true;
        }

        virtual bool threadLoop() {
            if (mRequestExit) {
                return false;
            }
            return mAWCarletRecorder->getAndWriteAudioStreamToMuxerThread();
        }
    };


public:

    status_t init();

    status_t setCamera(const sp<ICamera>& camera, const sp<ICameraRecordingProxy>& proxy, int cameraId);
    status_t setPreviewSurface(const sp<IGraphicBufferProducer>& surface);
    status_t setVideoSize(int width, int height, int out_width, int out_height);
    status_t setOutputFormat(int of);
    status_t setMicMute(int micStatu);
    status_t setParamVideoEncodingBitRate(int32_t bitRate);
    status_t setParameter(const String8 &key, const String8 &value);
    status_t setParameters(const String8 &params);

    /*get source buffer and put it into codec*/
    bool prepareVideoInputBuffersThread();
    bool prepareAudioInputBuffersThread();
    /*get encoded stream and write it to muxer*/
    bool getAndWriteVideoStreamToMuxerThread();
    bool getAndWriteAudioStreamToMuxerThread();

    status_t prepareVideoSource();
    status_t prepareAudioSource();

    status_t prepareVideoEncoder();
    status_t prepareAudioEncoder();

    status_t setClientName(const String16& clientName);
    status_t setOutputFile(const char* path);
    status_t setOutputFile(int fd, int64_t offset, int64_t length);
    status_t outputToNextFileStart(int fd, int64_t offset, int64_t length);
    status_t outputToNextFileStart(const String8 &path);

    status_t prepare();
    status_t start();
    status_t stop();
    status_t release();

    status_t stopAudioInputThread();
    status_t stopVideoInputThread();
    status_t stopVideoMuxerThread();
    status_t stopAudioMuxerThread();

    char* mFileName0;
    char* mFileName1;

    int mOutputFd0;
    int mOutputFd1;

    sp<MediaMuxer> mMuxer0;
    int mVideoTrackIdx0;
    int mAudioTrackIdx0;
    sp<MediaMuxer> mMuxer1;
    int mVideoTrackIdx1;
    int mAudioTrackIdx1;
    sp<MetaData> mAudioTrackMeta;

    int mMuxer0SyncStartFlag;
    MediaMuxer::OutputFormat mMuxerFormat;

    sp<MediaCodec> mVideoEncoder;
    sp<MediaCodec> mAudioEncoder;
    int32_t mAudioBitrate;
    int32_t mAudioSampleRate;
    int32_t mAudioChannelCount;

    sp<AudioSource> mAudioSource;
    sp<CameraSource> mCameraSource;

    bool mEnableMute;

    sp<ICamera> mCamera;
    sp<ICameraRecordingProxy> mCameraProxy;
    String16 mClientName;
    uid_t mClientUid;

    int mCameraID;
    int mFramerate;

    sp<IGraphicBufferProducer> mPreviewSurface;

    int mVideoSourceWidth;
    int mVideoSourceHeight;
    int mVideoOutWidth;
    int mVideoOutHeight;

    uint32_t mVideoBitrate;

    int mDequeueBufferTimeout;

    //status flag
    int mVideoInputThreadStatu;
    int mAudioInputThreadStatu;
    int mVideoMuxerThreadStatu;
    int mAudioMuxerThreadStatu;
    int mMuxerActiveStatu;
    int mMuxer0Statu;
    int mMuxer1Statu;

    bool mNeedAudioRecord;

    int mVideoFrameInCodecCnt;
    int mAudioFrameInCodecCnt;

    int64_t mVideoPTS;
    int64_t mAudioPTS;
    int64_t mPTSOffset;

    sp<PutAudioInputBufferThread> mAudioInputThread;
    pthread_mutex_t mAudioInputMutex;
    pthread_cond_t mAudioCondOneLoopFinish;

    sp<PutVideoInputBufferThread> mVideoInputThread;
    pthread_mutex_t mVideoInputMutex;
    pthread_cond_t mVideoCondOneLoopFinish;

    sp<GetVideoStreamToMuxerThread> mVideoMuxerThread;
    pthread_mutex_t mMuxerMutex;
    pthread_mutex_t mVideoMuxerMutex;
    pthread_mutex_t mAudioMuxerMutex;
    pthread_mutex_t mMutexCnt;
    pthread_mutex_t mMutexAudioCnt;
    pthread_cond_t mVideoMuxerCondOneLoopFinish;
    pthread_cond_t mVideoMuxerCondKeyFrame;
    pthread_cond_t mMuxerCondWaitSync;

    sp<GetAudioStreamToMuxerThread> mAudioMuxerThread;
    pthread_cond_t mAudioMuxerCondOneLoopFinish;

};
};
#endif
