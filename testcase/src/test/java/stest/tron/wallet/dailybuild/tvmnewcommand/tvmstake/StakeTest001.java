package stest.tron.wallet.dailybuild.tvmnewcommand.tvmstake;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class StakeTest001 extends TronBaseTest {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);
  private byte[] testWitnessAddress = PublicMethod.getFinalAddress(witnessKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    PublicMethod.printAddress(testKey001);    PublicMethod
        .sendcoin(testAddress001, 1000_000_00000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/testStakeSuicide.sol";
  String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000_000_0000L, 100, null, testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "Vote for witness", groups = {"contract", "daily"})
  void tvmStakeTest001() {
    long balanceBefore = PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance();
  String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 1000000;
  String txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =  PublicMethod.getTransactionInfoById(txid,blockingStubFull);
  int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,1);

    Account request = Account.newBuilder().setAddress(ByteString.copyFrom(contractAddress)).build();
    long balanceAfter = PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance();
    Assert.assertEquals(balanceAfter,balanceBefore - 1000000);
  byte[] voteAddress = (blockingStubFull.getAccount(request).getVotesList().get(0)
        .getVoteAddress().toByteArray());
    Assert.assertEquals(testWitnessAddress,voteAddress);
    Assert.assertEquals(1,blockingStubFull.getAccount(request).getVotes(0).getVoteCount());

  }

  @Test(enabled = false, description = "Non-witness account", groups = {"contract", "daily"})
  void tvmStakeTest002() {
    //account address
    String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testAddress001) + "\","  + 1000000;
  String txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =  PublicMethod.getTransactionInfoById(txid,blockingStubFull);
  int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,0);
  //contract address
    methodStr = "Stake(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(contractAddress) + "\","  + 1000000;
    txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    info =  PublicMethod.getTransactionInfoById(txid,blockingStubFull);
    contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,0);

  }

  @Test(enabled = false, description = "Number of votes over balance", groups = {"contract", "daily"})
  void tvmStakeTest003() {
    String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + Long.MAX_VALUE;
  String txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =  PublicMethod.getTransactionInfoById(txid,blockingStubFull);
  int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());

    Assert.assertEquals(contractResult,0);

  }

  @Test(enabled = false, description = "Enough votes for a second ballot", groups = {"contract", "daily"})
  void tvmStakeTest004() {

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 21000000;
  String txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =  PublicMethod.getTransactionInfoById(txid,blockingStubFull);
  int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,1);
    Account request = Account.newBuilder().setAddress(ByteString.copyFrom(contractAddress)).build();
  byte[] voteAddress = (blockingStubFull.getAccount(request).getVotesList().get(0)
        .getVoteAddress().toByteArray());
    Assert.assertEquals(testWitnessAddress,voteAddress);
    System.out.println(blockingStubFull.getAccount(request).getVotesCount());
    Assert.assertEquals(21,blockingStubFull.getAccount(request).getVotes(0).getVoteCount());

    argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 11000000;
    txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    info =  PublicMethod.getTransactionInfoById(txid,blockingStubFull);
    contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,1);
    request = Account.newBuilder().setAddress(ByteString.copyFrom(contractAddress)).build();
    voteAddress = (blockingStubFull.getAccount(request).getVotesList().get(0).getVoteAddress()
        .toByteArray());
    Assert.assertEquals(testWitnessAddress,voteAddress);
    Assert.assertEquals(11,blockingStubFull.getAccount(request).getVotes(0).getVoteCount());

  }

  @Test(enabled = false, description = "Revert test", groups = {"contract", "daily"})
  void tvmStakeTest005() {

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methodStr = "revertTest1(address,uint256,address)";
  String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 1000000 + ",\""
        + Base58.encode58Check(testAddress001) + "\"";
  String txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =  PublicMethod.getTransactionInfoById(txid,blockingStubFull);
  int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());

    Assert.assertEquals(contractResult,0);

  }

  @Test(enabled = false, description = "Contract Call Contract stake", groups = {"contract", "daily"})
  void tvmStakeTest006() {
    String methodStr = "deployB()";
  String argsStr = "";
  String txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("txid:" + txid);

    methodStr = "BStake(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 1000000;
    txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long callvalue = 1000000000L;
    txid = PublicMethod.triggerContract(contractAddress, "deployB()", "#", false,
        callvalue, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
  String addressHex =
        "41" + ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())
            .substring(24);
  byte[] contractAddressB = ByteArray.fromHexString(addressHex);
    long contractAddressBBalance = PublicMethod.queryAccount(contractAddressB, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBBalance);

    methodStr = "BStake(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\"," + 10000000;
    txid = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  int contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 1);
    Account account = PublicMethod.queryAccount(contractAddressB, blockingStubFull);
    long frozenBalance = account.getFrozen(0).getFrozenBalance();
  byte[] voteAddress = account.getVotes(0).getVoteAddress().toByteArray();
    long voteCount = account.getVotes(0).getVoteCount();
    long balanceAfter = account.getBalance();
    Assert.assertEquals(voteCount, 10);
    Assert.assertEquals(voteAddress, testWitnessAddress);
    Assert.assertEquals(frozenBalance, 10000000);
    Assert.assertEquals(balanceAfter, contractAddressBBalance - 10000000);

  }

  @Test(enabled = false, description = "Vote for the first witness and then vote for the second "
      + "witness.", groups = {"contract", "daily"})
  void tvmStakeTest007() {

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 21000000;
  String txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =  PublicMethod.getTransactionInfoById(txid,blockingStubFull);
  int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,1);
    Account request = Account.newBuilder().setAddress(ByteString.copyFrom(contractAddress)).build();
  byte[] voteAddress = (blockingStubFull.getAccount(request).getVotesList().get(0)
        .getVoteAddress().toByteArray());
    Assert.assertEquals(testWitnessAddress,voteAddress);
    System.out.println(blockingStubFull.getAccount(request).getVotesCount());
    Assert.assertEquals(21,blockingStubFull.getAccount(request).getVotes(0).getVoteCount());

    argsStr = "\"" + Base58.encode58Check(witnessAddress3) + "\","  + 11000000;
    txid  = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    info =  PublicMethod.getTransactionInfoById(txid,blockingStubFull);
    contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,1);
    request = Account.newBuilder().setAddress(ByteString.copyFrom(contractAddress)).build();
    voteAddress = (blockingStubFull.getAccount(request).getVotesList().get(0).getVoteAddress()
        .toByteArray());
    Assert.assertEquals(witnessAddress3,voteAddress);
    Assert.assertEquals(11,blockingStubFull.getAccount(request).getVotes(0).getVoteCount());

  }

}

