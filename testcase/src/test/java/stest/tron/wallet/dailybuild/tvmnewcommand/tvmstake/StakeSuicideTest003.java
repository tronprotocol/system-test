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
  public class StakeSuicideTest003 extends TronBaseTest {

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
  public void beforeClass() {    PublicMethod.sendcoin(testAddress001,10000000,testFoundationAddress,
        testFoundationKey,blockingStubFull);
  }

  @Test(enabled = false, description = "suicide target Address is owner Address", groups = {"contract", "daily"})
  public void stakeSuicideTest001() {
    String filePath = "src/test/resources/soliditycode/stackSuicide001.sol";
  String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000000L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txid = PublicMethod.triggerContract(contractAddress,"Stake(address,uint256)",
        "\"" + testWitnessAddress + "\",10000000",false,0,maxFeeLimit,
        testFoundationAddress, testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
    Frozen ownerFrozen = ownerAccount.getFrozen(0);
  String methedStr = "SelfdestructTest(address)";
  String argStr = "\"" + Base58.encode58Check(contractAddress) + "\"";
    txid = PublicMethod.triggerContract(contractAddress,methedStr,argStr,false,
        0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ex = PublicMethod.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    Account account = PublicMethod.queryAccount(contractAddress,blockingStubFull);
    Assert.assertEquals(account.getFrozenCount(),0);

  }

  @Test(enabled = false, description = "suicide target Address is BlackHoleAddress Address", groups = {"contract", "daily"})
  public void stakeSuicideTest002() {
    String filePath = "src/test/resources/soliditycode/stackSuicide001.sol";
  String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000000L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txid = PublicMethod.triggerContract(contractAddress,"Stake(address,uint256)",
        "\"" + testWitnessAddress + "\",10000000",false,0,maxFeeLimit,
        testFoundationAddress, testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
    Frozen ownerFrozen = ownerAccount.getFrozen(0);
  String blackHoleAddress = "TLsV52sRDL79HXGGm9yzwKibb6BeruhUzy";
  final Account accountBefore = PublicMethod
        .queryAccount(PublicMethod.decode58Check(blackHoleAddress),
            blockingStubFull);
  String methedStr = "SelfdestructTest(address)";
  String argStr = "\"" + blackHoleAddress + "\"";
    txid = PublicMethod.triggerContract(contractAddress,methedStr,argStr,false,
        0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ex = PublicMethod.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    Account account = PublicMethod.queryAccount(contractAddress,blockingStubFull);
    Assert.assertEquals(account.getFrozenCount(),0);

    Account accountAfter = PublicMethod
        .queryAccount(PublicMethod.decode58Check(blackHoleAddress),
            blockingStubFull);
    Assert.assertEquals(accountBefore.getBalance() + ex.get().getReceipt().getEnergyFee()
        + 10000000, accountAfter.getBalance());
  }

  @Test(enabled = false, description = "suicide target Address is BlackHoleAddress Address", groups = {"contract", "daily"})
  public void stakeSuicideTest003() {
    String filePath = "src/test/resources/soliditycode/stackSuicide001.sol";
  String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000000L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txid = PublicMethod.triggerContract(contractAddress,"Stake(address,uint256)",
        "\"" + testWitnessAddress + "\",10000000",false,0,maxFeeLimit,
        testFoundationAddress, testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
    Frozen ownerFrozen = ownerAccount.getFrozen(0);
  final Account accountBefore = PublicMethod
        .queryAccount(PublicMethod.decode58Check("T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb"),
            blockingStubFull);
  String methedStr = "SelfdestructTest(address)";
  String argStr = "\"" + "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb" + "\"";
    txid = PublicMethod.triggerContract(contractAddress,methedStr,argStr,false,
        0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ex = PublicMethod.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    Account account = PublicMethod.queryAccount(contractAddress,blockingStubFull);
    Assert.assertEquals(account.getFrozenCount(),0);

    Account accountAfter = PublicMethod
        .queryAccount(PublicMethod.decode58Check("T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb"),
            blockingStubFull);
    Assert.assertEquals(accountBefore.getBalance() + 10000000, accountAfter.getBalance());
  }

}
