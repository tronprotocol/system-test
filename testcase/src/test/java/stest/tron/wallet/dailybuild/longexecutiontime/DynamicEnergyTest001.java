package stest.tron.wallet.dailybuild.longexecutiontime;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.ContractState;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class DynamicEnergyTest001 {
  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  Long sendAmount = 100000000000000L;
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
    PublicMethed.printAddress(testAccountKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    if(!PublicMethed.getAllowDynamicEnergyProposalIsOpen(blockingStubFull)) {
      if (channelFull != null) {
        channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      }
      throw new SkipException("Skipping getAllowDynamicEnergy test case");
    }
    chainMaxFeeLimit = PublicMethed.getChainParametersValue(ProposalEnum.getMaxFeeLimit.getProposalName(), blockingStubFull);
    energyFee = PublicMethed.getChainParametersValue(ProposalEnum.GetEnergyFee.getProposalName(), blockingStubFull);
    maxFeeLimit = Math.min((long) (maxFeeLimit * (energyFee / 280.0)), chainMaxFeeLimit);
    PublicMethed.sendcoin(testAccountAddress,sendAmount,foundationAddress,foundationKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "./src/test/resources/soliditycode/contractLinkage005.sol";
    String contractName = "timeoutTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit, 0L, 100, null, testAccountKey,
        testAccountAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    contractAddress = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getContractAddress().toByteArray();
    Long nextMaintainTime = blockingStubFull.getNextMaintenanceTime(EmptyMessage.newBuilder().build()).getNum();

    while (System.currentTimeMillis() < nextMaintainTime + 9000) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }

  }


  @Test(enabled = true,description = "Test get contract state")
  public void test01GetContractStatus() throws Exception {
    ContractState contractState = PublicMethed.getContractInfo(contractAddress,blockingStubFull).getContractState();
    Assert.assertTrue(contractState.getEnergyUsage() == 0);
    Assert.assertTrue(contractState.getEnergyFactor() == 0);
    Long currentCycle = contractState.getUpdateCycle();

    Long getDynamicEnergyThreshold = PublicMethed.getChainParametersValue(ProposalEnum.GetDynamicEnergyThreshold
        .getProposalName(), blockingStubFull);
    Long getDynamicEnergyIncreaseFactor =  PublicMethed.getChainParametersValue(ProposalEnum.GetDynamicEnergyIncreaseFactor
        .getProposalName(), blockingStubFull);

    String argsStr = "500";
    String txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)", argsStr, false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long energyUsageTotal = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getReceipt().getEnergyUsageTotal();

    contractState = PublicMethed.getContractInfo(contractAddress,blockingStubFull).getContractState();
    Long currentContractUsed = contractState.getEnergyUsage();
    Assert.assertEquals(energyUsageTotal, currentContractUsed);



    //Failed trigger didn't increase contract energy used
    txid = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)", "5000", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    energyUsageTotal = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getReceipt().getEnergyUsageTotal();

    contractState = PublicMethed.getContractInfo(contractAddress,blockingStubFull).getContractState();
    Long newCurrentContractUsed = contractState.getEnergyUsage();
    Assert.assertEquals(currentContractUsed, newCurrentContractUsed);
    Assert.assertTrue(energyUsageTotal > 0);




    Long nextMaintainTime = blockingStubFull.getNextMaintenanceTime(EmptyMessage.newBuilder().build()).getNum();


    argsStr = "1200";
    int repeatTimes = 0;
    while (currentContractUsed < getDynamicEnergyThreshold) {
      //if repeat too many times , set case to fail
      Assert.assertTrue(repeatTimes < 25);
      PublicMethed.triggerContract(contractAddress,
          "testUseStorage(uint256)", argsStr, false,
          0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
      Thread.sleep(500);
      contractState = PublicMethed.getContractInfo(contractAddress,blockingStubFull).getContractState();
      currentContractUsed = contractState.getEnergyUsage();
      logger.info("currentContractUsed       : "  + currentContractUsed);
      logger.info("getDynamicEnergyThreshold : "  + getDynamicEnergyThreshold);
      repeatTimes += 1;
    }

    while (System.currentTimeMillis() - 3000 < nextMaintainTime) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }




    contractState = PublicMethed.getContractInfo(contractAddress,blockingStubFull).getContractState();
    Assert.assertTrue(contractState.getEnergyFactor() == getDynamicEnergyIncreaseFactor);
    Assert.assertTrue(currentCycle + 1 == contractState.getUpdateCycle());





    String transactionId = PublicMethed.triggerContract(contractAddress,
        "testUseStorage(uint256)", "500", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo transactionInfo = PublicMethed.getTransactionInfoById(transactionId,blockingStubFull).get();
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
    PublicMethed.freedResource(testAccountAddress, testAccountKey, foundationAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}