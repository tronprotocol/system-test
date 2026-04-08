package stest.tron.wallet.dailybuild.tvmnewcommand.create2;

import static org.hamcrest.core.StringContains.containsString;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS_VALUE;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class Create2Test019 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(contractExcKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    }

  @Test(enabled = true, description = "seted Value of Contract that created by create2,"
      + " should not be stored after contact suicided ande create2 again", groups = {"contract", "daily"})
  public void testTriggerContract() {
    String sendcoin = PublicMethod
        .sendcoinGetTransactionId(contractExcAddress, 1000000000L, testNetAccountAddress,
            testNetAccountKey,
            blockingStubFull);

    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 1000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById0 = null;
    infoById0 = PublicMethod.getTransactionInfoById(sendcoin, blockingStubFull);
    logger.info("infoById0   " + infoById0.get());
    Assert.assertEquals(ByteArray.toHexString(infoById0.get().getContractResult(0).toByteArray()),
        "");
    Assert.assertEquals(infoById0.get().getResult().getNumber(), 0);
    Optional<Transaction> ById = PublicMethod.getTransactionById(sendcoin, blockingStubFull);
    Assert.assertEquals(ById.get().getRet(0).getContractRet().getNumber(),
        SUCCESS_VALUE);
    Assert.assertEquals(ById.get().getRet(0).getContractRetValue(), SUCCESS_VALUE);
    Assert.assertEquals(ById.get().getRet(0).getContractRet(), contractResult.SUCCESS);
  String filePath = "src/test/resources/soliditycode/create2contractn2.sol";
  String contractName = "Factory";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String contractName1 = "TestConstract";
    HashMap retMap1 = PublicMethod.getBycodeAbi(filePath, contractName1);
  String code1 = retMap1.get("byteCode").toString();
  String abi1 = retMap1.get("abI").toString();
  String txid = "";
  String num = "\"" + code1 + "\"" + "," + 1;
    txid = PublicMethod
        .triggerContract(contractAddress,
            "deploy(bytes,uint256)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  byte[] returnAddressBytes = infoById.get().getInternalTransactions(0).getTransferToAddress()
        .toByteArray();
  String returnAddress = Base58.encode58Check(returnAddressBytes);
    logger.info("returnAddress:" + returnAddress);
    txid = PublicMethod
        .triggerContract(returnAddressBytes,
            "i()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long fee1 = infoById1.get().getFee();
  Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
  Long energyUsed1 = infoById1.get().getReceipt().getEnergyUsage();
  Long netFee1 = infoById1.get().getReceipt().getNetFee();
    long energyUsageTotal1 = infoById1.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee1:" + fee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("energyUsed1:" + energyUsed1);
    logger.info("netFee1:" + netFee1);
    logger.info("energyUsageTotal1:" + energyUsageTotal1);

    Account infoafter1 = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
  Long afterBalance1 = infoafter1.getBalance();
  Long afterEnergyUsed1 = resourceInfoafter1.getEnergyUsed();
  Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
  Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterEnergyUsed:" + afterEnergyUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance1 + fee1 == afterBalance);
    Assert.assertTrue(afterEnergyUsed + energyUsed1 >= afterEnergyUsed1);
  Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
    txid = PublicMethod
        .triggerContract(returnAddressBytes,
            "set()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    txid = PublicMethod
        .triggerContract(returnAddressBytes,
            "i()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById1 = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(5 == returnnumber);
  String param1 = "\"" + Base58.encode58Check(returnAddressBytes) + "\"";

    txid = PublicMethod
        .triggerContract(returnAddressBytes,
            "testSuicideNonexistentTarget(address)", param1, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById2 = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);

    Assert.assertEquals("suicide", ByteArray
        .toStr(infoById2.get().getInternalTransactions(0).getNote().toByteArray()));
    TransactionExtention transactionExtention = PublicMethod
        .triggerContractForExtention(returnAddressBytes,
            "i()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
    System.out.println(transactionExtention.toString());
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertEquals(true, transactionExtention.getResult().getResult());
    }else {
      Assert.assertThat(transactionExtention.getResult().getCode().toString(),
                      containsString("CONTRACT_VALIDATE_ERROR"));
      Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
                      containsString("Contract validate error : No contract or not a valid smart contract"));
    }

    txid = PublicMethod
        .triggerContract(contractAddress,
            "deploy(bytes,uint256)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById3 = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      System.out.println(infoById3);
      Assert.assertEquals(TransactionInfo.code.FAILED, infoById3.get().getResult());
    }else {
      byte[] returnAddressBytes1 = infoById3.get().getInternalTransactions(0).getTransferToAddress()
              .toByteArray();
      String returnAddress1 = Base58.encode58Check(returnAddressBytes1);
      Assert.assertEquals(returnAddress1, returnAddress);
      txid = PublicMethod
              .triggerContract(returnAddressBytes1,
                      "i()", "#", false,
                      0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

      PublicMethod.waitProduceNextBlock(blockingStubFull);
      infoById1 = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
      returnnumber = ByteArray.toLong(ByteArray
              .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
      Assert.assertTrue(1 == returnnumber);
    }

  }


}
