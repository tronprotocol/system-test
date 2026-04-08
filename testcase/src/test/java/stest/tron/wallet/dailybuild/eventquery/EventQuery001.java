package stest.tron.wallet.dailybuild.eventquery;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.zeromq.ZMQ;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import zmq.ZMQ.Event;

@Slf4j
public class EventQuery001 extends TronBaseTest {  private final String testKey003 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  private String eventnode =
      Configuration.getByPath("testng.conf").getStringList("eventnode.ip.list").get(0);

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {  }
  @Test(enabled = true, description = "Event query for block", groups = {"daily", "serial"})
  public void test01EventQueryForBlock() {
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);

    req.subscribe("blockTrigger");
  final ZMQ.Socket moniter = context.socket(ZMQ.PAIR);
    moniter.connect("inproc://reqmoniter");
    new Thread(
            new Runnable() {
              public void run() {
                while (true) {
                  Event event = Event.read(moniter.base());
                  System.out.println(event.event + "  " + event.addr);
                }
              }
            })
        .start();
    req.connect(eventnode);
    req.setReceiveTimeOut(10000);
  String blockMessage = "";

    Integer retryTimes = 20;
    while (retryTimes-- > 0) {
      byte[] message = req.recv();
      if (message != null) {
        // System.out.println("receive : " + new String(message));
        blockMessage = new String(message);
        if (!blockMessage.equals("blockTrigger") && !blockMessage.isEmpty()) {
          break;
        }
      }
    }

    Assert.assertTrue(retryTimes > 0);
    logger.info("block message:" + blockMessage);
    JSONObject blockObject = JSONObject.parseObject(blockMessage);
    Assert.assertTrue(blockObject.containsKey("timeStamp"));
    Assert.assertEquals(blockObject.getString("triggerName"), "blockTrigger");
    Assert.assertTrue(blockObject.getLong("blockNumber") > 0);
    Assert.assertTrue(blockObject.containsKey("blockHash"));
    Assert.assertTrue(blockObject.getInteger("transactionSize") >= 0);
  }

  @Test(enabled = true, description = "Event query for block on solidity", groups = {"daily", "serial"})
  public void test02EventQueryForBlockOnSolidity() {
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);

    req.subscribe("solidityTrigger");
  final ZMQ.Socket moniter = context.socket(ZMQ.PAIR);
    moniter.connect("inproc://reqmoniter");
    new Thread(
            new Runnable() {
              public void run() {
                while (true) {
                  Event event = Event.read(moniter.base());
                  System.out.println(event.event + "  " + event.addr);
                }
              }
            })
        .start();
    req.connect(eventnode);
    req.setReceiveTimeOut(10000);
  String blockMessage = "";

    Integer retryTimes = 20;

    while (retryTimes-- > 0) {
      byte[] message = req.recv();
      if (message != null) {
        System.out.println("receive : " + new String(message));
        blockMessage = new String(message);
        if (!blockMessage.equals("solidityTrigger") && !blockMessage.isEmpty()) {
          break;
        }
      }
    }

    Assert.assertTrue(retryTimes > 0);
    logger.info("block message:" + blockMessage);
    JSONObject blockObject = JSONObject.parseObject(blockMessage);
    Assert.assertTrue(blockObject.containsKey("timeStamp"));
    Assert.assertEquals(blockObject.getString("triggerName"), "solidityTrigger");
    Assert.assertTrue(blockObject.getLong("latestSolidifiedBlockNumber") > 0);
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {  }
}
