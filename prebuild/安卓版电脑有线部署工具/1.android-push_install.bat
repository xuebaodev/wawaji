@echo off
adb install -r xuebaoRtmpPush.apk
adb shell am start -n com.xuebao.rtmpPush/com.xuebao.rtmpPush.CameraPublishActivity
ping -n 15 127.0.0.1>nul
adb reboot
exit