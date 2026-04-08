package stest.tron.wallet.dailybuild.tvmnewcommand.istanbul;

import java.util.HashMap;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BlockExtention;
import org.tron.api.GrpcAPI.TransactionExtention;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
public class ChainidAndSelfBalance001 extends TronBaseTest {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey2.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private byte[] contractAddress;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(testKey001);    PublicMethod
        .sendcoin(testAddress001, 1000_000_000L, testFoundationAddress, testFoundationKey,
        blockingStubFull);
    PublicMethod
        .sendcoin(testAddress002, 1_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/chainid001.sol";
  String contractName = "IstanbulTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
      .deployContract(contractName, abi, code, "", maxFeeLimit, 123456789L, 100, null, testKey001,
        testAddress001, blockingStubFull);
  }

  @Test(enabled = true, description = "chainId should be block zero`s Hash", groups = {"contract", "daily"})
  public void chainidTest001() {
    String methodStr = "getId()";
    TransactionExtention returns = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, "#",
        false, 0, maxFeeLimit, "0", 0,  testAddress001, testKey001, blockingStubFull);
  String chainIdHex = ByteArray.toHexString(returns.getConstantResult(0).toByteArray());

    BlockExtention blockZero = PublicMethod.getBlock2(0, blockingStubFull);
  String tem = ByteArray.toHexString(blockZero.getBlockid().toByteArray()).substring(56);
  String blockZeroId = "00000000000000000000000000000000000000000000000000000000" + tem;

    Assert.assertEquals(chainIdHex, blockZeroId);
  }

  /*
   * New command selfBalance for solidity compiler,
   * optimize address.balance when contract`s balance
   */

  @Test(enabled = true, description = "selfBalance of addres(this).balance", groups = {"contract", "daily"})
  public void getBalanceTest001() {

    String methodStr = "getBalance()";
  String argsStr = "";
    TransactionExtention returns = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
  Long getBalance = ByteArray.toLong(returns.getConstantResult(0).toByteArray());
  Long contractBalance = PublicMethod
        .queryAccount(contractAddress, blockingStubFull).getBalance();

    Assert.assertEquals(contractBalance, getBalance);

  }


  @Test(enabled = true, description = "selfBalance of contractAddress", groups = {"contract", "daily"})
  public void getBalanceTest002() {

    String methodStr = "getBalance(address)";
  String argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"";
    TransactionExtention returns = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
  Long getBalance = ByteArray.toLong(returns.getConstantResult(0).toByteArray());
  Long contractBalance = PublicMethod
        .queryAccount(contractAddress, blockingStubFull).getBalance();

    Assert.assertEquals(contractBalance, getBalance);

  }

  @Test(enabled = true, description = "selfBalance of normal Address", groups = {"contract", "daily"})
  public void getBalanceTest003() {
    String methodStr = "getBalance(address)";
  String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"";
    TransactionExtention returns = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
  Long getBalance = ByteArray.toLong(returns.getConstantResult(0).toByteArray());
  Long accountBalance = PublicMethod
        .queryAccount(testAddress002, blockingStubFull).getBalance();

    Assert.assertEquals(accountBalance, getBalance);

  }

  @Test(enabled = true, description = "selfBalance of unActive Address", groups = {"contract", "daily"})
  public void getBalanceTest004() {
    String methodStr = "getBalance(address)";
  byte[] unActiveAddress = new ECKey(Utils.getRandom()).getAddress();
  String argsStr = "\"" + Base58.encode58Check(unActiveAddress) + "\"";
    TransactionExtention returns = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
  Long getBalance = ByteArray.toLong(returns.getConstantResult(0).toByteArray());

    Assert.assertEquals(0, getBalance.longValue());

  }


}
