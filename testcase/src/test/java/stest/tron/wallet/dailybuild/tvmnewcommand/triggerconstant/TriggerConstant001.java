package stest.tron.wallet.dailybuild.tvmnewcommand.triggerconstant;

import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
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
public class TriggerConstant001 extends TronBaseTest {

  private final String testNetAccountKey =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddressNoAbi = null;
  byte[] contractAddressWithAbi = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private ManagedChannel channelRealSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubRealSolidity = null;
  private String fullnode1 =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(1);
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(0);
  private String realSoliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(1);

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(contractExcKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1).usePlaintext().build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);    channelRealSolidity =
        ManagedChannelBuilder.forTarget(realSoliditynode).usePlaintext().build();
    blockingStubRealSolidity = WalletSolidityGrpc.newBlockingStub(channelRealSolidity);

    {
      Assert.assertTrue(
          PublicMethod.sendcoin(
              contractExcAddress,
              10000_000_000L,
              testNetAccountAddress,
              testNetAccountKey,
              blockingStubFull));
      PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/TriggerConstant001.sol";
  String contractName = "testConstantContract";
      HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  final String abi = retMap.get("abI").toString();

      contractAddressNoAbi =
          PublicMethod.deployContract(
              contractName,
              "[]",
              code,
              "",
              maxFeeLimit,
              0L,
              100,
              null,
              contractExcKey,
              contractExcAddress,
              blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      SmartContract smartContract =
          PublicMethod.getContract(contractAddressNoAbi, blockingStubFull);
      Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
      Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
      Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());

      contractAddressWithAbi =
          PublicMethod.deployContract(
              contractName,
              abi,
              code,
              "",
              maxFeeLimit,
              0L,
              100,
              null,
              contractExcKey,
              contractExcAddress,
              blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      SmartContract smartContract2 =
          PublicMethod.getContract(contractAddressWithAbi, blockingStubFull);
      Assert.assertFalse(smartContract2.getAbi().toString().isEmpty());
      Assert.assertTrue(smartContract2.getName().equalsIgnoreCase(contractName));
      Assert.assertFalse(smartContract2.getBytecode().toString().isEmpty());
    }
  }

  @Test(enabled = true, description = "TriggerConstantContract a payable function without ABI", groups = {"contract", "daily"})
  public void test01TriggerConstantContract() {

    String txid = "";

    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtention(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    TransactionExtention transactionExtentionFromSolidity =
        PublicMethod.triggerSolidityContractForExtention(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);
    System.out.println("Code = " + transactionExtentionFromSolidity.getResult().getCode());
    System.out.println(
        "Message = " + transactionExtentionFromSolidity.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a payable function" + " without ABI on solidity", groups = {"contract", "daily"})
  public void test01TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a payable function" + " without ABI on real solidity", groups = {"contract", "daily"})
  public void test01TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a non-payable function" + " without ABI", groups = {"contract", "daily"})
  public void test02TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtention(
            contractAddressNoAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a non-payable function" + " without ABI on solidity", groups = {"contract", "daily"})
  public void test02TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
  }

  @Test(
      enabled = true,
      description =
          "TriggerConstantContract a non-payable function" + " without ABI on real solidity", groups = {"contract", "daily"})
  public void test02TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
  }

  @Test(enabled = true, description = "TriggerConstantContract a view function without ABI", groups = {"contract", "daily"})
  public void test03TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtention(
            contractAddressNoAbi,
            "testView()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a view function" + " without ABI on solidity", groups = {"contract", "daily"})
  public void test03TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testView()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a view function" + " without ABI on real solidity", groups = {"contract", "daily"})
  public void test03TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testView()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerConstantContract a pure function without ABI", groups = {"contract", "daily"})
  public void test04TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtention(
            contractAddressNoAbi,
            "testPure()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a pure function" + " without ABI on solidity", groups = {"contract", "daily"})
  public void test04TriggerConstantContractOnSolidity() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testPure()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a pure function" + " without ABI on real solidity", groups = {"contract", "daily"})
  public void test04TriggerConstantContractOnRealSolidity() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testPure()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerConstantContract a payable function with ABI", groups = {"contract", "daily"})
  public void test05TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtention(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a payable function" + " with ABI on solidity", groups = {"contract", "daily"})
  public void test05TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a payable function" + " with ABI on real solidity", groups = {"contract", "daily"})
  public void test05TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "TriggerConstantContract a non-payable function with ABI", groups = {"contract", "daily"})
  public void test06TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtention(
            contractAddressWithAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a non-payable function" + " with ABI on solidity", groups = {"contract", "daily"})
  public void test06TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a non-payable function" + " with ABI on real solidity", groups = {"contract", "daily"})
  public void test06TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "TriggerConstantContract a view function with ABI", groups = {"contract", "daily"})
  public void test07TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtention(
            contractAddressWithAbi,
            "testView()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a view function" + " with ABI on solidity", groups = {"contract", "daily"})
  public void test07TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a view function" + " with ABI on real solidity", groups = {"contract", "daily"})
  public void test07TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerConstantContract a pure function with ABI", groups = {"contract", "daily"})
  public void test08TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtention(
            contractAddressWithAbi,
            "testPure()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a pure function" + " with ABI on solidity", groups = {"contract", "daily"})
  public void test08TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testPure()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a pure function" + " with ABI on real solidity", groups = {"contract", "daily"})
  public void test08TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testPure()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerContract a payable function without ABI", groups = {"contract", "daily"})
  public void test09TriggerContract() {
    Account info;

    AccountResourceMessage resourceInfo =
        PublicMethod.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String txid = "";

    txid =
        PublicMethod.triggerContract(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter =
        PublicMethod.getAccountResource(contractExcAddress, blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  Long returnnumber =
        ByteArray.toLong(
            ByteArray.fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
  }

  @Test(enabled = true, description = "TriggerContract a non-payable function without ABI", groups = {"contract", "daily"})
  public void test10TriggerContract() {
    Account info;

    AccountResourceMessage resourceInfo =
        PublicMethod.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String txid = "";

    txid =
        PublicMethod.triggerContract(
            contractAddressNoAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter =
        PublicMethod.getAccountResource(contractExcAddress, blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  Long returnnumber =
        ByteArray.toLong(
            ByteArray.fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
  }

  @Test(enabled = true, description = "TriggerContract a view function without ABI", groups = {"contract", "daily"})
  public void test11TriggerContract() {

    Account info;

    AccountResourceMessage resourceInfo =
        PublicMethod.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String txid = "";

    txid =
        PublicMethod.triggerContract(
            contractAddressNoAbi,
            "testView()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter =
        PublicMethod.getAccountResource(contractExcAddress, blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  Long returnnumber =
        ByteArray.toLong(
            ByteArray.fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
  }

  @Test(enabled = true, description = "TriggerContract a pure function without ABI", groups = {"contract", "daily"})
  public void test12TriggerContract() {

    Account info;

    AccountResourceMessage resourceInfo =
        PublicMethod.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String txid = "";

    txid =
        PublicMethod.triggerContract(
            contractAddressNoAbi,
            "testPure()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter =
        PublicMethod.getAccountResource(contractExcAddress, blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  Long returnnumber =
        ByteArray.toLong(
            ByteArray.fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
  }

  @Test(enabled = true, description = "TriggerContract a pure function with ABI", groups = {"contract", "daily"})
  public void test18TriggerContract() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerContractForExtention(
            contractAddressWithAbi,
            "testPure()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerContract a payable function with ABI", groups = {"contract", "daily"})
  public void test19TriggerContract() {

    Account info;

    AccountResourceMessage resourceInfo =
        PublicMethod.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String txid = "";
    txid =
        PublicMethod.triggerContract(
            contractAddressWithAbi,
            "testPayable()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter =
        PublicMethod.getAccountResource(contractExcAddress, blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  Long returnnumber =
        ByteArray.toLong(
            ByteArray.fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
  }

  @Test(enabled = true, description = "TriggerContract a non-payable function with ABI", groups = {"contract", "daily"})
  public void test20TriggerContract() {
    Account info;

    AccountResourceMessage resourceInfo =
        PublicMethod.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String txid = "";
    txid =
        PublicMethod.triggerContract(
            contractAddressNoAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter =
        PublicMethod.getAccountResource(contractExcAddress, blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  Long returnnumber =
        ByteArray.toLong(
            ByteArray.fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
  }

  @Test(enabled = true, description = "TriggerContract a view function with ABI", groups = {"contract", "daily"})
  public void test21TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerContractForExtention(
            contractAddressWithAbi,
            "testView()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerContract a view function with ABI on solidity", groups = {"contract", "daily"})
  public void test21TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerContract a view function with ABI on real solidity", groups = {"contract", "daily"})
  public void test21TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a view method with ABI ,method has " + "revert()", groups = {"contract", "daily"})
  public void test24TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtention(
            contractAddressWithAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description =
          "TriggerConstantContract a view method with ABI ,method has " + "revert() on solidity", groups = {"contract", "daily"})
  public void test24TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description =
          "TriggerConstantContract a view method with ABI ,method has "
              + "revert() on real solidity", groups = {"contract", "daily"})
  public void test24TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description = "TriggerContract a view method with ABI ,method has " + "revert()", groups = {"contract", "daily"})
  public void test25TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerContractForExtention(
            contractAddressWithAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description = "TriggerContract a view method with ABI ,method has " + "revert() on solidity", groups = {"contract", "daily"})
  public void test25TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description =
          "TriggerContract a view method with ABI ,method has " + "revert() on real solidity", groups = {"contract", "daily"})
  public void test25TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a view method without ABI,method has" + "revert()", groups = {"contract", "daily"})
  public void testTriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtention(
            contractAddressNoAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description =
          "TriggerConstantContract a view method without ABI,method has" + "revert() on solidity", groups = {"contract", "daily"})
  public void testTriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description =
          "TriggerConstantContract a view method without ABI,method has"
              + "revert() on real solidity", groups = {"contract", "daily"})
  public void testTriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethod.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }
}
