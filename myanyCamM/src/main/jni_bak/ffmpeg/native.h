/* Header for class com_greatcompany_myteam_ref_JNITest */
#include <jni.h>
#ifndef _Included_com_myanycam_ui_CloudLivingView
#define _Included_com_myanycam_ui_CloudLivingView
#ifdef __cplusplus
extern "C" {
#endif
#undef com_myanycam_ui_CloudLivingView_COMMAND_CHANGE_TITLE
#define com_myanycam_ui_CloudLivingView_COMMAND_CHANGE_TITLE 1L
#undef com_myanycam_ui_CloudLivingView_COMMAND_UNUSED
#define com_myanycam_ui_CloudLivingView_COMMAND_UNUSED 2L
#undef com_myanycam_ui_CloudLivingView_COMMAND_TEXTEDIT_HIDE
#define com_myanycam_ui_CloudLivingView_COMMAND_TEXTEDIT_HIDE 3L
/*
 * Class:     com_greatcompany_myteam_ref_JNITest
 * Method:    initSDL
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_myanycam_ui_CloudLivingView_initSDL
  (JNIEnv *, jclass);

/*
 * Class:     com_greatcompany_myteam_ref_JNITest
 * Method:    convertBytesToVideo
 * Signature: ([B[B[B)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_myanycam_ui_CloudLivingView_convertBytesToVideo
  (JNIEnv *, jclass, jbyteArray, jbyteArray, jbyteArray);

/*
 * Class:     com_greatcompany_myteam_ref_JNITest
 * Method:    destroySDL
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_myanycam_ui_CloudLivingView_destroySDL
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
