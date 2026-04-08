package stest.tron.wallet.dailybuild.dynamicenergy;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.ContractState;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.TronConstants;
import stest.tron.wallet.common.client.utils.Utils;

/**
 * TIP-586: Dynamic Energy model tests.
 *
 * <p>Tests the dynamic energy pricing mechanism where frequently-called
 * contracts pay higher energy costs to prevent resource abuse.
 */
@Slf4j
public class DynamicEnergyTest002 extends TronBaseTest {

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] testAddress = ecKey1.getAddress();
  private String testKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;
  private long energyFee;

  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    PublicMethod.printAddress(testKey);

    if (!PublicMethod.getAllowDynamicEnergyProposalIsOpen(blockingStubFull)) {
      throw new SkipException("Skipping dynamic energy test - proposal not open");
    }

    energyFee = PublicMethod.getChainParametersValue(
        ProposalEnum.GetEnergyFee.getProposalName(), blockingStubFull);

    Assert.assertTrue(PublicMethod.sendcoin(testAddress,
        100 * TronConstants.THOUSAND_TRX,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(testAddress,
        50 * TronConstants.THOUSAND_TRX, 0, 0, testKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Deploy a simple test contract
    String filePath = "./src/test/resources/soliditycode/contractLinkage005.sol";
    String contractName = "timeoutTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String txid = PublicMethod.deployContractAndGetTransactionInfoById(
        contractName, abi, code, "", maxFeeLimit, 0L, 100, null,
        testKey, testAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    contractAddress = info.get().getContractAddress().toByteArray();
    Assert.assertNotNull(contractAddress, "Contract deployment should succeed");
  }

  @Test(enabled = true, description = "Query dynamic energy chain parameters",
      groups = {"daily"})
  public void test01QueryDynamicEnergyParams() {
    long dynamicEnergyThreshold = PublicMethod.getChainParametersValue(
        "getDynamicEnergyThreshold", blockingStubFull);
    long dynamicEnergyIncreaseFactor = PublicMethod.getChainParametersValue(
        "getDynamicEnergyIncreaseFactor", blockingStubFull);
    long dynamicEnergyMaxFactor = PublicMethod.getChainParametersValue(
        "getDynamicEnergyMaxFactor", blockingStubFull);

    logger.info("Dynamic energy threshold: {}", dynamicEnergyThreshold);
    logger.info("Dynamic energy increase factor: {}", dynamicEnergyIncreaseFactor);
    logger.info("Dynamic energy max factor: {}", dynamicEnergyMaxFactor);

    Assert.assertTrue(dynamicEnergyThreshold >= 0,
        "Dynamic energy threshold should be non-negative");
    Assert.assertTrue(dynamicEnergyMaxFactor >= 0,
        "Dynamic energy max factor should be non-negative");
  }

  @Test(enabled = true, description = "Query contract energy usage state",
      groups = {"daily"})
  public void test02QueryContractState() {
    ContractState state = PublicMethod.getContractInfo(contractAddress, blockingStubFull)
        .getContractState();
    logger.info("Contract energy usage: {}", state.getEnergyUsage());
    logger.info("Contract energy factor: {}", state.getEnergyFactor());
    logger.info("Contract update cycle: {}", state.getUpdateCycle());

    // New contract should have default energy factor
    Assert.assertTrue(state.getEnergyFactor() >= 0,
        "Energy factor should be non-negative");
  }

  @Test(enabled = true, description = "Trigger contract and verify energy consumption recorded",
      groups = {"daily"})
  public void test03TriggerAndCheckEnergyUsage() {
    String txid = PublicMethod.triggerContract(contractAddress,
        "timeout()", "#", false, 0, maxFeeLimit,
        testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(info.isPresent(), "Transaction info should exist");

    long energyUsed = info.get().getReceipt().getEnergyUsageTotal();
    logger.info("Energy used for trigger: {}", energyUsed);
    Assert.assertTrue(energyUsed > 0, "Should consume energy when triggering contract");
  }

  @AfterClass(enabled = true)
  public void afterClass() {
    PublicMethod.freeResource(testAddress, testKey, foundationAddress, blockingStubFull);
  }
}
