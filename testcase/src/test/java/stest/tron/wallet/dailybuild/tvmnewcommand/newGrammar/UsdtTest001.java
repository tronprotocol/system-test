package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

@Slf4j
public class UsdtTest001 {

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private final String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private byte[] usdtAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  String dev58 = Base58.encode58Check(dev001Address);

  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] callerAddress = ecKey.getAddress();
  String callerAddress58 = Base58.encode58Check(callerAddress);
  String callerKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  String abi = Configuration.getByPath("testng.conf")
      .getString("abi.abi_usdt");
  String code = Configuration.getByPath("testng.conf")
      .getString("code.code_usdt");
  Long energyPrice = 0L;


  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed
        .sendcoin(dev001Address, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.freezeBalanceV2(
        testNetAccountAddress, 1000000000L, 0, testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceForReceiver(
        testNetAccountAddress, 2000000000L + PublicMethed.randomFreezeAmount.addAndGet(1),
        0, 1, ByteString.copyFrom(dev001Address), testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName = "Usdt-test";
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", 3000000000L,
            0, 70, 10000, "0", 0, null, dev001Key, dev001Address,
            blockingStubFull);
    logger.info("txid: " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);

    if (txid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }

    usdtAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed
        .getContract(usdtAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    logger.info("usdtAddress:" + Base58.encode58Check(usdtAddress));

    energyPrice = PublicMethed.getChainParametersValue(
        ProposalEnum.GetEnergyFee.getProposalName(), blockingStubFull);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "transfer when feeLimit = 0")
  public void test01TransferFeeLimitEqualZero() {
    String methedStr = "transfer(address,uint256)";
    String argsStr = "\"" + callerAddress58 + "\",100";
    PublicMethed.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.createAccount(
        testNetAccountAddress, callerAddress, testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //balance = 0 and energyLimit = 0
    ECKey tempKey = new ECKey(Utils.getRandom());
    argsStr = "\"" + Base58.encode58Check(tempKey.getAddress()) + "\",1";
    String txid = PublicMethed.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, 0, callerAddress, callerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo = PublicMethed.getTransactionInfoById(
        txid, blockingStubFull).get();
    final Protocol.Transaction transaction = PublicMethed.getTransactionById(
        txid, blockingStubFull).get();
    logger.info("transactionInfo" + transactionInfo.toString());
    Assert.assertEquals(transactionInfo.getResult(), TransactionInfo.code.FAILED);
    Assert.assertEquals(transactionInfo.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY);
    Assert.assertEquals(transactionInfo.getReceipt().getEnergyUsageTotal(), 0L);
    Assert.assertEquals(transaction.getRet(0).getContractRet(),
        Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY);
    Assert.assertEquals(transaction.getRawData().getFeeLimit(), 0L);

    //energy enough but feeLimit = 0
    PublicMethed.sendcoin(callerAddress, 2000000000L,
        testNetAccountAddress, testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(callerAddress, 300000000L, 0, callerKey, blockingStubFull);
    PublicMethed.freezeBalanceV2(callerAddress, 1000000000L, 1, callerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid2 = PublicMethed.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, 0, callerAddress, callerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo2 = PublicMethed.getTransactionInfoById(
        txid2, blockingStubFull).get();
    final Protocol.Transaction transaction2 = PublicMethed.getTransactionById(
        txid2, blockingStubFull).get();
    logger.info("transactionInfo" + transactionInfo2.toString());
    Assert.assertEquals(transactionInfo2.getResult(), TransactionInfo.code.FAILED);
    Assert.assertEquals(transactionInfo2.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY);
    Assert.assertEquals(transactionInfo2.getReceipt().getEnergyUsageTotal(), 0L);
    Assert.assertEquals(transaction2.getRet(0).getContractRet(),
        Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY);
    Assert.assertEquals(transaction2.getRawData().getFeeLimit(), 0L);
  }

  @Test(enabled = true, description = "transfer at FeeLimit boundary")
  public void test02TransferFeeLimitBoundaryTest() {
    Long exceptEnergy = 29631L - 8889; // total - origin
    String methedStr = "transfer(address,uint256)";
    ECKey tempKey = new ECKey(Utils.getRandom());
    String argsStr = "\"" + Base58.encode58Check(tempKey.getAddress()) + "\",1";
    String txid = PublicMethed.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, exceptEnergy * energyPrice, callerAddress, callerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo = PublicMethed.getTransactionInfoById(
        txid, blockingStubFull).get();
    final Protocol.Transaction transaction = PublicMethed.getTransactionById(
        txid, blockingStubFull).get();
    logger.info("transactionInfo" + transactionInfo.toString());
    Assert.assertEquals(transactionInfo.getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(transactionInfo.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.SUCCESS);
    Assert.assertEquals(transactionInfo.getReceipt().getEnergyUsage(), exceptEnergy.longValue());
    Assert.assertEquals(transaction.getRet(0).getContractRet(),
        Protocol.Transaction.Result.contractResult.SUCCESS);
    Assert.assertEquals(transaction.getRawData().getFeeLimit(), exceptEnergy * energyPrice);


    //energy is different to the address which has already had Usdt
    Long exceptEnergyOldAccount = 14631L - 4389; //total - origin
    String txid2 = PublicMethed.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, exceptEnergyOldAccount * energyPrice, callerAddress, callerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo2 = PublicMethed.getTransactionInfoById(
        txid2, blockingStubFull).get();
    final Protocol.Transaction transaction2 = PublicMethed.getTransactionById(
        txid2, blockingStubFull).get();
    logger.info("transactionInfo" + transactionInfo2.toString());
    Assert.assertEquals(transactionInfo2.getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(transactionInfo2.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.SUCCESS);
    Assert.assertEquals(transactionInfo2.getReceipt().getEnergyUsage(),
        exceptEnergyOldAccount.longValue());
    Assert.assertEquals(transaction2.getRet(0).getContractRet(),
        Protocol.Transaction.Result.contractResult.SUCCESS);
    Assert.assertEquals(transaction2.getRawData().getFeeLimit(),
        exceptEnergyOldAccount * energyPrice);
  }

  @Test(enabled = true, description = "test black list when transfer and approve")
  public void test03TransferWithBlackList() {
    ECKey tempKey = new ECKey(Utils.getRandom());
    ECKey spenderKey = new ECKey(Utils.getRandom());

    //add USDT balance to temp address
    String methodStr = "transfer(address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(tempKey.getAddress()) + "\",100";
    PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.sendcoin(tempKey.getAddress(), 1000000000,
        testNetAccountAddress, testNetAccountKey, blockingStubFull);
    PublicMethed.sendcoin(spenderKey.getAddress(), 1000000000,
        testNetAccountAddress, testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //transfer success before add black list
    methodStr = "transfer(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(spenderKey.getAddress()) + "\",1";
    String txid1 = PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, tempKey.getAddress(),
        ByteArray.toHexString(tempKey.getPrivKeyBytes()), blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo = PublicMethed.getTransactionInfoById(
        txid1, blockingStubFull).get();
    Assert.assertEquals(transactionInfo.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.SUCCESS);

    //add blackList
    Long addBlackListEnergyExcept = 21812L;
    methodStr = "addBlackList(address)";
    argsStr = "\"" + Base58.encode58Check(tempKey.getAddress()) + "\"";
    String txAddBlack = PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, addBlackListEnergyExcept * energyPrice,
        dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("txAddBlack: " + txAddBlack);
    TransactionInfo txAddBlackInfo = PublicMethed.getTransactionInfoById(
        txAddBlack, blockingStubFull).get();
    Assert.assertEquals(txAddBlackInfo.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.SUCCESS);
    Protocol.Transaction txAddBlackTx = PublicMethed.getTransactionById(
        txAddBlack, blockingStubFull).get();
    Assert.assertEquals(txAddBlackTx.getRawData().getFeeLimit(),
        energyPrice * addBlackListEnergyExcept);

    //transfer revert after add black list
    methodStr = "transfer(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(spenderKey.getAddress()) + "\",1";
    String txid2 = PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, tempKey.getAddress(),
        ByteArray.toHexString(tempKey.getPrivKeyBytes()), blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo2 = PublicMethed.getTransactionInfoById(
        txid2, blockingStubFull).get();
    Assert.assertEquals(transactionInfo2.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.REVERT);

    //approve token to another address
    methodStr = "approve(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(spenderKey.getAddress()) + "\",20";
    PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, tempKey.getAddress(),
        ByteArray.toHexString(tempKey.getPrivKeyBytes()), blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //transferFrom revert after add black list
    methodStr = "transferFrom(address,address,uint256)";
    argsStr = "\"" + Base58.encode58Check(tempKey.getAddress()) + "\"" + ",\""
      + Base58.encode58Check(dev001Address) + "\",1";
    String txid3 = PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, spenderKey.getAddress(),
        ByteArray.toHexString(spenderKey.getPrivKeyBytes()), blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo3 = PublicMethed.getTransactionInfoById(
        txid3, blockingStubFull).get();
    Assert.assertEquals(transactionInfo3.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.REVERT);


    //remove BlackList
    Long removeEnergyExcept = 7362L;
    methodStr = "removeBlackList(address)";
    argsStr = "\"" + Base58.encode58Check(tempKey.getAddress()) + "\"";
    String txRemoveBlackList = PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, energyPrice * removeEnergyExcept, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("txRemoveBlackList: " + txRemoveBlackList);
    TransactionInfo txRemoveBlackListInfo = PublicMethed.getTransactionInfoById(
        txRemoveBlackList, blockingStubFull).get();
    Assert.assertEquals(txRemoveBlackListInfo.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.SUCCESS);
    Protocol.Transaction txRemoveBlackListTx = PublicMethed.getTransactionById(
        txRemoveBlackList, blockingStubFull).get();
    Assert.assertEquals(txRemoveBlackListTx.getRawData().getFeeLimit(),
        energyPrice * removeEnergyExcept);

    //transfer success after remove black list
    methodStr = "transfer(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(spenderKey.getAddress()) + "\",1";
    String txid4 = PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, tempKey.getAddress(),
        ByteArray.toHexString(tempKey.getPrivKeyBytes()), blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo4 = PublicMethed.getTransactionInfoById(
        txid4, blockingStubFull).get();
    Assert.assertEquals(transactionInfo4.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.SUCCESS);

    //transferFrom success after remove black list
    methodStr = "transferFrom(address,address,uint256)";
    argsStr = "\"" + Base58.encode58Check(tempKey.getAddress()) + "\""
      + ",\"" + Base58.encode58Check(dev001Address) + "\",1";
    String txid5 = PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, spenderKey.getAddress(),
        ByteArray.toHexString(spenderKey.getPrivKeyBytes()), blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo5 = PublicMethed.getTransactionInfoById(
        txid5, blockingStubFull).get();
    Assert.assertEquals(transactionInfo5.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.SUCCESS);
  }

  @Test(enabled = true, description = "add and remove black list when feeLimit = 0")
  public void test04BlackListFeeLimitEqualZero() {
    final Long addBlackListEnergyExcept = 21812L;
    ECKey tempKey = new ECKey(Utils.getRandom());
    String methodStr = "addBlackList(address)";
    String argsStr = "\"" + Base58.encode58Check(tempKey.getAddress()) + "\"";
    String txAddBlack = PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, 0, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("txAddBlack: " + txAddBlack);
    TransactionInfo txAddBlackInfo = PublicMethed.getTransactionInfoById(
        txAddBlack, blockingStubFull).get();
    Assert.assertEquals(txAddBlackInfo.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY);
    Protocol.Transaction txAddBlackTx = PublicMethed.getTransactionById(
        txAddBlack, blockingStubFull).get();
    Assert.assertEquals(txAddBlackTx.getRawData().getFeeLimit(), 0L);

    String txAddBlack2 = PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, addBlackListEnergyExcept * energyPrice + 1,
        dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("txAddBlack2: " + txAddBlack2);
    TransactionInfo txAddBlackInfo2 = PublicMethed.getTransactionInfoById(
        txAddBlack2, blockingStubFull).get();
    Assert.assertEquals(txAddBlackInfo2.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.SUCCESS);
    Protocol.Transaction txAddBlackTx2 = PublicMethed.getTransactionById(
        txAddBlack2, blockingStubFull).get();
    Assert.assertEquals(txAddBlackTx2.getRawData().getFeeLimit(),
        addBlackListEnergyExcept * energyPrice + 1);

    final Long removeEnergyExcept = 7362L;
    methodStr = "removeBlackList(address)";
    argsStr = "\"" + Base58.encode58Check(tempKey.getAddress()) + "\"";
    String txRemoveBlack = PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, 0, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("txRemoveBlack: " + txRemoveBlack);
    TransactionInfo txRemoveBlackInfo = PublicMethed.getTransactionInfoById(
        txRemoveBlack, blockingStubFull).get();
    Assert.assertEquals(txRemoveBlackInfo.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY);
    Protocol.Transaction txRemoveBlackTx = PublicMethed.getTransactionById(
        txRemoveBlack, blockingStubFull).get();
    Assert.assertEquals(txRemoveBlackTx.getRawData().getFeeLimit(), 0L);


    methodStr = "removeBlackList(address)";
    argsStr = "\"" + Base58.encode58Check(tempKey.getAddress()) + "\"";
    String txRemoveBlack2 = PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, removeEnergyExcept * energyPrice, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("txRemoveBlack2: " + txRemoveBlack2);
    TransactionInfo txRemoveBlackInfo2 = PublicMethed.getTransactionInfoById(
        txRemoveBlack2, blockingStubFull).get();
    Assert.assertEquals(txRemoveBlackInfo2.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.SUCCESS);
    Protocol.Transaction txRemoveBlackTx2 = PublicMethed.getTransactionById(
        txRemoveBlack2, blockingStubFull).get();
    Assert.assertEquals(txRemoveBlackTx2.getRawData().getFeeLimit(),
        removeEnergyExcept * energyPrice);
  }

  @Test(enabled = true, description = "approve when feeLimit = 0 and boundary + 1")
  public void test05ApproveFeeLimitEqualZero() {
    final ECKey spender =  new ECKey(Utils.getRandom());
    final ECKey caller = new ECKey(Utils.getRandom());
    final byte[] callerAddress = caller.getAddress();
    final String callerKey = ByteArray.toHexString(caller.getPrivKeyBytes());
    String methodStr = "transfer(address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(caller.getAddress()) + "\",100";
    PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.sendcoin(caller.getAddress(), 1000000000L,
        testNetAccountAddress, testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //approve token to another address
    methodStr = "approve(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(spender.getAddress()) + "\",20";
    String txid =  PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, 0, callerAddress, callerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo = PublicMethed.getTransactionInfoById(
        txid, blockingStubFull).get();
    final Protocol.Transaction transaction = PublicMethed.getTransactionById(
        txid, blockingStubFull).get();
    logger.info("transactionInfo" + transactionInfo.toString());
    Assert.assertEquals(transactionInfo.getResult(), TransactionInfo.code.FAILED);
    Assert.assertEquals(transactionInfo.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY);
    Assert.assertEquals(transactionInfo.getReceipt().getEnergyUsageTotal(), 0L);
    Assert.assertEquals(transaction.getRet(0).getContractRet(),
        Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY);
    Assert.assertEquals(transaction.getRawData().getFeeLimit(), 0L);


    Long approveEnergyExcept = 22677L - 6803L; // total - origin
    String txid2 =  PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, approveEnergyExcept * energyPrice, callerAddress, callerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo2 = PublicMethed.getTransactionInfoById(
        txid2, blockingStubFull).get();
    final Protocol.Transaction transaction2 = PublicMethed.getTransactionById(
        txid2, blockingStubFull).get();
    logger.info("transactionInfo" + transactionInfo2.toString());
    Assert.assertEquals(transactionInfo2.getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(transactionInfo2.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.SUCCESS);
    Assert.assertEquals(transaction2.getRet(0).getContractRet(),
        Protocol.Transaction.Result.contractResult.SUCCESS);
    Assert.assertEquals(transaction2.getRawData().getFeeLimit(), energyPrice * approveEnergyExcept);
  }


  @Test(enabled = true, description = "approve when feeLimit = boundary - 1")
  public void test06ApproveFeeLimitBoundary() {
    final ECKey spender =  new ECKey(Utils.getRandom());
    final ECKey caller = new ECKey(Utils.getRandom());
    final byte[] callerAddress = caller.getAddress();
    final String callerKey = ByteArray.toHexString(caller.getPrivKeyBytes());
    String methodStr = "transfer(address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(caller.getAddress()) + "\",100";
    PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.sendcoin(caller.getAddress(), 1000000000L,
        testNetAccountAddress, testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long approveEnergyExcept = 22677L - 6803L; // total - origin
    //approve token to another address
    methodStr = "approve(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(spender.getAddress()) + "\",20";
    String txid =  PublicMethed.triggerContract(usdtAddress, methodStr, argsStr,
        false, 0, approveEnergyExcept * energyPrice - 1,
        callerAddress, callerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo = PublicMethed.getTransactionInfoById(
        txid, blockingStubFull).get();
    final Protocol.Transaction transaction = PublicMethed.getTransactionById(
        txid, blockingStubFull).get();
    logger.info("transactionInfo" + transactionInfo.toString());
    Assert.assertEquals(transactionInfo.getResult(), TransactionInfo.code.FAILED);
    Assert.assertEquals(transactionInfo.getReceipt().getResult(),
        Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY);
    Assert.assertEquals(transactionInfo.getReceipt().getEnergyUsageTotal(),
        22675L);
    Assert.assertEquals(transaction.getRet(0).getContractRet(),
        Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY);
    Assert.assertEquals(transaction.getRawData().getFeeLimit(),
        approveEnergyExcept * energyPrice - 1);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(dev001Address, dev001Key, testNetAccountAddress, blockingStubFull);
    PublicMethed.freedResource(callerAddress, callerKey, testNetAccountAddress, blockingStubFull);
    PublicMethed.unDelegateResourceV2(testNetAccountAddress, 1000000000L,
        1, dev001Address, testNetAccountKey, blockingStubFull);
    PublicMethed.unDelegateResourceV2(testNetAccountAddress, 300000000L,
        0, callerAddress, testNetAccountKey, blockingStubFull);

    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }
}


