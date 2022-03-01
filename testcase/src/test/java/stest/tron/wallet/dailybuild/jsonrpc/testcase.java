package stest.tron.wallet.dailybuild.jsonrpc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.junit.Assert;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.JsonRpcBase;

public class testcase {
  AtomicInteger atomicInteger = new AtomicInteger(0);
  ConcurrentHashSet<String> concurrentHashSet = new ConcurrentHashSet();
  String[] topicsArray = {
      "0x21f80ad16517f8cc77e4eada2d83f2b2",
      "0x21800cfb29623d7a93d03eec6bd38036",
      "0xca52b1edad238bf6e0efcc017e82698c",
      "0xc4e9b7615e37a055d8c033921a2a875b",
      "0x6c86e7a89f0fde45c330133fa5d6057c"
  };

  @Test(enabled = true, threadPoolSize = 7, invocationCount = 7)
  public void test06JsonRpcApiTestForEthEstimateGas() throws Exception {

    System.out.println(Thread.currentThread().getName().substring(Thread.currentThread().getName().length()-1));


    JsonArray param = new JsonArray();
    //param.add(topicsArray[Integer.valueOf(Thread.currentThread().getName().substring(Thread.currentThread().getName().length()-1))-1]);
    param.add("0x609785f327ea981fa6e3c8cb1f8472f2");
    System.out.println(param.toString());
    JsonArray params = new JsonArray();
    params.addAll(param);
    JsonObject requestBody = JsonRpcBase.getJsonRpcBody("eth_getFilterChanges", params);



    Integer total = 0;
    while (true) {
      HttpResponse response = getJsonRpc("39.106.110.245:50545", requestBody);
      JSONObject responseContent = HttpMethed.parseResponseContent(response);
      try {
        JSONArray jsonArray = responseContent.getJSONArray("result");

        if(jsonArray.size() != 0 ) {
          for(int i = 0; i < jsonArray.size();i++) {
            if(concurrentHashSet.contains(jsonArray.getJSONObject(i).getString("transactionHash"))) {
              System.out.println(jsonArray.getJSONObject(i).getString("transactionHash") + " is wrong");
              System.exit(1);
            }

            concurrentHashSet.add(jsonArray.getJSONObject(i).getString("transactionHash"));

          }
          System.out.println(atomicInteger.addAndGet(jsonArray.size()));
        }

      } catch (Exception e) {
        HttpMethed.printJsonContent(responseContent);
        System.out.println("query failed");
      }

    }


  }


  @Test
  public void aaaaa() throws Exception {
    MyThread event1 = new MyThread("0xc1212face8030981dba8c87177346bcc");
    MyThread event2 = new MyThread("0xf82da7256633956a39e9e83bab6409b0");
    MyThread event3 = new MyThread("0x89843c4c1d868e4db77ea1e45aedd4fd");
    MyThread event4 = new MyThread("0x758d872e4974a1339d805e4a05af9b80");
    MyThread event5 = new MyThread("0x2616ef03fd6c9509a40c30ad5749f467");

    Thread t1 = new Thread(event1);
    Thread t2 = new Thread(event2);
    Thread t3 = new Thread(event3);
    Thread t4 = new Thread(event4);
    Thread t5 = new Thread(event5);

    t1.start();
    t2.start();
    t3.start();
    t4.start();
    t5.start();
  }

    class MyThread implements Runnable{ // 实现Runnable接口，作为线程的实现类
      private String topics ;       // 表示线程的名称
      public MyThread(String topics){
               this.topics = topics ;      // 通过构造方法配置name属性
           }
      public void run(){  // 覆写run()方法，作为线程 的操作主体

        JsonArray param = new JsonArray();
        param.add(this.topics);
        System.out.println("topics:" + topics);

        JsonArray params = new JsonArray();
        params.addAll(param);
        JsonObject requestBody = JsonRpcBase.getJsonRpcBody("eth_getFilterChanges", params);

        while (true) {
          HttpResponse response = getJsonRpc("39.106.110.245:50545", requestBody);
          JSONObject responseContent = HttpMethed.parseResponseContent(response);
          try {
            Integer current = responseContent.getJSONArray("result").size();
            System.out.println("current total Query Num :" + atomicInteger.addAndGet(current));
          } catch (Exception e) {
            System.out.println("query failed");
          }

        }
           }
 };

  public static HttpResponse getJsonRpc(String jsonRpcNode, JsonObject jsonRpcObject) {
    try {
      String requestUrl = "http://" + jsonRpcNode + "/jsonrpc";
      HttpResponse response = createConnect(requestUrl, jsonRpcObject);
      return response;
    } catch (Exception e) {
      e.printStackTrace();
      //httppost.releaseConnection();
      return null;
    }

  }


  static Integer connectionTimeout = Configuration.getByPath("testng.conf")
      .getInt("defaultParameter.httpConnectionTimeout");
  static Integer soTimeout = Configuration.getByPath("testng.conf")
      .getInt("defaultParameter.httpSoTimeout");


  public static HttpResponse createConnect(String url, JsonObject requestBody) {
    HttpClient httpClient;

    PoolingClientConnectionManager pccm = new PoolingClientConnectionManager();
    pccm.setDefaultMaxPerRoute(80);
    pccm.setMaxTotal(100);

    httpClient = new DefaultHttpClient(pccm);

    try {
      HttpPost httppost;
      httpClient.getParams()
          .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httppost = new HttpPost(url);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      if (requestBody != null) {
        StringEntity entity = new StringEntity(requestBody.toString(), Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httppost.setEntity(entity);
      }

      HttpResponse response = httpClient.execute(httppost);
      return response;
    } catch (Exception e) {
      e.printStackTrace();
      //httppost.releaseConnection();
      return null;
    }

  }



}
