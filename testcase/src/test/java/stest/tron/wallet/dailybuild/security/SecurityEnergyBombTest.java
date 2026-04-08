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
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class SecurityEnergyBombTest extends TronBaseTest {

  private byte[] contractAddress;

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] testAddress = ecKey1.getAddress();
  private String testKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(testKey);
    Assert.assertTrue(PublicMethod.sendcoin(testAddress, 200_000_000_000L,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/security/SecurityEnergyBomb.sol";
    String contractName = "EnergyBomb";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, testKey,
        testAddress, blockingStubFull);
    Assert.assertNotNull("EnergyBomb contract deployment failed", contractAddress);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract contract =
        PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertNotNull("Contract ABI should not be null", contract.getAbi());
  }

  @Test(enabled = true,
      description = "Infinite loop should exhaust energy and revert (OUT_OF_ENERGY)",
      groups = {"daily"})
  public void test01InfiniteLoopExhaustsEnergy() {
    String txid = PublicMethod.triggerContract(contractAddress,
        "infiniteLoop()", "#",
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());

    Protocol.Transaction.Result.contractResult result = info.get().getReceipt().getResult();
    logger.info("Infinite loop result: " + result);
    // Should be OUT_OF_ENERGY or OUT_OF_TIME
    Assert.assertTrue(
        result == Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY
        || result == Protocol.Transaction.Result.contractResult.OUT_OF_TIME);
    logger.info("Infinite loop correctly stopped by energy/time limit");
    logger.info("Energy used: " + info.get().getReceipt().getEnergyUsageTotal());
  }

  @Test(enabled = true,
      description = "Bounded loop with small iteration count should succeed",
      groups = {"daily"})
  public void test02SmallBoundedLoopSucceeds() {
    String txid = PublicMethod.triggerContract(contractAddress,
        "boundedLoop(uint256)", "10",
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());
    logger.info("Small bounded loop succeeded, energy used: "
        + info.get().getReceipt().getEnergyUsageTotal());

    // Verify counter was updated
    GrpcAPI.TransactionExtention ext = PublicMethod
        .triggerConstantContractForExtention(contractAddress,
            "getCounter()", "#", false,
            0, maxFeeLimit, "0", 0, testAddress, testKey, blockingStubFull);
    Assert.assertTrue(ext.getResult().getResult());
    long counter = ByteArray.toLong(ext.getConstantResult(0).toByteArray());
    Assert.assertEquals(10L, counter);
    logger.info("Counter value after 10 iterations: " + counter);
  }

  @Test(enabled = true,
      description = "Large bounded loop should exhaust energy and revert",
      groups = {"daily"})
  public void test03LargeBoundedLoopExhaustsEnergy() {
    // Use a very large iteration count to exhaust energy
    String txid = PublicMethod.triggerContract(contractAddress,
        "boundedLoop(uint256)", "999999999",
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());

    Protocol.Transaction.Result.contractResult result = info.get().getReceipt().getResult();
    logger.info("Large loop result: " + result);
    Assert.assertTrue(
        result == Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY
        || result == Protocol.Transaction.Result.contractResult.OUT_OF_TIME);
    logger.info("Large bounded loop correctly stopped by energy/time limit");
  }

  @Test(enabled = true,
      description = "Storage expansion bomb should exhaust energy with large count",
      groups = {"daily"})
  public void test04StorageExpansionBomb() {
    // Small storage expansion should succeed
    String txid = PublicMethod.triggerContract(contractAddress,
        "storageExpansion(uint256)", "5",
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    long energySmall = info.get().getReceipt().getEnergyUsageTotal();
    logger.info("Storage expansion (5 items) energy: " + energySmall);

    // Large storage expansion should exhaust energy
    txid = PublicMethod.triggerContract(contractAddress,
        "storageExpansion(uint256)", "999999999",
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    logger.info("Large storage expansion correctly stopped by energy limit");
  }

  @Test(enabled = true,
      description = "Expensive O(n^2) computation: small succeeds, large triggers timeout",
      groups = {"daily"})
  public void test05ExpensiveComputation() {
    // Small input should succeed via constant call
    GrpcAPI.TransactionExtention extSmall = PublicMethod
        .triggerConstantContractForExtention(contractAddress,
            "expensiveComputation(uint256)", "3", false,
            0, maxFeeLimit, "0", 0, testAddress, testKey, blockingStubFull);
    Assert.assertTrue("Small computation (n=3) should succeed", extSmall.getResult().getResult());
    logger.info("Small computation (n=3) succeeded");

    // Medium input should also succeed
    GrpcAPI.TransactionExtention extMed = PublicMethod
        .triggerConstantContractForExtention(contractAddress,
            "expensiveComputation(uint256)", "50", false,
            0, maxFeeLimit, "0", 0, testAddress, testKey, blockingStubFull);
    Assert.assertTrue("Medium computation (n=50) should succeed", extMed.getResult().getResult());
    logger.info("Medium computation (n=50) succeeded");

    // Very large input should fail via constant call (CPU timeout)
    GrpcAPI.TransactionExtention extLarge = PublicMethod
        .triggerConstantContractForExtention(contractAddress,
            "expensiveComputation(uint256)", "99999", false,
            0, maxFeeLimit, "0", 0, testAddress, testKey, blockingStubFull);
    // Either the result is false, or the message contains timeout/energy info
    boolean largeFailed = !extLarge.getResult().getResult();
    String msg = ByteArray.toStr(extLarge.getResult().getMessage().toByteArray());
    logger.info("Large computation (n=99999) result: {}, message: {}",
        extLarge.getResult().getResult(), msg);
    Assert.assertTrue("Very large O(n^2) computation should fail. msg=" + msg,
        largeFailed || msg.contains("CPU") || msg.contains("timeout")
        || msg.contains("energy") || msg.contains("REVERT") || msg.contains("OUT_OF"));
  }

  @Test(enabled = true,
      description = "Infinite loop via constant call should return CPU timeout",
      groups = {"daily"})
  public void test06InfiniteLoopConstantCall() {
    GrpcAPI.TransactionExtention ext = PublicMethod
        .triggerConstantContractForExtention(contractAddress,
            "infiniteLoop()", "#", false,
            0, maxFeeLimit, "0", 0, testAddress, testKey, blockingStubFull);
    String message = ByteArray.toStr(ext.getResult().getMessage().toByteArray());
    logger.info("Infinite loop constant call message: " + message);
    Assert.assertTrue(message.contains("CPU timeout") || message.contains("energy")
        || !ext.getResult().getResult());
    logger.info("Infinite loop correctly stopped in constant call");
  }

  @Test(enabled = true,
      description = "Energy fee is charged even when transaction reverts due to energy exhaustion",
      groups = {"daily"})
  public void test07EnergyFeeChargedOnRevert() {
    long balanceBefore = PublicMethod.queryAccount(testAddress, blockingStubFull).getBalance();

    String txid = PublicMethod.triggerContract(contractAddress,
        "infiniteLoop()", "#",
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());

    long balanceAfter = PublicMethod.queryAccount(testAddress, blockingStubFull).getBalance();
    long energyFee = info.get().getReceipt().getEnergyFee();
    logger.info("Energy fee charged on reverted infinite loop: " + energyFee);
    logger.info("Balance diff: " + (balanceBefore - balanceAfter));
    // Energy fee should be greater than 0 - user pays for consumed energy
    Assert.assertTrue("Energy fee should be charged on revert", energyFee > 0);
    // Balance should have decreased
    Assert.assertTrue("Balance should decrease", balanceBefore > balanceAfter);
  }
}
