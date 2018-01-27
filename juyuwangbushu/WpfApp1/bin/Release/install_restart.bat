@echo off
adb -s %1 install -r xuebaoRtmpPush.apk
adb -s %1 shell am start -n com.xuebao.rtmpPush/com.xuebao.rtmpPush.CameraPublishActivity
echo waitFor 15 Seconds
ping -n 15 127.0.0.1>nul
adb -s %1 reboot