package stest.tron.wallet.block;

import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class WalletTestBlock006 extends TronBaseTest {  private ManagedChannel channelSolidity = null;  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {  }

  @Test(enabled = true)
  public void testGetTransactionCountByBlockNumFromFullnode() {
    initSolidityChannel();
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(0);
    Long transactionNumInBlock = 0L;
    transactionNumInBlock = blockingStubFull.getTransactionCountByBlockNum(builder
        .build()).getNum();
    Assert.assertTrue(transactionNumInBlock >= 1);

    builder.setNum(-10);
    transactionNumInBlock = blockingStubFull.getTransactionCountByBlockNum(builder
        .build()).getNum();
    Assert.assertTrue(transactionNumInBlock == -1);

    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    builder.setNum(currentBlockNum + 10000L);
    transactionNumInBlock = blockingStubFull.getTransactionCountByBlockNum(builder
        .build()).getNum();
    Assert.assertTrue(transactionNumInBlock == -1);
  }

  @Test(enabled = true)
  public void testGetTransactionCountByBlockNumFromSolidity() {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(0);
    Long transactionNumInBlock = 0L;
    transactionNumInBlock = blockingStubSolidity.getTransactionCountByBlockNum(builder
        .build()).getNum();
    Assert.assertTrue(transactionNumInBlock >= 1);

    builder.setNum(-10);
    transactionNumInBlock = blockingStubSolidity.getTransactionCountByBlockNum(builder
        .build()).getNum();
    Assert.assertTrue(transactionNumInBlock == -1);

    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    builder.setNum(currentBlockNum + 10000L);
    transactionNumInBlock = blockingStubSolidity.getTransactionCountByBlockNum(builder
        .build()).getNum();
    Assert.assertTrue(transactionNumInBlock == -1);
  }
}

