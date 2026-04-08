package stest.tron.wallet.dailybuild.manual;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;

import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
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
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestBlock001 extends TronBaseTest {
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  /**
   * constructor.
   */
  

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    initSolidityChannel();
  // Add metadata as grpc headers
    Metadata headers = new Metadata();
    headers.put(Metadata.Key.of("content-length", Metadata.ASCII_STRING_MARSHALLER), "5");
    headers
        .put(Metadata.Key.of("Content-Type", Metadata.ASCII_STRING_MARSHALLER), "application/grpc");
    headers
        .put(Metadata.Key.of("Host", Metadata.ASCII_STRING_MARSHALLER), "grpc.demo.com");
    headers
        .put(Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER), "testGroupAutoTest");
    headers
        .put(Metadata.Key.of("x-trace-path", Metadata.ASCII_STRING_MARSHALLER), "123 23123");
    headers
        .put(Metadata.Key.of("x-trace-name", Metadata.ASCII_STRING_MARSHALLER), "!@^&$!* ()^&%");
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] randomAddress = ecKey1.getAddress();
    headers
        .put(Metadata.Key.of("address-bin", Metadata.BINARY_BYTE_MARSHALLER), randomAddress);
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull)
        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
  //    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
  //        .usePlaintext()
  //        .build();
  //    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity)
  //        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now block from fullnode", groups = {"daily"})
  public void testCurrentBlock() {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(currentBlock.hasBlockHeader());
    Assert.assertFalse(currentBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getNumber() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("test getcurrentblock is " + Long.toString(currentBlock.getBlockHeader()
        .getRawData().getNumber()));
  //Improve coverage.
    currentBlock.equals(currentBlock);
    Block newBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    newBlock.equals(currentBlock);
    newBlock.hashCode();
    newBlock.getSerializedSize();
    newBlock.getTransactionsCount();
    newBlock.getTransactionsList();
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now block from solidity", groups = {"daily"})
  public void testCurrentBlockFromSolidity() {
    Block currentBlock = blockingStubSolidity
        .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(currentBlock.hasBlockHeader());
    Assert.assertFalse(currentBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getNumber() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("test getcurrentblock in soliditynode is " + Long.toString(currentBlock
        .getBlockHeader().getRawData().getNumber()));
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }

  /**
   * constructor.
   */
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
      String pubKey = loadPubKey(); //04 PubKey[128]
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

  /**
   * constructor.
   */
  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */
  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }
}


