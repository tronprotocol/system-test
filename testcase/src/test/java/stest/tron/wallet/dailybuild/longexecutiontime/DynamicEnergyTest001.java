package stest.tron.wallet.dailybuild.longexecutiontime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.ContractState;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class DynamicEnergyTest001 extends TronBaseTest {  Long sendAmount = 100000000000000L;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAccountAddress = ecKey1.getAddress();
  String testAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = 10000000000L;
  private Long energyFee = 0L;
  byte[] contractAddress;
  private Long chainMaxFeeLimit = 0L;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    PublicMethod.printAddress(testAccountKey);    if(!PublicMethod.getAllowDynamicEnergyProposalIsOpen(blockingStubFull)) {      throw new SkipException("Skipping getAllowDynamicEnergy test case");
    }
    chainMaxFeeLimit = PublicMethod.getChainParametersValue(ProposalEnum.getMaxFeeLimit.getProposalName(), blockingStubFull);
    energyFee = PublicMethod.getChainParametersValue(ProposalEnum.GetEnergyFee.getProposalName(), blockingStubFull);
    maxFeeLimit = Math.min((long) (maxFeeLimit * (energyFee / 280.0)), chainMaxFeeLimit);
    PublicMethod.sendcoin(testAccountAddress,sendAmount,foundationAddress,foundationKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/contractLinkage005.sol";
  String contractName = "timeoutTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit, 0L, 100, null, testAccountKey,
        testAccountAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    contractAddress = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get().getContractAddress().toByteArray();
  Long nextMaintainTime = blockingStubFull.getNextMaintenanceTime(EmptyMessage.newBuilder().build()).getNum();

    while (System.currentTimeMillis() < nextMaintainTime + 9000) {
      PublicMethod.waitProduceNextBlock(blockingStubFull);
    }

  }


  @Test(enabled = true,description = "Test get contract state", groups = {"daily"})
  public void test01GetContractStatus() throws Exception {
    ContractState contractState = PublicMethod.getContractInfo(contractAddress,blockingStubFull).getContractState();
    Assert.assertTrue(contractState.getEnergyUsage() == 0);
    Assert.assertTrue(contractState.getEnergyFactor() == 0);
  Long currentCycle = contractState.getUpdateCycle();
  Long getDynamicEnergyThreshold = PublicMethod.getChainParametersValue(ProposalEnum.GetDynamicEnergyThreshold
        .getProposalName(), blockingStubFull);
  Long getDynamicEnergyIncreaseFactor =  PublicMethod.getChainParametersValue(ProposalEnum.GetDynamicEnergyIncreaseFactor
        .getProposalName(), blockingStubFull);
  String argsStr = "500";
  String txid = PublicMethod.triggerContract(contractAddress,
        "testUseStorage(uint256)", argsStr, false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long energyUsageTotal = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get().getReceipt().getEnergyUsageTotal();

    contractState = PublicMethod.getContractInfo(contractAddress,blockingStubFull).getContractState();
  Long currentContractUsed = contractState.getEnergyUsage();
    Assert.assertEquals(energyUsageTotal, currentContractUsed);
  //Failed trigger didn't increase contract energy used
    txid = PublicMethod.triggerContract(contractAddress,
        "testUseStorage(uint256)", "5000", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    energyUsageTotal = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get().getReceipt().getEnergyUsageTotal();

    contractState = PublicMethod.getContractInfo(contractAddress,blockingStubFull).getContractState();
  Long newCurrentContractUsed = contractState.getEnergyUsage();
    Assert.assertEquals(currentContractUsed, newCurrentContractUsed);
    Assert.assertTrue(energyUsageTotal > 0);
  Long nextMaintainTime = blockingStubFull.getNextMaintenanceTime(EmptyMessage.newBuilder().build()).getNum();


    argsStr = "1200";
  int repeatTimes = 0;
    while (currentContractUsed < getDynamicEnergyThreshold) {
      //if repeat too many times , set case to fail
      Assert.assertTrue(repeatTimes < 25);
      PublicMethod.triggerContract(contractAddress,
          "testUseStorage(uint256)", argsStr, false,
          0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
      Thread.sleep(500);
      contractState = PublicMethod.getContractInfo(contractAddress,blockingStubFull).getContractState();
      currentContractUsed = contractState.getEnergyUsage();
      logger.info("currentContractUsed       : "  + currentContractUsed);
      logger.info("getDynamicEnergyThreshold : "  + getDynamicEnergyThreshold);
      repeatTimes += 1;
    }

    while (System.currentTimeMillis() - 3000 < nextMaintainTime) {
      PublicMethod.waitProduceNextBlock(blockingStubFull);
    }


    contractState = PublicMethod.getContractInfo(contractAddress,blockingStubFull).getContractState();
    Assert.assertTrue(contractState.getEnergyFactor() == getDynamicEnergyIncreaseFactor);
    Assert.assertTrue(currentCycle + 1 == contractState.getUpdateCycle());
  String transactionId = PublicMethod.triggerContract(contractAddress,
        "testUseStorage(uint256)", "500", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo = PublicMethod.getTransactionInfoById(transactionId,blockingStubFull).get();
  Long penaltyEnergyTotal = transactionInfo.getReceipt().getEnergyUsageTotal();
  Long penaltyEnergy = transactionInfo.getReceipt().getEnergyPenaltyTotal();
  //Assert.assertEquals((Long)(notPenaltyEnergyTotal + penaltyEnergy),penaltyEnergyTotal);
    Assert.assertTrue(penaltyEnergy <= (Long)((penaltyEnergyTotal -  penaltyEnergy) * contractState.getEnergyFactor() / 10000));
    Assert.assertTrue(penaltyEnergy >= (Long)((penaltyEnergyTotal -  penaltyEnergy) * contractState.getEnergyFactor() / 10000) - 30000);


  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(testAccountAddress, testAccountKey, foundationAddress, blockingStubFull);  }
}