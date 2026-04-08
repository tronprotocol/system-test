package stest.tron.wallet.dailybuild.operationupdate;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
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
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class MultiSignClearContractAbiTest extends TronBaseTest {  private final String operations = Configuration.getByPath("testng.conf")
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

  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1, groups = {"contract", "daily"})
  public void test1MultiSignForClearContractAbi() {
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
    PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
        blockingStubFull, ownerKeyString);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long maxFeeLimit = 1000000000L;
  String filePath = "./src/test/resources/soliditycode/TriggerConstant004.sol";
  String contractName = "testConstantContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress = PublicMethodForMultiSign.deployContract1(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, ownerKey, ownerAddress, blockingStubFull, 2, permissionKeyString);
    logger.info("address:" + Base58.encode58Check(contractAddress));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertNotEquals(smartContract.getAbi().toString(), "");
    Assert.assertTrue(PublicMethodForMultiSign
        .clearContractAbi(contractAddress, ownerAddress, ownerKey,
            blockingStubFull, 2, permissionKeyString));


  }

  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1, groups = {"contract", "daily"})
  public void test2MultiSignForClearContractAbiForDefault() {
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
        .freezeBalanceForReceiver(foundationAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(foundationAddress, 1000000000, 0, 1, ByteString.copyFrom(ownerAddress),
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
    PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
        blockingStubFull, ownerKeyString);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long maxFeeLimit = 1000000000L;
  String filePath = "./src/test/resources/soliditycode/TriggerConstant004.sol";
  String contractName = "testConstantContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress = PublicMethodForMultiSign.deployContract1(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, ownerKey, ownerAddress, blockingStubFull, 2, permissionKeyString);
    logger.info("address:" + Base58.encode58Check(contractAddress));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString() != null &&
        !("".equalsIgnoreCase(smartContract.getAbi().toString())));
    Assert.assertTrue(PublicMethodForMultiSign
        .clearContractAbi(contractAddress, ownerAddress, ownerKey,
            blockingStubFull, 2, permissionKeyString));

  }


  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1, groups = {"contract", "daily"})
  public void test3MultiSignForClearContractAbiForDefault() {
    ecKey3 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey3.getAddress();
    ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethod.printAddress(ownerKey);

    long needcoin = updateAccountPermissionFee + multiSignFee * 4;

    Assert.assertTrue(
        PublicMethod.sendcoin(ownerAddress, needcoin + 100000000L, foundationAddress, foundationKey,
            blockingStubFull));
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(foundationAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(foundationAddress, 1000000000, 0, 1, ByteString.copyFrom(ownerAddress),
            foundationKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    String[] activeDefaultKeyString = new String[1];

    activeDefaultKeyString[0] = ownerKey;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long maxFeeLimit = 1000000000L;
  String filePath = "./src/test/resources/soliditycode/TriggerConstant004.sol";
  String contractName = "testConstantContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress = PublicMethodForMultiSign.deployContract1(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, ownerKey, ownerAddress, blockingStubFull, 2, activeDefaultKeyString);
    logger.info("address:" + Base58.encode58Check(contractAddress));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString() != null);
    Assert.assertTrue(PublicMethodForMultiSign
        .clearContractAbi(contractAddress, ownerAddress, ownerKey,
            blockingStubFull, 2, activeDefaultKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
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