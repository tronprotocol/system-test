package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.PublicMethod;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

@Slf4j
public class HttpTestContentLength {


  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);


  @Test(enabled = true, description = "http request with Negative content-length", groups = {"daily", "serial"})
  public void test001RequestWithNegativeContentLength() {
    try {
      String cmd = String.format("curl -s --location http://%s/wallet/getnowblock --header Content-Length:-1", httpnode);
      String returnString = PublicMethod.exec(cmd);
      logger.info(returnString);
      Assert.assertTrue(returnString
          .contains("<h1>Bad Message 400</h1><pre>reason: Invalid Content-Length Value</pre>"));

      String cmd2 = String.format("curl -s --location http://%s/wallet/getnowblock --header Content-Length:+100", httpnode);
      String returnString2 = PublicMethod.exec(cmd2);
      logger.info(returnString2);
      Assert.assertTrue(returnString2
          .contains("<h1>Bad Message 400</h1><pre>reason: Invalid Content-Length Value</pre>"));
    }catch (Exception e) {
      e.printStackTrace();
    }
  }



}
