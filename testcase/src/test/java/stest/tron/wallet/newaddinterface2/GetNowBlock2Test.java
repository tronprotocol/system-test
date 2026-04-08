package stest.tron.wallet.newaddinterface2;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import java.math.BigInteger;
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
import stest.tron.wallet.common.client.utils.ECKey;

//import stest.tron.wallet.common.client.AccountComparator;

import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class GetNowBlock2Test extends TronBaseTest {  private ManagedChannel channelSolidity = null;  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {  }

  @Test
  public void testCurrentBlock2() {
    initSolidityChannel();
    //Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    GrpcAPI.BlockExtention currentBlock = blockingStubFull
        .getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(currentBlock.hasBlockHeader());
    Assert.assertFalse(currentBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getNumber() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("test getcurrentblock is " + Long
        .toString(currentBlock.getBlockHeader().getRawData().getNumber()));
    Assert.assertFalse(currentBlock.getBlockid().isEmpty());

    //Improve coverage.
    currentBlock.equals(currentBlock);
    //Block newBlock = blockingStubFull.getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    GrpcAPI.BlockExtention newBlock = blockingStubFull
        .getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    newBlock.equals(currentBlock);
    newBlock.hashCode();
    newBlock.getSerializedSize();
    newBlock.getTransactionsCount();
    newBlock.getTransactionsList();
    Assert.assertFalse(newBlock.getBlockid().isEmpty());
  }

  @Test
  public void testCurrentBlockFromSolidity2() {
    GrpcAPI.BlockExtention currentBlock = blockingStubSolidity
        .getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(currentBlock.hasBlockHeader());
    Assert.assertFalse(currentBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getNumber() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("test getcurrentblock in soliditynode is " + Long
        .toString(currentBlock.getBlockHeader().getRawData().getNumber()));
  }
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

