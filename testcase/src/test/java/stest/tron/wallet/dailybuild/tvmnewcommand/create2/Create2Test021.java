package stest.tron.wallet.dailybuild.tvmnewcommand.create2;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class Create2Test021 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final String name = "Asset008_" + Long.toString(now);
  private static final long totalSupply = now;
  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] bytes;
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";
  ByteString assetAccountId = null;
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] resourceOnwerAddress = ecKey2.getAddress();
  String resourceOnwerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  private byte[] contractExcAddress = ecKey3.getAddress();
  private String contractExcKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    initSolidityChannel();
    PublicMethod.printAddress(contractExcKey);
    PublicMethod.printAddress(resourceOnwerKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    if(PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {      throw new SkipException("Skipping freezeV2 test case");
    }

  }

  @Test(enabled = true, description = "resource delegate with create2 contract, and suicide ", groups = {"contract", "daily"})
  public void test1TriggerContract() {
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    Assert.assertTrue(PublicMethod
        .sendcoin(resourceOnwerAddress, 1000000000L + 1024000000L, testNetAccountAddress,
            testNetAccountKey,
            blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(testNetAccountAddress, 10000000000L, 0, 0,
        ByteString.copyFrom(resourceOnwerAddress), testNetAccountKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(testNetAccountAddress, 10000000000L, 0, 1,
        ByteString.copyFrom(resourceOnwerAddress), testNetAccountKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Create 3 the same name token.
    Long start = System.currentTimeMillis() + 2000;
  Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethod.createAssetIssue(resourceOnwerAddress,
        name, totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, resourceOnwerKey, blockingStubFull));
  String filePath = "src/test/resources/soliditycode/create2contractn.sol";
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
  Long beforeExcAccountBalance = PublicMethod
        .queryAccount(resourceOnwerAddress, blockingStubFull).getBalance();
  //  create2 TestContract
    String contractName1 = "TestConstract";
    HashMap retMap1 = PublicMethod.getBycodeAbi(filePath, contractName1);
  String code1 = retMap1.get("byteCode").toString();
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
  byte[] returnAddressBytes = infoById.get().getInternalTransactions(0).getTransferToAddress()
        .toByteArray();
  String returnAddress = Base58.encode58Check(returnAddressBytes);
    logger.info("returnAddress:" + returnAddress);

    bytes = returnAddressBytes;
  // freezeBalanceForReceiver to create2 contract Address, transaction Failed

    Assert.assertFalse(PublicMethod.freezeBalanceForReceiver(resourceOnwerAddress, 5000000L, 0, 0,
        ByteString.copyFrom(bytes), resourceOnwerKey, blockingStubFull));
    Assert.assertFalse(PublicMethod.freezeBalanceForReceiver(resourceOnwerAddress, 5000000L, 0, 1,
        ByteString.copyFrom(bytes), resourceOnwerKey, blockingStubFull));
  Long afterExcAccountBalance = PublicMethod.queryAccount(resourceOnwerAddress, blockingStubFull)
        .getBalance();
    Assert.assertTrue(PublicMethod.getAccountResource(bytes, blockingStubFull).getNetLimit() == 0);
    Assert
        .assertTrue(PublicMethod.getAccountResource(bytes, blockingStubFull).getEnergyLimit() == 0);
    logger.info("afterExcAccountBalance: " + afterExcAccountBalance);
    logger.info("beforeExcAccountBalance:" + beforeExcAccountBalance);

    Assert.assertTrue(afterExcAccountBalance - beforeExcAccountBalance == 0);
  // create2 Address Suicide
    String param2 = "\"" + Base58.encode58Check(contractExcAddress) + "\"";
  String txidn = PublicMethod
        .triggerContract(bytes,
            "testSuicideNonexistentTarget(address)", param2, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // active create2 Address to normal Address
    Assert.assertTrue(PublicMethod
        .sendcoin(bytes, 1000000L, contractExcAddress, contractExcKey, blockingStubFull));
  //Trigger contract to transfer trx and token.
    Account getAssetIdFromAccount = PublicMethod
        .queryAccount(resourceOnwerAddress, blockingStubFull);
    assetAccountId = getAssetIdFromAccount.getAssetIssuedID();
  Long contractBeforeBalance = PublicMethod.queryAccount(bytes, blockingStubFull).getBalance();

    Assert.assertTrue(
        PublicMethod.transferAsset(bytes, assetAccountId.toByteArray(), 100, resourceOnwerAddress,
            resourceOnwerKey,
            blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account account1 = PublicMethod.queryAccount(bytes, blockingStubFull);
  int typeValue1 = account1.getTypeValue();
    Assert.assertEquals(0, typeValue1);
  // freezeBalanceForReceiver to "create2" contract Address, transaction SUCCESS
    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(resourceOnwerAddress, 1000000L, 0, 0,
        ByteString.copyFrom(bytes), resourceOnwerKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(resourceOnwerAddress, 1000000L, 0, 1,
        ByteString.copyFrom(bytes), resourceOnwerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    beforeExcAccountBalance = PublicMethod.queryAccount(resourceOnwerAddress, blockingStubFull)
        .getBalance();

    Assert.assertTrue(PublicMethod.unFreezeBalance(resourceOnwerAddress, resourceOnwerKey,
        0, bytes, blockingStubFull));
    Assert.assertTrue(PublicMethod.unFreezeBalance(resourceOnwerAddress, resourceOnwerKey,
        1, bytes, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long afterUnfreezeBalance = PublicMethod.queryAccount(resourceOnwerAddress, blockingStubFull)
        .getBalance();
    Assert.assertTrue(afterUnfreezeBalance == beforeExcAccountBalance + 1000000L * 2);
  // create2 TestContract to turn AccountType To create2 Contract Address
    txid = PublicMethod
        .triggerContract(contractAddress,
            "deploy(bytes,uint256)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  // triggercontract Create2 address, function normal
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
  Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
    Account account = PublicMethod.queryAccount(returnAddressBytes, blockingStubFull);
  int typeValue = account.getTypeValue();
    Assert.assertEquals(2, typeValue);
    Assert.assertEquals(account.getBalance(), 1000000);
  }

  @Test(enabled = true, description = "Create2 contract can transfer trx and token.", groups = {"contract", "daily"})
  public void test2TriggerContract() {
    Account accountbefore = PublicMethod.queryAccount(bytes, blockingStubFull);
  int typeValue = accountbefore.getTypeValue();
    Assert.assertEquals(2, typeValue);
    long accountbeforeBalance = accountbefore.getBalance();
    Assert.assertEquals(accountbeforeBalance, 1000000);
    Account contractExcAddressbefore = PublicMethod
        .queryAccount(contractExcAddress, blockingStubFull);
    long contractExcAddressbeforeBalance = contractExcAddressbefore.getBalance();
  String num = "1";
  String txid = PublicMethod
        .triggerContract(bytes,
            "testTransfer(uint256)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> transactionInfoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(transactionInfoById.get().getResultValue() == 0);
  Long fee1 = transactionInfoById.get().getFee();

    Account accountafter = PublicMethod.queryAccount(bytes, blockingStubFull);
    long accountafterBalance = accountafter.getBalance();
    Assert.assertTrue(accountbeforeBalance - 1 == accountafterBalance);

    Account contractExcAddressafter = PublicMethod
        .queryAccount(contractExcAddress, blockingStubFull);
    long contractExcAddressafterBalance = contractExcAddressafter.getBalance();
    Assert.assertTrue(contractExcAddressbeforeBalance + 1 - fee1 == contractExcAddressafterBalance);

    num = "1" + ",\"" + assetAccountId.toStringUtf8() + "\"";
  Long returnAddressBytesAccountCountBefore = PublicMethod
        .getAssetIssueValue(bytes, assetAccountId, blockingStubFull);
  Long contractExcAddressAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  String txid1 = PublicMethod
        .triggerContract(bytes,
            "testTransferToken(uint256,trcToken)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> transactionInfoById1 = PublicMethod
        .getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(transactionInfoById1.get().getResultValue() == 0);
  Long returnAddressBytesAccountCountAfter = PublicMethod
        .getAssetIssueValue(bytes, assetAccountId, blockingStubFull);
  Long contractExcAddressAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Assert.assertTrue(
        returnAddressBytesAccountCountBefore - 1 == returnAddressBytesAccountCountAfter);
    Assert.assertTrue(
        contractExcAddressAccountCountBefore + 1 == contractExcAddressAccountCountAfter);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    Assert.assertTrue(PublicMethod.unFreezeBalance(testNetAccountAddress, testNetAccountKey,
        0, resourceOnwerAddress, blockingStubFull));
    Assert.assertTrue(PublicMethod.unFreezeBalance(testNetAccountAddress, testNetAccountKey,
        1, resourceOnwerAddress, blockingStubFull));    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }  }


}