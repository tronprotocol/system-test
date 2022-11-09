package stest.tron.wallet.dailybuild.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class FreezeWeightDeflationTest {
  private static final long sendAmount = 1000000000L;
  private static final long frozenAmount = 1500000L;
  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozen1Address = ecKey1.getAddress();
  String frozen1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] frozen2Address = ecKey4.getAddress();
  String frozen2Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiver1Address = ecKey2.getAddress();
  String receiver1Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] receiver2Address = ecKey3.getAddress();
  String receiver2Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(frozen1Key);
    PublicMethed.printAddress(frozen2Key);
    PublicMethed.printAddress(receiver1Key);
    PublicMethed.printAddress(receiver2Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    Assert.assertTrue(PublicMethed.sendcoin(frozen1Address, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(frozen2Address, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiver1Address, 1L,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiver2Address, 1L,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = true, description = "Freeze net cause weight deflation issue fix test")
  public void test01FreezeNetCauseWeightDeflationTest() {
    final Long beforeNetWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalNetWeight();

    PublicMethed.freezeBalance(frozen1Address,frozenAmount,0,frozen1Key,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterNetWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalNetWeight();
    Assert.assertTrue(afterNetWeight - beforeNetWeight == 1);



    PublicMethed.freezeBalance(frozen1Address,frozenAmount,0,frozen1Key,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    afterNetWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalNetWeight();
    Assert.assertTrue(afterNetWeight - beforeNetWeight == 3);



    PublicMethed.unFreezeBalance(frozen1Address,frozen1Key,0,null,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    afterNetWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalNetWeight();
    Assert.assertTrue(afterNetWeight - beforeNetWeight == 0);



  }



  @Test(enabled = true, description = "Freeze energy cause weight deflation issue fix test")
  public void test02FreezeEnergyCauseWeightDeflationTest() {
    final Long beforeEnergyWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalEnergyWeight();

    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(frozen1Address,frozenAmount,0,1,frozen1Key,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterEnergyWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalEnergyWeight();
    Assert.assertTrue(afterEnergyWeight - beforeEnergyWeight == 1);



    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(frozen1Address,frozenAmount,0,1,frozen1Key,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    afterEnergyWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalEnergyWeight();
    Assert.assertTrue(afterEnergyWeight - beforeEnergyWeight == 3);



    Assert.assertTrue(PublicMethed.unFreezeBalance(frozen1Address,frozen1Key,1,null,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    afterEnergyWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalEnergyWeight();
    Assert.assertTrue(afterEnergyWeight - beforeEnergyWeight == 0);
  }


  @Test(enabled = true, description = "Delegate energy to two receiver cause weight deflation test")
  public void test03DelegateEnergyToTwoReceiverCauseWeightDeflationTest() {
    final Long beforeEnergyWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalEnergyWeight();

    Assert.assertTrue(PublicMethed.freezeBalanceV1ForReceiver(frozen1Address,frozenAmount,
        0,1,receiver1Address,frozen1Key,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceV1ForReceiver(frozen1Address,frozenAmount,
        0,1,receiver2Address,frozen1Key,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterEnergyWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalEnergyWeight();
    Assert.assertTrue(afterEnergyWeight - beforeEnergyWeight == 2);



    Assert.assertTrue(PublicMethed.unFreezeBalance(frozen1Address,frozen1Key,
        1,receiver1Address,blockingStubFull));
    Assert.assertTrue(PublicMethed.unFreezeBalance(frozen1Address,frozen1Key,
        1,receiver2Address,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    afterEnergyWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalEnergyWeight();
    Assert.assertTrue(afterEnergyWeight - beforeEnergyWeight == 0);
  }


  @Test(enabled = true, description = "Two account delegate net to one receiver cause weight deflation test")
  public void test04TwoAccountDelegateNetToOneReceiverCauseWeightDeflationTest() {
    final Long beforeNetWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalNetWeight();

    Assert.assertTrue(PublicMethed.freezeBalanceV1ForReceiver(frozen1Address,frozenAmount,
        0,0,receiver2Address,frozen1Key,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceV1ForReceiver(frozen2Address,frozenAmount,
        0,0,receiver2Address,frozen2Key,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterNetWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalNetWeight();
    Assert.assertTrue(beforeNetWeight - afterNetWeight == 3);



    Assert.assertTrue(PublicMethed.unFreezeBalance(frozen1Address,frozen1Key,
        0,receiver2Address,blockingStubFull));
    Assert.assertTrue(PublicMethed.unFreezeBalance(frozen1Address,frozen1Key,
        0,receiver2Address,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    afterNetWeight = PublicMethed.getAccountResource(frozen1Address,blockingStubFull)
        .getTotalNetWeight();
    Assert.assertTrue(afterNetWeight - beforeNetWeight == 0);
  }
  

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(frozen1Address, frozen1Key, foundationAddress, blockingStubFull);
    PublicMethed.freedResource(frozen2Address, frozen2Key, foundationAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


