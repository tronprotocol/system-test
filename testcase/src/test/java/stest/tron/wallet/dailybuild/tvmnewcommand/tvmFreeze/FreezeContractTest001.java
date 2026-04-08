package stest.tron.wallet.dailybuild.tvmnewcommand.tvmFreeze;

import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.code;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class FreezeContractTest001 extends TronBaseTest {

  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey2.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] testAddress003 = ecKey3.getAddress();
  String testKey003 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  private long freezeEnergyUseage;
  private byte[] create2Address;
  private final long freezeCount = 1000_123456L;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {    if(PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {      throw new SkipException("Skipping freezeV2 test case");
    }

    Assert.assertTrue(PublicMethod.sendcoin(testAddress001,2000_000000L,
        testFoundationAddress,testFoundationKey,blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(testAddress002,10_000000L,
        testFoundationAddress,testFoundationKey,blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(testAddress003,12000_000000L,
        testFoundationAddress,testFoundationKey,blockingStubFull));
  String filePath = "src/test/resources/soliditycode/freezeContract001.sol";
  String contractName = "TestFreeze";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000_000000L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }


  @Test(description = "contract freeze to account", groups = {"contract", "daily"})
  void FreezeContractTest001() {

    AccountResourceMessage account002_before = PublicMethod
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_before = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"," + freezeCount + "," + "1";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethod
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_before.getEnergyLimit : " + account002_before.getEnergyLimit());
    logger.info("account002_after.getEnergyLimit : " + account002_after.getEnergyLimit());
    Assert.assertTrue(account002_before.getEnergyLimit() < account002_after.getEnergyLimit());
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy() + freezeCount,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance() - freezeCount,
        contractAccount_after.getBalance());

    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    freezeEnergyUseage = info.getReceipt().getEnergyUsageTotal();


  }

  @Test(description = "contract freeze to self", groups = {"contract", "daily"})
  void FreezeContractTest002() {
    AccountResourceMessage contractResource_before = PublicMethod
        .getAccountResource(contractAddress,blockingStubFull);
    Account contractAccount_before = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
  String argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"," + freezeCount + "," + "1";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage contractResource_after = PublicMethod
        .getAccountResource(contractAddress,blockingStubFull);
    Account contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_before.getEnergyLimit : " + contractResource_before.getEnergyLimit());
    logger.info("account002_after.getEnergyLimit : " + contractResource_after.getEnergyLimit());
    Assert.assertTrue(
        contractResource_before.getEnergyLimit() < contractResource_after.getEnergyLimit());
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getFrozenBalanceForEnergy().getFrozenBalance() + freezeCount,
        contractAccount_after.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance());

  }

  @Test(description = "contract freeze to other contract", groups = {"contract", "daily"})
  void FreezeContractTest003() {
    String filePath = "src/test/resources/soliditycode/freezeContract001.sol";
  String contractName = "TestFreeze";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] newContract = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 100_000000L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);


    Account contractAccount_before = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
  String argsStr = "\"" + Base58.encode58Check(newContract) + "\"," + freezeCount + "," + "1";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    Assert.assertEquals(TransactionInfo.code.FAILED,info.getResult());

    AccountResourceMessage contractResource_after = PublicMethod
        .getAccountResource(newContract,blockingStubFull);
    Account contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_after.getEnergyLimit : " + contractResource_after.getEnergyLimit());
    Assert.assertEquals(contractResource_after.getEnergyLimit(),0);
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy(),
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance(),contractAccount_after.getBalance());

  }

  @Test(description = "contract freeze to unactive account",
      dependsOnMethods = "FreezeContractTest001", groups = {"contract", "daily"})
  void FreezeContractTest004() {

    ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] testAddress = ecKey.getAddress();
  String testKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    Account contractAccount_before = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testAddress) + "\"," + freezeCount + "," + "1";
    logger.info("argsStr: " + argsStr);
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethod
        .getAccountResource(testAddress,blockingStubFull);
    Account contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_after.getEnergyLimit : " + account002_after.getEnergyLimit());
    Assert.assertTrue(account002_after.getEnergyLimit() > 0);
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy() + freezeCount,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance() - freezeCount,
        contractAccount_after.getBalance());
  // check active account status
    Account testAccount = PublicMethod.queryAccount(testAddress,blockingStubFull);
    Assert.assertTrue(testAccount.getCreateTime() > 0);
    Assert.assertNotNull(testAccount.getOwnerPermission());
    Assert.assertNotNull(testAccount.getActivePermissionList());


    TransactionInfo info = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get();
    Assert.assertEquals(freezeEnergyUseage + 25000L, info.getReceipt().getEnergyUsageTotal());


  }

  @Test(description = "contract freeze to pre create2 address, and UnFreeze",
      dependsOnMethods = "FreezeContractTest001", groups = {"contract", "daily"})
  void FreezeContractTest005() {
    String create2ArgsStr = "1";
  String create2MethedStr = "deploy(uint256)";
    TransactionExtention exten = PublicMethod.triggerConstantContractForExtention(
        contractAddress, create2MethedStr, create2ArgsStr, false, 0, maxFeeLimit,
        "#", 0, testAddress001, testKey001, blockingStubFull);
  String addressHex =
        "41" + ByteArray.toHexString(exten.getConstantResult(0).toByteArray())
            .substring(24);
    logger.info("address_hex: " + addressHex);
    create2Address = ByteArray.fromHexString(addressHex);
    logger.info("create2Address: " + Base58.encode58Check(create2Address));


    Account contractAccount_before = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
  String argsStr = "\"" + Base58.encode58Check(create2Address) + "\"," + freezeCount + "," + "1";
    logger.info("argsStr: " + argsStr);
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethod
        .getAccountResource(create2Address,blockingStubFull);
    Account contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_after.getEnergyLimit : " + account002_after.getEnergyLimit());
    Assert.assertTrue(account002_after.getEnergyLimit() > 0);
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy() + freezeCount,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance() - freezeCount,
        contractAccount_after.getBalance());

    TransactionInfo info = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get();
    Assert.assertEquals(freezeEnergyUseage + 25000L, info.getReceipt().getEnergyUsageTotal());

    txid = PublicMethod.triggerContract(contractAddress,create2MethedStr,
        create2ArgsStr,false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);


    contractAccount_before = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    methedStr = "getExpireTime(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(create2Address) + "\"" + ",1";
    TransactionExtention extention = PublicMethod
        .triggerConstantContractForExtention(contractAddress,methedStr,argsStr,
            false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
  Long ExpireTime = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime: " + ExpireTime);
    Assert.assertTrue(ExpireTime > 0);

    methedStr = "unfreeze(address,uint256)";
    txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy() - freezeCount,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance() + freezeCount,
        contractAccount_after.getBalance());

  }

  @Test(description = "Unfreeze when freeze to account",
      dependsOnMethods = "FreezeContractTest001", groups = {"contract", "daily"})
  void UnFreezeContractTest001() {

    AccountResourceMessage account002_before = PublicMethod
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_before = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "getExpireTime(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"" + ",1";
    TransactionExtention extention = PublicMethod
        .triggerConstantContractForExtention(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
  Long ExpireTime = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime: " + ExpireTime);
    Assert.assertTrue(ExpireTime > 0);

    methedStr = "unfreeze(address,uint256)";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethod
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_before.getEnergyLimit : " + account002_before.getEnergyLimit());
    logger.info("account002_after.getEnergyLimit : " + account002_after.getEnergyLimit());

    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy() - freezeCount,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());

    Assert.assertTrue(account002_before.getEnergyLimit() > account002_after.getEnergyLimit());

  }

  @Test(description = "Unfreeze when freeze to contract self",
      dependsOnMethods = "FreezeContractTest002", groups = {"contract", "daily"})
  void UnFreezeContractTest002() {

    Account contractAccount_before = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "getExpireTime(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"" + ",1";
    TransactionExtention extention = PublicMethod
        .triggerConstantContractForExtention(contractAddress,methedStr,argsStr,
            false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
  Long ExpireTime = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime: " + ExpireTime);
    Assert.assertTrue(ExpireTime > 0);

    methedStr = "unfreeze(address,uint256)";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethod
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_after.getEnergyLimit : " + account002_after.getEnergyLimit());

    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getFrozenBalanceForEnergy().getFrozenBalance() - freezeCount,
        contractAccount_after.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance());


  }

  @Test(description = "energy caulate after transaction end", groups = {"contract", "daily"})
  public void freezeEnergyCaulate() {

    String methedStr = "freeze(address,uint256,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testAddress001) + "\"," + freezeCount + "," + "1";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get();
    AccountResourceMessage testAccount001 = PublicMethod
        .getAccountResource(testAddress001,blockingStubFull);


    Assert.assertTrue(testAccount001.getEnergyLimit() > 0);
    Assert.assertTrue(info.getReceipt().getEnergyFee() > 0);
    Assert.assertTrue(testAccount001.getEnergyLimit() > info.getReceipt().getEnergyUsageTotal());

    methedStr = "unfreeze(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(testAddress001) + "\",1";
    txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    info = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get();
    testAccount001 = PublicMethod.getAccountResource(testAddress001,blockingStubFull);

    Assert.assertEquals(code.SUCESS,info.getResult());
    Assert.assertEquals(contractResult.SUCCESS,info.getReceipt().getResult());


    Assert.assertEquals(0, info.getReceipt().getEnergyFee());
    Assert.assertEquals(0, testAccount001.getEnergyLimit());
    Assert.assertTrue(testAccount001.getEnergyUsed() > 0);
  }

  @Test(description = "Get Zero Address ExpireTime,used to be that freeze to contract self",
      dependsOnMethods = "FreezeContractTest002", groups = {"contract", "daily"})
  public void getZeroExpireTimeTest() {
    long startTime = System.currentTimeMillis() - 6 * 1000;
  String ExpireTimeMethedStr = "getExpireTime(address,uint256)";
  String ExpireTimeArgsStr = "\"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb\"" + ",0";
    TransactionExtention extention = PublicMethod
        .triggerConstantContractForExtention(contractAddress,ExpireTimeMethedStr,ExpireTimeArgsStr,
            false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
  Long ExpireTime1 = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime1: " + ExpireTime1);
    Assert.assertEquals(0,ExpireTime1.longValue());

    ExpireTimeArgsStr = "\"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb\"" + ",1";
    extention = PublicMethod
        .triggerConstantContractForExtention(contractAddress,ExpireTimeMethedStr,ExpireTimeArgsStr,
            false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
  Long ExpireTime2 = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime2: " + ExpireTime2);
    Assert.assertEquals(0,ExpireTime2.longValue());
  // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
  String argsStr = "\"" + "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb" + "\"," + freezeCount + "," + "1";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    Assert.assertEquals(code.SUCESS,info.getResult());
    Assert.assertEquals(contractResult.SUCCESS,info.getReceipt().getResult());

    extention = PublicMethod
        .triggerConstantContractForExtention(contractAddress,ExpireTimeMethedStr,ExpireTimeArgsStr,
            false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
  Long ExpireTime = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime: " + ExpireTime + " nextBlockTimeStamp: " + info.getBlockTimeStamp());

    Assert.assertTrue(ExpireTime * 1000 <= info.getBlockTimeStamp());
    Assert.assertTrue(ExpireTime * 1000 > startTime);

  }

  @Test(description = "freeze in constructor", groups = {"contract", "daily"})
  public void FreezeContractTest006() {

    AccountResourceMessage account003_before = PublicMethod
        .getAccountResource(testAddress003,blockingStubFull);
  String filePath = "src/test/resources/soliditycode/freezeContract001.sol";
  String contractName = "D";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    long callValue = 10000_000000L;
  byte[] contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, callValue,
            100, null, testKey003,
            testAddress003, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account003_after = PublicMethod
        .getAccountResource(testAddress003,blockingStubFull);
    Account contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_before.getEnergyLimit : " + account003_before.getEnergyLimit());
    logger.info("account002_after.getEnergyLimit : " + account003_after.getEnergyLimit());
    Assert.assertTrue(account003_before.getEnergyLimit() < account003_after.getEnergyLimit());
    Assert.assertEquals(callValue,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(0, contractAccount_after.getBalance());
  }

}
