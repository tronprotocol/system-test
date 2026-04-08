package stest.tron.wallet.dailybuild.eventquery;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.zeromq.ZMQ;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import zmq.ZMQ.Event;

@Slf4j
public class EventQuery004 extends TronBaseTest {  private final String testKey003 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  byte[] contractAddress;
  byte[] contractAddress1;
  String txid;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] event001Address = ecKey1.getAddress();
  String event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String eventnode =
      Configuration.getByPath("testng.conf").getStringList("eventnode.ip.list").get(0);
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(0);  List<String> transactionIdList = null;

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();    ecKey1 = new ECKey(Utils.getRandom());
    event001Address = ecKey1.getAddress();
    event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethod.printAddress(event001Key);

    Assert.assertTrue(
        PublicMethod.sendcoin(
            event001Address, maxFeeLimit * 30, foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String contractName = "addressDemo";
  String code =
        Configuration.getByPath("testng.conf").getString("code.code_ContractEventAndLog1");
  String abi = Configuration.getByPath("testng.conf").getString("abi.abi_ContractEventAndLog1");
    contractAddress =
        PublicMethod.deployContract(
            contractName,
            abi,
            code,
            "",
            maxFeeLimit,
            0L,
            50,
            null,
            event001Key,
            event001Address,
            blockingStubFull);
  String filePath2 = "src/test/resources/soliditycode/contractTestLog.sol";
  String contractName1 = "C";
    HashMap retMap2 = PublicMethod.getBycodeAbi(filePath2, contractName1);
  String code1 = retMap2.get("byteCode").toString();
  String abi1 = retMap2.get("abI").toString();
    contractAddress1 =
        PublicMethod.deployContract(
            contractName,
            abi1,
            code1,
            "",
            maxFeeLimit,
            0L,
            50,
            null,
            event001Key,
            event001Address,
            blockingStubFull);
  }

  @Test(enabled = true, priority=3, description = "Filter  contractTopic event query for contract log", groups = {"daily", "serial"})
  public void test01filterContractTopicEventQueryForContractLog() {
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);
    req.subscribe("contractLogTrigger");
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
    req.setReceiveTimeOut(5000);
  String transactionMessage = "";
    Boolean sendTransaction = true;
    Integer retryTimes = 3;

    while (retryTimes-- > 0) {
      if (sendTransaction) {
        txid =
            PublicMethod.triggerContract(
                contractAddress,
                "depositForLog()",
                "#",
                false,
                1L,
                100000000L,
                event001Address,
                event001Key,
                blockingStubFull);
        logger.info(txid);

/*        if (PublicMethod.getTransactionInfoById(txid, blockingStubFull).get().getResultValue()
            == 0) {
          sendTransaction = false;
        }*/
      }
      byte[] message = req.recv();

      if (message != null) {
        transactionMessage = new String(message);
        if (!transactionMessage.equals("contractLogTrigger") && !transactionMessage.isEmpty()) {
          logger.info(retryTimes.toString() + " contractLogTrigger: " + transactionMessage);
          break;
        }
      }
    }
    //test native event filter function , should not find message in zmq
    Assert.assertTrue(retryTimes < 0);
  }

  @Test(enabled = true, priority=3, description = "Filter  contractTopic event query for solidity contract log", groups = {"daily", "serial"})
  public void test02filterContractTopicEventQueryForContractSolidityLog() {
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);

    req.subscribe("solidityLogTrigger");
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
    req.setReceiveTimeOut(5000);
  String transactionMessage = "";
    Boolean sendTransaction = true;
    Integer retryTimes = 3;
  String txid1 = "";

    while (retryTimes-- > 0) {
      if (sendTransaction) {
        txid1 =
            PublicMethod.triggerContract(
                contractAddress,
                "depositForLog()",
                "#",
                false,
                1L,
                100000000L,
                event001Address,
                event001Key,
                blockingStubFull);
        logger.info(txid1);
/*        if (PublicMethod.getTransactionInfoById(txid, blockingStubFull).get().getResultValue()
            == 0) {
          sendTransaction = false;
        }*/
      }
      byte[] message = req.recv();


      if (message != null) {

        transactionMessage = new String(message);
        logger.info("transaction message:" + transactionMessage);
        if (!transactionMessage.equals("solidityLogTrigger") && !transactionMessage.isEmpty()) {
          break;
        }
      }
    }
    //test native event filter function, should not find message in zmq
    Assert.assertTrue(retryTimes < 0);
  }

  @Test(enabled = true, priority=3, description = "Event query for contract log", groups = {"daily", "serial"})
  public void test03EventQueryForContractLog() {
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);

    req.subscribe("contractLogTrigger");
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
  String transactionMessage = "";
    Boolean sendTransaction = true;
    Integer retryTimes = 20;
    transactionIdList = new ArrayList<>();
    while (retryTimes-- > 0) {
      if (sendTransaction) {
        txid =
            PublicMethod.triggerContract(
                contractAddress1,
                "depositForLog()",
                "#",
                false,
                1L,
                100000000L,
                event001Address,
                event001Key,
                blockingStubFull);
        logger.info(txid);
        transactionIdList.add(txid);
        if (PublicMethod.getTransactionInfoById(txid, blockingStubFull).get().getResultValue()
            == 0) {
          sendTransaction = false;
        }
      }
      byte[] message = req.recv();

      if (message != null) {
        transactionMessage = new String(message);
        logger.info("transaction message:" + transactionMessage);

        if (!transactionMessage.equals("contractLogTrigger")
            && !transactionMessage.isEmpty()
            && transactionMessage.contains("transactionId")
            && transactionIdList.contains(
                JSONObject.parseObject(transactionMessage).getString("transactionId"))) {
          break;
        }
      }
    }
    logger.info("retryTimes:" + retryTimes);
    Assert.assertTrue(retryTimes > 0);
    logger.info("transaction message:" + transactionMessage);
    JSONObject blockObject = JSONObject.parseObject(transactionMessage);
    Assert.assertTrue(blockObject.containsKey("timeStamp"));
    Assert.assertEquals(blockObject.getString("triggerName"), "contractLogTrigger");
  }

  @Test(enabled = true, priority=3, description = "Event query for solidity contract log", groups = {"daily", "serial"})
  public void test04EventQueryForContractSolidityLog() {
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);

    req.subscribe("solidityLogTrigger");
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
  String transactionMessage = "";
    Boolean sendTransaction = true;
    Integer retryTimes = 20;
  String txid1 = "";
  String txid2 = "";
  String txid3 = "";
    transactionIdList = new ArrayList<>();
    while (retryTimes-- > 0) {
      if (sendTransaction) {
        txid1 =
            PublicMethod.triggerContract(
                contractAddress1,
                "depositForLog()",
                "#",
                false,
                1L,
                100000000L,
                event001Address,
                event001Key,
                blockingStubFull);
        transactionIdList.add(txid1);
        txid2 =
            PublicMethod.triggerContract(
                contractAddress1,
                "depositForLog()",
                "#",
                false,
                1L,
                100000000L,
                event001Address,
                event001Key,
                blockingStubFull);
        transactionIdList.add(txid2);
        txid3 =
            PublicMethod.triggerContract(
                contractAddress1,
                "depositForLog()",
                "#",
                false,
                1L,
                100000000L,
                event001Address,
                event001Key,
                blockingStubFull);
        transactionIdList.add(txid3);
        logger.info(txid);
        transactionIdList.add(txid);
        if (PublicMethod.getTransactionInfoById(txid, blockingStubFull).get().getResultValue()
            == 0) {
          sendTransaction = false;
        }
      }
      byte[] message = req.recv();

      if (message != null) {

        transactionMessage = new String(message);
        logger.info("transaction message:" + transactionMessage);

        if (!transactionMessage.equals("solidityLogTrigger")
            && !transactionMessage.isEmpty()
            && transactionMessage.contains("solidityLogTrigger")
            && transactionMessage.contains("transactionId")
            && transactionIdList.contains(
                JSONObject.parseObject(transactionMessage).getString("transactionId"))) {
          break;
        }
      } else {
        sendTransaction = true;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    logger.info("transaction message:" + transactionMessage);
    JSONObject blockObject = JSONObject.parseObject(transactionMessage);
    Assert.assertTrue(blockObject.containsKey("timeStamp"));
    Assert.assertEquals(blockObject.getString("triggerName"), "solidityLogTrigger");
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {  }
}
