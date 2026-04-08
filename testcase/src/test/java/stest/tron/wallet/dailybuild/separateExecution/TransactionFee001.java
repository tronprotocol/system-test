package stest.tron.wallet.dailybuild.separateExecution;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.contract.WitnessContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.CommonParameter;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Retry;
import stest.tron.wallet.common.client.utils.Sha256Hash;
import stest.tron.wallet.common.client.utils.TransactionUtils;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
// import org.tron.common.parameter.CommonParameter;

@Slf4j
public class TransactionFee001 extends TronBaseTest {

  private long multiSignFee =
      Configuration.getByPath("testng.conf").getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee =
      Configuration.getByPath("testng.conf").getLong("defaultParameter.updateAccountPermissionFee");
  private final String blackHoleAdd =
      Configuration.getByPath("testng.conf").getString("defaultParameter.blackHoleAddress");
  private Long costForCreateWitness = 9999000000L;
  String witness03Url = "http://witness03Url.com";
  byte[] createUrl = witness03Url.getBytes();
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] deployAddress = ecKey1.getAddress();
  final String deployKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(0);
  private String soliInPbft =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(2);
  Long startNum = 0L;
  Long endNum = 0L;
  Long witness01Allowance1 = 0L;
  Long witness02Allowance1 = 0L;
  Long blackHoleBalance1 = 0L;
  Long witness01Allowance2 = 0L;
  Long witness02Allowance2 = 0L;
  Long blackHoleBalance2 = 0L;
  Long witness01Increase = 0L;
  Long witness02Increase = 0L;
  Long beforeBurnTrxAmount = 0L;
  Long afterBurnTrxAmount = 0L;
  String txid = null;
  private boolean srStatus = true;

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    initPbftChannel();
    initSolidityChannel();  }

  @Test(enabled = true, retryAnalyzer = Retry.class, priority=2, description = "Test deploy contract with energy fee to sr", groups = {"daily", "serial"})
  public void test01DeployContractEnergyFeeToSr() {
    Assert.assertTrue(
        PublicMethod.sendcoin(
            deployAddress, 20000000000L, foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode//contractLinkage003.sol";
  String contractName = "divideIHaveArgsReturnStorage";
    HashMap retMap = null;
  String code = null;
  String abi = null;
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();

    startNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance1 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance1 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();

    blackHoleBalance1 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();
    beforeBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();

    txid =
        PublicMethod.deployContractAndGetTransactionInfoById(
            contractName,
            abi,
            code,
            "",
            maxFeeLimit,
            0L,
            0,
            null,
            deployKey,
            deployAddress,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    endNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance2 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance2 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance2 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();
    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;
  // blackHoleIncrease = blackHoleBalance2 - blackHoleBalance1;
    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info(
        "====== witness02Allowance1 :"
            + witness02Allowance1
            + "   witness02Allowance2 :"
            + witness02Allowance2
            + "increase :"
            + witness02Increase);
    logger.info(
        "====== witness01Allowance1 :"
            + witness01Allowance1
            + "  witness01Allowance2 :"
            + witness01Allowance2
            + "  increase :"
            + witness01Increase);

    Map<String, Long> witnessAllowance =
        PublicMethod.getAllowance2(startNum, endNum, blockingStubFull);

    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress)) - witness01Increase))
            <= 2);
    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress2)) - witness02Increase))
            <= 2);
    Assert.assertEquals(blackHoleBalance1, blackHoleBalance2);
    Optional<Protocol.TransactionInfo> infoById =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(infoById.get().getFee(), infoById.get().getPackingFee());
    afterBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();
    Assert.assertEquals(beforeBurnTrxAmount, afterBurnTrxAmount);
  }

  @Test(
      enabled = true, priority=2,
      retryAnalyzer = Retry.class,
      description =
          "Test update account permission fee to black hole,"
              + "trans with multi sign and fee to sr", groups = {"daily", "serial"})
  public void test02UpdateAccountPermissionAndMultiSiginTrans() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey1.getAddress();
  final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey tmpEcKey02 = new ECKey(Utils.getRandom());
  byte[] tmpAddr02 = tmpEcKey02.getAddress();
  final String tmpKey02 = ByteArray.toHexString(tmpEcKey02.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2 + multiSignFee;

    Assert.assertTrue(
        PublicMethod.sendcoin(
            ownerAddress, needCoin + 1_000_000, foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey);
    activePermissionKeys.add(tmpKey02);

    logger.info("** update owner and active permission to two address");
    startNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance1 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance1 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance1 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();
    beforeBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\""
            + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\""
            + ",\"threshold\":2,"
            + "\"operations\""
            + ":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\""
            + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1},"
            + "{\"address\":\""
            + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}"
            + "]}]}";

    txid =
        PublicMethodForMultiSign.accountPermissionUpdateForTransactionId(
            accountPermissionJson,
            ownerAddress,
            ownerKey,
            blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(infoById.get().getPackingFee(), 0);
    Assert.assertEquals(infoById.get().getFee(), 100000000L);

    endNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance2 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance2 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance2 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();
    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;
  // blackHoleIncrease = blackHoleBalance2 - blackHoleBalance1;
    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info(
        "====== witness02Allowance1 :"
            + witness02Allowance1
            + "   witness02Allowance2 :"
            + witness02Allowance2
            + "increase :"
            + witness02Increase);
    logger.info(
        "====== witness01Allowance1 :"
            + witness01Allowance1
            + "  witness01Allowance2 :"
            + witness01Allowance2
            + "  increase :"
            + witness01Increase);

    Map<String, Long> witnessAllowance =
        PublicMethod.getAllowance2(startNum, endNum, blockingStubFull);

    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress)) - witness01Increase))
            <= 2);
    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress2)) - witness02Increase))
            <= 2);
    Assert.assertEquals(blackHoleBalance2, blackHoleBalance1);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(
        2,
        PublicMethodForMultiSign.getActivePermissionKeyCount(
            PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(
        1,
        PublicMethod.queryAccount(ownerAddress, blockingStubFull)
            .getOwnerPermission()
            .getKeysCount());

    PublicMethodForMultiSign.printPermissionList(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    logger.info(
        PublicMethodForMultiSign.printPermission(
            PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    startNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance1 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance1 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance1 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();

    afterBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();
    Assert.assertTrue(afterBurnTrxAmount - beforeBurnTrxAmount == 100000000L);

    beforeBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();

    Protocol.Transaction transaction =
        PublicMethodForMultiSign.sendcoin2(
            foundationAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);
    txid =
        ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));
    logger.info("-----transaction: " + txid);

    Protocol.Transaction transaction1 =
        PublicMethodForMultiSign.addTransactionSignWithPermissionId(
            transaction, tmpKey02, 2, blockingStubFull);
    txid =
        ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction1.getRawData().toByteArray()));
    logger.info("-----transaction1: " + txid);

    Protocol.Transaction transaction2 =
        PublicMethodForMultiSign.addTransactionSignWithPermissionId(
            transaction1, witnessKey, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));

    GrpcAPI.TransactionSignWeight txWeight =
        PublicMethodForMultiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertTrue(PublicMethodForMultiSign.broadcastTransaction(transaction2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    endNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance2 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance2 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance2 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();
    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;
    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info(
        "====== witness02Allowance1 :"
            + witness02Allowance1
            + "   witness02Allowance2 :"
            + witness02Allowance2
            + "increase :"
            + witness02Increase);
    logger.info(
        "====== witness01Allowance1 :"
            + witness01Allowance1
            + "  witness01Allowance2 :"
            + witness01Allowance2
            + "  increase :"
            + witness01Increase);

    witnessAllowance = PublicMethod.getAllowance2(startNum, endNum, blockingStubFull);

    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress)) - witness01Increase))
            <= 2);
    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress2)) - witness02Increase))
            <= 2);
    Assert.assertEquals(blackHoleBalance2, blackHoleBalance1);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(infoById.get().getPackingFee(), 0);
    Assert.assertEquals(infoById.get().getFee(), 1000000L);
    afterBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();
    Assert.assertTrue(afterBurnTrxAmount - beforeBurnTrxAmount == 1000000L);
  }

  @Test(
      enabled = true, priority=2,
      description = "Test trigger result is \"OUT_OF_TIME\"" + " with energy fee to black hole", groups = {"daily", "serial"})
  public void test03OutOfTimeEnergyFeeToBlackHole() {
    Random rand = new Random();
    Integer randNum = rand.nextInt(4000);

    Assert.assertTrue(
        PublicMethod.sendcoin(
            deployAddress, maxFeeLimit * 10, foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String contractName = "StorageAndCpu" + Integer.toString(randNum);
  String code =
        Configuration.getByPath("testng.conf")
            .getString("code.code_TestStorageAndCpu_storageAndCpu");
  String abi =
        Configuration.getByPath("testng.conf").getString("abi.abi_TestStorageAndCpu_storageAndCpu");
  byte[] contractAddress = null;
    contractAddress =
        PublicMethod.deployContract(
            contractName,
            abi,
            code,
            "",
            maxFeeLimit,
            0L,
            100,
            null,
            deployKey,
            deployAddress,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    startNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance1 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance1 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance1 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();
    beforeBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();
    txid =
        PublicMethod.triggerContract(
            contractAddress,
            "testUseCpu(uint256)",
            "90100",
            false,
            0,
            maxFeeLimit,
            deployAddress,
            deployKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    endNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance2 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance2 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance2 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();
    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;

    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info(
        "====== witness02Allowance1 :"
            + witness02Allowance1
            + "   witness02Allowance2 :"
            + witness02Allowance2
            + "increase :"
            + witness02Increase);
    logger.info(
        "====== witness01Allowance1 :"
            + witness01Allowance1
            + "  witness01Allowance2 :"
            + witness01Allowance2
            + "  increase :"
            + witness01Increase);
    Optional<Protocol.TransactionInfo> infoById =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);

    logger.info("InfoById:" + infoById);

    Map<String, Long> witnessAllowance =
        PublicMethod.getAllowance2(startNum, endNum, blockingStubFull);
    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress)) - witness01Increase))
            <= 2);
    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress2)) - witness02Increase))
            <= 2);
    Assert.assertEquals(blackHoleBalance2, blackHoleBalance1);
  Long packingFee = infoById.get().getPackingFee();
    logger.info("receipt:" + infoById.get().getReceipt());
    Assert.assertTrue(packingFee == infoById.get().getReceipt().getNetFee());
    Assert.assertTrue(infoById.get().getFee() < maxFeeLimit + infoById.get().getReceipt().getNetFee());
    afterBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();
    Assert.assertTrue(afterBurnTrxAmount - beforeBurnTrxAmount == infoById.get().getReceipt().getEnergyFee());
  }

  @Test(enabled = true, priority=2, description = "Test create account with netFee to sr", groups = {"daily", "serial"})
  public void test04AccountCreate() {
    //use new account to create account cost 1.1 TRX
    ECKey creatorEcKey = new ECKey(Utils.getRandom());
  byte[] creatorAddress = creatorEcKey.getAddress();
  final String creatorKey = ByteArray.toHexString(creatorEcKey.getPrivKeyBytes());
    PublicMethod.sendcoin(creatorAddress, 100000000L, foundationAddress, foundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    startNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance1 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance1 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance1 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();
    beforeBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] lowBalAddress = ecKey.getAddress();
    txid =
        PublicMethod.createAccountGetTxid(creatorAddress, lowBalAddress, creatorKey, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    endNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance2 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance2 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance2 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();

    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;
    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info(
        "====== witness01Allowance1 :"
            + witness01Allowance1
            + "  witness01Allowance2 :"
            + witness01Allowance2
            + "  increase :"
            + witness01Increase);
    logger.info(
        "====== witness02Allowance1 :"
            + witness02Allowance1
            + "  witness02Allowance2 :"
            + witness02Allowance2
            + "  increase :"
            + witness02Increase);

    Map<String, Long> witnessAllowance =
        PublicMethod.getAllowance2(startNum, endNum, blockingStubFull);
    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress)) - witness01Increase))
            <= 2);
    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress2)) - witness02Increase))
            <= 2);
    Assert.assertEquals(blackHoleBalance1, blackHoleBalance2);
    Optional<Protocol.TransactionInfo> infoById =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getPackingFee() == 0L);
    Assert.assertTrue(infoById.get().getFee() == 1100000L);
    afterBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();
    Assert.assertTrue(afterBurnTrxAmount - beforeBurnTrxAmount == 1100000L);
  }

  @Test(
      enabled = true, priority=2,
      retryAnalyzer = Retry.class,
      description = "Test trigger contract with netFee and energyFee to sr", groups = {"daily", "serial"})
  public void test05NetFeeAndEnergyFee2Sr() {
    Random rand = new Random();
    Integer randNum = rand.nextInt(30) + 1;
    randNum = rand.nextInt(4000);

    Assert.assertTrue(
        PublicMethod.sendcoin(
            deployAddress, maxFeeLimit * 10, foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String contractName = "StorageAndCpu" + Integer.toString(randNum);
  String code =
        Configuration.getByPath("testng.conf")
            .getString("code.code_TestStorageAndCpu_storageAndCpu");
  String abi =
        Configuration.getByPath("testng.conf").getString("abi.abi_TestStorageAndCpu_storageAndCpu");
  byte[] contractAddress = null;
    contractAddress =
        PublicMethod.deployContract(
            contractName,
            abi,
            code,
            "",
            maxFeeLimit,
            0L,
            100,
            null,
            deployKey,
            deployAddress,
            blockingStubFull);
    for (int i = 0; i < 15; i++) {
      txid =
          PublicMethod.triggerContract(
              contractAddress,
              "testUseCpu(uint256)",
              "700",
              false,
              0,
              maxFeeLimit,
              deployAddress,
              deployKey,
              blockingStubFull);
    }

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    startNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance1 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance1 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance1 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();
    beforeBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();
    txid =
        PublicMethod.triggerContract(
            contractAddress,
            "testUseCpu(uint256)",
            "700",
            false,
            0,
            maxFeeLimit,
            deployAddress,
            deployKey,
            blockingStubFull);
  //    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    endNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance2 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance2 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance2 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();
    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;

    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info(
        "====== witness02Allowance1 :"
            + witness02Allowance1
            + "   witness02Allowance2 :"
            + witness02Allowance2
            + "increase :"
            + witness02Increase);
    logger.info(
        "====== witness01Allowance1 :"
            + witness01Allowance1
            + "  witness01Allowance2 :"
            + witness01Allowance2
            + "  increase :"
            + witness01Increase);
    Optional<Protocol.TransactionInfo> infoById =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("InfoById:" + infoById);
    Map<String, Long> witnessAllowance =
        PublicMethod.getAllowance2(startNum, endNum, blockingStubFull);
    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress)) - witness01Increase))
            <= 2);
    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress2)) - witness02Increase))
            <= 2);
    Assert.assertEquals(blackHoleBalance1, blackHoleBalance2);
    afterBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();
    Assert.assertEquals(beforeBurnTrxAmount, afterBurnTrxAmount);
  }

  /** constructor. */
  @Test(enabled = true, priority=2, description = "Test create trc10 token with fee not to sr", groups = {"daily", "serial"})
  public void test06CreateAssetIssue() {
    // get account
    ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] tokenAccountAddress = ecKey1.getAddress();
  final String tokenAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    PublicMethod.printAddress(tokenAccountKey);

    Assert.assertTrue(
        PublicMethod.sendcoin(
            tokenAccountAddress, 1028000000L, foundationAddress, foundationKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    startNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance1 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance1 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance1 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();
    beforeBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();
  Long start = System.currentTimeMillis() + 2000;
  Long end = System.currentTimeMillis() + 1000000000;
    long now = System.currentTimeMillis();
    long totalSupply = now;
  String description = "for case assetissue016";
  String url = "https://stest.assetissue016.url";
  String name = "AssetIssue016_" + Long.toString(now);
    txid =
        PublicMethod.createAssetIssueGetTxid(
            tokenAccountAddress,
            name,
            name,
            totalSupply,
            1,
            1,
            start,
            end,
            1,
            description,
            url,
            0L,
            0L,
            1L,
            1L,
            tokenAccountKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    endNum =
        blockingStubFull
            .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader()
            .getRawData()
            .getNumber();
    witness01Allowance2 =
        PublicMethod.queryAccount(witnessAddress, blockingStubFull).getAllowance();
    witness02Allowance2 =
        PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getAllowance();
    blackHoleBalance2 =
        PublicMethod.queryAccount(WalletClient.decode58Check(blackHoleAdd), blockingStubFull)
            .getBalance();

    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;
    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info(
        "====== witness01Allowance1 :"
            + witness01Allowance1
            + "  witness01Allowance2 :"
            + witness01Allowance2
            + "  increase :"
            + witness01Increase);
    logger.info(
        "====== witness02Allowance1 :"
            + witness02Allowance1
            + "  witness02Allowance2 :"
            + witness02Allowance2
            + "  increase :"
            + witness02Increase);

    Map<String, Long> witnessAllowance =
        PublicMethod.getAllowance2(startNum, endNum, blockingStubFull);
    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress)) - witness01Increase))
            <= 2);
    Assert.assertTrue(
        (Math.abs(
                witnessAllowance.get(ByteArray.toHexString(witnessAddress2)) - witness02Increase))
            <= 2);
    Assert.assertEquals(blackHoleBalance1, blackHoleBalance2);
    Optional<Protocol.TransactionInfo> infoById =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getPackingFee() == 0L);
    Assert.assertTrue(infoById.get().getFee() == 1024000000L);
    afterBurnTrxAmount = blockingStubFull.getBurnTrx(EmptyMessage.newBuilder().build()).getNum();
    Assert.assertTrue(afterBurnTrxAmount - beforeBurnTrxAmount == 1024000000L);
  }

  @Test(enabled = true, priority=2, description = "commit NO.47 value can be 1e17 if commit No.63 opened", groups = {"daily", "serial"})
  public void test07Commit47Value() {
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(47L, 100000000000000000L);
    org.testng.Assert.assertTrue(PublicMethod.createProposal(witnessAddress, witnessKey,
        proposalMap, blockingStubFull));
  }

  /** constructor. */
  @Test(enabled = true, priority=2, description = " create and vote witness, "
      + "after this case there will be 3 SR", groups = {"daily", "serial"})
  public void test08CreateAndVoteWitness() {
    int beforeCreateWitnessCount = PublicMethod.listWitnesses(blockingStubFull)
        .get().getWitnessesCount();
    Assert.assertEquals(2, beforeCreateWitnessCount);
    Assert.assertTrue(PublicMethod
        .sendcoin(witnessAddress3, costForCreateWitness + 100000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    org.testng.Assert.assertTrue(PublicMethod.freezeBalanceGetTronPower(witnessAddress3, 1000000L,
        0, 2, null, witnessKey3, blockingStubFull));
    Assert.assertTrue(createWitness(witnessAddress3, createUrl, witnessKey3));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  int afterCreateWitnessCount = PublicMethod.listWitnesses(blockingStubFull)
        .get().getWitnessesCount();
    Assert.assertEquals(3, afterCreateWitnessCount);
    Assert.assertTrue(PublicMethod.queryAccount(witnessAddress3, blockingStubFull).getIsWitness());

    HashMap<byte[], Long> witnessMap = new HashMap<>();
    witnessMap.put(witnessAddress3, 1L);
    Assert.assertTrue(PublicMethod.voteWitness(witnessAddress3, witnessKey3, witnessMap,
        blockingStubFull));
  }

  /**
   * constructor.
   */
  public Boolean createWitness(byte[] owner, byte[] url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    WitnessContract.WitnessCreateContract.Builder builder = WitnessContract.WitnessCreateContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUrl(ByteString.copyFrom(url));
    WitnessContract.WitnessCreateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createWitness(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    return response.getResult();
  }

  private Protocol.Transaction signTransaction(ECKey ecKey, Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.unFreezeBalance(deployAddress, deployKey, 1, deployAddress, blockingStubFull);
    PublicMethod.freeResource(deployAddress, deployKey, foundationAddress, blockingStubFull);    if (!srStatus) {
      System.exit(1);
    }
  }
}
