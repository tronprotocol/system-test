package stest.tron.wallet.dailybuild.tvmnewcommand.tvmstake;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
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
  public class StakeSuicideTest004 extends TronBaseTest {
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

  /**
   * constructor.
   */

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
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 1000_000000L, 100,
            null, testKey001, testAddress001, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "targetAddress has frozen 1,suicide contract stake 1", groups = {"contract", "daily"})
  void tvmStakeSuicideTest001() {
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
  String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 1000000;
  String txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  final Frozen ownerFrozen = ownerAccount.getFrozen(0);
  Long ownerBalance = ownerAccount.getBalance();
  String methodStrSuicide = "SelfdestructTest(address)";
  String argsStrSuicide = "\"" + Base58.encode58Check(targetAddress) + "\"";
  String txidSuicide  = PublicMethod
        .triggerContract(contractAddress, methodStrSuicide, argsStrSuicide,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ex = PublicMethod.getTransactionInfoById(txidSuicide, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Account targetAccountAfter = PublicMethod.queryAccount(targetAddress,blockingStubFull);
    Frozen targetFrozenAfter = targetAccountAfter.getFrozen(0);

    BigInteger expected =
        BigInteger.valueOf(ownerFrozen.getExpireTime())
            .multiply(BigInteger.valueOf(ownerFrozen.getFrozenBalance()))
            .add(BigInteger.valueOf(targetFrozenBefore.getExpireTime())
                .multiply(BigInteger.valueOf(targetFrozenBefore.getFrozenBalance())))
            .divide(BigInteger.valueOf(ownerFrozen.getFrozenBalance())
                .add(BigInteger.valueOf(targetFrozenBefore.getFrozenBalance())));

    Assert.assertEquals(expected.longValue(), targetFrozenAfter.getExpireTime());
    Assert.assertEquals(targetFrozenAfter.getFrozenBalance(),
        ownerFrozen.getFrozenBalance() + targetFrozenBefore.getFrozenBalance());

  }

  @Test(enabled = false, description = "targetAddress has frozen 1,suicide contract stake all", groups = {"contract", "daily"})
  void tvmStakeSuicideTest002() {
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
        100_000000L, 100, null, testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 100_000000L;
  String txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  final Frozen ownerFrozen = ownerAccount.getFrozen(0);
  Long ownerBalance = ownerAccount.getBalance();
  String methodStrSuicide = "SelfdestructTest(address)";
  String argsStrSuicide = "\"" + Base58.encode58Check(targetAddress) + "\"";
  String txidSuicide  = PublicMethod
        .triggerContract(contractAddress, methodStrSuicide, argsStrSuicide,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ex = PublicMethod.getTransactionInfoById(txidSuicide, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Account targetAccountAfter = PublicMethod.queryAccount(targetAddress,blockingStubFull);
    Frozen targetFrozenAfter = targetAccountAfter.getFrozen(0);

    BigInteger expected =
        BigInteger.valueOf(ownerFrozen.getExpireTime())
            .multiply(BigInteger.valueOf(ownerFrozen.getFrozenBalance()))
            .add(BigInteger.valueOf(targetFrozenBefore.getExpireTime())
                .multiply(BigInteger.valueOf(targetFrozenBefore.getFrozenBalance())))
            .divide(BigInteger.valueOf(ownerFrozen.getFrozenBalance())
                .add(BigInteger.valueOf(targetFrozenBefore.getFrozenBalance())));

    Assert.assertEquals(expected.longValue(), targetFrozenAfter.getExpireTime());
    Assert.assertEquals(targetFrozenAfter.getFrozenBalance(),
        ownerFrozen.getFrozenBalance() + targetFrozenBefore.getFrozenBalance());

  }

  @Test(enabled = false, description = "targetAddress has frozen all,suicide contract stake all", groups = {"contract", "daily"})
  void tvmStakeSuicideTest003() {
    ECKey ecKeyTargetAddress = new ECKey(Utils.getRandom());
  byte[] targetAddress = ecKeyTargetAddress.getAddress();
  String testKeyTargetAddress = ByteArray.toHexString(ecKeyTargetAddress.getPrivKeyBytes());
    Assert.assertTrue(PublicMethod
        .sendcoin(targetAddress, 20_000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(targetAddress,5_000000L,
        3,1, ByteString.copyFrom(testAddress001),testKeyTargetAddress,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod
        .freezeBalance(targetAddress,10_000000L,3,testKeyTargetAddress,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account targetAccount = PublicMethod.queryAccount(targetAddress,blockingStubFull);
  final Frozen targetFrozenBefore = targetAccount.getFrozen(0);
    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        100_000000L, 100, null, testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 100_000000L;
  String txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  final Frozen ownerFrozen = ownerAccount.getFrozen(0);
  Long ownerBalance = ownerAccount.getBalance();
  String methodStrSuicide = "SelfdestructTest(address)";
  String argsStrSuicide = "\"" + Base58.encode58Check(targetAddress) + "\"";
  String txidSuicide  = PublicMethod
        .triggerContract(contractAddress, methodStrSuicide, argsStrSuicide,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ex = PublicMethod.getTransactionInfoById(txidSuicide, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Account targetAccountAfter = PublicMethod.queryAccount(targetAddress,blockingStubFull);
    Frozen targetFrozenAfter = targetAccountAfter.getFrozen(0);

    BigInteger expected =
        BigInteger.valueOf(ownerFrozen.getExpireTime())
            .multiply(BigInteger.valueOf(ownerFrozen.getFrozenBalance()))
            .add(BigInteger.valueOf(targetFrozenBefore.getExpireTime())
                .multiply(BigInteger.valueOf(targetFrozenBefore.getFrozenBalance())))
            .divide(BigInteger.valueOf(ownerFrozen.getFrozenBalance())
                .add(BigInteger.valueOf(targetFrozenBefore.getFrozenBalance())));

    Assert.assertEquals(expected.longValue(), targetFrozenAfter.getExpireTime());
    Assert.assertEquals(targetFrozenAfter.getFrozenBalance(),
        ownerFrozen.getFrozenBalance() + targetFrozenBefore.getFrozenBalance());

  }

  @Test(enabled = false, description = "targetAddress is new account ,suicide contract stake all", groups = {"contract", "daily"})
  void tvmStakeSuicideTest004() {
    ECKey ecKeyTargetAddress = new ECKey(Utils.getRandom());
  byte[] targetAddress = ecKeyTargetAddress.getAddress();
  String testKeyTargetAddress = ByteArray.toHexString(ecKeyTargetAddress.getPrivKeyBytes());
    System.out.println(Base58.encode58Check(targetAddress));

    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        100_000000L, 100, null, testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 100_000000L;
  String txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  final Frozen ownerFrozen = ownerAccount.getFrozen(0);
  Long ownerBalance = ownerAccount.getBalance();
  String methodStrSuicide = "SelfdestructTest(address)";
  String argsStrSuicide = "\"" + Base58.encode58Check(targetAddress) + "\"";
  String txidSuicide  = PublicMethod
        .triggerContract(contractAddress, methodStrSuicide, argsStrSuicide,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ex = PublicMethod.getTransactionInfoById(txidSuicide, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Account targetAccountAfter = PublicMethod.queryAccount(targetAddress,blockingStubFull);
    Frozen targetFrozenAfter = targetAccountAfter.getFrozen(0);

    Assert.assertEquals(ownerFrozen.getExpireTime(), targetFrozenAfter.getExpireTime());
    Assert.assertEquals(targetFrozenAfter.getFrozenBalance(),
        ownerFrozen.getFrozenBalance());

  }

  @Test(enabled = false, description = "targetAddress frozen to other address ,suicide contract "
      + "stake all", groups = {"contract", "daily"})
  void tvmStakeSuicideTest005() {
    ECKey ecKeyTargetAddress = new ECKey(Utils.getRandom());
  byte[] targetAddress = ecKeyTargetAddress.getAddress();
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] address = ecKey.getAddress();
  String testKeyTargetAddress = ByteArray.toHexString(ecKeyTargetAddress.getPrivKeyBytes());
    Assert.assertTrue(PublicMethod
        .sendcoin(targetAddress, 10_000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(targetAddress,5_000000L,
        3,1, ByteString.copyFrom(testAddress001),testKeyTargetAddress,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    System.out.println("aaaa" + Base58.encode58Check(targetAddress));

    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        100_000000L, 100, null, testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 100_000000L;
  String txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    System.out.println("aaaaa" + Base58.encode58Check(contractAddress));
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  final Frozen ownerFrozen = ownerAccount.getFrozen(0);
  Long ownerBalance = ownerAccount.getBalance();
  String methodStrSuicide = "SelfdestructTest(address)";
  String argsStrSuicide = "\"" + Base58.encode58Check(targetAddress) + "\"";
  String txidSuicide  = PublicMethod
        .triggerContract(contractAddress, methodStrSuicide, argsStrSuicide,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account targetAccount = PublicMethod.queryAccount(targetAddress,blockingStubFull);
  final Frozen targetFrozenAfter = targetAccount.getFrozen(0);
    ex = PublicMethod.getTransactionInfoById(txidSuicide, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(ownerFrozen.getExpireTime(), targetFrozenAfter.getExpireTime());
    Assert.assertEquals(targetFrozenAfter.getFrozenBalance(),
        ownerFrozen.getFrozenBalance());

  }

}
