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

@Slf4j
public class WalletTestBlock003 {

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

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

      if(currentBlockFromGetBlock.equals(currentBlockFromGetNowBlock)) {
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

      if(currentBlockFromGetBlock.equals(currentBlockFromGetNowBlock)) {
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
  @Test(enabled = true, description = "Get block by block num")
  public void test03GetBlockByBlockNum() {
    BlockReq.Builder builder = BlockReq.newBuilder();
    BlockExtention currentBlockFromGetBlock = blockingStubFull.getBlock(builder.build());
    Long lastBlockNum = currentBlockFromGetBlock.getBlockHeader().getRawData().getNumber() - 1;
    ByteString lastBlockHash = currentBlockFromGetBlock.getBlockHeader().getRawData().getParentHash();

    builder.setIdOrNum(String.valueOf(lastBlockNum));
    builder.setDetail(true);
    BlockExtention lastBlockByNum = blockingStubFull.getBlock(builder.build());
    builder.clear();
    builder.setIdOrNum(ByteArray.toHexString(lastBlockHash.toByteArray()));
    BlockExtention lastBlockById = blockingStubFull.getBlock(builder.build());

    NumberMessage.Builder builder1 = NumberMessage.newBuilder();
    builder1.setNum(lastBlockNum);
    BlockExtention lastBlockByGetBlockByNum = blockingStubFull.getBlockByNum2(builder1.build());

    Assert.assertEquals(lastBlockByNum,lastBlockById);
    Assert.assertEquals(lastBlockByNum,lastBlockByGetBlockByNum);
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


