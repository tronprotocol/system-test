package stest.tron.wallet.dailybuild.tvmnewcommand.tvmstake;

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
public class StakeSuicideTest002 extends TronBaseTest {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);
  private String testWitnessAddress = PublicMethod.getAddressString(witnessKey);
  private byte[] contractAddress;

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {    String filePath = "src/test/resources/soliditycode/stackSuicide001.sol";
  String contractName = "B";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000000L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "create2 -> stake -> suicide -> create2 the same Address", groups = {"contract", "daily"})
  public void stackSuicideAndCreate2Test001() {

    String filePath = "src/test/resources/soliditycode/stackSuicide001.sol";
  String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String methedStr = "deploy(bytes,uint256)";
  String argStr = "\"" + code + "\"," + 1;
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argStr,false,
        0,maxFeeLimit,testFoundationAddress,testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  String hex = "41" + ByteArray.toHexString(ex.get().getContractResult(0).toByteArray())
        .substring(24);
    logger.info("Deploy Address : " + Base58.encode58Check(ByteArray.fromHexString(hex)));
  byte[] ownerAddress = ByteArray.fromHexString(hex);

    methedStr = "Stake(address,uint256)";
    argStr = "\"" + testWitnessAddress + "\"," + 10_000_000;
    txid = PublicMethod.triggerContract(ownerAddress,methedStr,
        argStr,false,10_000_000,maxFeeLimit,
        testFoundationAddress, testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ex = PublicMethod.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    Account ownerAccount = PublicMethod.queryAccount(ownerAddress,blockingStubFull);
  final Frozen ownerFrozen = ownerAccount.getFrozen(0);

    methedStr = "SelfdestructTest(address)";
    argStr = "\"" + Base58.encode58Check(contractAddress) + "\"";
    txid = PublicMethod.triggerContract(ownerAddress,methedStr,argStr,false,
        0,maxFeeLimit,testFoundationAddress,testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    methedStr = "deploy(bytes,uint256)";
    argStr = "\"" + code + "\"," + 1;
    txid = PublicMethod.triggerContract(contractAddress,methedStr,argStr,false,
        0,maxFeeLimit,testFoundationAddress,testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerAccount =  PublicMethod.queryAccount(ownerAddress,blockingStubFull);
    Assert.assertEquals(ownerAccount.getBalance(),0);
    Assert.assertEquals(ownerAccount.getFrozenCount(),0);
    Assert.assertEquals(ownerAccount.getVotesCount(),0);

    Account targetAccount = PublicMethod.queryAccount(contractAddress,blockingStubFull);
    Frozen targetFrozen = targetAccount.getFrozen(0);

    Assert.assertEquals(ownerFrozen.getExpireTime(),targetFrozen.getExpireTime());
    Assert.assertEquals(ownerFrozen.getFrozenBalance(),targetFrozen.getFrozenBalance());

  }

  @Test(enabled = false, description = "create2 -> stake -> suicide -> sendcoin to create2 Address", groups = {"contract", "daily"})
  public void stackSuicideAndCreate2Test002() {
    String filePath = "src/test/resources/soliditycode/stackSuicide001.sol";
  String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String methedStr = "deploy(bytes,uint256)";
  String argStr = "\"" + code + "\"," + 2;
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argStr,false,
        0,maxFeeLimit,testFoundationAddress,testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> ex = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  String hex = "41" + ByteArray.toHexString(ex.get().getContractResult(0).toByteArray())
        .substring(24);
    logger.info("Deploy Address : " + Base58.encode58Check(ByteArray.fromHexString(hex)));
  byte[] ownerAddress = ByteArray.fromHexString(hex);

    methedStr = "Stake(address,uint256)";
    argStr = "\"" + testWitnessAddress + "\"," + 10_000_000;
    txid = PublicMethod.triggerContract(ownerAddress,methedStr,
        argStr,false,10_000_000,maxFeeLimit,
        testFoundationAddress, testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ex = PublicMethod.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    Account ownerAccount = PublicMethod.queryAccount(ownerAddress,blockingStubFull);
  final Frozen ownerFrozen = ownerAccount.getFrozen(0);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();

    methedStr = "SelfdestructTest(address)";
    argStr = "\"" + Base58.encode58Check(testAddress001) + "\"";
    txid = PublicMethod.triggerContract(ownerAddress,methedStr,argStr,false,
        0,maxFeeLimit,testFoundationAddress,testFoundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long sendcoin = 1;
    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress,sendcoin,testFoundationAddress,
        testFoundationKey,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerAccount =  PublicMethod.queryAccount(ownerAddress,blockingStubFull);
    Assert.assertEquals(ownerAccount.getBalance(),sendcoin);
    Assert.assertEquals(ownerAccount.getFrozenCount(),0);
    Assert.assertEquals(ownerAccount.getVotesCount(),0);

    Account targetAccount = PublicMethod.queryAccount(testAddress001,blockingStubFull);
    Frozen targetFrozen = targetAccount.getFrozen(0);

    Assert.assertEquals(ownerFrozen.getExpireTime(),targetFrozen.getExpireTime());
    Assert.assertEquals(ownerFrozen.getFrozenBalance(),targetFrozen.getFrozenBalance());
  }

}
