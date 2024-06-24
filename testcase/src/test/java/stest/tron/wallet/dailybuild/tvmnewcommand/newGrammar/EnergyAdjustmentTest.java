package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.code;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;


@Slf4j
public class EnergyAdjustmentTest {

  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethed.getFinalAddress(testFoundationKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
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
    PublicMethed.printAddress(testKey001);
    PublicMethed.printAddress(testKey002);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext().build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    Assert.assertTrue(PublicMethed.sendcoin(testAddress001, 100000_000000L,
        testFoundationAddress, testFoundationKey, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/energyAdjustment.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddressC = PublicMethed
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, trxValue,
            100, null, testKey001,
            testAddress001, blockingStubFull);

    contractName = "D";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractAddressD = PublicMethed
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, testKey001,
            testAddress001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed
        .getContract(contractAddressC, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    smartContract = PublicMethed.getContract(contractAddressD, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
  }

  @Test(enabled = true, description = "before committe no.81,memory extend does not cost fee,"
      + " offset too big will result in cpu time out. after committe no.81, offset too big will result in out of memory,"
      + " and offset le 3145696 will cost memory extend fee.")
  void voteCostExtraEnergy01() {
    String methedStr = "test(int256,int256)";
    String argsStr = "0,3145697";
    String txid = PublicMethed.triggerContract(contractAddressD, methedStr, argsStr,
        false, 0, 15000000000L, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("voteCostExtraEnergy01 txid: " + txid);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("voteCostExtraEnergy01 info: " + info.toString());
    Assert.assertEquals(code.FAILED, info.getResult());
    Assert.assertEquals(contractResult.OUT_OF_MEMORY, info.getReceipt().getResult());
  }

  @Test(enabled = true, description = "before committe no.81,memory extend does not cost fee,"
      + " offset too big will result in cpu time out. after committe no.81, "
      + "offset gt 3145696 will result in out of memory,"
      + " and offset smaller than 3145696 will cost memory extend fee.")
  void voteCostExtraEnergy02() {
    String methedStr = "test(int256,int256)";
    String argsStr = "0,3145696";
    String txid = PublicMethed.triggerContract(contractAddressD, methedStr, argsStr,
        false, 0, 15000000000L, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("voteCostExtraEnergy02 txid: " + txid);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("voteCostExtraEnergy02 info: " + info.toString());
    long energyUsageTotal = info.getReceipt().getEnergyUsageTotal();
    Assert.assertTrue(energyUsageTotal > 31000);
    Assert.assertEquals(19199555, energyUsageTotal);

  }

  @Test(enabled = true, description = "sucide to active one account, active 1 account cost 25000 energy extra")
  void sucideToActiveAcount01() {
    String methedStr = "killme(address)";
    String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"";
    String txid = PublicMethed.triggerContract(contractAddressC, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("sucideToActiveAcount01 txid: " + txid);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("voteCostExtraEnergy02 info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    Assert.assertEquals(25319, info.getReceipt().getEnergyUsageTotal());
    String contractC = PublicMethed.queryAccount(contractAddressC, blockingStubFull).toString();
    System.out.println("contractC  ccc: " + contractC);
    Assert.assertEquals("", contractC);
    long balance = PublicMethed.queryAccount(testAddress002, blockingStubFull).getBalance();
    Assert.assertEquals(trxValue, balance);
  }

  @Test(enabled = true, description = "call sucide twice to active the same account")
  void sucideToActiveAcount02() {
    deployContractC();
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] testAddress003 = ecKey3.getAddress();
    String testKey003 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(testKey003);

    String methedStr = "testKill(address,address,address)";
    String argsStr = "\"" + Base58.encode58Check(contractAddressC) + "\",\"" + Base58.encode58Check(testAddress003)
        + "\",\"" + Base58.encode58Check(testAddress003)+"\"";
    String txid = PublicMethed.triggerContract(contractAddressD, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("sucideToActiveAcount02 txid: " + txid);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("sucideToActiveAcount02 info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    Assert.assertEquals(27283, info.getReceipt().getEnergyUsageTotal());
    String contractC = PublicMethed.queryAccount(contractAddressC, blockingStubFull).toString();
    System.out.println("contractC  ccc: " + contractC);
    Assert.assertEquals("", contractC);
    long balance = PublicMethed.queryAccount(testAddress003, blockingStubFull).getBalance();
    Assert.assertEquals(trxValue, balance);

  }

  @Test(enabled = true, description = "call sucide twice to active two different accounts,"
      + " active 1 account cost 25000 energy extra")
  void sucideToActiveAcount03() {
    deployContractC();
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] testAddress003 = ecKey3.getAddress();
    String testKey003 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

    ECKey ecKey4 = new ECKey(Utils.getRandom());
    byte[] testAddress004 = ecKey4.getAddress();
    String testKey004 = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
    PublicMethed.printAddress(testKey003);
    PublicMethed.printAddress(testKey004);

    String methedStr = "testKill(address,address,address)";
    String argsStr = "\"" + Base58.encode58Check(contractAddressC) + "\",\"" + Base58.encode58Check(testAddress003)
        + "\",\"" + Base58.encode58Check(testAddress004)+"\"";
    String txid = PublicMethed.triggerContract(contractAddressD, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("sucideToActiveAcount03 txid: " + txid);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("sucideToActiveAcount03 info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    Assert.assertEquals(52283, info.getReceipt().getEnergyUsageTotal());
    String contractC = PublicMethed.queryAccount(contractAddressC, blockingStubFull).toString();
    System.out.println("contractC  ccc: " + contractC);
    Assert.assertEquals("", contractC);
    long balance03 = PublicMethed.queryAccount(testAddress003, blockingStubFull).getBalance();
    Assert.assertEquals(trxValue, balance03);
    long balance04 = PublicMethed.queryAccount(testAddress004, blockingStubFull).getBalance();
    Assert.assertEquals(0, balance04);

  }

  @Test(enabled = true, description = "sucide: receiver account only the last char is different with the contract")
  void sucideToActiveAcount04() {
    deployContractC();
    String hex41 = ByteArray.toHexString(contractAddressC);
    int len = hex41.length();
    String sub = hex41.substring(0,len - 2) + hex41.charAt(len - 1) + hex41.charAt(len - 2);
    System.out.println("hex41: " + hex41);
    System.out.println("sub: " + sub);

    String methedStr = "killme(address)";
    String argsStr = "\"" + Base58.encode58Check(ByteArray.fromHexString(sub)) + "\"";
    String txid = PublicMethed.triggerContract(contractAddressC, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("sucideToActiveAcount04 txid: " + txid);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("sucideToActiveAcount04 info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    Assert.assertEquals(25319, info.getReceipt().getEnergyUsageTotal());
    String contractC = PublicMethed.queryAccount(contractAddressC, blockingStubFull).toString();
    System.out.println("contractC  ccc: " + contractC);
    Assert.assertEquals("", contractC);
    long balance = PublicMethed.queryAccount(ByteArray.fromHexString(sub), blockingStubFull).getBalance();
    Assert.assertEquals(trxValue, balance);

  }
  @Test(enabled = true, description = "create2 address sucide to active one account")
  void sucideToActiveAcount05() {
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] testAddress003 = ecKey3.getAddress();
    String testKey003 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(testKey003);

    byte[] create2Add = create2NewFreezeContract();
    String methedStr = "killme(address)";
    String argsStr = "\"" + Base58.encode58Check(testAddress003) + "\"";
    String txid = PublicMethed.triggerContract(create2Add, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("sucideToActiveAcount05 txid: " + txid);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("sucideToActiveAcount05 info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    Assert.assertEquals(25319, info.getReceipt().getEnergyUsageTotal());
    String contractC = PublicMethed.queryAccount(create2Add, blockingStubFull).toString();
    System.out.println("contractC  ccc: " + contractC);
    Assert.assertEquals("", contractC);
    long balance = PublicMethed.queryAccount(testAddress003, blockingStubFull).getBalance();
    Assert.assertEquals(trxValue, balance);

  }

  void deployContractC(){
    String filePath = "src/test/resources/soliditycode/energyAdjustment.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddressC = PublicMethed
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, trxValue,
            100, null, testKey001,
            testAddress001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed
        .getContract(contractAddressC, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
  }

  byte[]  create2NewFreezeContract() {
    String methedStr = "getPredictedAddress(bytes32)";
    String argsStr = "1232";
    GrpcAPI.TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(contractAddressD, methedStr, argsStr,
            false, 0, maxFeeLimit, "0", 0, testFoundationAddress, testFoundationKey, blockingStubFull);

    logger.info("getPredictedAddress transactionExtention: " + transactionExtention.toString());
    String create2Add41 = "41" + ByteArray.toHexString(transactionExtention.getConstantResult(0)
        .toByteArray()).substring(24);
    byte[] create2AddBytes = ByteArray.fromHexString(create2Add41);
    String create2Add58 = Base58.encode58Check(create2AddBytes);
    PublicMethed.sendcoin(create2AddBytes, trxValue, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    methedStr = "createDSalted(bytes32)";
    PublicMethed.triggerContract(contractAddressD, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(create2AddBytes, blockingStubFull);
//    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    return create2AddBytes;
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
