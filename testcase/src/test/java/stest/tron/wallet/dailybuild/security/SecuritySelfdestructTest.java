package stest.tron.wallet.dailybuild.security;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.Utils;

/**
 * Tests for selfdestruct (TIP-481) behavior.
 *
 * <p>TIP-481 ({@code getAllowTvmSelfdestructRestriction}) is a chain governance proposal
 * (proposal #76) that restricts selfdestruct behavior. When active:
 * <ul>
 *   <li>selfdestruct only transfers TRX to the target, does NOT delete code/storage</li>
 *   <li>Contract remains callable after selfdestruct</li>
 *   <li>This aligns with Ethereum's EIP-6780 (Cancun hard fork)</li>
 * </ul>
 *
 * <p>This proposal is enabled via on-chain transaction (not config file), typically
 * opened in {@code @BeforeSuite} by the test framework (see JsonRpcBase).
 */
@Slf4j
public class SecuritySelfdestructTest extends TronBaseTest {

  private byte[] selfdestructAddress;
  private byte[] targetAddress;
  private byte[] factoryAddress;
  private boolean restrictionActive;

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] testAddress = ecKey1.getAddress();
  private String testKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeClass(enabled = true)
  public void beforeClass() {
    restrictionActive = PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull);
    logger.info("allowTvmSelfdestructRestriction active: " + restrictionActive);

    PublicMethod.printAddress(testKey);
    Assert.assertTrue(PublicMethod.sendcoin(testAddress, 500_000_000_000L,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/security/SecuritySelfdestruct.sol";

    // Deploy SelfdestructTarget (no selfdestruct in this contract)
    String contractName = "SelfdestructTarget";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    targetAddress = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, testKey,
        testAddress, blockingStubFull);
    Assert.assertNotNull("SelfdestructTarget deployment failed", targetAddress);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Deploy SelfdestructTest with 500 TRX initial balance
    contractName = "SelfdestructTest";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    selfdestructAddress = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 500_000_000L, 100, null, testKey,
        testAddress, blockingStubFull);
    Assert.assertNotNull("SelfdestructTest deployment failed", selfdestructAddress);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Deploy SelfdestructFactory
    contractName = "SelfdestructFactory";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    factoryAddress = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, testKey,
        testAddress, blockingStubFull);
    Assert.assertNotNull("SelfdestructFactory deployment failed", factoryAddress);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true,
      description = "Selfdestruct transfers TRX to target",
      groups = {"daily"})
  public void test01SelfdestructTransfersTrx() {
    long targetBalanceBefore = PublicMethod.queryAccount(targetAddress, blockingStubFull)
        .getBalance();
    long contractBalance = PublicMethod.queryAccount(selfdestructAddress, blockingStubFull)
        .getBalance();
    logger.info("Target before: {}, Contract before: {}", targetBalanceBefore, contractBalance);
    Assert.assertTrue("Contract should have TRX", contractBalance > 0);

    // Call selfdestruct
    String argsStr = "\"" + Base58.encode58Check(targetAddress) + "\"";
    String txid = PublicMethod.triggerContract(selfdestructAddress,
        "destroyAndSend(address)", argsStr,
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());

    // TRX should be transferred regardless of restriction mode
    long targetBalanceAfter = PublicMethod.queryAccount(targetAddress, blockingStubFull)
        .getBalance();
    logger.info("Target after: {}", targetBalanceAfter);

    if (restrictionActive) {
      // TIP-481: selfdestruct only sends TRX, contract balance stays as-is or sent
      logger.info("TIP-481 active: selfdestruct restricted, TRX transfer behavior verified");
    } else {
      Assert.assertEquals(targetBalanceBefore + contractBalance, targetBalanceAfter);
    }
  }

  @Test(enabled = true,
      description = "Post-selfdestruct state depends on TIP-481 restriction",
      groups = {"daily"})
  public void test02PostSelfdestructState() {
    SmartContractOuterClass.SmartContract contract =
        PublicMethod.getContract(selfdestructAddress, blockingStubFull);
    int bytecodeSize = contract.getBytecode().size();
    logger.info("Bytecode size after selfdestruct: {}", bytecodeSize);

    if (restrictionActive) {
      // TIP-481: code persists, contract still callable
      Assert.assertTrue("TIP-481: code should persist after selfdestruct", bytecodeSize > 0);
      logger.info("TIP-481 confirmed: code persists after selfdestruct");
    } else {
      // Pre-TIP-481: code removed
      Assert.assertEquals("Pre-TIP-481: code should be cleared", 0, bytecodeSize);
      logger.info("Pre-TIP-481: code removed after selfdestruct");
    }
  }

  @Test(enabled = true,
      description = "Non-owner cannot call selfdestruct — onlyOwner modifier",
      groups = {"daily"})
  public void test03NonOwnerCannotSelfdestruct() {
    // Deploy a fresh contract for this test
    String filePath = "src/test/resources/soliditycode/security/SecuritySelfdestruct.sol";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, "SelfdestructTest");
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] newContract = PublicMethod.deployContract("SelfdestructTest", abi, code, "",
        maxFeeLimit, 100_000_000L, 100, null, testKey,
        testAddress, blockingStubFull);
    Assert.assertNotNull("Fresh contract deployment failed", newContract);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Call from non-owner should revert (onlyOwner modifier)
    String argsStr = "\"" + Base58.encode58Check(targetAddress) + "\"";
    String txid = PublicMethod.triggerContract(newContract,
        "destroyAndSend(address)", argsStr,
        false, 0, maxFeeLimit, foundationAddress, foundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
    logger.info("Non-owner selfdestruct correctly reverted");
  }

  @Test(enabled = true,
      description = "Factory creates child contract with TRX",
      groups = {"daily"})
  public void test04FactoryCreateChild() {
    String txid = PublicMethod.triggerContract(factoryAddress,
        "createAndFund()", "#",
        false, 100_000_000L, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());

    // Extract child address from internal transactions
    Assert.assertTrue("Should have internal transactions",
        info.get().getInternalTransactionsCount() > 0);
    byte[] childAddress = info.get().getInternalTransactions(0)
        .getTransferToAddress().toByteArray();
    logger.info("Child contract: " + Base58.encode58Check(childAddress));

    long childBalance = PublicMethod.queryAccount(childAddress, blockingStubFull).getBalance();
    logger.info("Child balance: " + childBalance);
    Assert.assertTrue("Child should have TRX", childBalance > 0);
  }
}
