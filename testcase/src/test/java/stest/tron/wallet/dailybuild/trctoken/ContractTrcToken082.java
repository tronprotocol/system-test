package stest.tron.wallet.dailybuild.trctoken;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class ContractTrcToken082 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKeyReceive = new ECKey(Utils.getRandom());
  private byte[] receiveAdderss = ecKeyReceive.getAddress();
  private String receiveStr = Base58.encode58Check(receiveAdderss);
  private String receiveKey = ByteArray.toHexString(ecKeyReceive.getPrivKeyBytes());
  byte[] contractD = null;
  byte[] create2Address;
  String create2Str;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
  }

  @Test(enabled = true, description = "deploy contract and generate create2 address", groups = {"contract", "daily"})
  public void test01DeployContract() {

    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 300100_000_000L,
            fromAddress, testKey002, blockingStubFull));
//    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress,
//        PublicMethod.getFreezeBalanceCount(dev001Address, dev001Key, 130000L, blockingStubFull), 0,
//        1, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
//    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress, 100000_000_000L, 0, 0,
//        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
  //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethod
        .createAssetIssue(dev001Address, tokenName, TotalSupply, 1, 1000, start, end, 1,
            description, url, 100000L, 100000L, 3L, 30L, dev001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethod.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());
  String filePath = "src/test/resources/soliditycode/contractTrcToken082.sol";
  String contractName = "D";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String tokenId = assetAccountId.toStringUtf8();
    long tokenValue = 350;
    long callValue = 5;
  String transferTokenTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callValue, 0, 10000, tokenId, tokenValue, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);
    if (transferTokenTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    contractD = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractD, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  Long contractAssetCount = PublicMethod
        .getAssetIssueValue(contractD, assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);
    Assert.assertEquals(Long.valueOf(tokenValue), contractAssetCount);
  String methedStr = "deploy(uint256)";
  String argsStr = "7";
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
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
    create2Str = Base58.encode58Check(create2Address);
    logger.info("create2Address: " + create2Str);
  String toCreate2Num = "1";
  String param = "\"" + create2Str + "\",\"" + assetAccountId.toStringUtf8()
        + "\",\"" + toCreate2Num + "\"";
  String txid1 = PublicMethod.triggerContract(contractD,
        "TransferTokenTo(address,trcToken,uint256)",
        param, false, 0, 100000000L, "0",
        0, dev001Address, dev001Key, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, info.get().getResultValue());
  Long create2Count = PublicMethod
        .getAssetIssueValue(create2Address, assetAccountId, blockingStubFull);
    Assert.assertEquals(Long.valueOf(toCreate2Num), create2Count);
  }

  @Test(enabled = true, description = "kill,create2,kill,and check trc10 amount" +
          "when No.94 committee is been opened,after contracts been killed, " +
          "cannot create the contract again, the transaction will be rejected as a result",retryAnalyzer = Retry.class, groups = {"contract", "daily"})
  public void test02KillCreate2Kill() throws Exception{
    PublicMethod.triggerContract(contractD, "deploy(uint256)", "7",
        false, 0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String param1 = "\"" + receiveStr + "\"";
  String txid1 = PublicMethod.triggerContract(create2Address,
        "kill(address)",
        param1, false, 0, 100000000L, "0",
        0, fromAddress, testKey002, blockingStubFull);
    Thread.sleep(30);
  String methedStr = "deploy(uint256)";
  String argsStr = "7";
  String txid2 = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    logger.info("test02KillCreate2Kill create2Address: " + PublicMethod.getContract(create2Address,blockingStubFull).toString());
    Thread.sleep(30);
  String txid3 = PublicMethod.triggerContract(create2Address,
        "kill(address)",
        param1, false, 0, 100000000L, "0",
        0, fromAddress, testKey002, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info1 =
        PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertEquals(TransactionInfo.code.SUCESS, info1.get().getResult());

    Optional<Protocol.TransactionInfo> info2 =
        PublicMethod.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertEquals(TransactionInfo.code.SUCESS, info2.get().getResult());
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertTrue(info2.get().getInternalTransactions(0).getRejected());
    }
    Optional<Protocol.TransactionInfo> info3 =
        PublicMethod.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertEquals(TransactionInfo.code.SUCESS, info3.get().getResult());

    Protocol.Account create2Account = PublicMethod.queryAccount(create2Address, blockingStubFull);
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertNotEquals(create2Account.toString(), "");
      Assert.assertEquals(0L, create2Account.getBalance());
      Assert.assertEquals(0L, create2Account.getFrozenV2(0).getAmount());
      Assert.assertEquals(0L, create2Account.getFrozenV2(1).getAmount());
      Assert.assertEquals(0L, create2Account.getFrozenV2(2).getAmount());
    }else {
      Assert.assertEquals(create2Account.toString(), "");
    }
  Long create2AssetCount = PublicMethod
        .getAssetIssueValue(receiveAdderss, assetAccountId, blockingStubFull);
    Assert.assertEquals(create2AssetCount, Long.valueOf("1"));
    Assert.assertEquals(info1.get().getBlockNumber(), info2.get().getBlockNumber());
    Assert.assertEquals(info2.get().getBlockNumber(), info3.get().getBlockNumber());

  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);  }
}


