package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.code;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.HashMap;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class EnergyAdjustmentTest extends TronBaseTest {

  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddressC;
  private byte[] contractAddressD;
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey2.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  long trxValue = 100L;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(testKey001);
    PublicMethod.printAddress(testKey002);    Assert.assertTrue(PublicMethod.sendcoin(testAddress001, 100000_000000L,
        testFoundationAddress, testFoundationKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/energyAdjustment.sol";
  String contractName = "C";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddressC = PublicMethod
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, trxValue,
            100, null, testKey001,
            testAddress001, blockingStubFull);

    contractName = "D";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractAddressD = PublicMethod
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, testKey001,
            testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod
        .getContract(contractAddressC, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    smartContract = PublicMethod.getContract(contractAddressD, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
  }

  @Test(enabled = true, description = "before committe no.81,memory extend does not cost fee,"
      + " offset too big will result in cpu time out. after committe no.81, offset too big will result in out of memory,"
      + " and offset le 3145696 will cost memory extend fee.", groups = {"contract", "daily"})
  void voteCostExtraEnergy01() {
    String methedStr = "test(int256,int256)";
  String argsStr = "0,3145697";
  String txid = PublicMethod.triggerContract(contractAddressD, methedStr, argsStr,
        false, 0, 15000000000L, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("voteCostExtraEnergy01 txid: " + txid);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("voteCostExtraEnergy01 info: " + info.toString());
    Assert.assertEquals(code.FAILED, info.getResult());
    Assert.assertEquals(contractResult.OUT_OF_MEMORY, info.getReceipt().getResult());
  }

  @Test(enabled = true, description = "before committe no.81,memory extend does not cost fee,"
      + " offset too big will result in cpu time out. after committe no.81, "
      + "offset gt 3145696 will result in out of memory,"
      + " and offset smaller than 3145696 will cost memory extend fee.", groups = {"contract", "daily"})
  void voteCostExtraEnergy02() {
    String methedStr = "test(int256,int256)";
  String argsStr = "0,3145696";
  String txid = PublicMethod.triggerContract(contractAddressD, methedStr, argsStr,
        false, 0, 15000000000L, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("voteCostExtraEnergy02 txid: " + txid);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("voteCostExtraEnergy02 info: " + info.toString());
    long energyUsageTotal = info.getReceipt().getEnergyUsageTotal();
    Assert.assertTrue(energyUsageTotal > 31000);
    Assert.assertEquals(19199554, energyUsageTotal);

  }

  @Test(enabled = true, description = "sucide to active one account, active 1 account cost 25000 energy extra", groups = {"contract", "daily"})
  void suicideToActiveAccount01() {
    String methedStr = "killme(address)";
  String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"";
  String txid = PublicMethod.triggerContract(contractAddressC, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("suicideToActiveAccount01 txid: " + txid);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("voteCostExtraEnergy02 info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertEquals(30319, info.getReceipt().getEnergyUsageTotal());
    }else {
      Assert.assertEquals(25319, info.getReceipt().getEnergyUsageTotal());
    }
    Protocol.Account contractC = PublicMethod.queryAccount(contractAddressC, blockingStubFull);
    System.out.println("contractC  ccc: " + contractC);
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertNotEquals("", contractC.toString());
      Assert.assertEquals(0L, contractC.getBalance());
      Assert.assertEquals(0L, contractC.getFrozenV2(0).getAmount());
      Assert.assertEquals(0L, contractC.getFrozenV2(1).getAmount());
      Assert.assertEquals(0L, contractC.getFrozenV2(2).getAmount());
    }else {
      Assert.assertEquals("", contractC.toString());
    }
    long balance = PublicMethod.queryAccount(testAddress002, blockingStubFull).getBalance();
    Assert.assertEquals(trxValue, balance);
  }

  @Test(enabled = true, description = "call sucide twice to active the same account", groups = {"contract", "daily"})
  void suicideToActiveAccount02() {
    deployContractC();
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] testAddress003 = ecKey3.getAddress();
  String testKey003 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethod.printAddress(testKey003);
  String methedStr = "testKill(address,address,address)";
  String argsStr = "\"" + Base58.encode58Check(contractAddressC) + "\",\"" + Base58.encode58Check(testAddress003)
        + "\",\"" + Base58.encode58Check(testAddress003)+"\"";
  String txid = PublicMethod.triggerContract(contractAddressD, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("suicideToActiveAccount02 txid: " + txid);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("suicideToActiveAccount02 info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertEquals(36992, info.getReceipt().getEnergyUsageTotal());
    }else {
      Assert.assertEquals(26993, info.getReceipt().getEnergyUsageTotal());
    }
    String contractC = PublicMethod.queryAccount(contractAddressC, blockingStubFull).toString();
    System.out.println("contractC  ccc: " + contractC);
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertNotEquals("", contractC);
    }else {
      Assert.assertEquals("", contractC);
    }
    long balance = PublicMethod.queryAccount(testAddress003, blockingStubFull).getBalance();
    Assert.assertEquals(trxValue, balance);

  }

  @Test(enabled = true, description = "call sucide twice to active two different accounts,"
      + " active 1 account cost 25000 energy extra", groups = {"contract", "daily"})
  void suicideToActiveAccount03() {
    deployContractC();
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] testAddress003 = ecKey3.getAddress();
  String testKey003 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] testAddress004 = ecKey4.getAddress();
  String testKey004 = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
    PublicMethod.printAddress(testKey003);
    PublicMethod.printAddress(testKey004);
  String methedStr = "testKill(address,address,address)";
  String argsStr = "\"" + Base58.encode58Check(contractAddressC) + "\",\"" + Base58.encode58Check(testAddress003)
        + "\",\"" + Base58.encode58Check(testAddress004)+"\"";
  String txid = PublicMethod.triggerContract(contractAddressD, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("suicideToActiveAccount03 txid: " + txid);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("suicideToActiveAccount03 info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertEquals(61992, info.getReceipt().getEnergyUsageTotal());
    }else {
      Assert.assertEquals(51993, info.getReceipt().getEnergyUsageTotal());
    }
    String contractC = PublicMethod.queryAccount(contractAddressC, blockingStubFull).toString();
    System.out.println("contractC  ccc: " + contractC);
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertNotEquals("", contractC);
    }else {
      Assert.assertEquals("", contractC);
    }
    long balance03 = PublicMethod.queryAccount(testAddress003, blockingStubFull).getBalance();
    Assert.assertEquals(trxValue, balance03);
    long balance04 = PublicMethod.queryAccount(testAddress004, blockingStubFull).getBalance();
    Assert.assertEquals(0, balance04);

  }

  @Test(enabled = true, description = "sucide: receiver account only the last char is different with the contract", groups = {"contract", "daily"})
  void suicideToActiveAccount04() {
    deployContractC();
  String hex41 = ByteArray.toHexString(contractAddressC);
  int len = hex41.length();
  int c = hex41.charAt(len - 1) - '0';
  int last = c % 9 + 1;
  String sub = hex41.substring(0,len - 1) + last;
    logger.info("hex41: " + hex41);
    logger.info("sub: " + sub);
  String methedStr = "killme(address)";
  String argsStr = "\"" + Base58.encode58Check(ByteArray.fromHexString(sub)) + "\"";
  String txid = PublicMethod.triggerContract(contractAddressC, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("suicideToActiveAccount04 txid: " + txid);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("suicideToActiveAccount04 info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertEquals(30319, info.getReceipt().getEnergyUsageTotal());
    }else {
      Assert.assertEquals(25319, info.getReceipt().getEnergyUsageTotal());
    }
    String contractC = PublicMethod.queryAccount(contractAddressC, blockingStubFull).toString();
    System.out.println("contractC  ccc: " + contractC);
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertNotEquals("", contractC);
    }else {
      Assert.assertEquals("", contractC);
    }
    long balance = PublicMethod.queryAccount(ByteArray.fromHexString(sub), blockingStubFull).getBalance();
    Assert.assertEquals(trxValue, balance);

  }
  @Test(enabled = true, description = "create2 address sucide to active one account", groups = {"contract", "daily"})
  void suicideToActiveAccount05() {
    ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] testAddress003 = ecKey3.getAddress();
  String testKey003 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethod.printAddress(testKey003);
  byte[] create2Add = create2NewFreezeContract();
  String methedStr = "killme(address)";
  String argsStr = "\"" + Base58.encode58Check(testAddress003) + "\"";
  String txid = PublicMethod.triggerContract(create2Add, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("suicideToActiveAccount05 txid: " + txid);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("suicideToActiveAccount05 info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertEquals(30319, info.getReceipt().getEnergyUsageTotal());
    }else {
      Assert.assertEquals(25319, info.getReceipt().getEnergyUsageTotal());
    }
    String contractC = PublicMethod.queryAccount(create2Add, blockingStubFull).toString();
    System.out.println("contractC  ccc: " + contractC);
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertNotEquals("", contractC);
    }else {
      Assert.assertEquals("", contractC);
    }
    long balance = PublicMethod.queryAccount(testAddress003, blockingStubFull).getBalance();
    Assert.assertEquals(trxValue, balance);

  }

  void deployContractC(){
    String filePath = "src/test/resources/soliditycode/energyAdjustment.sol";
  String contractName = "C";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddressC = PublicMethod
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, trxValue,
            100, null, testKey001,
            testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod
        .getContract(contractAddressC, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
  }

  byte[]  create2NewFreezeContract() {
    String methedStr = "getPredictedAddress(bytes32)";
  String argsStr = "1232";
    GrpcAPI.TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtention(contractAddressD, methedStr, argsStr,
            false, 0, maxFeeLimit, "0", 0, testFoundationAddress, testFoundationKey, blockingStubFull);

    logger.info("getPredictedAddress transactionExtention: " + transactionExtention.toString());
  String create2Add41 = "41" + ByteArray.toHexString(transactionExtention.getConstantResult(0)
        .toByteArray()).substring(24);
  byte[] create2AddBytes = ByteArray.fromHexString(create2Add41);
  String create2Add58 = Base58.encode58Check(create2AddBytes);
    PublicMethod.sendcoin(create2AddBytes, trxValue, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    methedStr = "createDSalted(bytes32)";
    PublicMethod.triggerContract(contractAddressD, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(create2AddBytes, blockingStubFull);
//    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    return create2AddBytes;
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {  }


}
