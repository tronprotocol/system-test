package stest.tron.wallet.dailybuild.security;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class SecurityPermissionTest extends TronBaseTest {

  private byte[] permissionAddress;
  private byte[] callerAddress;

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] testAddress = ecKey1.getAddress();
  private String testKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] nonOwnerAddress = ecKey2.getAddress();
  private String nonOwnerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(testKey);
    PublicMethod.printAddress(nonOwnerKey);
    Assert.assertTrue(PublicMethod.sendcoin(testAddress, 200_000_000_000L,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(nonOwnerAddress, 200_000_000_000L,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/security/SecurityPermission.sol";

    // Deploy PermissionTest (owner = testAddress)
    String contractName = "PermissionTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    permissionAddress = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, testKey,
        testAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(PublicMethod.getContract(permissionAddress, blockingStubFull).getAbi());

    // Deploy PermissionCaller with permissionAddress as constructor arg
    contractName = "PermissionCaller";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    String permBase58 = Base58.encode58Check(permissionAddress);
    String callerTxid = PublicMethod.deployContractWithConstantParame(contractName, abi, code,
        "constructor(address)", "\"" + permBase58 + "\"", "",
        maxFeeLimit, 0L, 100, null, testKey,
        testAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> callerInfo =
        PublicMethod.getTransactionInfoById(callerTxid, blockingStubFull);
    callerAddress = callerInfo.get().getContractAddress().toByteArray();
    Assert.assertNotNull(PublicMethod.getContract(callerAddress, blockingStubFull).getAbi());
  }

  @Test(enabled = true,
      description = "Owner can call onlyOwner function",
      groups = {"daily"})
  public void test01OwnerCanCallProtected() {
    String txid = PublicMethod.triggerContract(permissionAddress,
        "setProtectedValue(uint256)", "42",
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    // Verify value was set
    GrpcAPI.TransactionExtention ext = PublicMethod
        .triggerConstantContractForExtention(permissionAddress,
            "protectedValue()", "#", false,
            0, maxFeeLimit, "0", 0, testAddress, testKey, blockingStubFull);
    Assert.assertTrue(ext.getResult().getResult());
    long value = ByteArray.toLong(ext.getConstantResult(0).toByteArray());
    Assert.assertEquals(42L, value);
    logger.info("Owner set protected value to: " + value);
  }

  @Test(enabled = true,
      description = "Non-owner cannot call onlyOwner function - should revert",
      groups = {"daily"})
  public void test02NonOwnerCannotCallProtected() {
    String txid = PublicMethod.triggerContract(permissionAddress,
        "setProtectedValue(uint256)", "999",
        false, 0, maxFeeLimit, nonOwnerAddress, nonOwnerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
    logger.info("Non-owner correctly blocked from calling onlyOwner function");
  }

  @Test(enabled = true,
      description = "Anyone can call public function",
      groups = {"daily"})
  public void test03AnyoneCanCallPublic() {
    String txid = PublicMethod.triggerContract(permissionAddress,
        "setPublicValue(uint256)", "123",
        false, 0, maxFeeLimit, nonOwnerAddress, nonOwnerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    logger.info("Non-owner can call public function");
  }

  @Test(enabled = true,
      description = "tx.origin vs msg.sender: msg.sender is contract when called through proxy",
      groups = {"daily"})
  public void test04TxOriginVsMsgSender() {
    // When calling through PermissionCaller, msg.sender should be the caller contract address
    // but tx.origin should be the EOA
    GrpcAPI.TransactionExtention extSender = PublicMethod
        .triggerConstantContractForExtention(callerAddress,
            "callGetMsgSender()", "#", false,
            0, maxFeeLimit, "0", 0, testAddress, testKey, blockingStubFull);
    Assert.assertTrue(extSender.getResult().getResult());
    String msgSenderHex = "41" + ByteArray.toHexString(
        extSender.getConstantResult(0).toByteArray()).substring(24);
    byte[] msgSenderBytes = ByteArray.fromHexString(msgSenderHex);
    logger.info("msg.sender when called through proxy: " + Base58.encode58Check(msgSenderBytes));

    GrpcAPI.TransactionExtention extOrigin = PublicMethod
        .triggerConstantContractForExtention(callerAddress,
            "callGetTxOrigin()", "#", false,
            0, maxFeeLimit, "0", 0, testAddress, testKey, blockingStubFull);
    Assert.assertTrue(extOrigin.getResult().getResult());
    String txOriginHex = "41" + ByteArray.toHexString(
        extOrigin.getConstantResult(0).toByteArray()).substring(24);
    byte[] txOriginBytes = ByteArray.fromHexString(txOriginHex);
    logger.info("tx.origin when called through proxy: " + Base58.encode58Check(txOriginBytes));

    // msg.sender should be the caller contract, NOT the EOA
    Assert.assertEquals(Base58.encode58Check(callerAddress),
        Base58.encode58Check(msgSenderBytes));
    // tx.origin should be the EOA (testAddress)
    Assert.assertEquals(Base58.encode58Check(testAddress),
        Base58.encode58Check(txOriginBytes));
    logger.info("tx.origin vs msg.sender difference verified correctly");
  }

  @Test(enabled = true,
      description = "Proxy contract cannot call onlyOwner function (msg.sender mismatch)",
      groups = {"daily"})
  public void test05ProxyCannotBypassOwnerCheck() {
    // The caller contract tries to call setProtectedValue on PermissionTest.
    // Even though tx.origin is the owner (testAddress), msg.sender is the caller contract.
    // Since the modifier checks msg.sender, this should fail.
    String txid = PublicMethod.triggerContract(callerAddress,
        "trySetProtectedValue(uint256)", "888",
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
    logger.info("Proxy contract correctly blocked - msg.sender is contract, not owner");
  }

  @Test(enabled = true,
      description = "Proxy can call public function through delegation",
      groups = {"daily"})
  public void test06ProxyCanCallPublicFunction() {
    String txid = PublicMethod.triggerContract(callerAddress,
        "trySetPublicValue(uint256)", "777",
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    logger.info("Proxy can call public function through delegation");
  }

  @Test(enabled = true,
      description = "Admin role can call onlyAdmin function",
      groups = {"daily"})
  public void test07AdminRoleAccess() {
    // Add nonOwner as admin
    String argsStr = "\"" + Base58.encode58Check(nonOwnerAddress) + "\"";
    String txid = PublicMethod.triggerContract(permissionAddress,
        "addAdmin(address)", argsStr,
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());

    // Now nonOwner should be able to call setAdminValue
    txid = PublicMethod.triggerContract(permissionAddress,
        "setAdminValue(uint256)", "555",
        false, 0, maxFeeLimit, nonOwnerAddress, nonOwnerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    logger.info("Admin can call admin-restricted function");

    // Remove admin
    txid = PublicMethod.triggerContract(permissionAddress,
        "removeAdmin(address)", argsStr,
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Now nonOwner should NOT be able to call setAdminValue
    txid = PublicMethod.triggerContract(permissionAddress,
        "setAdminValue(uint256)", "666",
        false, 0, maxFeeLimit, nonOwnerAddress, nonOwnerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    logger.info("Removed admin correctly blocked from admin function");
  }

  @Test(enabled = true,
      description = "Ownership transfer changes who can call protected functions",
      groups = {"daily"})
  public void test08OwnershipTransfer() {
    // Transfer ownership to nonOwnerAddress
    String argsStr = "\"" + Base58.encode58Check(nonOwnerAddress) + "\"";
    String txid = PublicMethod.triggerContract(permissionAddress,
        "transferOwnership(address)", argsStr,
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());

    // Now nonOwner should be the owner and can call protected function
    txid = PublicMethod.triggerContract(permissionAddress,
        "setProtectedValue(uint256)", "1000",
        false, 0, maxFeeLimit, nonOwnerAddress, nonOwnerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    logger.info("New owner can call protected function after ownership transfer");

    // Original owner should now be blocked
    txid = PublicMethod.triggerContract(permissionAddress,
        "setProtectedValue(uint256)", "2000",
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    logger.info("Original owner correctly blocked after ownership transfer");
  }
}
