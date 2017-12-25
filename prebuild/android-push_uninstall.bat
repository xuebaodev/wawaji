@echo off
echo pm uninstall com.daniulive.smartpublisher > temp.txt
echo exit >> temp.txt
adb shell < temp.txt
del temp.txt
exit