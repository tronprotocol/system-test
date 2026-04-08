package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class AddressChange extends TronBaseTest {
  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  byte[] contractAddressOld = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(contractExcKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    PublicMethod
        .sendcoin(contractExcAddress, 1000_000_000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/getAddressChange.sol";
  String contractName = "getAddressChange";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_getAddressChange");
    code = Configuration.getByPath("testng.conf")
        .getString("code.code_getAddressChange");
    contractName = "getAddressChangeOldVersion";
    contractAddressOld = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "get external function address", groups = {"contract", "daily"})
  public void test01GetExternalAddress() {
    String txid = "";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "testaddress1()", "#", false, 0, 0,
            "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
  byte[] b1 = new byte[21];
    b1[0] = 0x41;
    System.arraycopy(result, 12, b1, 1, 20);
    Assert.assertEquals(Base58.encode58Check(contractAddress), Base58.encode58Check(b1));
  }

  @Test(enabled = true, description = "get external function address, solidity version < 0.6.0", groups = {"contract", "daily"})
  public void test02GetExternalAddressOldVersion() {
    String txid = "";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddressOld, "testaddress1()", "#", false, 0, 0,
            "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
  byte[] b1 = new byte[21];
    b1[0] = 0x41;
    System.arraycopy(result, 12, b1, 1, 20);
    Assert.assertEquals(Base58.encode58Check(contractAddressOld), Base58.encode58Check(b1));
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod
        .freeResource(contractAddress, contractExcKey, testNetAccountAddress, blockingStubFull);
    PublicMethod
        .freeResource(contractAddressOld, contractExcKey, testNetAccountAddress, blockingStubFull);    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }  }
}
