package stest.tron.wallet.dailybuild.tvmnewcommand.tvmstake;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class StackSuicideTest001 extends TronBaseTest {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);
  private String testWitnessAddress = PublicMethod.getAddressString(witnessKey);  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    PublicMethod.printAddress(testKey001);    PublicMethod
        .sendcoin(testAddress001, 1000_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = false, description = "targetAddress no TRX, and no frozen", groups = {"contract", "daily"})
  public void stackSuicideTest001() {

    String filePath = "src/test/resources/soliditycode/stackSuicide001.sol";
  String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000000L, 100, null, testKey001,
            testAddress001, blockingStubFull);
  final byte[] targetAddress = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0, 100, null, testKey001, testAddress001, blockingStubFull);
  String txid = PublicMethod.triggerContract(contractAddress,"Stake(address,uint256)",
        "\"" + testWitnessAddress + "\",10000000",false,0,maxFeeLimit,
        testFoundationAddress, testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  final Frozen ownerFrozen = ownerAccount.getFrozen(0);
  String methedStr = "SelfdestructTest(address)";
  String argStr = "\"" + Base58.encode58Check(targetAddress) + "\"";
    txid = PublicMethod.triggerContract(contractAddress,methedStr,argStr,false,
        0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ex = PublicMethod.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    Account targetAccount = PublicMethod.queryAccount(targetAddress,blockingStubFull);
    Frozen targetFrozen = targetAccount.getFrozen(0);

    Assert.assertEquals(ownerFrozen.getExpireTime(),targetFrozen.getExpireTime());
    Assert.assertEquals(ownerFrozen.getFrozenBalance(),targetFrozen.getFrozenBalance());

  }

  @Test(enabled = false, description = "targetAddress has TRX, but no frozen", groups = {"contract", "daily"})
  public void stackSuicideTest002() {
    String filePath = "src/test/resources/soliditycode/stackSuicide001.sol";
  String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000000L, 100, null, testKey001,
            testAddress001, blockingStubFull);
  Long targetBalance = 10_000_000L;
  final byte[] targetAddress = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, targetBalance, 100, null, testKey001, testAddress001, blockingStubFull);
  String methedStr = "Stake(address,uint256)";
  String argStr = "\"" + testWitnessAddress + "\",10000000";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,
        argStr,false,0,maxFeeLimit,
        testFoundationAddress, testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  final Frozen ownerFrozen = ownerAccount.getFrozen(0);

    methedStr = "SelfdestructTest(address)";
    argStr = "\"" + Base58.encode58Check(targetAddress) + "\"";
    txid = PublicMethod.triggerContract(contractAddress,methedStr,argStr,false,
        0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ex = PublicMethod.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    Account targetAccount = PublicMethod.queryAccount(targetAddress,blockingStubFull);
    Frozen targetFrozen = targetAccount.getFrozen(0);

    Assert.assertEquals(ownerFrozen.getExpireTime(),targetFrozen.getExpireTime());
    Assert.assertEquals(ownerFrozen.getFrozenBalance(),targetFrozen.getFrozenBalance());

    methedStr = "transfer(address,uint256)";
    argStr = "\"" + Base58.encode58Check(testAddress001) + "\"," + targetBalance;
    txid = PublicMethod.triggerContract(targetAddress,methedStr,argStr,false,0,
        maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(0,PublicMethod.queryAccount(targetAddress,blockingStubFull).getBalance());
  }

  @Test(enabled = false, description = "targetAddress has TRX, and has frozen", groups = {"contract", "daily"})
  public void stackSuicideTest003() {
    Long targetBalance = 10_000_000L;
  String filePath = "src/test/resources/soliditycode/stackSuicide001.sol";
  String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, targetBalance, 100,
            null, testKey001, testAddress001, blockingStubFull);
  final byte[] targetAddress = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 12_345_678L, 100, null, testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methedStr = "Stake(address,uint256)";
  String argStr = "\"" + testWitnessAddress + "\"," + targetBalance;
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,
        argStr,false,0,maxFeeLimit, testFoundationAddress, testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    argStr = "\"" + testWitnessAddress + "\"," + 12_000_000L;
  String txid2 = PublicMethod.triggerContract(targetAddress,methedStr,argStr,false,
        0,maxFeeLimit,testFoundationAddress,testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ex = PublicMethod.getTransactionInfoById(txid2,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  final Frozen ownerFrozen = ownerAccount.getFrozen(0);

    Account targetAccount = PublicMethod.queryAccount(targetAddress,blockingStubFull);
  final Frozen targetFrozen = targetAccount.getFrozen(0);

    methedStr = "SelfdestructTest(address)";
    argStr = "\"" + Base58.encode58Check(targetAddress) + "\"";
    txid = PublicMethod.triggerContract(contractAddress,methedStr,argStr,false,
        0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ex = PublicMethod.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    targetAccount = PublicMethod.queryAccount(targetAddress,blockingStubFull);
    Frozen targetFrozenAfter = targetAccount.getFrozen(0);

    BigInteger expected =
        BigInteger.valueOf(ownerFrozen.getExpireTime())
        .multiply(BigInteger.valueOf(ownerFrozen.getFrozenBalance()))
        .add(BigInteger.valueOf(targetFrozen.getExpireTime())
            .multiply(BigInteger.valueOf(targetFrozen.getFrozenBalance())))
        .divide(BigInteger.valueOf(ownerFrozen.getFrozenBalance())
            .add(BigInteger.valueOf(targetFrozen.getFrozenBalance())));

    Assert.assertEquals(expected.longValue(), targetFrozenAfter.getExpireTime());
    Assert.assertEquals(targetFrozenAfter.getFrozenBalance(),
        ownerFrozen.getFrozenBalance() + targetFrozen.getFrozenBalance());

    methedStr = "transfer(address,uint256)";
    argStr = "\"" + Base58.encode58Check(testAddress001) + "\"," + 345678;
    txid = PublicMethod.triggerContract(targetAddress,methedStr,argStr,false,0,
        maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(0,PublicMethod.queryAccount(targetAddress,blockingStubFull).getBalance());
  }

}
