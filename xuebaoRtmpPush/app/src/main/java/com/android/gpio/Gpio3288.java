package com.android.gpio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

public class Gpio3288
{
  private static final String TAG = "GPIO";
  private int mPort;
  private boolean isGpioPortPrepared = false;
  public static final int RK3288_PIN7_PA5 = 229;// SET
  public static final int RK3288_PIN7_PA6 = 230;// CON
  public static final int RK3288_PIN7_PB3 = 235;
  public static final int RK3288_PIN7_PB4 = 236;
  public static final int RK3288_PIN7_PB5 = 237;
  public static final int RK3288_PIN7_PA7 = 231;
  public static final int RK3288_PIN7_PB0 = 232;

  public static final int TYPE_DIRECTION_IN = -1;
  public static final int TYPE_DIRECTION_OUT = -2;
  public static final int TYPE_VALUE_HIGH = 1;
  public static final int TYPE_VALUE_LOW = 0;
  public static final int TYPE_UNKONW = -10000;
  private File mGpioExport = null;
  private File mGpioUnExport = null;
  private File mGpioPort = null;
  private File mGpioPortDirection = null;
  private File mGpioPortValue = null;

  private String gpio_export = "/sys/class/gpio/export";
  private String gpio_unexport = "/sys/class/gpio/unexport";
  private String gpio_port = "/sys/class/gpio/gpio";

  public Gpio3288(int port)
  {
    this.mPort = port;
    this.mGpioExport = new File(this.gpio_export);
    this.mGpioUnExport = new File(this.gpio_unexport);
    this.isGpioPortPrepared = prepare_gpio_port(this.mPort);
//    System.out.println("GPIO contstuct, isPrepared: " + this.isGpioPortPrepared);
  }

  public boolean gpio_request()
  {
    return this.isGpioPortPrepared;
  }

  public void gpio_free()
  {
    writeGpioNode(this.mGpioUnExport, this.mPort);
  }

  public void setDirectionValue(int flag, int value)
  {
    writeGpioNode(this.mGpioPortDirection, flag);
    writeGpioNode(this.mGpioPortValue, value);
  }

  public int getDirectionValue()
  {
    String string = null;
    if (this.mGpioPortDirection.exists()) {
      string = readGpioNode(this.mGpioPortDirection);
    }else{
    	return -10000;
    }
    int value = 0;
    
    if (string.equals("in"))
      value = -1;
    else if (string.equals("out"))
      value = -2;
    else {
      value = -10000;
    }
    return value;
  }

  public int getPortValue()
  {
    String string = readGpioNode(this.mGpioPortValue);
    int value = 0;
    if(string==null){
    	return -1;
    }
    if (string.equals("0"))
      value = 0;
    else if (string.equals("1")) {
      value = 1;
    }
    return value;
  }

  public void setPortValue(int flag)
  {
    writeGpioNode(this.mGpioPortValue, flag);
  }

  private boolean prepare_gpio_port(int port)
  {
    if (this.mGpioExport.exists()) {
      writeGpioNode(this.mGpioExport, port);
      String path = this.gpio_port + port;
      String path_direction = path + "/direction";
      String path_value = path + "/value";

//      System.out.println("prepare gpio port: " + port);
//      System.out.println(path);
//      System.out.println(path_direction);
//      System.out.println(path_value);

      this.mGpioPort = new File(path);
      this.mGpioPortDirection = new File(path_direction);
      this.mGpioPortValue = new File(path_value);
    }
    return (this.mGpioPort.exists()) && (this.mGpioPortDirection.exists()) && (this.mGpioPortValue.exists());
  }

  private void writeGpioNode(File file, int flag) {
    if (file.exists()) {
//      System.out.println("write " + flag + " to " + file);
      try {
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter outputWrite = new OutputStreamWriter(fos);
        PrintWriter print = new PrintWriter(outputWrite);
        if (flag > 159)
          print.print(flag);
        else {
          switch (flag) {
          case -1:
            print.print("in");
            break;
          case -2:
            print.print("out");
            break;
          case 1:
            print.print(1);
            break;
          case 0:
            print.print(0);
            break;
          }

        }

        print.flush();
        fos.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private String readGpioNode(File file)
  {
    BufferedReader reader = null;
    String string = null;
    try {
      reader = new BufferedReader(new FileReader(file));

      string = reader.readLine();

      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return string;
  }
}