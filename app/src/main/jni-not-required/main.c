#include "de_uni_kl_informatik_disco_discowall_MainActivity.h"

/*
 * Class:     de_uni_kl_informatik_disco_discowall_MainActivity
 * Method:    getStringFromNative
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_de_uni_1kl_informatik_disco_discowall_MainActivity_getStringFromNative
  (JNIEnv * env, jobject obj)
  {
    return (*env)->NewStringUTF(env, "Hello from JNI!");
  }
