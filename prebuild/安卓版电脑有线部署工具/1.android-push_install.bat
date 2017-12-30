@echo off
adb install -r xuebaoPush.apk
adb shell am start -n com.daniulive.smartpublisher/com.daniulive.smartpublisher.CameraPublishActivity
ping -n 15 127.0.0.1>nul
adb reboot
exit