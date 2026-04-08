package stest.tron.wallet.dailybuild.operationupdate;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class MultiSignAccountTest002 extends TronBaseTest {  private final String operations = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.operations");
  ByteString assetAccountId1;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[3];
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
  byte[] newAddress = ecKey4.getAddress();
  String newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
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
  public void testMultiSignForAccount() {
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

    ecKey4 = new ECKey(Utils.getRandom());
    newAddress = ecKey4.getAddress();
    newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 10;

    Assert.assertTrue(
        PublicMethod.sendcoin(ownerAddress, needCoin + 100000000L, foundationAddress, foundationKey,
            blockingStubFull));

    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(foundationAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            foundationKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    ownerKeyString[2] = manager2Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":3,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1},"
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
  final String updateName = Long.toString(System.currentTimeMillis());
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
    Assert.assertEquals(fee, energyFee + netFee + updateAccountPermissionFee);

    balanceBefore = balanceAfter;


    if(!PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {
      byte[] accountName = String.valueOf(System.currentTimeMillis()).getBytes();
      Assert.assertTrue(PublicMethodForMultiSign.createAccount1(
          ownerAddress, newAddress, ownerKey, blockingStubFull, 2, permissionKeyString));
      Assert.assertTrue(
          PublicMethodForMultiSign.setAccountId1(accountName,
              ownerAddress, ownerKey, 2, blockingStubFull, permissionKeyString));
      Assert.assertTrue(PublicMethodForMultiSign.sendcoinWithPermissionId(
          newAddress, 100L, ownerAddress, 2, ownerKey, blockingStubFull, permissionKeyString));
      Assert.assertTrue(PublicMethodForMultiSign.freezeBalanceWithPermissionId(
          ownerAddress, 1000000L, 0, 2, ownerKey, blockingStubFull, permissionKeyString));
      Assert.assertTrue(PublicMethodForMultiSign.freezeBalanceGetEnergyWithPermissionId(
          ownerAddress, 1000000L, 0, PublicMethod.tronPowerProposalIsOpen(blockingStubFull) ? 2 : 1, ownerKey, blockingStubFull, 2, permissionKeyString));

      Assert.assertTrue(PublicMethodForMultiSign.freezeBalanceForReceiverWithPermissionId(
          ownerAddress, 1000000L, 0, 0, ByteString.copyFrom(newAddress),
          ownerKey, blockingStubFull, 2, permissionKeyString));
      Assert.assertTrue(PublicMethodForMultiSign.unFreezeBalanceWithPermissionId(
          ownerAddress, ownerKey, 0, null, 2, blockingStubFull, permissionKeyString));
      Assert.assertTrue(PublicMethodForMultiSign.unFreezeBalanceWithPermissionId(
          ownerAddress, ownerKey, 0, newAddress, 2, blockingStubFull, permissionKeyString));
      Assert.assertTrue(PublicMethodForMultiSign.updateAccountWithPermissionId(
          ownerAddress, updateName.getBytes(), ownerKey, blockingStubFull, 2, permissionKeyString));
  String voteStr = Base58.encode58Check(witnessAddress);
      HashMap<String, String> smallVoteMap = new HashMap<String, String>();
      smallVoteMap.put(voteStr, "1");
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      Assert.assertTrue(PublicMethodForMultiSign.voteWitnessWithPermissionId(
          smallVoteMap, ownerAddress, ownerKey, blockingStubFull, 2, permissionKeyString));

      PublicMethod.waitProduceNextBlock(blockingStubFull);

      balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
      logger.info("balanceAfter: " + balanceAfter);
      Assert.assertEquals(balanceBefore - balanceAfter, multiSignFee * 10 + 2000000 + 100);

      Assert.assertTrue(
          PublicMethod.unFreezeBalance(foundationAddress, foundationKey, 0, ownerAddress, blockingStubFull));
    }


  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {  }
}


