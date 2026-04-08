package stest.tron.wallet.dailybuild.manual;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;;

@Slf4j
public class WalletTestAccount015 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static long amount = 100000000L;
  private static String accountId = "accountid_" + Long.toString(now);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] account015Address = ecKey1.getAddress();
  String account015Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelSoliInFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSoliInFull = null;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    initPbftChannel();
    initSolidityChannel();
    PublicMethod.printAddress(testKey002);    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
        .usePlaintext()
        .build();
    blockingStubSoliInFull = WalletSolidityGrpc.newBlockingStub(channelSoliInFull);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext()
        .build();
    Random rand = new Random();
    amount = amount + rand.nextInt(10000);
  }

  @Test(enabled = true, description = "Set account id", groups = {"daily"})
  public void test01SetAccountId() {
    //Create account014
    ecKey1 = new ECKey(Utils.getRandom());
    account015Address = ecKey1.getAddress();
    account015Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    PublicMethod.printAddress(account015Key);
    Assert.assertTrue(PublicMethod.sendcoin(account015Address, amount, foundationAddress,
        testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod.setAccountId(accountId.getBytes(),
        account015Address, account015Key, blockingStubFull));
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSoliInFull);
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSoliInFull);
  }

  @Test(enabled = true, description = "Get account by id", groups = {"daily"})
  public void test02GetAccountById() {
    Assert.assertEquals(amount, PublicMethod.getAccountById(
        accountId, blockingStubFull).getBalance());
  }


  @Test(enabled = true, description = "Get account by id from solidity", groups = {"daily"})
  public void test03GetAccountByIdFromSolidity() {
    Assert.assertEquals(amount, PublicMethod.getAccountByIdFromSolidity(
        accountId, blockingStubSoliInFull).getBalance());
  }

  @Test(enabled = true, description = "Get account by id from PBFT", groups = {"daily"})
  public void test04GetAccountByIdFromPbft() {
    Assert.assertEquals(amount, PublicMethod.getAccountByIdFromSolidity(
        accountId, blockingStubPbft).getBalance());
  }


  @Test(enabled = true, description = "Get account from PBFT", groups = {"daily"})
  public void test05GetAccountFromPbft() {
    Assert.assertEquals(amount, PublicMethod.queryAccount(
        account015Address, blockingStubPbft).getBalance());
  }


  @Test(enabled = true, description = "List witnesses", groups = {"daily"})
  public void test06ListWitness() {
    Assert.assertTrue(PublicMethod.listWitnesses(blockingStubFull)
        .get().getWitnessesCount() >= 2);
  }

  @Test(enabled = true, description = "List witnesses from solidity node", groups = {"daily"})
  public void test07ListWitnessFromSolidity() {
    Assert.assertTrue(PublicMethod.listWitnessesFromSolidity(blockingStubSolidity)
        .get().getWitnessesCount() >= 2);
    Assert.assertTrue(PublicMethod.listWitnessesFromSolidity(blockingStubSoliInFull)
        .get().getWitnessesCount() >= 2);
  }

  @Test(enabled = true, description = "List witnesses from PBFT node", groups = {"daily"})
  public void test08ListWitnessFromPbft() {
    Assert.assertTrue(PublicMethod.listWitnessesFromSolidity(blockingStubPbft)
        .get().getWitnessesCount() >= 2);
  }


  // TODO: Enable when proto supports getPaginatedNowWitnessList (v4.8.1+ API)
  // @Test(enabled = false, description = "List witness realTime vote data")
  // public void test09CheckVoteChangesRealtimeAfterVote(){ ... }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(account015Address, account015Key, foundationAddress, blockingStubFull);    if (channelSoliInFull != null) {
      channelSoliInFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}