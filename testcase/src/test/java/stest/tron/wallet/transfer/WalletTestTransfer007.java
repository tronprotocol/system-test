package stest.tron.wallet.transfer;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;

import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class WalletTestTransfer007 extends TronBaseTest {  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);  private ManagedChannel searchChannelFull = null;  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidityInFullnode = null;

  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] sendAccountAddress = ecKey1.getAddress();
  String sendAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());  private ManagedChannel channelSolidityInFullnode = null;  /*  private String solidityInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);*/

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {    searchChannelFull = ManagedChannelBuilder.forTarget(searchFullnode)
        .usePlaintext()
        .build();
    searchBlockingStubFull = WalletGrpc.newBlockingStub(searchChannelFull);    /*    channelSolidityInFullnode = ManagedChannelBuilder.forTarget(solidityInFullnode)
        .usePlaintext()
        .build();
    blockingStubSolidityInFullnode = WalletSolidityGrpc.newBlockingStub(channelSolidityInFullnode);
    */
  }

  @Test
  public void testSendCoin() {
    initSolidityChannel();
    String transactionId = PublicMethod.sendcoinGetTransactionId(sendAccountAddress, 90000000000L,
        fromAddress, foundationKey2, blockingStubFull);
    Optional<Transaction> infoById = PublicMethod
        .getTransactionById(transactionId, blockingStubFull);
    Long timestamptis = PublicMethod.printTransactionRow(infoById.get().getRawData());
    Long timestamptispBlockOne = PublicMethod.getBlock(1, blockingStubFull).getBlockHeader()
        .getRawData().getTimestamp();
    Assert.assertTrue(timestamptis >= timestamptispBlockOne);
  }

  @Test
  public void testSendCoin2() {
    String transactionId = PublicMethod.sendcoinGetTransactionId(sendAccountAddress, 90000000000L,
        fromAddress, foundationKey2, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Transaction> infoById = PublicMethod
        .getTransactionById(transactionId, blockingStubFull);
    Long timestamptis = PublicMethod.printTransactionRow(infoById.get().getRawData());
    Long timestampBlockOne = PublicMethod.getBlock(1, blockingStubFull).getBlockHeader()
        .getRawData().getTimestamp();
    Assert.assertTrue(timestamptis >= timestampBlockOne);
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);

    infoById = PublicMethod.getTransactionById(transactionId, blockingStubSolidity);
    timestamptis = PublicMethod.printTransactionRow(infoById.get().getRawData());
    timestampBlockOne = PublicMethod.getBlock(1, blockingStubFull).getBlockHeader()
        .getRawData().getTimestamp();
    Assert.assertTrue(timestamptis >= timestampBlockOne);

    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(transactionId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo;

    transactionInfo = blockingStubSolidity.getTransactionInfoById(request);
    Assert.assertTrue(transactionInfo.getBlockTimeStamp() >= timestampBlockOne);

    transactionInfo = blockingStubFull.getTransactionInfoById(request);
    Assert.assertTrue(transactionInfo.getBlockTimeStamp() >= timestampBlockOne);

    //transactionInfo = blockingStubSolidityInFullnode.getTransactionInfoById(request);
    //Assert.assertTrue(transactionInfo.getBlockTimeStamp() >= timestampBlockOne);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (searchChannelFull != null) {
      searchChannelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
