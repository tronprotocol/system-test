package stest.tron.wallet.dailybuild.manual;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BlockExtention;
import org.tron.api.GrpcAPI.BlockReq;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class WalletTestBlock003 {
  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);


  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);


  String txid;
  Long blockNum;
  ByteString blockHash;



  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] receiverAddress = ecKey1.getAddress();
    txid = PublicMethed.sendcoinGetTransactionId(receiverAddress,1L,foundationAddress,foundationKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    blockNum = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getBlockNumber();
    blockHash = PublicMethed.getBlock(blockNum+1,blockingStubFull).getBlockHeader().getRawData().getParentHash();

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block from fullnode")
  public void test01GetBlock() {
    Boolean getBlockEqualGetNowBlock = false;
    Integer retryTimes = 5;

    while (!getBlockEqualGetNowBlock && retryTimes-- >= 0) {
      BlockReq.Builder builder = BlockReq.newBuilder();
      BlockExtention currentBlockFromGetBlock = blockingStubFull.getBlock(builder.build());

      BlockExtention currentBlockFromGetNowBlock = blockingStubFull.getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
      logger.info("currentBlockFromGetBlock: " + currentBlockFromGetBlock.getBlockHeader().toString());
      logger.info("currentBlockFromGetNowBlock: " + currentBlockFromGetNowBlock.getBlockHeader().toString());

      if(currentBlockFromGetBlock.getBlockHeader().equals(currentBlockFromGetNowBlock.getBlockHeader())) {
        getBlockEqualGetNowBlock = true;
      } else {
        PublicMethed.waitProduceNextBlock(blockingStubFull);
      }
    }

    Assert.assertTrue(getBlockEqualGetNowBlock);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block from solidity")
  public void test02GetBlockFromSolidity() {
    Boolean getBlockEqualGetNowBlock = false;
    Integer retryTimes = 5;

    while (!getBlockEqualGetNowBlock && retryTimes-- >= 0) {
      BlockReq.Builder builder = BlockReq.newBuilder();
      BlockExtention currentBlockFromGetBlock = blockingStubSolidity.getBlock(builder.build());

      BlockExtention currentBlockFromGetNowBlock = blockingStubSolidity.getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());

      logger.info("test02GetBlockFromSolidity: currentBlockFromGetBlock"+currentBlockFromGetBlock.toString());
      logger.info("test02GetBlockFromSolidity: currentBlockFromGetNowBlock"+currentBlockFromGetNowBlock.getBlockHeader().toString());
      if(currentBlockFromGetBlock.getBlockHeader().equals(currentBlockFromGetNowBlock.getBlockHeader())) {
        getBlockEqualGetNowBlock = true;
      } else {
        PublicMethed.waitProduceNextBlock(blockingStubFull);
      }
    }

    Assert.assertTrue(getBlockEqualGetNowBlock);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block by block num with detail true")
  public void test03GetBlockByBlockNumWithDetailTrue() {
    BlockReq.Builder builder = BlockReq.newBuilder();

    builder.setIdOrNum(String.valueOf(blockNum));
    builder.setDetail(true);
    BlockExtention lastBlockByNumWithDetailTrue = blockingStubFull.getBlock(builder.build());
    builder.clear();
    builder.setIdOrNum(ByteArray.toHexString(blockHash.toByteArray()));
    builder.setDetail(true);
    BlockExtention lastBlockByIdWithDetailTrue = blockingStubFull.getBlock(builder.build());

    NumberMessage.Builder builder1 = NumberMessage.newBuilder();
    builder1.setNum(blockNum);
    BlockExtention blockByGetBlockByNum = blockingStubFull.getBlockByNum2(builder1.build());

    Assert.assertEquals(blockByGetBlockByNum,lastBlockByNumWithDetailTrue);
    Assert.assertEquals(lastBlockByNumWithDetailTrue,lastBlockByIdWithDetailTrue);
    Assert.assertTrue(lastBlockByNumWithDetailTrue.getTransactionsCount() >= 1);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block by block num with detail default false")
  public void test03GetBlockByBlockNumWithDetailDefaultFalse() {
    BlockReq.Builder builder = BlockReq.newBuilder();

    builder.setIdOrNum(String.valueOf(blockNum));
    BlockExtention lastBlockByNumWithDetailFalse = blockingStubFull.getBlock(builder.build());
    builder.clear();
    builder.setIdOrNum(ByteArray.toHexString(blockHash.toByteArray()));
    BlockExtention lastBlockByIdWithDetailFalse = blockingStubFull.getBlock(builder.build());

    NumberMessage.Builder builder1 = NumberMessage.newBuilder();
    builder1.setNum(blockNum);
    BlockExtention blockByGetBlockByNum = blockingStubFull.getBlockByNum2(builder1.build());

    Assert.assertNotEquals(blockByGetBlockByNum,lastBlockByNumWithDetailFalse);
    Assert.assertEquals(lastBlockByNumWithDetailFalse,lastBlockByIdWithDetailFalse);
    Assert.assertTrue(blockByGetBlockByNum.getTransactionsCount() >= 1);
    Assert.assertEquals(lastBlockByIdWithDetailFalse.getTransactionsCount(),0);
  }






  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}


