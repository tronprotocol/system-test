package stest.tron.wallet.dailybuild.eventquery;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.zeromq.ZMQ;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EventQuery005 {

  private final String foundationKey =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);


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


  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext().build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode).usePlaintext().build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }


  @Test(enabled = true, description = "Test new Field for FreezeBalanceV2 in NativeQueue")
  public void test01EventQueryForTransactionFreezeBalanceV2() throws InterruptedException {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey1.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Long freezeAmount = maxFeeLimit * 40;
    Assert.assertTrue(
        PublicMethed.sendcoin(
            freezeAccount, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> transactionIdList = new ArrayList<>();

    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);

    req.subscribe("transactionTrigger");
    final ZMQ.Socket moniter = context.socket(ZMQ.PAIR);
    moniter.connect("inproc://reqmoniter");
    new Thread(
        new Runnable() {
          public void run() {
            while (true) {
              zmq.ZMQ.Event event = zmq.ZMQ.Event.read(moniter.base());
                logger.info("!!!!{}  {}", event.event, event.addr);
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
    transactionIdList = new ArrayList<>();
    ArrayList<byte[]> messageArray = new ArrayList<byte[]>();
    Thread readLoopThread = new Thread(
        new Runnable() {
          public void run() {
            while (true) {
              byte[] message = req.recv();
              messageArray.add(message);
            }
          }
        });
    readLoopThread.start();
    boolean success = false;

    while (retryTimes-- > 0) {
      if(success){
        break;
      }
      String txid = PublicMethed.freezeBalanceV2AndGetTxId(freezeAccount,
          maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
      transactionIdList.add(txid);
      PublicMethed.waitProduceNextBlock(blockingStubFull);

      for(byte[] message: messageArray){
        if (message != null) {
          transactionMessage = new String(message);
          logger.info("transaction message:" + transactionMessage);

          if (!transactionMessage.equals("transactionTrigger")
              && !transactionMessage.isEmpty()
              && transactionMessage.contains("transactionId")) {
            JSONObject data = JSON.parseObject(transactionMessage);
            String id = data.getString("transactionId");
            logger.info("trxId : " + id);
            if (transactionIdList.contains(id)) {
              logger.info("find target tx, begin to Assert and abort loop");
              Assert.assertEquals(data.getString("contractType"), "FreezeBalanceV2Contract");
              Assert.assertEquals(data.getString("fromAddress"), Base58.encode58Check(freezeAccount));
              Assert.assertEquals(data.getString("assetName"), "trx");
              Assert.assertEquals(data.getLong("assetAmount"), maxFeeLimit);
              success = true;
              break;
            }
          }
        }
      }
    }
    logger.info("Final transaction message:" + transactionMessage);
    logger.info("retryTimes: " + retryTimes);
    Assert.assertTrue(retryTimes >= 0);
  }



  @Test(enabled = true, description = "Test new Field for UnfreezeBalanceV2 in NativeQueue")
  public void test02EventQueryForTransactionUnfreezeBalanceV2() throws InterruptedException {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey1.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Long freezeAmount = maxFeeLimit * 20;
    Assert.assertTrue(
        PublicMethed.sendcoin(
            freezeAccount, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> transactionIdList = new ArrayList<>();

    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);

    req.subscribe("transactionTrigger");
    final ZMQ.Socket moniter = context.socket(ZMQ.PAIR);
    moniter.connect("inproc://reqmoniter");
    new Thread(
        new Runnable() {
          public void run() {
            while (true) {
              zmq.ZMQ.Event event = zmq.ZMQ.Event.read(moniter.base());
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
    transactionIdList = new ArrayList<>();
    ArrayList<byte[]> messageArray = new ArrayList<byte[]>();
    Thread readLoopThread = new Thread(
        new Runnable() {
          public void run() {
            while (true) {
              byte[] message = req.recv();
              messageArray.add(message);
            }
          }
        });
    readLoopThread.start();
    boolean success = false;

    Long unfreezeAmount = 10000000L;
    while (retryTimes-- > 0) {
      if (success){
        break;
      }
      String txid = PublicMethed.unFreezeBalanceV2AndGetTxId(freezeAccount,
          freezeAccountKey, unfreezeAmount, 0, blockingStubFull);

      transactionIdList.add(txid);
      PublicMethed.waitProduceNextBlock(blockingStubFull);


      for(byte[] message: messageArray){
        if (message != null) {
          transactionMessage = new String(message);
          logger.info("transaction message:" + transactionMessage);

          if (!transactionMessage.equals("transactionTrigger")
              && !transactionMessage.isEmpty()
              && transactionMessage.contains("transactionId")) {
            JSONObject data = JSON.parseObject(transactionMessage);
            String id = data.getString("transactionId");
            if(transactionIdList.contains(id)) {
              logger.info("find target tx, begin to Assert and abort loop");
              logger.info("trxId : " + data.getString("transactionId"));
              Assert.assertEquals(data.getString("contractType"), "UnfreezeBalanceV2Contract");
              Assert.assertEquals(data.getString("fromAddress"), Base58.encode58Check(freezeAccount));
              Assert.assertEquals(data.getString("assetName"), "trx");
              Assert.assertEquals(data.getLong("assetAmount"), unfreezeAmount);
              success = true;
              break;
            }
          }
        }
      }


    }
    logger.info("Final transaction message:" + transactionMessage);
    logger.info("retryTimes: " + retryTimes);
    Assert.assertTrue(retryTimes >= 0);
  }



  @Test(enabled = true, description = "Test new Field for DelegateResource in NativeQueue")
  public void test03EventQueryForTransactionDelegateResource() throws InterruptedException {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey1.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(freezeAccountKey);


    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] receiverAddress = ecKey2.getAddress();
    String receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(receiverKey);

    Long freezeAmount = maxFeeLimit * 20;
    Assert.assertTrue(
        PublicMethed.sendcoin(
            freezeAccount, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.sendcoin(
            receiverAddress, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);

    List<String> transactionIdList = new ArrayList<>();



    req.subscribe("transactionTrigger");
    final ZMQ.Socket moniter = context.socket(ZMQ.PAIR);
    moniter.connect("inproc://reqmoniter");
    new Thread(
        new Runnable() {
          public void run() {
            while (true) {
              zmq.ZMQ.Event event = zmq.ZMQ.Event.read(moniter.base());
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
    ArrayList<byte[]> messageArray = new ArrayList<byte[]>();
    Thread readLoopThread = new Thread(
        new Runnable() {
          public void run() {
            while (true) {
              byte[] message = req.recv();
              messageArray.add(message);
            }
          }
        });
    readLoopThread.start();
    boolean success = false;

    Long delegateAmount = 10000000L;
    while (retryTimes-- > 0) {
      if(success) {
        break;
      }
      String txid = PublicMethed.delegateResourceV2AndGetTxId(freezeAccount,
          delegateAmount, 0, receiverAddress, freezeAccountKey, blockingStubFull);

      transactionIdList.add(txid);
      PublicMethed.waitProduceNextBlock(blockingStubFull);

      for(byte[] message: messageArray) {
        if (message != null) {
          transactionMessage = new String(message);
          logger.info("transaction message:" + transactionMessage);

          if (!transactionMessage.equals("transactionTrigger")
              && !transactionMessage.isEmpty()
              && transactionMessage.contains("transactionId")) {
            JSONObject data = JSON.parseObject(transactionMessage);
            String id = data.getString("transactionId");
            if (transactionIdList.contains(id)) {
              logger.info("find target tx, begin to Assert and abort loop");
              logger.info("trxId : " + id);
              Assert.assertEquals(data.getString("contractType"), "DelegateResourceContract");
              Assert.assertEquals(data.getString("fromAddress"), Base58.encode58Check(freezeAccount));
              Assert.assertEquals(data.getString("toAddress"), Base58.encode58Check(receiverAddress));
              Assert.assertEquals(data.getString("assetName"), "trx");
              Assert.assertEquals(data.getLong("assetAmount"), delegateAmount);
              success = true;
              break;
            }

          }
        }
      }
    }
    logger.info("Final transaction message:" + transactionMessage);
    logger.info("retryTimes: " + retryTimes);
    Assert.assertTrue(retryTimes >= 0);
  }


  @Test(enabled = false, description = "Test new Field for UnDelegateResource in NativeQueue")
  public void test04EventQueryForTransactionUnDelegateResource() throws InterruptedException {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey1.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(freezeAccountKey);


    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] receiverAddress = ecKey2.getAddress();
    String receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(receiverKey);

    Long freezeAmount = maxFeeLimit * 20;
    Assert.assertTrue(
        PublicMethed.sendcoin(
            freezeAccount, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(
        PublicMethed.sendcoin(
            receiverAddress, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 1, freezeAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long delegateAmount = 20000000L;
    Long unDelegateAmount = 1000000L;

    PublicMethed.delegateResourceV2AndGetTxId(freezeAccount,
        delegateAmount, 1, receiverAddress, freezeAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> transactionIdList = new ArrayList<>();

    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);

    req.subscribe("transactionTrigger");
    final ZMQ.Socket moniter = context.socket(ZMQ.PAIR);
    moniter.connect("inproc://reqmoniter");
    new Thread(
        new Runnable() {
          public void run() {
            while (true) {
              zmq.ZMQ.Event event = zmq.ZMQ.Event.read(moniter.base());
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
        String txid = PublicMethed.unDelegateResourceV2AndGetTxId(freezeAccount,
            unDelegateAmount, 1, receiverAddress, freezeAccountKey, blockingStubFull);

        transactionIdList.add(txid);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        if (PublicMethed.getTransactionInfoById(txid, blockingStubFull).get().getResultValue()
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
          JSONObject data = JSON.parseObject(transactionMessage);
          String id = data.getString("transactionId");
          if (transactionIdList.contains(id)) {
            logger.info("find target tx, begin to Assert and abort loop");
            logger.info("trxId : " + data.getString("transactionId"));
            Assert.assertEquals(data.getString("contractType"), "UnDelegateResourceContract");
            Assert.assertEquals(data.getString("fromAddress"), Base58.encode58Check(freezeAccount));
            Assert.assertEquals(data.getString("toAddress"), Base58.encode58Check(receiverAddress));
            Assert.assertEquals(data.getString("assetName"), "trx");
            Assert.assertEquals(data.getLong("assetAmount"), unDelegateAmount);
            break;
          }

        }
      } else {
        sendTransaction = true;
      }
    }
    logger.info("Final transaction message:" + transactionMessage);
    logger.info("retryTimes: " + retryTimes);
    Assert.assertTrue(retryTimes >= 0);
  }




}
