package stest.tron.wallet.dailybuild.manual;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
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
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestBlock003 extends TronBaseTest {  private final byte[] foundationAddress = PublicMethod.getFinalAddress(foundationKey);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  String txid;
  Long blockNum;
  ByteString blockHash;


  @BeforeClass
  public void beforeClass() {    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey1.getAddress();
    txid = PublicMethod.sendcoinGetTransactionId(receiverAddress,1L,foundationAddress,foundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    blockNum = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get().getBlockNumber();
    blockHash = PublicMethod.getBlock(blockNum+1,blockingStubFull).getBlockHeader().getRawData().getParentHash();

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block from fullnode", groups = {"daily"})
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
        PublicMethod.waitProduceNextBlock(blockingStubFull);
      }
    }

    Assert.assertTrue(getBlockEqualGetNowBlock);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block from solidity", groups = {"daily"})
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
        PublicMethod.waitProduceNextBlock(blockingStubFull);
      }
    }

    Assert.assertTrue(getBlockEqualGetNowBlock);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block by block num with detail true", groups = {"daily"})
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
  @Test(enabled = true, description = "Get block by block num with detail default false", groups = {"daily"})
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
  public void shutdown() throws InterruptedException {  }

}


