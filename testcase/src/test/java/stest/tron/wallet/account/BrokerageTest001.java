package stest.tron.wallet.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethod;

import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class BrokerageTest001 extends TronBaseTest {

  private ManagedChannel channelSoliInFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSoliInFull = null;
  private String soliInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);

  private String dev001Key = foundationKey;
  private byte[] dev001Address = foundationAddress;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    initPbftChannel();

    channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
        .usePlaintext()
        .build();
    blockingStubSoliInFull = WalletSolidityGrpc.newBlockingStub(channelSoliInFull);

    PublicMethod.printAddress(dev001Key);
  }

  @Test
  public void updateBrokerageTest001() {
    // witness updateBrokerage
    Assert.assertTrue(updateBrokerage(witnessAddress, 55, blockingStubFull));

    Assert.assertTrue(updateBrokerage(witnessAddress, 0, blockingStubFull));

    Assert.assertTrue(updateBrokerage(witnessAddress, 100, blockingStubFull));

    Assert.assertFalse(updateBrokerage(witnessAddress, -55, blockingStubFull));

    // normal account updateBrokerage fail
    Assert.assertFalse(updateBrokerage(dev001Address, 55, blockingStubFull));
  }

  @Test
  public void getBrokerageTest001() {
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(witnessAddress))
        .build();

    Assert.assertEquals(20, blockingStubFull.getBrokerageInfo(bytesMessage).getNum());

    // getBrokerageInfo from solidity node
    Assert.assertEquals(20, blockingStubSolidity.getBrokerageInfo(bytesMessage).getNum());
    Assert.assertEquals(20, blockingStubSoliInFull.getBrokerageInfo(bytesMessage).getNum());
    Assert.assertEquals(20, blockingStubPbft.getBrokerageInfo(bytesMessage).getNum());
  }

  @Test
  public void getRewardTest002() {
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(witnessAddress))
        .build();
    Assert.assertTrue(blockingStubFull.getRewardInfo(bytesMessage) != null);

    // getRewardInfo from solidity node
    Assert.assertTrue(blockingStubSolidity.getRewardInfo(bytesMessage) != null);
    Assert.assertTrue(blockingStubPbft.getRewardInfo(bytesMessage) != null);
    Assert.assertTrue(blockingStubSoliInFull.getRewardInfo(bytesMessage) != null);
  }
  boolean updateBrokerage(byte[] owner, int brokerage,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
    updateBrokerageContract.setOwnerAddress(ByteString.copyFrom(owner)).setBrokerage(brokerage);
    TransactionExtention transactionExtention = blockingStubFull
        .updateBrokerage(updateBrokerageContract.build());
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }
    logger.info("transaction:" + transaction);
    if (transactionExtention.getResult().getResult()) {
      return true;
    }
    return true;
  }

  public void getBrokerage() {

  }
}