#bin/sh
export PATH=/opt/android/android-ndk-r11c:$PATH
ndk-build
cp -f ../libs/armeabi/* ../jniLibs/armeabi/
cp -f ../libs/armeabi/* ../jniLibs/armeabi-v7a/

