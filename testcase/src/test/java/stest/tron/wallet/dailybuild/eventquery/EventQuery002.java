package stest.tron.wallet.dailybuild.eventquery;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
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
public class EventQuery002 extends TronBaseTest {  private final String testKey003 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  private String eventnode =
      Configuration.getByPath("testng.conf").getStringList("eventnode.ip.list").get(0);
  byte[] contractAddress;
  String txid;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] event001Address = ecKey1.getAddress();
  String event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  List<String> transactionIdList = null;

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {    ecKey1 = new ECKey(Utils.getRandom());
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
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Event query for transaction", groups = {"daily", "serial"})
  public void test01EventQueryForTransaction() {
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);

    req.subscribe("transactionTrigger");
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
                contractAddress,
                "triggerUintEvent()",
                "#",
                false,
                0,
                maxFeeLimit,
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

        if (!transactionMessage.equals("transactionTrigger")
            && !transactionMessage.isEmpty()
            && transactionMessage.contains("transactionId")) {
         break;
        }
      } else {
        sendTransaction = true;
      }
    }
    logger.info("Final transaction message:" + transactionMessage);
    Assert.assertTrue(retryTimes > 0);

    JSONObject blockObject = JSONObject.parseObject(transactionMessage);
    Assert.assertTrue(blockObject.containsKey("timeStamp"));
    Assert.assertEquals(blockObject.getString("triggerName"), "transactionTrigger");
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {  }
}
