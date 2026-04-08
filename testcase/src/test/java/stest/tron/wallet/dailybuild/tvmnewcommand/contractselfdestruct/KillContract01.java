package stest.tron.wallet.dailybuild.tvmnewcommand.contractselfdestruct;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.code;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.HashMap;
import java.util.List;
import stest.tron.wallet.common.client.utils.Flaky;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
@Flaky(reason = "Selfdestruct timing between blocks",
    since = "2026-04-03")
public class KillContract01 extends TronBaseTest {

  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  ECKey ecKeyExc = new ECKey(Utils.getRandom());
  byte[] excAdd = ecKeyExc.getAddress();
  String excKey = ByteArray.toHexString(ecKeyExc.getPrivKeyBytes());

  ECKey targetEckey = new ECKey(Utils.getRandom());
  byte[] targetAdd = targetEckey.getAddress();
  String target58 = Base58.encode58Check(targetAdd);
  String targetKey = ByteArray.toHexString(targetEckey.getPrivKeyBytes());

  ECKey delegateReceiverEckey = new ECKey(Utils.getRandom());
  byte[] delegateReceiverAdd = delegateReceiverEckey.getAddress();
  String delegateReceiver58 = Base58.encode58Check(delegateReceiverAdd);
  String delegateReceiverEckeyKey = ByteArray.toHexString(delegateReceiverEckey.getPrivKeyBytes());

  private byte[] contractAddressD;
  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private String description = Configuration.getByPath("testng.conf")
          .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
          .getString("defaultParameter.assetUrl");

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(excKey);
    PublicMethod.printAddress(targetKey);
    PublicMethod.printAddress(delegateReceiverEckeyKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext().build();    Assert.assertTrue(PublicMethod.sendcoin(excAdd, 100000_000000L,
        testFoundationAddress, testFoundationKey, blockingStubFull));

    Assert.assertTrue(PublicMethod.sendcoin(delegateReceiverAdd, 1_000000L,
        testFoundationAddress, testFoundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/killcontract01.sol";
    String contractName = "D";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddressD = PublicMethod
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, excKey,
                excAdd, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(contractAddressD, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethod
            .createAssetIssue(excAdd, tokenName, TotalSupply, 1, 1000, start, end, 1,
                    description, url, 100000L, 100000L, 3L, 30L, excKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethod.queryAccount(excAdd, blockingStubFull).getAssetIssuedID();
    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());

    //init sr and vote array
    String methedStr = "initArray()";
    String txid  = PublicMethod.triggerContract(contractAddressD, methedStr, "",
            false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get();
    logger.info("before class initArray: " +info);
    Assert.assertEquals(TransactionInfo.code.SUCESS, info.getResult());
  }

  @Test(enabled = true, description = "getPredictedAddress,sendcoin,transferAsset,and then " +
          "make create2,freezev2,delegateResource,delegateResource,unfreezeBalanceV2,cancelAllUnfreezeV2,vote,kill " +
          "into one tranaction", groups = {"contract", "daily"})
  void kill01() {
    String methedStr = "getPredictedAddress(bytes32)";
    String argsStr = "1122";
    TransactionExtention transactionExtention =
            PublicMethod.triggerConstantContractForExtention(contractAddressD, methedStr, argsStr,
                    false, 0, maxFeeLimit, "0", 0, excAdd, excKey, blockingStubFull);

    logger.info("getPredictedAddress transactionExtention: " + transactionExtention.toString());
    String create2Add41 = "41" + ByteArray.toHexString(transactionExtention.getConstantResult(0)
            .toByteArray()).substring(24);
    byte[] create2AddBytes = ByteArray.fromHexString(create2Add41);
    String create2Add58 = Base58.encode58Check(create2AddBytes);
    Assert.assertTrue(PublicMethod.sendcoin(create2AddBytes, 2001000000L,
            testFoundationAddress, testFoundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.transferAsset(create2AddBytes,
            assetAccountId.toByteArray(), 100L, excAdd, excKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    methedStr = "complexCreateKill(bytes32,address,address)";
    String args = argsStr +",\"" + target58 + "\",\"" + delegateReceiver58 + "\"";
    String txid  = PublicMethod.triggerContract(contractAddressD, methedStr, args,
            false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get();
    logger.info("kill01: " +info);
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(23, info.getInternalTransactionsCount());
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(create2AddBytes, blockingStubFull);
    Account create2Account = PublicMethod.queryAccount(create2AddBytes, blockingStubFull);
    logger.info("kill01 create2Account: " + create2Account.toString());
    logger.info("kill01 smartContract: " + smartContract.toString());
    Assert.assertEquals("", smartContract.toString());
    Assert.assertEquals("", create2Account.toString());
    Account targetAccount = PublicMethod.queryAccount(targetAdd,blockingStubFull);
    Account delegateReceiverAccount = PublicMethod.queryAccount(delegateReceiverAdd,blockingStubFull);
    Assert.assertEquals(1801000000L, targetAccount.getBalance());
    Assert.assertEquals(100000000L, targetAccount.getFrozenV2(0).getAmount());
    Assert.assertEquals(100000000L, targetAccount.getFrozenV2(1).getAmount());
    Assert.assertEquals(0, targetAccount.getVotesList().size());
    long targetAssetCount = PublicMethod.getAssetIssueValue(targetAdd,
            assetAccountId, blockingStubFull);
    Assert.assertEquals(100, targetAssetCount);
    Assert.assertEquals(1_000000L, delegateReceiverAccount.getBalance());
    AccountResourceMessage delegateReceiverResource = PublicMethod.getAccountResource(delegateReceiverAdd, blockingStubFull);
    Assert.assertEquals(0, delegateReceiverResource.getNetLimit());
    Assert.assertEquals(0, delegateReceiverResource.getEnergyLimit());
    AccountResourceMessage targetResource = PublicMethod.getAccountResource(targetAdd, blockingStubFull);
    Assert.assertNotEquals(0, targetResource.getNetLimit());
    Assert.assertNotEquals(0, targetResource.getEnergyLimit());
    long delegateReceiverAssetCount = PublicMethod.getAssetIssueValue(delegateReceiverAdd,
            assetAccountId, blockingStubFull);
    Assert.assertEquals(0, delegateReceiverAssetCount);
    long execAssetCount = PublicMethod.getAssetIssueValue(excAdd,
            assetAccountId, blockingStubFull);
    Assert.assertEquals(897, execAssetCount);
  }

  @Test(enabled = true, description = "getPredictedAddress,sendcoin,transferAsset,and then " +
          "make create2,freezev2,delegateResource,delegateResource,unfreezeBalanceV2,cancelAllUnfreezeV2," +
          "vote into one transaction, and kill in another transaction. and can not static call kill", groups = {"contract", "daily"})
  void kill02() {
    String methedStr = "getPredictedAddress(bytes32)";
    String argsStr = "1122";
    TransactionExtention transactionExtention =
            PublicMethod.triggerConstantContractForExtention(contractAddressD, methedStr, argsStr,
                    false, 0, maxFeeLimit, "0", 0, excAdd, excKey, blockingStubFull);

    logger.info("getPredictedAddress transactionExtention: " + transactionExtention.toString());
    String create2Add41 = "41" + ByteArray.toHexString(transactionExtention.getConstantResult(0)
            .toByteArray()).substring(24);
    byte[] create2AddBytes = ByteArray.fromHexString(create2Add41);
    String create2Add58 = Base58.encode58Check(create2AddBytes);
    Assert.assertTrue(PublicMethod.sendcoin(create2AddBytes, 2001000000L,
            testFoundationAddress, testFoundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.transferAsset(create2AddBytes,
            assetAccountId.toByteArray(), 100L, excAdd, excKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    methedStr = "complexCreate(bytes32,address,address)";
    String args = argsStr +",\"" + target58 + "\",\"" + delegateReceiver58 + "\"";
    String txid1  = PublicMethod.triggerContract(contractAddressD, methedStr, args,
            false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    //can not static call kill
    methedStr = "staticcallSelfdestruct(address,address)";
    args = "\"" + create2Add58 + "\"," + "\"" + target58 + "\"";
    String txid22  = PublicMethod.triggerContract(contractAddressD, methedStr, args,
            false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info22 = PublicMethod.getTransactionInfoById(txid22,blockingStubFull).get();
    logger.info("kill02-info22: " +info22);
    Assert.assertEquals(code.SUCESS, info22.getResult());

    methedStr = "killme(address)";
    args = "\"" + target58 + "\"";
    String txid2  = PublicMethod.triggerContract(create2AddBytes, methedStr, args,
            false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    //check result
    TransactionInfo info1 = PublicMethod.getTransactionInfoById(txid1,blockingStubFull).get();
    logger.info("kill02: " +info1);
    Assert.assertEquals(code.SUCESS, info1.getResult());
    Assert.assertEquals(21, info1.getInternalTransactionsCount());
    TransactionInfo info2 = PublicMethod.getTransactionInfoById(txid2, blockingStubFull).get();
    logger.info("kill02 info2 : " +info2);
    Assert.assertEquals(code.SUCESS, info2.getResult());
    Assert.assertEquals(1, info2.getInternalTransactionsCount());
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(create2AddBytes, blockingStubFull);
    Account create2Account = PublicMethod.queryAccount(create2AddBytes, blockingStubFull);
    logger.info("kill02 create2Account: " + create2Account.toString());
    logger.info("kill02 smartContract: " + smartContract.toString());
    Assert.assertNotEquals("", smartContract.toString());
    Assert.assertNotEquals("", create2Account.toString());
    Assert.assertEquals(0, create2Account.getBalance());
    Assert.assertEquals(0, create2Account.getFrozenV2(0).getAmount());
    Assert.assertEquals(0, create2Account.getFrozenV2(1).getAmount());
    Assert.assertEquals(0, create2Account.getVotesList().size());

    Account targetAccount = PublicMethod.queryAccount(targetAdd,blockingStubFull);
    Account delegateReceiverAccount = PublicMethod.queryAccount(delegateReceiverAdd,blockingStubFull);
    Assert.assertEquals(1801000000L*2, targetAccount.getBalance());
    Assert.assertEquals(100000000L*2, targetAccount.getFrozenV2(0).getAmount());
    Assert.assertEquals(100000000L*2, targetAccount.getFrozenV2(1).getAmount());
    Assert.assertEquals(0, targetAccount.getVotesList().size());
    long targetAssetCount = PublicMethod.getAssetIssueValue(targetAdd,
            assetAccountId, blockingStubFull);
    Assert.assertEquals(100*2, targetAssetCount);
    Assert.assertEquals(1_000000L, delegateReceiverAccount.getBalance());
    AccountResourceMessage delegateReceiverResource = PublicMethod.getAccountResource(delegateReceiverAdd, blockingStubFull);
    Assert.assertEquals(0, delegateReceiverResource.getNetLimit());
    Assert.assertEquals(0, delegateReceiverResource.getEnergyLimit());
    AccountResourceMessage targetResource = PublicMethod.getAccountResource(targetAdd, blockingStubFull);
    Assert.assertNotEquals(0, targetResource.getNetLimit());
    Assert.assertNotEquals(0, targetResource.getEnergyLimit());
    long delegateReceiverAssetCount = PublicMethod.getAssetIssueValue(delegateReceiverAdd,
            assetAccountId, blockingStubFull);
    Assert.assertEquals(0, delegateReceiverAssetCount);
    long execAssetCount = PublicMethod.getAssetIssueValue(excAdd,
            assetAccountId, blockingStubFull);
    Assert.assertEquals(797, execAssetCount);
  }

  @Test(enabled = true, description = "getPredictedAddress,sendcoin,transferAsset,and then " +
          "make create2,freezev2,kill into one transaction, and kill through 3 call", groups = {"contract", "daily"})
  void kill03() {
    ECKey targetEckey = new ECKey(Utils.getRandom());
    byte[] targetAdd03 = targetEckey.getAddress();
    String target03 = Base58.encode58Check(targetAdd03);
    String targetKey03 = ByteArray.toHexString(targetEckey.getPrivKeyBytes());
    String filePath = "src/test/resources/soliditycode/killcontract01.sol";
    String contractName = "C";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    String code1 = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractC = PublicMethod
            .deployContractFallback(contractName, abi, code1, "", maxFeeLimit, 0L,
                    100, null, excKey, excAdd, blockingStubFull);
    String contractCBase58 = Base58.encode58Check(contractC);
    contractName = "B";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code1 = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    byte[] contractB = PublicMethod
            .deployContractFallback(contractName, abi, code1, "", maxFeeLimit, 0L,
                    100, null, excKey, excAdd, blockingStubFull);
    String contractBBase58 = Base58.encode58Check(contractB);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(contractC, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());

    smartContract = PublicMethod.getContract(contractB, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());

    String methedStr = "getPredictedAddress(bytes32)";
    String argsStr = "1123";
    TransactionExtention transactionExtention =
            PublicMethod.triggerConstantContractForExtention(contractAddressD, methedStr, argsStr,
                    false, 0, maxFeeLimit, "0", 0, excAdd, excKey, blockingStubFull);

    logger.info("kill03 getPredictedAddress transactionExtention: " + transactionExtention.toString());
    String create2Add41 = "41" + ByteArray.toHexString(transactionExtention.getConstantResult(0)
            .toByteArray()).substring(24);
    byte[] create2AddBytes = ByteArray.fromHexString(create2Add41);
    String create2Add58 = Base58.encode58Check(create2AddBytes);
    Assert.assertTrue(PublicMethod.sendcoin(create2AddBytes, 2001000000L,
            testFoundationAddress, testFoundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.transferAsset(create2AddBytes,
            assetAccountId.toByteArray(), 100L, excAdd, excKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    methedStr = "createKill(bytes32,address,address,address)";
    String args = argsStr +",\"" + contractCBase58 + "\",\"" + contractBBase58 + "\",\"" +target03 + "\"" ;
    String txid1  = PublicMethod.triggerContract(contractAddressD, methedStr, args,
            false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info1 = PublicMethod.getTransactionInfoById(txid1,blockingStubFull).get();
    logger.info("kill03 info1: " +info1);
    Assert.assertEquals(code.SUCESS, info1.getResult());
    System.out.println(PublicMethod.queryAccount(create2AddBytes, blockingStubFull));
    Assert.assertEquals(9, info1.getInternalTransactionsCount());

    Account create2Account = PublicMethod.queryAccount(create2AddBytes, blockingStubFull);
    logger.info("kill02 create2Account: " + create2Account.toString());
    Assert.assertEquals("", create2Account.toString());
    Account targetAccount = PublicMethod.queryAccount(targetAdd03,blockingStubFull);
    Assert.assertEquals(1801000000L, targetAccount.getBalance());
    Assert.assertEquals(100000000L, targetAccount.getFrozenV2(0).getAmount());
    Assert.assertEquals(100000000L, targetAccount.getFrozenV2(1).getAmount());
    Assert.assertEquals(0, targetAccount.getVotesList().size());
    long targetAssetCount = PublicMethod.getAssetIssueValue(targetAdd03, assetAccountId, blockingStubFull);
    Assert.assertEquals(100, targetAssetCount);
    AccountResourceMessage targetResource = PublicMethod.getAccountResource(targetAdd03, blockingStubFull);
    Assert.assertNotEquals(0, targetResource.getNetLimit());
    Assert.assertNotEquals(0, targetResource.getEnergyLimit());
  }

  }
