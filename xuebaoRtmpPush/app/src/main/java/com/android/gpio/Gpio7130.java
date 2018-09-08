package com.android.gpio;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

public class Gpio7130
{
  private static final String TAG = "GPIO7130";
  String PINDirection = "/sys/class/gpio/gpio111/direction";
  String PINValue = "/sys/class/gpio/gpio111/value";
  //fda = open("/sys/class/gpio/gpio111/direction", O_RDWR);
  //fd = open("/sys/class/gpio/gpio111/value", O_RDWR);
  public Gpio7130()
  {
    //设置这里的文件权限
    String chDir = "chmod 777 "+ PINDirection;
    String chVa = "chmod 777 "+ PINValue;
    try {
      //execRootCmdSilent(PINDirection);
      //execRootCmdSilent(PINValue);
    } catch (Exception e) {
      // TODO: handle exception
    }
  }

  public int execRootCmdSilent(String cmd) {
//	    	System.out.println(cmd);
    int result = -1;
    DataOutputStream dos = null;

    try {
      Process p = Runtime.getRuntime().exec("su");
      dos = new DataOutputStream(p.getOutputStream());

      Log.i(TAG, cmd);
      dos.writeBytes(cmd + "\n");
      dos.flush();
      dos.writeBytes("exit\n");
      dos.flush();
      result = p.waitFor();
//	            result = p.exitValue();
//	            System.out.println(result);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (dos != null) {
        try {
          dos.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return result;
  }

  public void GPIO_SetValue(int value)
  {
    File fDir =  new File(PINDirection);
    if(fDir == null || fDir.exists() == false)
      return;

    try {
        FileOutputStream fos = new FileOutputStream(fDir);
        OutputStreamWriter outputWrite = new OutputStreamWriter(fos);
        PrintWriter print = new PrintWriter(outputWrite);
        print.print("out");
        print.flush();
        fos.close();
    } catch (IOException e) {
        e.printStackTrace();
    }

    File fVa =  new File(PINValue);
    if(fVa == null || fVa.exists() == false)
      return;

    try {
      FileOutputStream fos = new FileOutputStream(fVa);
      OutputStreamWriter outputWrite = new OutputStreamWriter(fos);
      PrintWriter print = new PrintWriter(outputWrite);
      print.print(value);
      print.flush();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}