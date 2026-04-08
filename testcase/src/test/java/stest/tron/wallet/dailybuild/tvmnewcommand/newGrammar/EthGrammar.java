package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class EthGrammar extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  private final byte[] witness001Address = PublicMethod.getFinalAddress(witnessKey);
  byte[] contractC = null;
  byte[] contractD = null;
  byte[] create2Address;
  String create2Str;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contractExcKey);    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 300100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/EthGrammar.sol";
  String contractName = "C";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractC = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(contractC,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    contractName = "D";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);

    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractD = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    smartContract = PublicMethod.getContract(contractD,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }


  @Test(enabled = true, description = "test get base fee value = commit.No 11 energy fee", groups = {"contract", "daily"})
  public void test01baseFee() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractC,
            "baseFee()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long basefee = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("basefee: " + basefee);
    long energyfee;
    Protocol.ChainParameters chainParameters = blockingStubFull
        .getChainParameters(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<Protocol.ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
  String key = "";
    boolean flag = false;
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      key = getChainParameters.get().getChainParameter(i).getKey();
      if ("getEnergyFee".equals(key)) {
        energyfee = getChainParameters.get().getChainParameter(i).getValue();
        logger.info("energyfee: " + energyfee);
        Assert.assertEquals(basefee, energyfee);
        flag = true;
      }
    }
    Assert.assertTrue(flag);
  }

  @Test(enabled = true, description = "test get gas price value = commit.No 11 energy fee", groups = {"contract", "daily"})
  public void test02GasPrice() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractC,
            "gasPrice()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
  }

  @Test(enabled = true, description = "get create2 address, test get base fee ", groups = {"contract", "daily"})
  public void test03BaseFeeFromCreate2() {
    String methedStr = "deploy(uint256)";
  String argsStr = "1";
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());
  String create2Str =
        "41" + ByteArray.toHexString(info.get().getContractResult(0).toByteArray())
            .substring(24);
    logger.info("hex create2 address: " + create2Str);
    create2Address = ByteArray.fromHexString(create2Str);
    logger.info("create2Address: " + Base58.encode58Check(create2Address));

    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(create2Address,
            "baseFeeOnly()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long basefee = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("basefee: " + basefee);
    long energyfee;
    Protocol.ChainParameters chainParameters = blockingStubFull
        .getChainParameters(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<Protocol.ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
  String key = "";
    boolean flag = false;
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      key = getChainParameters.get().getChainParameter(i).getKey();
      if ("getEnergyFee".equals(key)) {
        energyfee = getChainParameters.get().getChainParameter(i).getValue();
        logger.info("energyfee: " + energyfee);
        Assert.assertEquals(basefee, energyfee);
        flag = true;
      }
    }
    Assert.assertTrue(flag);

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(create2Address,
            "gasPriceOnly()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long gasprice = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("gasprice: " + gasprice);
    Assert.assertEquals(basefee, gasprice);
  }

  @Test(enabled = true, description = "call can use 63/64 energy in new contract", groups = {"contract", "daily"})
  public void test04CallEnergy() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] transferToAddress = ecKey1.getAddress();
  String transferToKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethod.printAddress(transferToKey);
  Long temMaxLimitFee = 200000000L;
  String methedStr = "testCall(address,address)";
  String argsStr = "\"" + Base58.encode58Check(contractD) + "\"," + "\""
        + Base58.encode58Check(transferToAddress) + "\"";
  String txid = PublicMethod.triggerContract(contractC, methedStr, argsStr,
        false, 0, temMaxLimitFee, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Protocol.Account testAccount =
        PublicMethod.queryAccountByAddress(transferToAddress, blockingStubFull);
    logger.info("testAccount: " + testAccount.toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
    Assert.assertTrue(info.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(info.get().getReceipt().getEnergyFee() < temMaxLimitFee);
  }

  @Test(enabled = true, description = "create2 address call can use 63/64 energy in new contract", groups = {"contract", "daily"})
  public void test05Create2AddressCallEnergy() {
    String methedStr = "deploy(uint256)";
  String argsStr = "2";
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());
  String create2Str =
        "41" + ByteArray.toHexString(info.get().getContractResult(0).toByteArray())
            .substring(24);
    logger.info("hex create2 address: " + create2Str);
    create2Address = ByteArray.fromHexString(create2Str);
    logger.info("create2Address: " + Base58.encode58Check(create2Address));
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] transferToAddress = ecKey1.getAddress();
  String transferToKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethod.printAddress(transferToKey);
  Long temMaxLimitFee = 200000000L;
    methedStr = "testCall(address,address)";
    argsStr = "\"" + Base58.encode58Check(contractD) + "\"," + "\""
        + Base58.encode58Check(transferToAddress) + "\"";
    txid = PublicMethod.triggerContract(create2Address, methedStr, argsStr,
        false, 0, temMaxLimitFee, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);

    Protocol.Account testAccount =
        PublicMethod.queryAccountByAddress(transferToAddress, blockingStubFull);
    Assert.assertEquals("", testAccount.toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
    Assert.assertTrue(info.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(info.get().getReceipt().getEnergyFee() < temMaxLimitFee);
  }

  @Test(enabled = true, description = "create2 address delegatecall "
      + "can use 63/64 energy in new contract", groups = {"contract", "daily"})
  public void test06Create2AddressDelegateCallEnergy() {
    String methedStr = "deploy(uint256)";
  String argsStr = "5";
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());
  String create2Str =
        "41" + ByteArray.toHexString(info.get().getContractResult(0).toByteArray())
            .substring(24);
    logger.info("hex create2 address: " + create2Str);
    create2Address = ByteArray.fromHexString(create2Str);
    logger.info("create2Address: " + Base58.encode58Check(create2Address));
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] transferToAddress = ecKey1.getAddress();
  String transferToKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethod.printAddress(transferToKey);
  Long temMaxLimitFee = 200000000L;
    methedStr = "testDelegateCall(address,address)";
    argsStr = "\"" + Base58.encode58Check(contractD) + "\"," + "\""
        + Base58.encode58Check(transferToAddress) + "\"";
    txid = PublicMethod.triggerContract(create2Address, methedStr, argsStr,
        false, 0, temMaxLimitFee, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);

    Protocol.Account testAccount =
        PublicMethod.queryAccountByAddress(transferToAddress, blockingStubFull);
    Assert.assertEquals("", testAccount.toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
    Assert.assertTrue(info.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(info.get().getReceipt().getEnergyFee() < temMaxLimitFee);
  }

  @Test(enabled = true, description = "create2 address this.function "
      + "can use 63/64 energy in new contract", groups = {"contract", "daily"})
  public void test07Create2AddressCallFunctionEnergy() {
    String methedStr = "deploy(uint256)";
  String argsStr = "6";
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());
  String create2Str =
        "41" + ByteArray.toHexString(info.get().getContractResult(0).toByteArray())
            .substring(24);
    logger.info("hex create2 address: " + create2Str);
    create2Address = ByteArray.fromHexString(create2Str);
    logger.info("create2Address: " + Base58.encode58Check(create2Address));
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] transferToAddress = ecKey1.getAddress();
  String transferToKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethod.printAddress(transferToKey);
  Long temMaxLimitFee = 200000000L;
    methedStr = "testCallFunctionInContract(address)";
    argsStr = "\"" + Base58.encode58Check(transferToAddress) + "\"";
    txid = PublicMethod.triggerContract(create2Address, methedStr, argsStr,
        false, 0, temMaxLimitFee, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);

    Protocol.Account testAccount =
        PublicMethod.queryAccountByAddress(transferToAddress, blockingStubFull);
    Assert.assertEquals("", testAccount.toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
    Assert.assertTrue(info.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(info.get().getReceipt().getEnergyFee() < temMaxLimitFee);
  }

  //
  @Test(enabled = true, description = "test get Ripemd160 input is 123", groups = {"contract", "daily"})
  public void test08getRipemd160() {
    String args = "\"123\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractC,
            "getRipemd160(string)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  String result = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("e3431a8e0adbf96fd140103dc6f63a3f8fa343ab000000000000000000000000", result);
  }

  @Test(enabled = true, description = "test get Ripemd160 input is empty", groups = {"contract", "daily"})
  public void test09getRipemd160() {
    String args = "\"\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractC,
            "getRipemd160(string)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  String result = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("9c1185a5c5e9fc54612808977ee8f548b2258d31000000000000000000000000", result);
  }

  @Test(enabled = true, description = "test get Ripemd160 input length is greater than 256", groups = {"contract", "daily"})
  public void test10getRipemd160() {
    String args = "\"111111111111ddddddddddddd0x0000000000000000000000008b56a0602cc81fb0"
        + "b99bce992b3198c0bab181ac111111111111ddddddddddddd0x0000000000000000000000008b56"
        + "a0602cc81fb0b99bce992b3198c0bab181ac%^$#0000008b56a0602cc81fb0b99bce99"
        + "2b3198c0bab181ac%^$#0000008b56a0602cc81fb0b99bce992b3198c0bab181ac%^$#\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractC,
            "getRipemd160(string)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  String result = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("173c283ebcbad0e1c623a5c0f6813cb663338369000000000000000000000000", result);
  }

  @Test(enabled = true, description = "test get Ripemd160 input is string "
      + "and do not convert to bytes", groups = {"contract", "daily"})
  public void test11getRipemd160Str() {
    String args = "\"data\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractC,
            "getRipemd160Str(string)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  String result = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("cd43325b85172ca28e96785d0cb4832fd62cdf43000000000000000000000000", result);
  }

  @Test(enabled = true, description = "test get Ripemd160 input is string and "
      + "do not convert to bytes", groups = {"contract", "daily"})
  public void test12getRipemd160Str() {
    String args = "\"000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractC,
            "getRipemd160Str(string)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  String result = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("efe2df697b79b5eb73a577251ce3911078811fa4000000000000000000000000", result);
  }

  @Test(enabled = true, description = "test blake2f", groups = {"contract", "daily"})
  public void test13getBlak2f() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractC,
            "callF()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  String result = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("ba80a53f981c4d0d6a2797b69f12f6e94c212f14685ac"
        + "4b74b12bb6fdbffa2d17d87c5392aab792dc252d5de4533cc9518d38aa8dbf1925ab92386edd4009923",
        result);

  }

  @Test(enabled = true, description = "when call create2, stack depth will be checked"
      + "if stack depth is greater than 64, then create command will revert"
      + "but run environment can not compute so much, so the actual result is time out", groups = {"contract", "daily"})
  public void test14FixCreate2StackDepth() {
    String methedStr = "fixCreate2StackDepth(uint256)";
  String argsStr = "123";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("transactionExtention: " + transactionExtention.toString());
  String message = ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray());
    Assert.assertTrue(message.contains("CPU timeout"));
    /*int interCount = transactionExtention.getInternalTransactionsCount();
  int createCount = 0;
    for(int i=0;i<interCount;i++){
      if("63726561746532".equals(transactionExtention.getInternalTransactions(i).getNote())){
        createCount ++;
      }
    }
    Assert.assertTrue(createCount >= 15 && createCount <= 64);*/
  }

  @Test(enabled = true, description = "when call create, stack depth will be checked."
      + "if stack depth is greater than 64, then create command will revert"
      + "but run environment can not compute so much, so the actual result is time out", groups = {"contract", "daily"})
  public void test15FixCreateStackDepth() {

    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractD,
            "fixCreateStackDepth()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  String message = ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray());
    logger.info("transactionExtention: " + transactionExtention.toString());
    Assert.assertTrue(message.contains("CPU timeout"));
    /*int interCount = transactionExtention.getInternalTransactionsCount();
  int createCount = 0;
    for(int i=0;i<interCount;i++){
      if("637265617465".equals(transactionExtention.getInternalTransactions(i).getNote())){
        createCount ++;
      }
    }
    Assert.assertTrue(createCount >= 15 && createCount <= 64);*/

  }

  @Test(enabled = false, description = "test max Energy Limit For trigger Constant contract", groups = {"contract", "daily"})
  public void test16MaxEnergyLimitForConstant() {
    String methedStr = "transfer(address)";
  String argsStr = "\"" + Base58.encode58Check(testNetAccountAddress) + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    System.out.println("transactionExtention: " + transactionExtention.toString());
  }

  @Test(enabled = false, description = "commit NO.47 value can be 1e17 if commit No.63 opened"
      +  " move this case to TransactionFee001.java", groups = {"contract", "daily"})
  public void test17Commit47Value() {
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(47L, 100000000000000000L);
    org.testng.Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);  }


}

