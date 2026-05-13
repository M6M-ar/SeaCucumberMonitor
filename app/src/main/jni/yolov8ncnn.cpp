#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <string>
#include <vector>
#include <mutex>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "net.h"
#include "gpu.h"
#include "yolov8.h"

#define LOG_TAG "SeaCucumberYolo"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::mutex g_lock;
static YOLOv8* g_yolov8 = nullptr;

static const char* YOLO_CLASS_PATH = "com/example/seacucumbermonitor/Yolov8Ncnn";
static const char* YOLO_OBJ_CLASS_PATH = "com/example/seacucumbermonitor/Yolov8Ncnn$Obj";

static const char* PARAM_FILE_NAME = "yolov8.param";
static const char* MODEL_FILE_NAME = "yolov8.bin";
static const char* DEFAULT_LABEL_NAME = "sea_cucumber";

static bool asset_exists(AAssetManager* mgr, const char* file_name) {
    if (mgr == nullptr || file_name == nullptr) {
        return false;
    }

    AAsset* asset = AAssetManager_open(mgr, file_name, AASSET_MODE_BUFFER);
    if (asset == nullptr) {
        return false;
    }

    AAsset_close(asset);
    return true;
}

static void release_detector() {
    std::lock_guard<std::mutex> guard(g_lock);

    if (g_yolov8 != nullptr) {
        delete g_yolov8;
        g_yolov8 = nullptr;
    }
}

static YOLOv8* create_detector() {
    return new YOLOv8_det_coco;
}

static cv::Mat bitmap_to_rgb(JNIEnv* env, jobject bitmap) {
    cv::Mat rgb;

    if (bitmap == nullptr) {
        return rgb;
    }

    AndroidBitmapInfo info;
    int get_info_result = AndroidBitmap_getInfo(env, bitmap, &info);
    if (get_info_result != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_getInfo failed: %d", get_info_result);
        return rgb;
    }

    void* pixels = nullptr;
    int lock_result = AndroidBitmap_lockPixels(env, bitmap, &pixels);
    if (lock_result != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_lockPixels failed: %d", lock_result);
        return rgb;
    }

    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        cv::Mat rgba(info.height, info.width, CV_8UC4, pixels, info.stride);
        cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);
    } else if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
        cv::Mat rgb565(info.height, info.width, CV_8UC2, pixels, info.stride);
        cv::cvtColor(rgb565, rgb, cv::COLOR_BGR5652RGB);
    } else {
        LOGE("Unsupported bitmap format: %d", info.format);
    }

    AndroidBitmap_unlockPixels(env, bitmap);

    return rgb;
}

static jobjectArray objects_to_java(JNIEnv* env, const std::vector<Object>& objects) {
    jclass obj_class = env->FindClass(YOLO_OBJ_CLASS_PATH);
    if (obj_class == nullptr) {
        LOGE("Cannot find Obj class");
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(obj_class, "<init>", "()V");
    if (constructor == nullptr) {
        LOGE("Cannot find Obj constructor");
        env->DeleteLocalRef(obj_class);
        return nullptr;
    }

    jfieldID field_x = env->GetFieldID(obj_class, "x", "F");
    jfieldID field_y = env->GetFieldID(obj_class, "y", "F");
    jfieldID field_w = env->GetFieldID(obj_class, "w", "F");
    jfieldID field_h = env->GetFieldID(obj_class, "h", "F");
    jfieldID field_label = env->GetFieldID(obj_class, "label", "Ljava/lang/String;");
    jfieldID field_prob = env->GetFieldID(obj_class, "prob", "F");

    if (field_x == nullptr ||
        field_y == nullptr ||
        field_w == nullptr ||
        field_h == nullptr ||
        field_label == nullptr ||
        field_prob == nullptr) {

        LOGE("Cannot find Obj fields");
        env->DeleteLocalRef(obj_class);
        return nullptr;
    }

    jobjectArray result_array = env->NewObjectArray(
            static_cast<jsize>(objects.size()),
            obj_class,
            nullptr
    );

    if (result_array == nullptr) {
        env->DeleteLocalRef(obj_class);
        return nullptr;
    }

    for (size_t i = 0; i < objects.size(); i++) {
        const Object& obj = objects[i];

        jobject java_obj = env->NewObject(obj_class, constructor);
        if (java_obj == nullptr) {
            continue;
        }

        env->SetFloatField(java_obj, field_x, obj.rect.x);
        env->SetFloatField(java_obj, field_y, obj.rect.y);
        env->SetFloatField(java_obj, field_w, obj.rect.width);
        env->SetFloatField(java_obj, field_h, obj.rect.height);
        env->SetFloatField(java_obj, field_prob, obj.prob);

        jstring label = env->NewStringUTF(DEFAULT_LABEL_NAME);
        env->SetObjectField(java_obj, field_label, label);

        env->SetObjectArrayElement(result_array, static_cast<jsize>(i), java_obj);

        env->DeleteLocalRef(label);
        env->DeleteLocalRef(java_obj);
    }

    env->DeleteLocalRef(obj_class);
    return result_array;
}

static jboolean native_loadModel(
        JNIEnv* env,
        jobject thiz,
        jobject assetManager,
        jint modelid,
        jint cpugpu) {

    if (assetManager == nullptr) {
        LOGE("assetManager is null");
        return JNI_FALSE;
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    if (mgr == nullptr) {
        LOGE("AAssetManager_fromJava failed");
        return JNI_FALSE;
    }

    if (!asset_exists(mgr, PARAM_FILE_NAME)) {
        LOGE("Model param file not found: %s", PARAM_FILE_NAME);
        return JNI_FALSE;
    }

    if (!asset_exists(mgr, MODEL_FILE_NAME)) {
        LOGE("Model bin file not found: %s", MODEL_FILE_NAME);
        return JNI_FALSE;
    }

    bool use_gpu = cpugpu == 1;

#if !NCNN_VULKAN
    if (use_gpu) {
        LOGE("NCNN Vulkan is not enabled, fallback to CPU");
        use_gpu = false;
    }
#endif

    std::lock_guard<std::mutex> guard(g_lock);

    if (g_yolov8 != nullptr) {
        delete g_yolov8;
        g_yolov8 = nullptr;
    }

    g_yolov8 = create_detector();

    if (g_yolov8 == nullptr) {
        LOGE("create_detector failed");
        return JNI_FALSE;
    }

    int target_size = 640;
    g_yolov8->set_det_target_size(target_size);

    int ret = g_yolov8->load(mgr, PARAM_FILE_NAME, MODEL_FILE_NAME, use_gpu);
    if (ret != 0) {
        LOGE("load model failed, ret=%d", ret);

        delete g_yolov8;
        g_yolov8 = nullptr;

        return JNI_FALSE;
    }

    LOGD("load model success: %s / %s, target_size=%d, use_gpu=%d",
         PARAM_FILE_NAME,
         MODEL_FILE_NAME,
         target_size,
         use_gpu ? 1 : 0);

    return JNI_TRUE;
}

static jobjectArray native_detect(
        JNIEnv* env,
        jobject thiz,
        jobject bitmap,
        jboolean use_gpu) {

    if (bitmap == nullptr) {
        return nullptr;
    }

    cv::Mat rgb = bitmap_to_rgb(env, bitmap);
    if (rgb.empty()) {
        LOGE("bitmap_to_rgb failed");
        return nullptr;
    }

    std::vector<Object> objects;

    {
        std::lock_guard<std::mutex> guard(g_lock);

        if (g_yolov8 == nullptr) {
            LOGE("g_yolov8 is null, model not loaded");
            return nullptr;
        }

        int ret = g_yolov8->detect(rgb, objects);
        if (ret != 0) {
            LOGE("detect failed, ret=%d", ret);
            return nullptr;
        }
    }

    return objects_to_java(env, objects);
}

static JNINativeMethod g_methods[] = {
        {
                "loadModel",
                "(Landroid/content/res/AssetManager;II)Z",
                reinterpret_cast<void*>(native_loadModel)
        },
        {
                "detect",
                "(Landroid/graphics/Bitmap;Z)[Lcom/example/seacucumbermonitor/Yolov8Ncnn$Obj;",
                reinterpret_cast<void*>(native_detect)
        }
};

extern "C" jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGD("JNI_OnLoad");

    JNIEnv* env = nullptr;

    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("GetEnv failed");
        return JNI_ERR;
    }

    jclass clazz = env->FindClass(YOLO_CLASS_PATH);
    if (clazz == nullptr) {
        LOGE("Cannot find class: %s", YOLO_CLASS_PATH);
        return JNI_ERR;
    }

    int method_count = sizeof(g_methods) / sizeof(g_methods[0]);
    int register_result = env->RegisterNatives(clazz, g_methods, method_count);

    env->DeleteLocalRef(clazz);

    if (register_result != JNI_OK) {
        LOGE("RegisterNatives failed: %d", register_result);
        return JNI_ERR;
    }

#if NCNN_VULKAN
    ncnn::create_gpu_instance();
#endif

    return JNI_VERSION_1_6;
}

extern "C" void JNI_OnUnload(JavaVM* vm, void* reserved) {
    LOGD("JNI_OnUnload");

    release_detector();

#if NCNN_VULKAN
    ncnn::destroy_gpu_instance();
#endif
}