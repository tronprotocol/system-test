package stest.tron.wallet.dailybuild.eventquery;

import com.alibaba.fastjson.JSONObject;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.zeromq.ZMQ;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;
import zmq.ZMQ.Event;

@Slf4j
public class EventQuery004 {

  private final String testKey002 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  byte[] contractAddress;
  byte[] contractAddress1;
  String txid;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] event001Address = ecKey1.getAddress();
  String event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
  private String eventnode =
      Configuration.getByPath("testng.conf").getStringList("eventnode.ip.list").get(0);
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(0);
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private Long maxFeeLimit =
      Configuration.getByPath("testng.conf").getLong("defaultParameter.maxFeeLimit");
  List<String> transactionIdList = null;

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode).usePlaintext(true).build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    ecKey1 = new ECKey(Utils.getRandom());
    event001Address = ecKey1.getAddress();
    event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(event001Key);

    Assert.assertTrue(
        PublicMethed.sendcoin(
            event001Address, maxFeeLimit * 30, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName = "addressDemo";
    String code =
        Configuration.getByPath("testng.conf").getString("code.code_ContractEventAndLog1");
    String abi = Configuration.getByPath("testng.conf").getString("abi.abi_ContractEventAndLog1");
    contractAddress =
        PublicMethed.deployContract(
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
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath2 = "src/test/resources/soliditycode/contractTestLog.sol";
    String contractName1 = "C";
    HashMap retMap2 = PublicMethed.getBycodeAbi(filePath2, contractName1);
    String code1 = retMap2.get("byteCode").toString();
    String abi1 = retMap2.get("abI").toString();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    contractAddress1 =
        PublicMethed.deployContract(
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
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, priority=3, description = "Filter  contractTopic event query for contract log")
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
    req.setReceiveTimeOut(10000);
    String transactionMessage = "";
    Boolean sendTransaction = true;
    Integer retryTimes = 10;

    while (retryTimes-- > 0) {
      byte[] message = req.recv();
      if (sendTransaction) {
        txid =
            PublicMethed.triggerContract(
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
/*        if (PublicMethed.getTransactionInfoById(txid, blockingStubFull).get().getResultValue()
            == 0) {
          sendTransaction = false;
        }*/
      }

      if (message != null) {
        transactionMessage = new String(message);
        if (!transactionMessage.equals("contractLogTrigger") && !transactionMessage.isEmpty()) {
          break;
        }
      }
    }

    Assert.assertTrue(retryTimes < 0);
  }

  @Test(enabled = true, priority=3, description = "Filter  contractTopic event query for solidity contract log")
  public void test02filterContractTopicEventQueryForContractSolidityLog() {
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
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
    Integer retryTimes = 10;
    String txid1 = "";
    String txid2 = "";
    String txid3 = "";

    while (retryTimes-- > 0) {
      byte[] message = req.recv();
      if (sendTransaction) {
        txid1 =
            PublicMethed.triggerContract(
                contractAddress,
                "depositForLog()",
                "#",
                false,
                1L,
                100000000L,
                event001Address,
                event001Key,
                blockingStubFull);
        txid2 =
            PublicMethed.triggerContract(
                contractAddress,
                "depositForLog()",
                "#",
                false,
                1L,
                100000000L,
                event001Address,
                event001Key,
                blockingStubFull);
        txid3 =
            PublicMethed.triggerContract(
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
/*        if (PublicMethed.getTransactionInfoById(txid, blockingStubFull).get().getResultValue()
            == 0) {
          sendTransaction = false;
        }*/
      }

      if (message != null) {

        transactionMessage = new String(message);
        logger.info("transaction message:" + transactionMessage);
        if (!transactionMessage.equals("solidityLogTrigger") && !transactionMessage.isEmpty()) {
          break;
        }
      }
    }
    Assert.assertTrue(retryTimes < 0);
  }

  @Test(enabled = true, priority=3, description = "Event query for contract log")
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
      byte[] message = req.recv();
      if (sendTransaction) {
        txid =
            PublicMethed.triggerContract(
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
        if (PublicMethed.getTransactionInfoById(txid, blockingStubFull).get().getResultValue()
            == 0) {
          sendTransaction = false;
        }
      }

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

  @Test(enabled = true, priority=3, description = "Event query for solidity contract log")
  public void test04EventQueryForContractSolidityLog() {
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
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
    Integer retryTimes = 40;
    String txid1 = "";
    String txid2 = "";
    String txid3 = "";
    transactionIdList = new ArrayList<>();
    while (retryTimes-- > 0) {
      byte[] message = req.recv();
      if (sendTransaction) {
        txid1 =
            PublicMethed.triggerContract(
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
            PublicMethed.triggerContract(
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
            PublicMethed.triggerContract(
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
        if (PublicMethed.getTransactionInfoById(txid, blockingStubFull).get().getResultValue()
            == 0) {
          sendTransaction = false;
        }
      }

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
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
