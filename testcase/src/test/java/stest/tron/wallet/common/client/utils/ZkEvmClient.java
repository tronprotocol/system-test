package stest.tron.wallet.common.client.utils;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.StringUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.BatchRequest;
import org.web3j.protocol.core.BatchResponse;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.events.Notification;


public class ZkEvmClient {
  private static volatile Web3j web3j;
  private static volatile Admin admin;
  private ZkEvmClient() {
  }

  public static Web3j getClient() {
    if (web3j == null) {
      Class var0 = ZkEvmClient.class;
      synchronized(ZkEvmClient.class) {
        if (web3j == null) {

          web3j = Web3j.build(new HttpService("http://54.160.226.39:8123"));
        }
      }
    }

    return web3j;
  }

  static {
    try {
      System.out.println(">>>>>>>> web3JClientHost:" + " http://54.160.226.39:8123");
    } catch (Exception var4) {
      var4.printStackTrace();
    }

  }

  public static String get64LengthStr(String str) {
    if(str.length() >= 64) {
      return str;
    }
    if(str.substring(0,2).equalsIgnoreCase("0x")) {
      str = str.substring(2);
    }
    while (str.length()!=64) {
      str = "0" + str;
    }

    return str;

  }

  public static String[] stringToStringArray(String src, int length) {
    //检查参数是否合法
    if (null == src || src.equals("")) {
      return null;
    }

    if (length <= 0) {
      return null;
    }
    int n = (src.length() + length - 1) / length; //获取整个字符串可以被切割成字符子串的个数
    String[] split = new String[n];
    for (int i = 0; i < n; i++) {
      if (i < (n - 1)) {
        split[i] = src.substring(i * length, (i + 1) * length);
      } else {
        split[i] = src.substring(i * length);
      }
    }
    return split;
  }


}
