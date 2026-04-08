package stest.tron.wallet.committee;

import io.grpc.ManagedChannel;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletSolidityGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethod;

import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class WalletTestCommittee002 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  //Witness 47.93.9.236
  //Witness 47.93.33.201
  //Witness 123.56.10.6
  //Wtiness 39.107.80.135
  //Witness 47.93.184.2
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  private final byte[] witness001Address = PublicMethod.getFinalAddress(witnessKey);
  private final byte[] witness002Address = PublicMethod.getFinalAddress(witnessKey2);
  private final byte[] witness003Address = PublicMethod.getFinalAddress(witnessKey3);
  private final byte[] witness004Address = PublicMethod.getFinalAddress(witnessKey4);
  private final byte[] witness005Address = PublicMethod.getFinalAddress(witnessKey5);  private ManagedChannel channelSolidity = null;  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {  }

  @Test(enabled = true)
  public void testCreateProposalMaintenanceTimeInterval() {    Assert.assertTrue(PublicMethod.sendcoin(witness001Address, 10000000L,
        toAddress, testKey003, blockingStubFull));

    //0:MAINTENANCE_TIME_INTERVAL,[3*27s,24h]
    //Minimum interval
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(0L, 81000L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum interval
    proposalMap.put(0L, 86400000L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Minimum -1 interval, create failed.
    proposalMap.put(0L, 80000L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum + 1 interval
    proposalMap.put(0L, 86401000L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(0L, 86400000L);
    Assert.assertFalse(PublicMethod.createProposal(toAddress, testKey003, proposalMap,
        blockingStubFull));
  }

  @Test(enabled = true)
  public void testCreateProposalAccountUpgradeCost() {
    initSolidityChannel();
    //1:ACCOUNT_UPGRADE_COST,[0,100 000 000 000 000 000]//drop
    //Minimum AccountUpgradeCost
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(1L, 0L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum AccountUpgradeCost
    proposalMap.put(1L, 100000000000000000L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Minimum - 1 AccountUpgradeCost
    proposalMap.put(1L, -1L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum + 1 AccountUpgradeCost
    proposalMap.put(1L, 100000000000000001L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(1L, 86400000L);
    Assert.assertFalse(PublicMethod.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));
  }

  @Test(enabled = true)
  public void testCreateProposalCreateAccountFee() {
    //2:CREATE_ACCOUNT_FEE,[0,100 000 000 000 000 000]//drop
    //Minimum CreateAccountFee
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(2L, 0L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum CreateAccountFee
    proposalMap.put(2L, 100000000000000000L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Minimum - 1 CreateAccountFee
    proposalMap.put(2L, -1L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum + 1 CreateAccountFee
    proposalMap.put(2L, 100000000000000001L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(2L, 86400000L);
    Assert.assertFalse(PublicMethod.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testTransactionFee() {
    //3:TRANSACTION_FEE,[0,100 000 000 000 000 000]//drop
    //Minimum TransactionFee
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(3L, 0L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum TransactionFee
    proposalMap.put(3L, 100000000000000000L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Minimum - 1 TransactionFee
    proposalMap.put(3L, -1L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum + 1 TransactionFee
    proposalMap.put(3L, 100000000000000001L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(3L, 86400000L);
    Assert.assertFalse(PublicMethod.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testAssetIssueFee() {
    //4:ASSET_ISSUE_FEE,[0,100 000 000 000 000 000]//drop
    //Minimum AssetIssueFee
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(4L, 0L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Duplicat proposals
    proposalMap.put(4L, 0L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum AssetIssueFee
    proposalMap.put(4L, 100000000000000000L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Minimum - 1 AssetIssueFee
    proposalMap.put(4L, -1L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum + 1 AssetIssueFee
    proposalMap.put(4L, 100000000000000001L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(4L, 86400000L);
    Assert.assertFalse(PublicMethod.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testWitnessPayPerBlock() {
    //5:WITNESS_PAY_PER_BLOCK,[0,100 000 000 000 000 000]//drop
    //Minimum WitnessPayPerBlock
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(5L, 0L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum WitnessPayPerBlock
    proposalMap.put(5L, 100000000000000000L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Minimum - 1 WitnessPayPerBlock
    proposalMap.put(5L, -1L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum + 1 WitnessPayPerBlock
    proposalMap.put(5L, 100000000000000001L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(5L, 86400000L);
    Assert.assertFalse(PublicMethod.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testWitnessStandbyAllowance() {
    //6:WITNESS_STANDBY_ALLOWANCE,[0,100 000 000 000 000 000]//drop
    //Minimum WitnessStandbyAllowance
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(6L, 0L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum WitnessStandbyAllowance
    proposalMap.put(6L, 100000000000000000L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Minimum - 1 WitnessStandbyAllowance
    proposalMap.put(6L, -1L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum + 1 WitnessStandbyAllowance
    proposalMap.put(6L, 100000000000000001L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(6L, 86400000L);
    Assert.assertFalse(PublicMethod.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testCreateNewAccountFeeInSystemControl() {
    //7:CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT,0 or 1
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(7L, 1L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum WitnessStandbyAllowance
    proposalMap.put(7L, 100000000000000000L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Minimum - 1 WitnessStandbyAllowance
    proposalMap.put(6L, -1L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Maximum + 1 WitnessStandbyAllowance
    proposalMap.put(6L, 100000000000000001L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(6L, 86400000L);
    Assert.assertFalse(PublicMethod.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testInvalidProposals() {
    // The index isn't from 0-9
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(10L, 60L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

    //The index is -1
    proposalMap.put(-1L, 6L);
    Assert.assertFalse(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));

  }
}

