package stest.tron.wallet.dailybuild.transaction;

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
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;


@Slf4j
public class WalletTestBlock004 {

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private final String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
  public ManagedChannel channelSolidity = null;
  public WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
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
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext().build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    txId =
        PublicMethed.sendcoinGetTransactionId(
            testAddress, 10000L, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("txId:" + txId);
  }

  @Test(enabled = true, description = "Get block by limit next.")
  public void testGetBlockByLimitNext() {
    //
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    while (currentBlockNum <= 5) {
      logger.info("Now has very little block, Please wait");
      currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    }

    GrpcAPI.BlockLimit.Builder builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(2);
    builder.setEndNum(4);
    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    Optional<GrpcAPI.BlockList> getBlockByLimitNext = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLimitNext.isPresent());
    Assert.assertTrue(getBlockByLimitNext.get().getBlockCount() == 2);
    logger.info(
        Long.toString(
            getBlockByLimitNext.get().getBlock(0).getBlockHeader().getRawData().getNumber()));
    logger.info(
        Long.toString(
            getBlockByLimitNext.get().getBlock(1).getBlockHeader().getRawData().getNumber()));
    Assert.assertTrue(
        getBlockByLimitNext.get().getBlock(0).getBlockHeader().getRawData().getNumber() < 4);
    Assert.assertTrue(
        getBlockByLimitNext.get().getBlock(1).getBlockHeader().getRawData().getNumber() < 4);
    Assert.assertTrue(getBlockByLimitNext.get().getBlock(0).hasBlockHeader());
    Assert.assertTrue(getBlockByLimitNext.get().getBlock(1).hasBlockHeader());
    Assert.assertFalse(
        getBlockByLimitNext
            .get()
            .getBlock(0)
            .getBlockHeader()
            .getRawData()
            .getParentHash()
            .isEmpty());
    Assert.assertFalse(
        getBlockByLimitNext
            .get()
            .getBlock(1)
            .getBlockHeader()
            .getRawData()
            .getParentHash()
            .isEmpty());
  }

  @Test(enabled = true, description = "Get block by limit next with type is zero.")
  public void testGetBlockByLimitNextWithTypeIsZero() throws InterruptedException {
    Long blockNumber =
        PublicMethed.getTransactionInfoById(txId, blockingStubFull).get().getBlockNumber();
    logger.info("blockNumber:" + blockNumber);
    GrpcAPI.BlockLimit.Builder builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(blockNumber);
    builder.setEndNum(blockNumber + 1);
    builder.setType(0);

    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    Optional<GrpcAPI.BlockList> getBlockByLimitNext = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLimitNext.get().getBlockCount() == 1);
    Long num = getBlockByLimitNext.get().getBlock(0).getBlockHeader().getRawData().getNumber();
    Assert.assertEquals(blockNumber, num);
    Assert.assertTrue(!getBlockByLimitNext.get().getBlock(0).getTransactionsList().isEmpty());
  }

  @Test(enabled = false, description = "Get block by limit next with type is one.")
  public void testGetBlockByLimitNextWithTypeIsOne() throws InterruptedException {
    Long blockNumber =
        PublicMethed.getTransactionInfoById(txId, blockingStubFull).get().getBlockNumber();
    logger.info("blockNumber:" + blockNumber);
    GrpcAPI.BlockLimit.Builder builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(blockNumber);
    builder.setEndNum(blockNumber + 1);
    builder.setType(1);
    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    Optional<GrpcAPI.BlockList> getBlockByLimitNext = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLimitNext.get().getBlockCount() == 1);
    Long num = getBlockByLimitNext.get().getBlock(0).getBlockHeader().getRawData().getNumber();
    Assert.assertEquals(blockNumber, num);
    Assert.assertTrue(getBlockByLimitNext.get().getBlock(0).getTransactionsList().isEmpty());
  }

  @Test(enabled = false, description = "Get block by exception limit next.")
  public void testGetBlockByExceptionLimitNext() {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    while (currentBlockNum <= 5) {
      logger.info("Now has very little block, Please wait");
      currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    }

    // From -1 to 1
    GrpcAPI.BlockLimit.Builder builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(-1);
    builder.setEndNum(1);
    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    Optional<GrpcAPI.BlockList> getBlockByLimitNext = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLimitNext.get().getBlockCount() == 0);

    // From 3 to 3
    builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(3);
    builder.setEndNum(3);
    blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    getBlockByLimitNext = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLimitNext.get().getBlockCount() == 0);

    // From 4 to 2
    builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(4);
    builder.setEndNum(2);
    blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    getBlockByLimitNext = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLimitNext.get().getBlockCount() == 0);

    // From 999999990 to 999999999
    builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(999999990);
    builder.setEndNum(999999999);
    blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    getBlockByLimitNext = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLimitNext.get().getBlockCount() == 0);
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
