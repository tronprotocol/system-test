package stest.tron.wallet.dailybuild.tvmnewcommand.tvmstake;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.code;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class ContractRewardTest001 extends TronBaseTest {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);
  private String witnessAddress = PublicMethod.getAddressString(witnessKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;
  //= Base58.decode58Check("TQYK8QPAFtxjmse1dShHWYXEMsF836jxxe");
  //  @BeforeSuite(enabled = false, description = "stake beforeSuite delete")
//  public void beforeSuite() {
//
//    PublicMethod.printAddress(testKey001);
//////
//    PublicMethod
//        .sendcoin(testAddress001, 1000_000_000L, testFoundationAddress, testFoundationKey,
//            blockingStubFull);
//
//    String filePath = "src/test/resources/soliditycode/stackContract001.sol";
//    String contractName = "B";
//    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
//    String code = retMap.get("byteCode").toString();
//    String abi = retMap.get("abI").toString();
//    contractAddress = PublicMethod
//        .deployContract(contractName, abi, code, "", maxFeeLimit, 100_000_000L, 100, null,
//            testFoundationKey, testFoundationAddress, blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
//
//    PublicMethod.triggerContract(contractAddress,"Stake(address,uint256)",
//        "\"" + witnessAddress + "\",10000000",false,0,maxFeeLimit,
//        testFoundationAddress, testFoundationKey,blockingStubFull);
//  }

  @Test(enabled = false,description = "querry SR account, reward should equal to gerRewardInfo", groups = {"contract", "daily"})
  void rewardbalanceTest001() {
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(PublicMethod.getFinalAddress(witnessKey)))
        .build();
    long reward = blockingStubFull.getRewardInfo(bytesMessage).getNum();
  String methedStr = "rewardBalance(address)";
  String argStr = "\"" + witnessAddress + "\"";
    TransactionExtention txen = PublicMethod.triggerConstantContractForExtention(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);
    System.out.println(txen);
    long rewardBalance = ByteArray.toLong(txen.getConstantResult(0).toByteArray());

    Assert.assertEquals(txen.getResult().getCode(), response_code.SUCCESS);
    Assert.assertEquals(reward,rewardBalance);
  }

  @Test(enabled = false,description = "querry 0x00, reward should be 0", groups = {"contract", "daily"})
  void rewardbalanceTest002() {
    String methedStr = "nullAddressTest()";
  String argStr = "";
    TransactionExtention txen = PublicMethod.triggerConstantContractForExtention(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    long rewardBalance = ByteArray.toLong(txen.getConstantResult(0).toByteArray());

    Assert.assertEquals(txen.getResult().getCode(), response_code.SUCCESS);
    Assert.assertEquals(rewardBalance,0);
  }

  @Test(enabled = false,description = "querry UnActive account , reward should be 0", groups = {"contract", "daily"})
  void rewardbalanceTest003() {
    ECKey ecKey2 = new ECKey(Utils.getRandom());
  String key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  String methedStr = "rewardBalance(address)";
  String argStr = "\"" + PublicMethod.getAddressString(key) + "\"";
    TransactionExtention txen = PublicMethod.triggerConstantContractForExtention(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    long rewardBalance = ByteArray.toLong(txen.getConstantResult(0).toByteArray());

    Assert.assertEquals(txen.getResult().getCode(), response_code.SUCCESS);
    Assert.assertEquals(rewardBalance,0);
  }

  @Test(enabled = false,description = "querry contract account,reward should equal to "
      + "gerRewardInfo", groups = {"contract", "daily"})
  void rewardbalanceTest004() {
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(contractAddress))
        .build();
    long reward = blockingStubFull.getRewardInfo(bytesMessage).getNum();
  String methedStr = "rewardBalance(address)";
  String argStr = "\"" + Base58.encode58Check(contractAddress) + "\"";
    TransactionExtention txen = PublicMethod.triggerConstantContractForExtention(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    long rewardBalance = ByteArray.toLong(txen.getConstantResult(0).toByteArray());

    logger.info("rewardBalance: " + rewardBalance);
    logger.info("reward: " + reward);
    Assert.assertEquals(txen.getResult().getCode(), response_code.SUCCESS);
    Assert.assertEquals(rewardBalance,reward);
  }

  @Test(enabled = false,description = "querry ZeroReward account, reward should be 0", groups = {"contract", "daily"})
  void rewardbalanceTest005() {
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(PublicMethod.getFinalAddress(testFoundationKey)))
        .build();
    long reward = blockingStubFull.getRewardInfo(bytesMessage).getNum();
  String methedStr = "rewardBalance(address)";
  String argStr = "\"" + PublicMethod.getAddressString(testFoundationKey) + "\"";
    TransactionExtention txen = PublicMethod.triggerConstantContractForExtention(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    long rewardBalance = ByteArray.toLong(txen.getConstantResult(0).toByteArray());

    Assert.assertEquals(txen.getResult().getCode(), response_code.SUCCESS);
    Assert.assertEquals(reward,rewardBalance,0);
  }

  @Test(enabled = false,description = "withdrawBalance", groups = {"contract", "daily"})
  void withdrawBalanceTest006() {
    //contractAddress = Base58.decode58Check("TBsf2FCSht83CEA8CSZ1ReQTRDByNB7FCe");
  String methedStr = "withdrawRewardTest()";
  String argStr = "";
  String txid = PublicMethod.triggerContract(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo ext = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
  int result = ByteArray.toInt(ext.getContractResult(0).toByteArray());
    Assert.assertEquals(result,0);
    Assert.assertEquals(ext.getResult(), code.SUCESS);
  }

  @Test(enabled = false,description = "withdrawBalance twice", groups = {"contract", "daily"})
  void withdrawBalanceTest007() {
    String methedStr = "withdrawRewardTest()";
  String argStr = "";
  String txid = PublicMethod.triggerContract(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo ext = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
  int result = ByteArray.toInt(ext.getContractResult(0).toByteArray());
    Assert.assertEquals(result,0);
    Assert.assertEquals(ext.getResult(), code.SUCESS);
  }

  @Test(enabled = false,description = "withdrawBalance other contract", groups = {"contract", "daily"})
  void withdrawBalanceTest008() {
    String filePath = "src/test/resources/soliditycode/stackContract001.sol";
  String contractName = "B";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] otherContract = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 100_000_000L, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methedStr = "contractBWithdrawRewardTest(address)";
  String argStr = "\"" + Base58.encode58Check(otherContract) + "\"";
  String txid = PublicMethod.triggerContract(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo ext = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
  int result = ByteArray.toInt(ext.getContractResult(0).toByteArray());
    Assert.assertEquals(result,0);
    Assert.assertEquals(ext.getResult(), TransactionInfo.code.SUCESS);
  }

  @Test(enabled = false,description = "new withdrawBalance constructor", groups = {"contract", "daily"})
  void withdrawBalanceTest009() {
    String methedStr = "createA()";
  String argStr = "";
  String txid = PublicMethod.triggerContract(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo ext = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
  int result = ByteArray.toInt(ext.getLog(0).getData().toByteArray());
    Assert.assertEquals(result,0);
  int result2 = ByteArray.toInt(ext.getLog(1).getData().toByteArray());
    Assert.assertEquals(result2,0);
    Assert.assertEquals(ext.getResult(), code.SUCESS);
  }

}
