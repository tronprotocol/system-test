package stest.tron.wallet.dailybuild.tvmnewcommand.tvmstake;

import java.util.HashMap;
import java.util.Optional;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.Frozen;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
  public class StakeSuicideTest005 extends TronBaseTest {

  private byte[] testWitnessAddress = PublicMethod.getFinalAddress(witnessKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey2.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private byte[] contractAddress;
  String filePath = "src/test/resources/soliditycode/testStakeSuicide.sol";
  String contractName = "testStakeSuicide";
  String code = "";
  String abi = "";

  @BeforeClass(enabled = false)
  public void beforeClass() {
    System.out.println(testKey001);
    PublicMethod.printAddress(testKey001);    PublicMethod
        .sendcoin(testAddress001, 1000_000_00000L, foundationAddress, foundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000_000000L, 100, null, testKey001, testAddress001, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "targetAddress is account no TRX, and no frozen", groups = {"contract", "daily"})
  void tvmStakeSuicideTest001() {
    ECKey ecKeyTargetAddress = new ECKey(Utils.getRandom());
  byte[] targetAddress = ecKeyTargetAddress.getAddress();
  String testKeyTargetAddress = ByteArray.toHexString(ecKeyTargetAddress.getPrivKeyBytes());

    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 1000_000000L, 100,
            null, testKey001, testAddress001, blockingStubFull);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  final Long ownerBalance = ownerAccount.getBalance();
  String methodStrSuicide = "SelfdestructTest(address)";
  String argsStrSuicide = "\"" + Base58.encode58Check(targetAddress) + "\"";
  String txidSuicide  = PublicMethod
        .triggerContract(contractAddress, methodStrSuicide, argsStrSuicide,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txidSuicide,
        blockingStubFull);
    ex = PublicMethod.getTransactionInfoById(txidSuicide,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    Account targetAccount = PublicMethod.queryAccount(targetAddress,blockingStubFull);
  Long targetBalance = targetAccount.getBalance();

    System.out.println(targetBalance);
    Assert.assertEquals(ownerBalance,targetBalance);

  }

  @Test(enabled = false, description = "targetAddress is account 1 TRX, and no frozen", groups = {"contract", "daily"})
  void tvmStakeSuicideTest002() {
    ECKey ecKeyTargetAddress = new ECKey(Utils.getRandom());
  byte[] targetAddress = ecKeyTargetAddress.getAddress();
  final String testKeyTargetAddress = ByteArray.toHexString(ecKeyTargetAddress.getPrivKeyBytes());

    PublicMethod
        .sendcoin(targetAddress, 1_000000L, foundationAddress, foundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000_000000L, 100, null, testKey001, testAddress001, blockingStubFull);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  final Long ownerBalance = ownerAccount.getBalance();
  String methodStrSuicide = "SelfdestructTest(address)";
  String argsStrSuicide = "\"" + Base58.encode58Check(targetAddress) + "\"";
  String txidSuicide  = PublicMethod
        .triggerContract(contractAddress, methodStrSuicide, argsStrSuicide,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txidSuicide,
        blockingStubFull);
    ex = PublicMethod.getTransactionInfoById(txidSuicide,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    Account targetAccount = PublicMethod.queryAccount(targetAddress,blockingStubFull);
  Long targetBalance = targetAccount.getBalance() - 1_000000L;

    Assert.assertEquals(ownerBalance,targetBalance);

    Assert.assertTrue(PublicMethod
        .freezeBalance(targetAddress,1_000000L,3,testKeyTargetAddress,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = false, description = "targetAddress is account 1 TRX, and 1 frozen", groups = {"contract", "daily"})
  void tvmStakeSuicideTest003() {
    ECKey ecKeyTargetAddress = new ECKey(Utils.getRandom());
  byte[] targetAddress = ecKeyTargetAddress.getAddress();
  String testKeyTargetAddress = ByteArray.toHexString(ecKeyTargetAddress.getPrivKeyBytes());
    Assert.assertTrue(PublicMethod
        .sendcoin(targetAddress, 10_000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod
        .freezeBalance(targetAddress,1_000000L,3,testKeyTargetAddress,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account targetAccount = PublicMethod.queryAccount(targetAddress,blockingStubFull);
  final Frozen targetFrozenBefore = targetAccount.getFrozen(0);
    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000_000000L, 100, null, testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  final Long ownerBalance = ownerAccount.getBalance();
  String methodStrSuicide = "SelfdestructTest(address)";
  String argsStrSuicide = "\"" + Base58.encode58Check(targetAddress) + "\"";
  String txidSuicide  = PublicMethod
        .triggerContract(contractAddress, methodStrSuicide, argsStrSuicide,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txidSuicide,
        blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account targetAccountAfter = PublicMethod.queryAccount(targetAddress,blockingStubFull);
    Frozen targetFrozenAfter = targetAccountAfter.getFrozen(0);
  Long targetBalance = targetAccountAfter.getBalance() - 9_000000L;
    Assert.assertEquals(targetFrozenBefore,targetFrozenAfter);
    Assert.assertEquals(ownerBalance,targetBalance);

  }

}

