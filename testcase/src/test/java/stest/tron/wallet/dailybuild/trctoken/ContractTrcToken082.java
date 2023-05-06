package stest.tron.wallet.dailybuild.trctoken;

import com.google.protobuf.ByteString;
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
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;




@Slf4j
public class ContractTrcToken082 {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

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
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.printAddress(dev001Key);
  }

  @Test(enabled = true, description = "deploy contract and generate create2 address")
  public void test01DeployContract() {

    Assert.assertTrue(PublicMethed
        .sendcoin(dev001Address, 300100_000_000L,
            fromAddress, testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(dev001Address, dev001Key, 130000L, blockingStubFull), 0,
        1, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 100000_000_000L, 0, 0,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;

    //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethed
        .createAssetIssue(dev001Address, tokenName, TotalSupply, 1, 1000, start, end, 1,
            description, url, 100000L, 100000L, 3L, 30L, dev001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethed.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());

    String filePath = "src/test/resources/soliditycode/contractTrcToken082.sol";
    String contractName = "D";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String tokenId = assetAccountId.toStringUtf8();
    long tokenValue = 350;
    long callValue = 5;
    String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callValue, 0, 10000, tokenId, tokenValue, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);
    if (transferTokenTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    contractD = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractD, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    Long contractAssetCount = PublicMethed
        .getAssetIssueValue(contractD, assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);
    Assert.assertEquals(Long.valueOf(tokenValue), contractAssetCount);

    String methedStr = "deploy(uint256)";
    String argsStr = "7";
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    String txid1 = PublicMethed.triggerContract(contractD,
        "TransferTokenTo(address,trcToken,uint256)",
        param, false, 0, 100000000L, "0",
        0, dev001Address, dev001Key, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, info.get().getResultValue());
    Long create2Count = PublicMethed
        .getAssetIssueValue(create2Address, assetAccountId, blockingStubFull);
    Assert.assertEquals(Long.valueOf(toCreate2Num), create2Count);
  }

  @Test(enabled = true, description = "kill,create2,kill,and check trc10 amount")
  public void test02KillCreate2Kill() {
    String param1 = "\"" + receiveStr + "\"";
    String txid1 = PublicMethed.triggerContract(create2Address,
        "kill(address)",
        param1, false, 0, 100000000L, "0",
        0, fromAddress, testKey002, blockingStubFull);
    String methedStr = "deploy(uint256)";
    String argsStr = "7";
    String txid2 = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    logger.info("test02KillCreate2Kill create2Address: " + PublicMethed.getContract(create2Address,blockingStubFull).toString());
    String txid3 = PublicMethed.triggerContract(create2Address,
        "kill(address)",
        param1, false, 0, 100000000L, "0",
        0, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info1 =
        PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertEquals(TransactionInfo.code.SUCESS, info1.get().getResult());

    Optional<Protocol.TransactionInfo> info2 =
        PublicMethed.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertEquals(TransactionInfo.code.SUCESS, info2.get().getResult());

    Optional<Protocol.TransactionInfo> info3 =
        PublicMethed.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertEquals(TransactionInfo.code.SUCESS, info3.get().getResult());

    Protocol.Account create2Account = PublicMethed.queryAccount(create2Address, blockingStubFull);
    Assert.assertEquals(create2Account.toString(), "");
    Long create2AssetCount = PublicMethed
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
    PublicMethed.freedResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


