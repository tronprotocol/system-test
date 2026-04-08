package stest.tron.wallet.dailybuild.manual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class ContractScenario002 extends TronBaseTest {  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract002Address = ecKey1.getAddress();
  String contract002Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String txid;
  private ManagedChannel channelSoliInFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSoliInFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initPbftChannel();
    initSolidityChannel();
    PublicMethod.printAddress(contract002Key);    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
        .usePlaintext()
        .build();
    blockingStubSoliInFull = WalletSolidityGrpc.newBlockingStub(channelSoliInFull);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext()
        .build();
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

  }

  @Test(enabled = true, description = "Deploy contract with java-tron support interface", groups = {"daily"})
  public void test01DeployTronNative() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract002Address = ecKey1.getAddress();
  String contract002Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethod.sendcoin(contract002Address, 500000000L, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(contract002Address, 1000000L,
        0, 1, contract002Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contract002Address,
        blockingStubFull);
  Long energyLimit = accountResource.getEnergyLimit();
  Long energyUsage = accountResource.getEnergyUsed();
  Long balanceBefore = PublicMethod.queryAccount(contract002Key, blockingStubFull).getBalance();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    logger.info("before balance is " + Long.toString(balanceBefore));
  String contractName = "TronNative";
  String filePath = "./src/test/resources/soliditycode/contractScenario002.sol";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    txid = PublicMethod.deployContractAndGetTransactionInfoById(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, contract002Key, contract002Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull1);

    logger.info(txid);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    com.google.protobuf.ByteString contractAddress = infoById.get().getContractAddress();
    SmartContract smartContract = PublicMethod
        .getContract(contractAddress.toByteArray(), blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
    PublicMethod.waitProduceNextBlock(blockingStubFull1);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    accountResource = PublicMethod.getAccountResource(contract002Address, blockingStubFull1);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
  Long balanceAfter = PublicMethod.queryAccount(contract002Address, blockingStubFull1)
        .getBalance();

    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
    logger.info("after balance is " + Long.toString(balanceAfter));
    logger.info("transaction fee is " + Long.toString(infoById.get().getFee()));

    Assert.assertTrue(energyUsage > 0);
    Assert.assertTrue(balanceBefore == balanceAfter + infoById.get().getFee());
    PublicMethod.unFreezeBalance(contract002Address, contract002Key, 1,
        contract002Address, blockingStubFull);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get smart contract with invalid address", groups = {"daily"})
  public void test02GetContractWithInvalidAddress() {
    byte[] contractAddress = contract002Address;
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    logger.info(smartContract.getAbi().toString());
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from solidity", groups = {"daily"})
  public void test03GetTransactionByIdFromSolidity() {
    Assert.assertFalse(PublicMethod.getTransactionById(txid, blockingStubSolidity)
        .get().getSignature(0).isEmpty());
    Assert.assertEquals(PublicMethod.getTransactionById(txid, blockingStubFull),
        PublicMethod.getTransactionById(txid, blockingStubSolidity));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from PBFT", groups = {"daily"})
  public void test04GetTransactionByIdFromPbft() {
    Assert.assertFalse(PublicMethod.getTransactionById(txid, blockingStubPbft)
        .get().getSignature(0).isEmpty());
    Assert.assertEquals(PublicMethod.getTransactionById(txid, blockingStubSoliInFull),
        PublicMethod.getTransactionById(txid, blockingStubPbft));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from Solidity", groups = {"daily"})
  public void test05GetTransactionInfoByIdFromSolidity() throws Exception {
    long netUsage = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get().getReceipt()
        .getNetUsage();

    Assert.assertEquals(PublicMethod.getTransactionInfoByIdFromSolidity(txid, blockingStubSolidity)
        .get().getReceipt().getNetUsage(), netUsage);

    Assert
        .assertEquals(PublicMethod.getTransactionInfoByIdFromSolidity(txid, blockingStubSoliInFull)
            .get().getReceipt().getNetUsage(), netUsage);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from PBFT", groups = {"daily"})
  public void test06GetTransactionInfoByIdFromPbft() {
    long energyUsage = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get()
        .getReceipt()
        .getEnergyUsage();

    Assert.assertEquals(PublicMethod.getTransactionInfoByIdFromSolidity(txid, blockingStubPbft)
        .get().getReceipt().getEnergyUsage(), energyUsage);
  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(contract002Address, contract002Key, foundationAddress, blockingStubFull);    if (channelSoliInFull != null) {
      channelSoliInFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
