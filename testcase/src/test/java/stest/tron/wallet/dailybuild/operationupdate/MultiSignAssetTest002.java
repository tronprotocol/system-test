package stest.tron.wallet.dailybuild.operationupdate;

import com.google.protobuf.ByteString;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class MultiSignAssetTest002 extends TronBaseTest {
  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "MultiSign001_" + Long.toString(now);
  private final String testKey001 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress01 = PublicMethod.getFinalAddress(testKey001);
  private final String operations = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.operations");
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  ByteString assetAccountId1;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey3.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] participateAddress = ecKey4.getAddress();
  String participateKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {  }

  @Test(enabled = true, groups = {"contract", "daily"})
  public void testMultiSign1CreateAssetissue() {
    ecKey1 = new ECKey(Utils.getRandom());
    manager1Address = ecKey1.getAddress();
    manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    manager2Address = ecKey2.getAddress();
    manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    ecKey3 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey3.getAddress();
    ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethod.printAddress(ownerKey);

    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 3;

    Assert.assertTrue(
        PublicMethod.sendcoin(ownerAddress, needCoin + 2048000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    String[] permissionKeyString1 = new String[1];
    permissionKeyString1[0] = ownerKey;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
  String txid = PublicMethodForMultiSign
        .accountPermissionUpdateForTransactionId(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull, ownerKeyString);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);

    long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, energyFee + netFee + updateAccountPermissionFee);

    balanceBefore = balanceAfter;
  Long start = System.currentTimeMillis() + 5000;
  Long end = System.currentTimeMillis() + 1000000000;
    logger.info("try create asset issue");

    txid = PublicMethodForMultiSign
        .createAssetIssueForTransactionId1(ownerAddress, name, totalSupply, 1,
            1, start, end, 1, description, url, 2000L, 2000L,
            1L, 1L, ownerKey, blockingStubFull, 2, permissionKeyString);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertNotNull(txid);

    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    energyFee = infoById.get().getReceipt().getEnergyFee();
    netFee = infoById.get().getReceipt().getNetFee();
    fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, energyFee + netFee + multiSignFee + 1024_000000L);

    logger.info(" create asset end");
  }

  /**
   * constructor.
   */

  @Test(enabled = true, groups = {"contract", "daily"})
  public void testMultiSign2TransferAssetissue() {
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.printAddress(manager1Key);
    Account getAssetIdFromOwnerAccount;
    getAssetIdFromOwnerAccount = PublicMethod.queryAccount(ownerAddress, blockingStubFull);
    assetAccountId1 = getAssetIdFromOwnerAccount.getAssetIssuedID();
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
  String txid = PublicMethodForMultiSign.transferAssetForTransactionId1(manager1Address,
        assetAccountId1.toByteArray(), 10, ownerAddress, ownerKey, blockingStubFull,
        2, permissionKeyString);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertNotNull(txid);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, energyFee + netFee + multiSignFee + 1000000L);
  }

  /**
   * constructor.
   */

  @Test(enabled = true, groups = {"contract", "daily"})
  public void testMultiSign3ParticipateAssetissue() {
    ecKey4 = new ECKey(Utils.getRandom());
    participateAddress = ecKey4.getAddress();
    participateKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 2;

    Assert.assertTrue(
        PublicMethod.sendcoin(participateAddress, needCoin + 2048000000L, foundationAddress, foundationKey,
            blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(participateAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    ownerKeyString[0] = participateKey;
    ownerKeyString[1] = manager1Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(participateKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
  String txid = PublicMethodForMultiSign
        .accountPermissionUpdateForTransactionId(accountPermissionJson, participateAddress,
            participateKey, blockingStubFull, ownerKeyString);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertNotNull(txid);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    long balanceAfter = PublicMethod.queryAccount(participateAddress, blockingStubFull)
        .getBalance();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, energyFee + netFee + updateAccountPermissionFee);

    balanceBefore = balanceAfter;

    txid = PublicMethodForMultiSign.participateAssetIssueForTransactionId(ownerAddress,
        assetAccountId1.toByteArray(), 10, participateAddress, participateKey, 2,
        blockingStubFull, permissionKeyString);

    Assert.assertNotNull(txid);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    balanceAfter = PublicMethod.queryAccount(participateAddress, blockingStubFull)
        .getBalance();
    energyFee = infoById.get().getReceipt().getEnergyFee();
    netFee = infoById.get().getReceipt().getNetFee();
    fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);


    Assert.assertEquals(balanceBefore - balanceAfter, fee + 10);
    Assert.assertEquals(fee, energyFee + netFee + multiSignFee);
  }

  /**
   * constructor.
   */

  @Test(enabled = true, groups = {"contract", "daily"})
  public void testMultiSign4updateAssetissue() {
    url = "MultiSign001_update_url" + Long.toString(now);
    ownerKeyString[0] = ownerKey;
    description = "MultiSign001_update_description" + Long.toString(now);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
  String txid = PublicMethodForMultiSign
        .updateAssetForTransactionId(ownerAddress, description.getBytes(), url.getBytes(), 100L,
            100L, ownerKey, 2, blockingStubFull, permissionKeyString);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, energyFee + netFee + multiSignFee);
  }


  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {  }
}


