package stest.tron.wallet.dailybuild.manual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Assert;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
  public class GetTransactionInfoByBlockNumFromSolidity extends TronBaseTest {  public String fullNode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
  public String solidityNode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(0);

  @Test(enabled = true, description = "test getTransactionInfoByBlockNumFromSolidity", groups = {"daily"})
  public void test01GetTransactionInfoByBlockNumFromSolidity() {    channelSolidity = ManagedChannelBuilder.forTarget(solidityNode).usePlaintext().build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    Protocol.Block solidityCurrentBlock =
        blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    long block = solidityCurrentBlock.getBlockHeader().getRawData().getNumber();
    long targetBlock;
    for (targetBlock = block; targetBlock > 0; targetBlock--) {
      GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
      builder.setNum(targetBlock);
      if (blockingStubSolidity.getTransactionCountByBlockNum(builder.build()).getNum() > 0) {
        break;
      }
    }

    GrpcAPI.TransactionInfoList transactionList =
        PublicMethod.getTransactionInfoByBlockNum(targetBlock, blockingStubFull).get();

    GrpcAPI.TransactionInfoList transactionListFromSolidity =
        PublicMethod.getTransactionInfoByBlockNum(targetBlock, blockingStubFull).get();
    Assert.assertEquals(transactionList, transactionListFromSolidity);
  }
}
