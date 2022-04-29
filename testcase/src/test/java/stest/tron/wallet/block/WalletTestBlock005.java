package stest.tron.wallet.block;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;
import zmq.socket.pubsub.Pub;

// import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion;

// import stest.tron.wallet.common.client.AccountComparator;

@Slf4j
public class WalletTestBlock005 {

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  public ManagedChannel channelSolidity = null;
  public WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
  private String solidityNode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(0);
  private final String testKey002 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress = ecKey1.getAddress();
  private String txId = null;

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  /** constructor. */
  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelSolidity = ManagedChannelBuilder.forTarget(solidityNode).usePlaintext(true).build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    /* txId =
        PublicMethed.sendcoinGetTransactionId(
            testAddress, 10000l, fromAddress, testKey002, blockingStubFull);
    logger.info("txId:" + txId);*/
  }

  @Test(enabled = true)
  public void testGetBlockByLatestNum() {
    //
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    while (currentBlockNum <= 5) {
      logger.info("Now the block num is " + Long.toString(currentBlockNum) + " Please wait");
      currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    }

    NumberMessage numberMessage = NumberMessage.newBuilder().setNum(3).build();
    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLatestNum(numberMessage);
    Optional<GrpcAPI.BlockList> getBlockByLatestNum = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLatestNum.isPresent());
    Assert.assertTrue(getBlockByLatestNum.get().getBlockCount() == 3);
    Assert.assertTrue(getBlockByLatestNum.get().getBlock(0).hasBlockHeader());
    Assert.assertTrue(
        getBlockByLatestNum.get().getBlock(1).getBlockHeader().getRawData().getNumber() > 0);
    Assert.assertFalse(
        getBlockByLatestNum
            .get()
            .getBlock(2)
            .getBlockHeader()
            .getRawData()
            .getParentHash()
            .isEmpty());
    logger.info("TestGetBlockByLatestNum ok!!!");
  }

  @Test(enabled = true)
  public void testGetBlockByLatestNumWithTypeIsZero() throws InterruptedException {
    txId =
        PublicMethed.sendcoinGetTransactionId(
            testAddress, 10000L, fromAddress, testKey002, blockingStubFull);
    logger.info("txId:" + txId);
    Thread.sleep(2000);
    Long blockNumber =
        PublicMethed.getTransactionInfoById(txId, blockingStubFull).get().getBlockNumber();
    logger.info("blockNumber:" + blockNumber);
    Protocol.Block currentBlock =
        blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long nowBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    logger.info("nowBlockNum:" + nowBlockNum);
    long numDiff = nowBlockNum - nowBlockNum + 3;
    logger.info("numDiff:" + numDiff);
    NumberMessage.Builder numberMessage = NumberMessage.newBuilder();
    numberMessage.setNum(numDiff);
    numberMessage.setType(0);
    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLatestNum(numberMessage.build());
    Optional<GrpcAPI.BlockList> getBlockByLatestNum = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLatestNum.isPresent());
    Assert.assertTrue(getBlockByLatestNum.get().getBlockCount() == numDiff);
    boolean flag = false;
    for (int i = 0; i < numDiff; i++) {
      Assert.assertTrue(getBlockByLatestNum.get().getBlock(i).hasBlockHeader());
      Assert.assertTrue(
          getBlockByLatestNum.get().getBlock(i).getBlockHeader().getRawData().getNumber() > 0);
      Assert.assertFalse(
          getBlockByLatestNum
              .get()
              .getBlock(i)
              .getBlockHeader()
              .getRawData()
              .getParentHash()
              .isEmpty());
      if (!getBlockByLatestNum.get().getBlock(i).getTransactionsList().isEmpty()) {
        flag = true;
        break;
      }
    }
    Assert.assertTrue(flag);

    logger.info("TestGetBlockByLatestNum ok!!!");
  }

  @Test(enabled = true)
  public void testGetBlockByLatestNumWithTypeIsOne() throws InterruptedException {
    txId =
        PublicMethed.sendcoinGetTransactionId(
            testAddress, 10000L, fromAddress, testKey002, blockingStubFull);
    logger.info("txId:" + txId);
    Thread.sleep(2000);
    Long blockNumber =
        PublicMethed.getTransactionInfoById(txId, blockingStubFull).get().getBlockNumber();
    logger.info("blockNumber:" + blockNumber);
    Protocol.Block currentBlock =
        blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long nowBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    logger.info("nowBlockNum:" + nowBlockNum);
    long numDiff = nowBlockNum - nowBlockNum + 3;
    logger.info("numDiff:" + numDiff);
    NumberMessage.Builder numberMessage = NumberMessage.newBuilder();
    numberMessage.setNum(numDiff);
    numberMessage.setType(1);
    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLatestNum(numberMessage.build());
    Optional<GrpcAPI.BlockList> getBlockByLatestNum = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLatestNum.isPresent());
    Assert.assertTrue(getBlockByLatestNum.get().getBlockCount() == numDiff);
    boolean flag = true;
    for (int i = 0; i < numDiff; i++) {
      Assert.assertTrue(getBlockByLatestNum.get().getBlock(i).hasBlockHeader());
      Assert.assertTrue(
          getBlockByLatestNum.get().getBlock(i).getBlockHeader().getRawData().getNumber() > 0);
      Assert.assertFalse(
          getBlockByLatestNum
              .get()
              .getBlock(i)
              .getBlockHeader()
              .getRawData()
              .getParentHash()
              .isEmpty());
      if (!getBlockByLatestNum.get().getBlock(i).getTransactionsList().isEmpty()) {
        flag = false;
        break;
      }
    }
    Assert.assertTrue(flag);

    logger.info("TestGetBlockByLatestNum ok!!!");
  }

  @Test(enabled = true)
  public void testGetBlockByExceptionNum() {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    while (currentBlockNum <= 5) {
      logger.info("Now the block num is " + Long.toString(currentBlockNum) + " Please wait");
      currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    }
    NumberMessage numberMessage = NumberMessage.newBuilder().setNum(-1).build();
    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLatestNum(numberMessage);
    Optional<GrpcAPI.BlockList> getBlockByLatestNum = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLatestNum.get().getBlockCount() == 0);

    numberMessage = NumberMessage.newBuilder().setNum(0).build();
    blockList = blockingStubFull.getBlockByLatestNum(numberMessage);
    getBlockByLatestNum = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLatestNum.get().getBlockCount() == 0);

    numberMessage = NumberMessage.newBuilder().setNum(100).build();
    blockList = blockingStubFull.getBlockByLatestNum(numberMessage);
    getBlockByLatestNum = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLatestNum.get().getBlockCount() == 0);
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /** constructor. */
  public Account queryAccount(String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    if (ecKey == null) {
      String pubKey = loadPubKey(); // 04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  /** constructor. */
  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /** constructor. */
  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());
  }
}
