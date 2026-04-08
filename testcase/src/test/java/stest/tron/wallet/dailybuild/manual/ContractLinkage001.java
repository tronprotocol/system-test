package stest.tron.wallet.dailybuild.manual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
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
public class ContractLinkage001 extends TronBaseTest {  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] linkage001Address = ecKey1.getAddress();
  String linkage001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(linkage001Key);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

  }

  @Test(enabled = true, description = "Deploy contract with valid or invalid value", groups = {"daily"})
  public void deployContentValue() {
    Assert.assertTrue(PublicMethod.sendcoin(linkage001Address, 30000000000L, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account info;
    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(linkage001Address,
        blockingStubFull);
    info = PublicMethod.queryAccount(linkage001Address, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeNetLimit = resourceInfo.getNetLimit();
  Long beforeFreeNetLimit = resourceInfo.getFreeNetLimit();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeEnergyLimit = resourceInfo.getEnergyLimit();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyLimit:" + beforeEnergyLimit);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeFreeNetLimit:" + beforeFreeNetLimit);
    logger.info("beforeNetLimit:" + beforeNetLimit);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  //Value is equal balance,this will be failed.Only use FreeNet,Other not change.
    String filePath = "./src/test/resources/soliditycode/contractLinkage001.sol";
  String contractName = "divideIHaveArgsReturnStorage";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String payableCode = retMap.get("byteCode").toString();
  String payableAbi = retMap.get("abI").toString();
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account accountGet = PublicMethod.queryAccount(linkage001Key, blockingStubFull);
  Long accountBalance = accountGet.getBalance();
  String txid = PublicMethod.deployContractAndGetTransactionInfoById(contractName, payableAbi,
        payableCode, "", maxFeeLimit, accountBalance, 100, null,
        linkage001Key, linkage001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
  Long fee = infoById.get().getFee();
  Long energyFee = infoById.get().getReceipt().getEnergyFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  Long netFee = infoById.get().getReceipt().getNetFee();
    logger.info("energyUsageTotal:" + energyUsageTotal);
    logger.info("fee:" + fee);
    logger.info("energyFee:" + energyFee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);

    Account infoafter = PublicMethod.queryAccount(linkage001Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(linkage001Address,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyLimit = resourceInfoafter.getEnergyLimit();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterFreeNetLimit = resourceInfoafter.getFreeNetLimit();
  Long afterNetLimit = resourceInfoafter.getNetLimit();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyLimit:" + afterEnergyLimit);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterFreeNetLimit:" + afterFreeNetLimit);
    logger.info("afterNetLimit:" + afterNetLimit);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertTrue(fee == 0);
    Assert.assertTrue(afterNetUsed == 0);
    Assert.assertTrue(afterEnergyUsed == 0);
    Assert.assertTrue(afterFreeNetUsed > 0);
  Long freezeBalance = 2000000000L;
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(linkage001Address, freezeBalance,
        0, 1, linkage001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //maxFeeLimit = maxFeeLimit - freezeBalance;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage resourceInfo1 = PublicMethod.getAccountResource(linkage001Address,
        blockingStubFull);
    Account info1 = PublicMethod.queryAccount(linkage001Address, blockingStubFull);
  Long beforeBalance1 = info1.getBalance();
  Long beforeEnergyLimit1 = resourceInfo1.getEnergyLimit();
  Long beforeEnergyUsed1 = resourceInfo1.getEnergyUsed();
  Long beforeFreeNetLimit1 = resourceInfo1.getFreeNetLimit();
  Long beforeNetLimit1 = resourceInfo1.getNetLimit();
  Long beforeNetUsed1 = resourceInfo1.getNetUsed();
  Long beforeFreeNetUsed1 = resourceInfo1.getFreeNetUsed();
    logger.info("beforeBalance1:" + beforeBalance1);
    logger.info("beforeEnergyLimit1:" + beforeEnergyLimit1);
    logger.info("beforeEnergyUsed1:" + beforeEnergyUsed1);
    logger.info("beforeFreeNetLimit1:" + beforeFreeNetLimit1);
    logger.info("beforeNetLimit1:" + beforeNetLimit1);
    logger.info("beforeNetUsed1:" + beforeNetUsed1);
    logger.info("beforeFreeNetUsed1:" + beforeFreeNetUsed1);
  //Value is 1,use BalanceGetEnergy,use FreeNet,fee==0.
    txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, payableAbi, payableCode,
            "", maxFeeLimit, 1L, 100, null, linkage001Key,
            linkage001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
  Long energyUsageTotal1 = infoById1.get().getReceipt().getEnergyUsageTotal();
  Long fee1 = infoById1.get().getFee();
  Long energyFee1 = infoById1.get().getReceipt().getEnergyFee();
  Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
  Long energyUsed1 = infoById1.get().getReceipt().getEnergyUsage();
  Long netFee1 = infoById1.get().getReceipt().getNetFee();
    logger.info("energyUsageTotal1:" + energyUsageTotal1);
    logger.info("fee1:" + fee1);
    logger.info("energyFee1:" + energyFee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("energyUsed1:" + energyUsed1);
    logger.info("netFee1:" + netFee1);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);

    Account infoafter1 = PublicMethod.queryAccount(linkage001Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter1 = PublicMethod.getAccountResource(linkage001Address,
        blockingStubFull1);
  Long afterBalance1 = infoafter1.getBalance();
  Long afterEnergyLimit1 = resourceInfoafter1.getEnergyLimit();
  Long afterEnergyUsed1 = resourceInfoafter1.getEnergyUsed();
  Long afterFreeNetLimit1 = resourceInfoafter1.getFreeNetLimit();
  Long afterNetLimit1 = resourceInfoafter1.getNetLimit();
  Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
  Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance1:" + afterBalance1);
    logger.info("afterEnergyLimit1:" + afterEnergyLimit1);
    logger.info("afterEnergyUsed1:" + afterEnergyUsed1);
    logger.info("afterFreeNetLimit1:" + afterFreeNetLimit1);
    logger.info("afterNetLimit1:" + afterNetLimit1);
    logger.info("afterNetUsed1:" + afterNetUsed1);
    logger.info("afterFreeNetUsed1:" + afterFreeNetUsed1);

    Assert.assertTrue(beforeBalance1 - fee1 - 1L == afterBalance1);
  byte[] contractAddress = infoById1.get().getContractAddress().toByteArray();
    Account account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
    Assert.assertTrue(account.getBalance() == 1L);
    Assert.assertTrue(afterNetUsed1 == 0);
    Assert.assertTrue(afterEnergyUsed1 > 0);
    Assert.assertTrue(afterFreeNetUsed1 > 0);
  //Value is account all balance plus 1. balance is not sufficient,Nothing changde.
    AccountResourceMessage resourceInfo2 = PublicMethod.getAccountResource(linkage001Address,
        blockingStubFull);
    Account info2 = PublicMethod.queryAccount(linkage001Address, blockingStubFull);
  Long beforeBalance2 = info2.getBalance();
  Long beforeEnergyLimit2 = resourceInfo2.getEnergyLimit();
  Long beforeEnergyUsed2 = resourceInfo2.getEnergyUsed();
  Long beforeFreeNetLimit2 = resourceInfo2.getFreeNetLimit();
  Long beforeNetLimit2 = resourceInfo2.getNetLimit();
  Long beforeNetUsed2 = resourceInfo2.getNetUsed();
  Long beforeFreeNetUsed2 = resourceInfo2.getFreeNetUsed();
    logger.info("beforeBalance2:" + beforeBalance2);
    logger.info("beforeEnergyLimit2:" + beforeEnergyLimit2);
    logger.info("beforeEnergyUsed2:" + beforeEnergyUsed2);
    logger.info("beforeFreeNetLimit2:" + beforeFreeNetLimit2);
    logger.info("beforeNetLimit2:" + beforeNetLimit2);
    logger.info("beforeNetUsed2:" + beforeNetUsed2);
    logger.info("beforeFreeNetUsed2:" + beforeFreeNetUsed2);

    account = PublicMethod.queryAccount(linkage001Key, blockingStubFull);
  Long valueBalance = account.getBalance();
    contractAddress = PublicMethod.deployContract(contractName, payableAbi, payableCode, "",
        maxFeeLimit, valueBalance + 1, 100, null, linkage001Key,
        linkage001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(contractAddress == null);
    Account infoafter2 = PublicMethod.queryAccount(linkage001Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter2 = PublicMethod.getAccountResource(linkage001Address,
        blockingStubFull1);
  Long afterBalance2 = infoafter2.getBalance();
  Long afterEnergyLimit2 = resourceInfoafter2.getEnergyLimit();
  Long afterEnergyUsed2 = resourceInfoafter2.getEnergyUsed();
  Long afterFreeNetLimit2 = resourceInfoafter2.getFreeNetLimit();
  Long afterNetLimit2 = resourceInfoafter2.getNetLimit();
  Long afterNetUsed2 = resourceInfoafter2.getNetUsed();
  Long afterFreeNetUsed2 = resourceInfoafter2.getFreeNetUsed();
    logger.info("afterBalance2:" + afterBalance2);
    logger.info("afterEnergyLimit2:" + afterEnergyLimit2);
    logger.info("afterEnergyUsed2:" + afterEnergyUsed2);
    logger.info("afterFreeNetLimit2:" + afterFreeNetLimit2);
    logger.info("afterNetLimit2:" + afterNetLimit2);
    logger.info("afterNetUsed2:" + afterNetUsed2);
    logger.info("afterFreeNetUsed2:" + afterFreeNetUsed2);
    Assert.assertTrue(afterNetUsed2 == 0);
    Assert.assertTrue(afterEnergyUsed2 > 0);
    Assert.assertTrue(afterFreeNetUsed2 > 0);
    Assert.assertEquals(beforeBalance2, afterBalance2);
  //Value is account all balance.use freezeBalanceGetEnergy ,freezeBalanceGetNet .Balance ==0
    Assert.assertTrue(PublicMethod.freezeBalance(linkage001Address, freezeBalance,
        0, linkage001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage resourceInfo3 = PublicMethod.getAccountResource(linkage001Address,
        blockingStubFull);
    Account info3 = PublicMethod.queryAccount(linkage001Address, blockingStubFull);
  Long beforeBalance3 = info3.getBalance();
  Long beforeEnergyLimit3 = resourceInfo3.getEnergyLimit();
  Long beforeEnergyUsed3 = resourceInfo3.getEnergyUsed();
  Long beforeFreeNetLimit3 = resourceInfo3.getFreeNetLimit();
  Long beforeNetLimit3 = resourceInfo3.getNetLimit();
  Long beforeNetUsed3 = resourceInfo3.getNetUsed();
  Long beforeFreeNetUsed3 = resourceInfo3.getFreeNetUsed();
    logger.info("beforeBalance3:" + beforeBalance3);
    logger.info("beforeEnergyLimit3:" + beforeEnergyLimit3);
    logger.info("beforeEnergyUsed3:" + beforeEnergyUsed3);
    logger.info("beforeFreeNetLimit3:" + beforeFreeNetLimit3);
    logger.info("beforeNetLimit3:" + beforeNetLimit3);
    logger.info("beforeNetUsed3:" + beforeNetUsed3);
    logger.info("beforeFreeNetUsed3:" + beforeFreeNetUsed3);
    account = PublicMethod.queryAccount(linkage001Key, blockingStubFull);
    valueBalance = account.getBalance();
    txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, payableAbi, payableCode,
            "", maxFeeLimit, valueBalance, 100, null, linkage001Key,
            linkage001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    fee = infoById.get().getFee();
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contractAddress = infoById.get().getContractAddress().toByteArray();
    Account infoafter3 = PublicMethod.queryAccount(linkage001Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter3 = PublicMethod.getAccountResource(linkage001Address,
        blockingStubFull1);
  Long afterBalance3 = infoafter3.getBalance();
  Long afterEnergyLimit3 = resourceInfoafter3.getEnergyLimit();
  Long afterEnergyUsed3 = resourceInfoafter3.getEnergyUsed();
  Long afterFreeNetLimit3 = resourceInfoafter3.getFreeNetLimit();
  Long afterNetLimit3 = resourceInfoafter3.getNetLimit();
  Long afterNetUsed3 = resourceInfoafter3.getNetUsed();
  Long afterFreeNetUsed3 = resourceInfoafter3.getFreeNetUsed();
    logger.info("afterBalance3:" + afterBalance3);
    logger.info("afterEnergyLimit3:" + afterEnergyLimit3);
    logger.info("afterEnergyUsed3:" + afterEnergyUsed3);
    logger.info("afterFreeNetLimit3:" + afterFreeNetLimit3);
    logger.info("afterNetLimit3:" + afterNetLimit3);
    logger.info("afterNetUsed3:" + afterNetUsed3);
    logger.info("afterFreeNetUsed3:" + afterFreeNetUsed3);

    Assert.assertTrue(afterNetUsed3 > 0);
    Assert.assertTrue(afterEnergyUsed3 > 0);
    Assert.assertTrue(afterFreeNetUsed3 > 0);
    Assert.assertTrue(beforeBalance2 - fee == afterBalance2);
    Assert.assertTrue(afterBalance3 == 0);
    Assert.assertTrue(PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getBalance() == valueBalance);
    PublicMethod
        .unFreezeBalance(linkage001Address, linkage001Key, 1,
            linkage001Address, blockingStubFull);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(linkage001Address, linkage001Key, foundationAddress, blockingStubFull);  }
}


