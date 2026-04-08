package stest.tron.wallet.dailybuild.operationupdate;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class MultiSignSmartContractTest extends TronBaseTest {  private final String operations = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.operations");
  ArrayList<String> txidList = new ArrayList<String>();
  Optional<TransactionInfo> infoById = null;
  Long beforeTime;
  Long afterTime;
  Long beforeBlockNum;
  Long afterBlockNum;
  Block currentBlock;
  Long currentBlockNum;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey3.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, groups = {"contract", "daily"})
  public void testMultiSignForSmartContract() {
    ecKey1 = new ECKey(Utils.getRandom());
    manager1Address = ecKey1.getAddress();
    manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    manager2Address = ecKey2.getAddress();
    manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    ecKey3 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey3.getAddress();
    ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethod.printAddress(ownerKey);

    long needcoin = updateAccountPermissionFee + multiSignFee * 4;

    Assert.assertTrue(
        PublicMethod.sendcoin(ownerAddress, needcoin + 100000000L, foundationAddress, foundationKey,
            blockingStubFull));
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(foundationAddress, 1000000000 + PublicMethod.randomFreezeAmount.getAndAdd(1), 0, 0, ByteString.copyFrom(ownerAddress),
            foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(foundationAddress, 1000000000 + PublicMethod.randomFreezeAmount.getAndAdd(1), 0, 1, ByteString.copyFrom(ownerAddress),
            foundationKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull, ownerKeyString));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Random rand = new Random();
    Integer randNum = rand.nextInt(30) + 1;
    randNum = rand.nextInt(4000);
  Long maxFeeLimit = 1000000000L;
  String contractName = "StorageAndCpu" + Integer.toString(randNum);
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestStorageAndCpu_storageAndCpu");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestStorageAndCpu_storageAndCpu");
  String txid = PublicMethodForMultiSign.deployContractAndGetTransactionInfoById(contractName, abi, code,
            "", maxFeeLimit, 0L, 100, null, ownerKey, ownerAddress,  0, ownerKeyString, blockingStubFull);
    Assert.assertNotEquals(txid, null);
    PublicMethod.waitUntilTransactionInfoFound(blockingStubFull, txid, 30);

    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getBlockTimeStamp() > 0);
  byte[] contractAddress = infoById.get().getContractAddress().toByteArray();

    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
  String abiStr = smartContract.getAbi().toString();
    System.out.println("abiStr:    " + abiStr);
    Assert.assertTrue(abiStr != null && abiStr.length() > 0);
  String initParmes = "\"" + "930" + "\"";
    txid = PublicMethodForMultiSign.triggerContract1(contractAddress,
        "testUseCpu(uint256)", initParmes, false,
        0, maxFeeLimit, ownerAddress, ownerKey, blockingStubFull, 0, ownerKeyString);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.getTransactionById(txid, blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(
        PublicMethodForMultiSign.updateSettingWithPermissionId(contractAddress, 50, ownerKey,
            ownerAddress, 0, blockingStubFull, ownerKeyString));
    Assert.assertTrue(
        PublicMethodForMultiSign.updateEnergyLimitWithPermissionId(contractAddress, 50, ownerKey,
            ownerAddress, 0, blockingStubFull, ownerKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}