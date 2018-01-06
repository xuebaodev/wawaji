@echo off
echo pm uninstall com.xuebao.rtmpPush > temp.txt
echo exit >> temp.txt
adb shell < temp.txt
del temp.txt
exit