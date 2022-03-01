package stest.tron.wallet.onlinestress;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.Utils;

/**
 * Md5 工具
 */
public class Md5Util {

  private static MessageDigest md5 = null;
  static {
    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }


  public static String getMd5(MessageDigest md5,String str) {
    byte[] bs = md5.digest(str.getBytes());
    StringBuilder sb = new StringBuilder(40);
    for(byte x:bs) {
      if((x & 0xff)>>4 == 0) {
        sb.append("0").append(Integer.toHexString(x & 0xff));
      } else {
        sb.append(Integer.toHexString(x & 0xff));
      }
    }
    return sb.toString();
  }

  @Test(enabled = true, threadPoolSize = 20, invocationCount = 20)
  public void createMd5Value() {
    MessageDigest md5 = null;
    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    Integer publicKeyCount = 100000;
    while (publicKeyCount-- > 0) {
      long timestamp = System.currentTimeMillis()/1000;
      ECKey ecKey1 = new ECKey(Utils.getRandom());
      byte[] event001Address = ecKey1.getAddress();

      String address = Base58.encode58Check(event001Address);
      String csvData = address + "," + timestamp + "," + getMd5(md5,address + "," + timestamp + "," + "CXFDNcFA3cc423Rc4F4FcVvcnvcvk4G5c3KSfGcF3F5FF54GV");
      writeDataToCsvFile("md5Value.csv",csvData);

    }
  }


  public static void writeDataToCsvFile(String fileName,String writeData) {

    {
      try {
        File file = new File(fileName);

        if (!file.exists()) {
          file.createNewFile();
        }
        FileWriter fileWritter = new FileWriter(file.getName(), true);
        fileWritter.write(writeData + "\n");
        fileWritter.close();
        //System.out.println("finish");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }



}