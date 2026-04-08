package stest.tron.wallet.dailybuild.zentoken;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.Note;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestZenToken003 extends TronBaseTest {  List<Note> shieldOutList = new ArrayList<>();
  DecryptNotes notes;
  Note note;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverPublicAddress = ecKey2.getAddress();
  String receiverPublicKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethod.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long zenTokenWhenCreateNewAddress = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenWhenCreateNewAddress");
  private Long costTokenAmount = 20 * zenTokenFee;
  private String txid;
  private Optional<TransactionInfo> infoById;
  private Optional<Transaction> byId;

  /**
   * constructor.
   */


  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(foundationZenTokenKey);
    PublicMethod.printAddress(zenTokenOwnerKey);    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Args.setFullNodeAllowShieldedTransaction(true);
  }

  @Test(enabled = false,
      description = "Public to two shield transaction", groups = {"daily", "shield"})
  public void test1Public2ShieldTransaction() {
    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethod.generateShieldAddress();
  String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo2 = PublicMethod.generateShieldAddress();
  String shieldAddress2 = shieldAddressInfo2.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
    logger.info("shieldAddress2:" + shieldAddress2);
  final Long beforeAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  final Long beforeNetUsed = PublicMethod
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull).getFreeNetUsed();
  Long sendToShiledAddress1Amount = 3 * zenTokenFee;
  Long sendToShiledAddress2Amount = costTokenAmount - sendToShiledAddress1Amount - zenTokenFee;
  String memo1 = "Public to  shield address1 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
  String memo2 = "Public to  shield address2 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);
    txid = PublicMethod.sendShieldCoinGetTxid(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getShieldedTransactionFee() == zenTokenFee);
    byId = PublicMethod.getTransactionById(txid, blockingStubFull);
    Assert.assertTrue(byId.get().getSignatureCount() == 1);
  Long afterAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  final Long afterNetUsed = PublicMethod
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull)
        .getFreeNetUsed();
    logger.info("beforeAssetBalance:" + beforeAssetBalance);
    logger.info("afterAssetBalance:" + afterAssetBalance);
    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == costTokenAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);
    notes = PublicMethod.listShieldNote(shieldAddressInfo1, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount1 = note.getValue();
    logger.info("receiverShieldTokenAmount1:" + receiverShieldTokenAmount1);
    logger.info("sendToShiledAddress1Amount:" + sendToShiledAddress1Amount);
    Assert.assertEquals(receiverShieldTokenAmount1, sendToShiledAddress1Amount);
    Assert.assertEquals(memo1, PublicMethod.getMemo(note));

    notes = PublicMethod.listShieldNote(shieldAddressInfo2, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount2 = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount2, sendToShiledAddress2Amount);
    Assert.assertEquals(memo2, PublicMethod.getMemo(note));

  }

  @Test(enabled = false,
      description = "Public to one public and one shield transaction", groups = {"daily", "shield"})
  public void test2Public2OneShieldAndOnePublicTransaction() {
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethod.generateShieldAddress();
  String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
  final Long beforeAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  final Long beforeNetUsed = PublicMethod
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull).getFreeNetUsed();
  final Long beforeBalance = PublicMethod
        .queryAccount(receiverPublicAddress, blockingStubFull).getBalance();
  Long sendToShiledAddress1Amount = 1 * zenTokenFee;
  //When receiver public address don't active,the fee is 1000000
    Long sendToPublicAddressAmount = costTokenAmount
        - sendToShiledAddress1Amount - zenTokenWhenCreateNewAddress;
    logger.info("costTokenAmount " + costTokenAmount);
    logger.info("sendToShiledAddress1Amount " + sendToShiledAddress1Amount);
    logger.info("sendToPublicAddressAmount " + sendToPublicAddressAmount);
    shieldOutList.clear();
  String memo1 = "Public to  shield address1 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);

    txid = PublicMethod.sendShieldCoinGetTxid(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        receiverPublicAddress, sendToPublicAddressAmount,
        zenTokenOwnerKey, blockingStubFull);
    logger.info("txid:" + txid);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getShieldedTransactionFee() == zenTokenWhenCreateNewAddress);
    byId = PublicMethod.getTransactionById(txid, blockingStubFull);
    Assert.assertTrue(byId.get().getSignatureCount() == 1);
  Long afterAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  final Long afterNetUsed = PublicMethod
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull)
        .getFreeNetUsed();
  final Long afterBalance = PublicMethod
        .queryAccount(receiverPublicAddress, blockingStubFull).getBalance();
    logger.info("beforeAssetBalance:" + beforeAssetBalance);
    logger.info("afterAssetBalance:" + afterAssetBalance);
    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == costTokenAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);
    Assert.assertTrue(beforeBalance - afterBalance == 0);

    notes = PublicMethod.listShieldNote(shieldAddressInfo1, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount1 = note.getValue();
    logger.info("receiverShieldTokenAmount1:" + receiverShieldTokenAmount1);
    logger.info("sendToShiledAddress1Amount:" + sendToShiledAddress1Amount);
    Assert.assertEquals(receiverShieldTokenAmount1, sendToShiledAddress1Amount);
    Assert.assertEquals(memo1, PublicMethod.getMemo(note));
  Long afterReceiverPublicAssetBalance = PublicMethod.getAssetIssueValue(receiverPublicAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
    Assert.assertEquals(afterReceiverPublicAssetBalance, sendToPublicAddressAmount);
  }

  @Test(enabled = false,
      description = "Public to one public and two shield transaction", groups = {"daily", "shield"})
  public void test3Public2OneShieldAndOnePublicTransaction() {
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethod.generateShieldAddress();
  String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo2 = PublicMethod.generateShieldAddress();
  String shieldAddress2 = shieldAddressInfo2.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
    logger.info("shieldAddress2:" + shieldAddress2);
  final Long beforeAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  final Long beforeNetUsed = PublicMethod
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull).getFreeNetUsed();
  Long sendToShiledAddress1Amount = 1 * zenTokenFee;
  Long sendToShiledAddress2Amount = 2 * zenTokenFee;
  final Long sendToPublicAddressAmount = costTokenAmount - sendToShiledAddress1Amount
        - sendToShiledAddress2Amount - zenTokenFee;
    shieldOutList.clear();
  String memo1 = "Public to  shield address1 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
  String memo2 = "Public to  shield address2 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);
  final Long beforeReceiverPublicAssetBalance = PublicMethod
        .getAssetIssueValue(receiverPublicAddress,
            PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull);

    txid = PublicMethod.sendShieldCoinGetTxid(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        receiverPublicAddress, sendToPublicAddressAmount,
        zenTokenOwnerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getShieldedTransactionFee() == zenTokenFee);
    byId = PublicMethod.getTransactionById(txid, blockingStubFull);
    Assert.assertTrue(byId.get().getSignatureCount() == 1);
  Long afterAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  final Long afterNetUsed = PublicMethod
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull)
        .getFreeNetUsed();
    logger.info("beforeAssetBalance:" + beforeAssetBalance);
    logger.info("afterAssetBalance:" + afterAssetBalance);
    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == costTokenAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);

    notes = PublicMethod.listShieldNote(shieldAddressInfo1, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount1 = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount1, sendToShiledAddress1Amount);
    Assert.assertEquals(memo1, PublicMethod.getMemo(note));

    notes = PublicMethod.listShieldNote(shieldAddressInfo2, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount2 = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount2, sendToShiledAddress2Amount);
    Assert.assertEquals(memo2, PublicMethod.getMemo(note));
  Long afterReceiverPublicAssetBalance = PublicMethod.getAssetIssueValue(receiverPublicAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
    Assert.assertTrue(afterReceiverPublicAssetBalance
        - beforeReceiverPublicAssetBalance == sendToPublicAddressAmount);
  }

  @Test(enabled = false,
      description = "Public to one smart contract and one shield transaction", groups = {"daily", "shield"})
  public void test4Public2OneShieldAndOneSmartContractAddressTransaction() {
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethod.generateShieldAddress();
  String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
  final Long beforeAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  final Long beforeNetUsed = PublicMethod
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull).getFreeNetUsed();
  final Long beforeBalance = PublicMethod
        .queryAccount(receiverPublicAddress, blockingStubFull).getBalance();
  Long sendToShiledAddress1Amount = 1 * zenTokenFee;

    shieldOutList.clear();
  String memo1 = "Public to  shield address1 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
  String contractName = "tokenBalanceContract";
  String code = "608060405260ff806100126000396000f30060806040526"
        + "004361060485763ffffffff7c010000000000000"
        + "0000000000000000000000000000000000000000000600035041663a730416e8114604d578063b69ef8a8146"
        + "081575b600080fd5b606f73ffffffffffffffffffffffffffffffffffffffff6004351660243560ab565b604"
        + "08051918252519081900360200190f35b348015608c57600080fd5b50d38015609857600080fd5b50d280156"
        + "0a457600080fd5b50606f60cd565b73ffffffffffffffffffffffffffffffffffffffff90911690d16000908"
        + "15590565b600054815600a165627a7a723058202b6235122df66c062c2e723ad58a9fea93346f3bc19898971"
        + "8f211aa1dbd2d7a0029";
  String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},"
        + "{\"name\":\"tokenId\",\"type\":\"trcToken\"}],\"name\":\"getTokenBalnce\",\"outputs\":"
        + "[{\"name\":\"b\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":"
        + "\"balance\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,"
        + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,"
        + "\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    txid = PublicMethod.deployContractAndGetTransactionInfoById(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, foundationKey, foundationAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    com.google.protobuf.ByteString contractAddress = infoById.get().getContractAddress();
    SmartContract smartContract = PublicMethod
        .getContract(contractAddress.toByteArray(), blockingStubFull);
    org.junit.Assert.assertTrue(smartContract.getAbi() != null);
  Long sendToPublicAddressAmount = costTokenAmount - sendToShiledAddress1Amount - zenTokenFee;
    txid = PublicMethod.sendShieldCoinGetTxid(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        contractAddress.toByteArray(), sendToPublicAddressAmount,
        zenTokenOwnerKey, blockingStubFull);
    logger.info("txid:" + txid);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getShieldedTransactionFee() == zenTokenFee);
    byId = PublicMethod.getTransactionById(txid, blockingStubFull);
    Assert.assertTrue(byId.get().getSignatureCount() == 1);
  Long afterAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  final Long afterNetUsed = PublicMethod
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull)
        .getFreeNetUsed();
  final Long afterBalance = PublicMethod
        .queryAccount(receiverPublicAddress, blockingStubFull).getBalance();
    logger.info("beforeAssetBalance:" + beforeAssetBalance);
    logger.info("afterAssetBalance:" + afterAssetBalance);
    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == costTokenAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);
    Assert.assertTrue(beforeBalance - afterBalance == 0);

    notes = PublicMethod.listShieldNote(shieldAddressInfo1, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount1 = note.getValue();
    logger.info("receiverShieldTokenAmount1:" + receiverShieldTokenAmount1);
    logger.info("sendToShiledAddress1Amount:" + sendToShiledAddress1Amount);
    Assert.assertEquals(receiverShieldTokenAmount1, sendToShiledAddress1Amount);
    Assert.assertEquals(memo1, PublicMethod.getMemo(note));
  Long afterReceiverPublicAssetBalance = PublicMethod
        .getAssetIssueValue(contractAddress.toByteArray(),
            PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull);
    Assert.assertEquals(afterReceiverPublicAssetBalance, sendToPublicAddressAmount);
  }

  @Test(enabled = false,
      description = "Public to two same shield address", groups = {"daily", "shield"})
  public void test5Public2TwoSameShieldAddress() {
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethod.generateShieldAddress();
  String shieldAddress1 = shieldAddressInfo1.get().getAddress();

    logger.info("shieldAddress1:" + shieldAddress1);
  final Long beforeAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  final Long beforeNetUsed = PublicMethod
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull).getFreeNetUsed();
  Long sendToShiledAddress1Amount = 3 * zenTokenFee;
  Long sendToShiledAddress2Amount = costTokenAmount - sendToShiledAddress1Amount - zenTokenFee;
    shieldOutList.clear();
  String memo1 = "First public to  shield same address transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
  String memo2 = "Second public to  shield same address transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress2Amount, memo2);
    txid = PublicMethod.sendShieldCoinGetTxid(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getShieldedTransactionFee() == zenTokenFee);
    byId = PublicMethod.getTransactionById(txid, blockingStubFull);
    Assert.assertTrue(byId.get().getSignatureCount() == 1);
  Long afterAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  final Long afterNetUsed = PublicMethod
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull)
        .getFreeNetUsed();
    logger.info("beforeAssetBalance:" + beforeAssetBalance);
    logger.info("afterAssetBalance:" + afterAssetBalance);
    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == costTokenAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);
    notes = PublicMethod.getShieldNotesByIvk(shieldAddressInfo1, blockingStubFull);
    Assert.assertTrue(notes.getNoteTxsCount() == 2);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount1 = note.getValue();
    logger.info("receiverShieldTokenAmount1:" + receiverShieldTokenAmount1);
    logger.info("sendToShiledAddress1Amount:" + sendToShiledAddress1Amount);
    Assert.assertEquals(receiverShieldTokenAmount1, sendToShiledAddress1Amount);
    Assert.assertEquals(memo1, PublicMethod.getMemo(note));

    note = notes.getNoteTxs(1).getNote();
  Long receiverShieldTokenAmount2 = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount2, sendToShiledAddress2Amount);
    Assert.assertEquals(memo2, PublicMethod.getMemo(note));

  }


  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
            PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress, zenTokenOwnerKey, blockingStubFull);  }
}