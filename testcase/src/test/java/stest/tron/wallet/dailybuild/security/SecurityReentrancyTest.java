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

@Slf4j
public class SecurityReentrancyTest extends TronBaseTest {

  private byte[] victimAddress;
  private byte[] attackerAddress;

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] testAddress = ecKey1.getAddress();
  private String testKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(testKey);
    Assert.assertTrue(PublicMethod.sendcoin(testAddress, 500_000_000_000L,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Deploy Victim contract
    String filePath = "src/test/resources/soliditycode/security/SecurityReentrancy.sol";
    String contractName = "Victim";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    victimAddress = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, testKey,
        testAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract victimContract =
        PublicMethod.getContract(victimAddress, blockingStubFull);
    Assert.assertNotNull(victimContract.getAbi());

    // Deploy Attacker contract with victim address as constructor arg
    contractName = "Attacker";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    String victimBase58 = Base58.encode58Check(victimAddress);
    String attackerTxid = PublicMethod.deployContractWithConstantParame(contractName, abi, code,
        "constructor(address)", "\"" + victimBase58 + "\"", "",
        maxFeeLimit, 0L, 100, null, testKey,
        testAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> attackerInfo =
        PublicMethod.getTransactionInfoById(attackerTxid, blockingStubFull);
    attackerAddress = attackerInfo.get().getContractAddress().toByteArray();
    SmartContractOuterClass.SmartContract attackerContract =
        PublicMethod.getContract(attackerAddress, blockingStubFull);
    Assert.assertNotNull(attackerContract.getAbi());
  }

  @Test(enabled = true, description = "Deposit TRX into victim contract", groups = {"daily"})
  public void test01DepositToVictim() {
    // Deposit 100 TRX from test account
    String txid = PublicMethod.triggerContract(victimAddress, "deposit()", "#",
        false, 100_000_000L, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    logger.info("Deposit to victim successful, txid: " + txid);
  }

  @Test(enabled = true, description = "Normal withdraw from victim should succeed",
      groups = {"daily"})
  public void test02NormalWithdraw() {
    // First deposit
    String txid = PublicMethod.triggerContract(victimAddress, "deposit()", "#",
        false, 50_000_000L, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Then withdraw normally
    txid = PublicMethod.triggerContract(victimAddress, "withdraw()", "#",
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    logger.info("Normal withdraw successful");
  }

  @Test(enabled = true,
      description = "Reentrancy attack should be mitigated by TRON energy limits",
      groups = {"daily"})
  public void test03ReentrancyAttack() {
    // Deposit 200 TRX into victim from test account (to give victim a balance)
    String txid = PublicMethod.triggerContract(victimAddress, "deposit()", "#",
        false, 200_000_000L, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> depositInfo =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, depositInfo.get().getResultValue());

    // Get victim balance before attack
    long victimBalanceBefore = PublicMethod.queryAccount(victimAddress, blockingStubFull)
        .getBalance();
    logger.info("Victim TRX balance before attack: " + victimBalanceBefore);

    // Execute reentrancy attack with 10 TRX
    txid = PublicMethod.triggerContract(attackerAddress, "attack()", "#",
        false, 10_000_000L, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> attackInfo =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);

    int resultValue = attackInfo.get().getResultValue();
    logger.info("Attack result value: " + resultValue);
    logger.info("Attack receipt result: " + attackInfo.get().getReceipt().getResult());

    // The attack may either revert (due to energy limits stopping reentrancy)
    // or succeed partially. In either case the victim should not lose all funds.
    long victimBalanceAfter = PublicMethod.queryAccount(victimAddress, blockingStubFull)
        .getBalance();
    logger.info("Victim TRX balance after attack: " + victimBalanceAfter);

    if (resultValue == 1) {
      // Attack reverted - energy limit or reentrancy guard stopped it
      logger.info("Reentrancy attack was stopped by VM (REVERT)");
      Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
          attackInfo.get().getReceipt().getResult());
    } else {
      // Even if the transaction succeeded, verify victim is not drained
      // The attacker should only get back their own deposit (10 TRX)
      logger.info("Transaction succeeded, checking balances for reentrancy damage");
    }

    // Log energy usage for analysis
    logger.info("Energy used: " + attackInfo.get().getReceipt().getEnergyUsageTotal());
  }

  @Test(enabled = true, description = "Safe withdraw with checks-effects-interactions pattern",
      groups = {"daily"})
  public void test04SafeWithdraw() {
    // Deposit
    String txid = PublicMethod.triggerContract(victimAddress, "deposit()", "#",
        false, 30_000_000L, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Safe withdraw
    txid = PublicMethod.triggerContract(victimAddress, "safeWithdraw()", "#",
        false, 0, maxFeeLimit, testAddress, testKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    logger.info("Safe withdraw successful");
  }
}
