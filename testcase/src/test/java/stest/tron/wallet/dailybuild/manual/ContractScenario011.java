package stest.tron.wallet.dailybuild.manual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.util.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class ContractScenario011 extends TronBaseTest {  String kittyCoreAddressAndCut = "";
  byte[] kittyCoreContractAddress = null;
  byte[] saleClockAuctionContractAddress = null;
  byte[] siringClockAuctionContractAddress = null;
  byte[] geneScienceInterfaceContractAddress = null;
  Integer consumeUserResourcePercent = 50;
  String txid = "";
  Optional<TransactionInfo> infoById = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] deployAddress = ecKey1.getAddress();
  String deployKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] triggerAddress = ecKey2.getAddress();
  String triggerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long energyFee = 0L;
  private double feeLimitRate = 1.0;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(deployKey);
    PublicMethod.printAddress(triggerKey);    Assert.assertTrue(PublicMethod.sendcoin(deployAddress, 50000000000L, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod.sendcoin(triggerAddress, 50000000000L, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    energyFee = PublicMethod.getChainParametersValue(ProposalEnum.GetEnergyFee.getProposalName(), blockingStubFull);
    feeLimitRate = energyFee / 280.0;
  }

  @Test(enabled = true, description = "Deploy Erc721 contract \"Kitty Core\"", groups = {"daily"})
  public void deployErc721KittyCore() {
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(deployAddress, 3000000000L,
        0, 1, deployKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull1);
    Assert.assertTrue(PublicMethod.freezeBalance(deployAddress, 6000000000L, 0,
        deployKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull1);
    Assert.assertTrue(PublicMethod.freezeBalance(triggerAddress, 6000000000L, 0,
        triggerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull1);
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(deployAddress,
        blockingStubFull);
  Long cpuLimit = accountResource.getEnergyLimit();
  Long cpuUsage = accountResource.getEnergyUsed();
    Account account = PublicMethod.queryAccount(deployAddress, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
  String contractName = "KittyCore";
  String filePath = "./src/test/resources/soliditycode/contractScenario011.sol";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    logger.info("Kitty Core");
    kittyCoreContractAddress = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
        deployAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(kittyCoreContractAddress,
        blockingStubFull);
    Assert.assertFalse(StringUtils.isEmpty(smartContract.getBytecode()));

    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethod.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    cpuUsage = accountResource.getEnergyUsed();
    account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
    logger.info(ByteArray.toHexString(kittyCoreContractAddress));
    logger.info(ByteArray.toHexString(kittyCoreContractAddress).substring(2));

    kittyCoreAddressAndCut = "000000000000000000000000" + ByteArray
        .toHexString(kittyCoreContractAddress).substring(2);
    kittyCoreAddressAndCut = kittyCoreAddressAndCut + "0000000000000000000000000000000000000000000"
        + "000000000000000000100";
  }

  @Test(enabled = true, description = "Deploy Erc721 contract \"Sale Clock Auction\"", groups = {"daily"})
  public void deploySaleClockAuction() {
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(deployAddress,
        blockingStubFull);
  Long cpuLimit = accountResource.getEnergyLimit();
  Long cpuUsage = accountResource.getEnergyUsed();
    Account account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
  String contractName = "SaleClockAuction";
  String filePath = "./src/test/resources/soliditycode/contractScenario011.sol";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    logger.info("Sale Clock Auction");
  //saleClockAuctionContractAddress;
  String data = "\"" + Base58.encode58Check(kittyCoreContractAddress) + "\"," + 100;
  String deplTxid = PublicMethod
        .deployContractWithConstantParame(contractName, abi, code, "constructor(address,uint256)",
            data, "", maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey, deployAddress,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethod
        .getTransactionInfoById(deplTxid, blockingStubFull);
    Assert.assertTrue(info.get().getResultValue() == 0);

    saleClockAuctionContractAddress = info.get().getContractAddress().toByteArray();
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(saleClockAuctionContractAddress,
        blockingStubFull);
    Assert.assertFalse(StringUtils.isEmpty(smartContract.getBytecode()));
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethod.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    cpuUsage = accountResource.getEnergyUsed();
    account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
  String triggerTxid = PublicMethod
        .triggerContract(saleClockAuctionContractAddress, "isSaleClockAuction()", "#", false, 0,
            maxFeeLimit, deployAddress, deployKey, blockingStubFull);
    Optional<TransactionInfo> inFoByid = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Ttttt " + triggerTxid);
    Assert.assertTrue(inFoByid.get().getResultValue() == 0);
  }

  @Test(enabled = true, description = "Deploy Erc721 contract \"Siring Clock Auction\"", groups = {"daily"})
  public void deploySiringClockAuction() {
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(deployAddress,
        blockingStubFull);
  Long cpuLimit = accountResource.getEnergyLimit();
  Long cpuUsage = accountResource.getEnergyUsed();
    Account account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
  String contractName = "SiringClockAuction";
  String filePath = "./src/test/resources/soliditycode/contractScenario011.sol";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String data = "\"" + Base58.encode58Check(kittyCoreContractAddress) + "\"," + 100;
  String siringClockAuctionContractAddressTxid = PublicMethod
        .deployContractWithConstantParame(contractName, abi, code, "constructor(address,uint256)",
            data,
            "", maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
            deployAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info2 = PublicMethod
        .getTransactionInfoById(siringClockAuctionContractAddressTxid, blockingStubFull);
    siringClockAuctionContractAddress = info2.get().getContractAddress().toByteArray();
    Assert.assertTrue(info2.get().getResultValue() == 0);
    SmartContract smartContract = PublicMethod.getContract(siringClockAuctionContractAddress,
        blockingStubFull);
    Assert.assertFalse(StringUtils.isEmpty(smartContract.getBytecode()));
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethod.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    cpuUsage = accountResource.getEnergyUsed();
    account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
  }

  @Test(enabled = true, description = "Deploy Erc721 contract \"Gene Science Interface\"", groups = {"daily"})
  public void deployGeneScienceInterface() {
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(deployAddress,
        blockingStubFull);
  Long cpuLimit = accountResource.getEnergyLimit();
  Long cpuUsage = accountResource.getEnergyUsed();
    Account account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
  String contractName = "GeneScienceInterface";
  String filePath = "./src/test/resources/soliditycode/contractScenario011.sol";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit,
        0L, consumeUserResourcePercent, null, deployKey, deployAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info2 = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    geneScienceInterfaceContractAddress = info2.get().getContractAddress().toByteArray();
    Assert.assertTrue(info2.get().getResultValue() == 0);

    SmartContract smartContract = PublicMethod.getContract(geneScienceInterfaceContractAddress,
        blockingStubFull);
    Assert.assertFalse(StringUtils.isEmpty(smartContract.getBytecode()));
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethod.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
    cpuUsage = accountResource.getEnergyUsed();
    account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
  }

  @Test(enabled = true, description = "Set three contract address for Kitty Core, "
      + "set three CXO roles", groups = {"daily"})
  public void triggerToSetThreeContractAddressToKittyCore() {
    //Set SaleAuctionAddress to kitty core.
    String saleContractString = "\"" + Base58.encode58Check(saleClockAuctionContractAddress) + "\"";
    txid = PublicMethod.triggerContract(kittyCoreContractAddress, "setSaleAuctionAddress(address)",
        saleContractString, false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  //Set SiringAuctionAddress to kitty core.
    String siringContractString = "\"" + Base58.encode58Check(siringClockAuctionContractAddress)
        + "\"";
    txid = PublicMethod
        .triggerContract(kittyCoreContractAddress, "setSiringAuctionAddress(address)",
            siringContractString, false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    logger.info(txid);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  //Set gen contract to kitty core
    String genContractString = "\"" + Base58.encode58Check(geneScienceInterfaceContractAddress)
        + "\"";
    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "setGeneScienceAddress(address)", genContractString,
        false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  //Start the game.
    Integer result = 1;
    Integer times = 0;
    while (result == 1) {
      txid = PublicMethod.triggerContract(kittyCoreContractAddress, "unpause()", "", false, 0,
          10000000L, deployAddress, deployKey, blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
      result = infoById.get().getResultValue();
      if (times++ == 3) {
        break;
      }
    }

    Assert.assertTrue(result == 0);
    logger.info("start the game " + txid);
  //Create one gen0 cat.
    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "createGen0Auction(uint256)", "-1000000000000000", false,
        0, (long)(100000000L * feeLimitRate), deployAddress, deployKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "gen0CreatedCount()", "#", false,
        0, (long)(100000000L * feeLimitRate), deployAddress, deployKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "getKitty(uint256)", "1", false, 0, (long) (10000000L * feeLimitRate), triggerAddress,
        triggerKey, blockingStubFull);
    logger.info("getKitty " + txid);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  String newCxoAddress = "\"" + Base58.encode58Check(triggerAddress)
        + "\"";

    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "setCOO(address)", newCxoAddress, false, 0, (long) (10000000L * feeLimitRate), deployAddress,
        deployKey, blockingStubFull);
    logger.info("COO " + txid);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "setCFO(address)", newCxoAddress, false, 0, (long) (10000000L * feeLimitRate), deployAddress,
        deployKey, blockingStubFull);
    logger.info("CFO " + txid);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "setCEO(address)", newCxoAddress, false, 0, (long) (1000000000L * feeLimitRate), deployAddress,
        deployKey, blockingStubFull);
    logger.info("CEO " + txid);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  @Test(enabled = true, description = "Create Gen0 cat", groups = {"daily"})
  public void triggerUseTriggerEnergyUsage() {
    ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] triggerUseTriggerEnergyUsageAddress = ecKey3.getAddress();
  final String triggerUseTriggerEnergyUsageKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    Assert.assertTrue(
        PublicMethod.sendcoin(triggerUseTriggerEnergyUsageAddress, 100000000000L,
            foundationAddress, foundationKey, blockingStubFull));
  String newCxoAddress = "\"" + Base58.encode58Check(triggerUseTriggerEnergyUsageAddress)
        + "\"";
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  final String txid1;
  final String txid2;
  final String txid3;
    txid1 = PublicMethod.triggerContract(kittyCoreContractAddress,
        "setCOO(address)", newCxoAddress, false, 0, maxFeeLimit, triggerAddress,
        triggerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("COO " + txid);

    txid2 = PublicMethod.triggerContract(kittyCoreContractAddress,
        "setCFO(address)", newCxoAddress, false, 0, maxFeeLimit, triggerAddress,
        triggerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("CFO " + txid);

    txid3 = PublicMethod.triggerContract(kittyCoreContractAddress,
        "setCEO(address)", newCxoAddress, false, 0, maxFeeLimit, triggerAddress,
        triggerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("CEO " + txid);

    infoById = PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    infoById = PublicMethod.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    infoById = PublicMethod.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long beforeBalance = PublicMethod
        .queryAccount(triggerUseTriggerEnergyUsageKey, blockingStubFull).getBalance();
    logger.info("before balance is " + Long.toString(beforeBalance));
    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "createGen0Auction(uint256)", "0", false,
        0, 100000000L, triggerUseTriggerEnergyUsageAddress, triggerUseTriggerEnergyUsageKey,
        blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull1);
    logger.info("Q " + Long
        .toString(infoById.get().getReceipt().getEnergyFee()));
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsage() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyFee() > 10000);
  //    Assert.assertTrue(infoById.get().getReceipt().getOriginEnergyUsage() > 10000);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal()
        == infoById.get().getReceipt().getEnergyFee() / energyFee + infoById.get().getReceipt()
        .getOriginEnergyUsage());
  Long fee = infoById.get().getFee();
  Long afterBalance = PublicMethod
        .queryAccount(triggerUseTriggerEnergyUsageKey, blockingStubFull1).getBalance();
    logger.info("after balance is " + Long.toString(afterBalance));
    logger.info("fee is " + Long.toString(fee));
    Assert.assertTrue(beforeBalance == afterBalance + fee);

    logger.info("before EnergyUsage is " + infoById.get().getReceipt().getEnergyUsage());
    logger.info("before EnergyFee is " + infoById.get().getReceipt().getEnergyFee());
    logger.info("before OriginEnergyUsage is " + infoById.get().getReceipt()
        .getOriginEnergyUsage());
    logger.info("before EnergyTotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Assert.assertTrue(
        PublicMethod.freezeBalanceGetEnergy(triggerUseTriggerEnergyUsageAddress, 6000000000L,
            0, 1, triggerUseTriggerEnergyUsageKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    beforeBalance = PublicMethod.queryAccount(triggerUseTriggerEnergyUsageKey, blockingStubFull)
        .getBalance();
    logger.info("before balance is " + Long.toString(beforeBalance));

    AccountResourceMessage accountResource = PublicMethod
        .getAccountResource(triggerUseTriggerEnergyUsageAddress, blockingStubFull);
  Long energyLimit = accountResource.getEnergyLimit();
    logger.info("before EnergyLimit is " + Long.toString(energyLimit));

    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "createGen0Auction(uint256)", "0", false,
        0, (long) (100000000L * feeLimitRate), triggerUseTriggerEnergyUsageAddress, triggerUseTriggerEnergyUsageKey,
        blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull1);
    logger.info("after EnergyUsage is " + infoById.get().getReceipt().getEnergyUsage());
    logger.info("after EnergyFee is " + infoById.get().getReceipt().getEnergyFee());
    logger.info("after OriginEnergyUsage is " + infoById.get().getReceipt().getOriginEnergyUsage());
    logger.info("after EnergyTotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    fee = infoById.get().getFee();
    afterBalance = PublicMethod.queryAccount(triggerUseTriggerEnergyUsageKey, blockingStubFull1)
        .getBalance();
    logger.info("after balance is " + Long.toString(afterBalance));
    logger.info("fee is " + Long.toString(fee));

    accountResource = PublicMethod
        .getAccountResource(triggerUseTriggerEnergyUsageAddress, blockingStubFull1);
    energyLimit = accountResource.getEnergyLimit();

    logger.info("after EnergyLimit is " + Long.toString(energyLimit));

    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsage() > 10000);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyFee() == 0);
  //Assert.assertTrue(infoById.get().getReceipt().getOriginEnergyUsage() > 10000);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() == infoById.get()
        .getReceipt().getEnergyUsage() + infoById.get().getReceipt().getOriginEnergyUsage());
  //    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsage() == infoById.get()
    //        .getReceipt().getOriginEnergyUsage());

    Assert.assertTrue(beforeBalance == afterBalance + fee);
    PublicMethod.unFreezeBalance(deployAddress, deployKey, 1,
        deployAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(triggerAddress, triggerKey, 1,
        triggerAddress, blockingStubFull);

    PublicMethod
        .unFreezeBalance(triggerUseTriggerEnergyUsageAddress, triggerUseTriggerEnergyUsageKey, 1,
            triggerUseTriggerEnergyUsageAddress, blockingStubFull);
    PublicMethod.freeResource(triggerUseTriggerEnergyUsageAddress, triggerUseTriggerEnergyUsageKey,
        foundationAddress, blockingStubFull);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}


