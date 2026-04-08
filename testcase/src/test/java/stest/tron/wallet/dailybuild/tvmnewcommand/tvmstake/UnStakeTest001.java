package stest.tron.wallet.dailybuild.tvmnewcommand.tvmstake;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class UnStakeTest001 extends TronBaseTest {

  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] testAddress002 = ecKey2.getAddress();
  private String testKey002 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private byte[] contractAddress;

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    PublicMethod.printAddress(testKey001);  }

  @Test(enabled = false, description = "unstake normal", groups = {"contract", "daily"})
  public void tvmStakeTest001Normal() {
    PublicMethod
        .sendcoin(testAddress001, 1120_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/unStake001.sol";
  String contractName = "unStakeTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 1000000000L, 100, null,
            testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(witnessAddress4) + "\"," + 10000000;
    long balanceBefore = PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance();
  String txid = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 1);
    Account account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
    long balanceAfter = account.getBalance();
    Assert.assertEquals(balanceAfter, balanceBefore - 10000000);
    long frozenBalance = account.getFrozen(0).getFrozenBalance();
  byte[] voteAddress = account.getVotes(0).getVoteAddress().toByteArray();
    long voteCount = account.getVotes(0).getVoteCount();
    Assert.assertEquals(voteCount, 10);
    Assert.assertEquals(voteAddress, witnessAddress4);
    Assert.assertEquals(frozenBalance, 10000000);

    methodStr = "unStake()";
    txid = PublicMethod
        .triggerContract(contractAddress, methodStr, "#", false, 0, maxFeeLimit, testAddress001,
            testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 1);
    account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
  int frozenCount = account.getFrozenCount();
  int votesCount = account.getVotesCount();
    Assert.assertEquals(0, frozenCount);
    Assert.assertEquals(0, votesCount);
    Assert.assertEquals(account.getBalance(), balanceBefore);
  }

  @Test(enabled = false, description = "unstake when no stake", groups = {"contract", "daily"})
  public void tvmUnstakeTest002NoStake() {
    PublicMethod
        .sendcoin(testAddress001, 1120_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/unStake001.sol";
  String contractName = "unStakeTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 1000000000L, 100, null,
            testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methodStr = "unStake()";
    long balanceBefore = PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance();
  String txid = PublicMethod
        .triggerContract(contractAddress, methodStr, "", false, 0, maxFeeLimit, testAddress001,
            testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    Account account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
    Assert.assertEquals(account.getBalance(), balanceBefore);
  int contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 0);
  int frozenCount = account.getFrozenCount();
  int votesCount = account.getVotesCount();
    Assert.assertEquals(0, frozenCount);
    Assert.assertEquals(0, votesCount);
  }

  @Test(enabled = false, description = "unstake twice", groups = {"contract", "daily"})
  public void tvmUnstakeTest003UnstakeTwice() {
    PublicMethod
        .sendcoin(testAddress001, 1120_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/unStake001.sol";
  String contractName = "unStakeTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 1000000000L, 100, null,
            testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(witnessAddress4) + "\"," + 10000000;
    long balanceBefore = PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance();
  String txid = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 1);
    Account account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
    long balanceAfter = account.getBalance();
    Assert.assertEquals(balanceAfter, balanceBefore - 10000000);
    long frozenBalance = account.getFrozen(0).getFrozenBalance();
  byte[] voteAddress = account.getVotes(0).getVoteAddress().toByteArray();
    long voteCount = account.getVotes(0).getVoteCount();
    Assert.assertEquals(voteCount, 10);
    Assert.assertEquals(voteAddress, witnessAddress4);
    Assert.assertEquals(frozenBalance, 10000000);

    methodStr = "unStake2()";
    txid = PublicMethod
        .triggerContract(contractAddress, methodStr, "", false, 0, maxFeeLimit, testAddress001,
            testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 0);
    account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
  int frozenCount = account.getFrozenCount();
  int votesCount = account.getVotesCount();
    Assert.assertEquals(0, frozenCount);
    Assert.assertEquals(0, votesCount);
    Assert.assertEquals(account.getBalance(), balanceBefore);
  }

  @Test(enabled = false, description = "unstake revert", groups = {"contract", "daily"})
  public void tvmUnstakeTest004Revert() {
    PublicMethod
        .sendcoin(testAddress001, 1120_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/unStake001.sol";
  String contractName = "unStakeTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 1000000000L, 100, null,
            testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(witnessAddress4) + "\"," + 10000000;
    long balanceBefore = PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance();
  String txid = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 1);
    Account account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
    long balanceAfter = account.getBalance();
    Assert.assertEquals(balanceAfter, balanceBefore - 10000000);
    long frozenBalance = account.getFrozen(0).getFrozenBalance();
  byte[] voteAddress = account.getVotes(0).getVoteAddress().toByteArray();
    long voteCount = account.getVotes(0).getVoteCount();
    Assert.assertEquals(voteCount, 10);
    Assert.assertEquals(voteAddress, witnessAddress4);
    Assert.assertEquals(frozenBalance, 10000000);

    methodStr = "revertTest2(address)";
    argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"";
    txid = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr, false, 0, maxFeeLimit, testAddress001,
            testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 0);
    account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
  int frozenCount = account.getFrozenCount();
  int votesCount = account.getVotesCount();
    Assert.assertEquals(0, frozenCount);
    Assert.assertEquals(0, votesCount);
    Assert.assertEquals(account.getBalance(), 993000000L);
    long balance = PublicMethod.queryAccount(testAddress002, blockingStubFull).getBalance();
    Assert.assertEquals(7000000L, balance);
  }

  @Test(enabled = false, description = "unstake call another contract in one contract", groups = {"contract", "daily"})
  public void tvmUnstakeTest005CallAnotherInOneContract() {
    PublicMethod
        .sendcoin(testAddress001, 2120_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/unStake001.sol";
  String contractName = "unStakeTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 1000000000L, 100, null,
            testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long callvalue = 1000000000L;
  String txid = PublicMethod.triggerContract(contractAddress, "deployB()", "#", false,
        callvalue, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
  String addressHex =
        "41" + ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())
            .substring(24);
    logger.info("address_hex: " + addressHex);
  byte[] contractAddressB = ByteArray.fromHexString(addressHex);
    logger.info("contractAddressB: " + Base58.encode58Check(contractAddressB));
    long contractAddressBBalance = PublicMethod.queryAccount(contractAddressB, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBBalance);
  String methodStr = "BStake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(witnessAddress4) + "\"," + 10000000;
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
    Assert.assertEquals(voteAddress, witnessAddress4);
    Assert.assertEquals(frozenBalance, 10000000);
    Assert.assertEquals(balanceAfter, contractAddressBBalance - 10000000);
    long contractAddressBalance = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(contractAddressBalance, 1000000000);

    methodStr = "BUnStake()";
    txid = PublicMethod
        .triggerContract(contractAddress, methodStr, "", false, 0, maxFeeLimit, testAddress001,
            testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 1);
    account = PublicMethod.queryAccount(contractAddressB, blockingStubFull);
  int frozenCount = account.getFrozenCount();
  int votesCount = account.getVotesCount();
    Assert.assertEquals(0, frozenCount);
    Assert.assertEquals(0, votesCount);
    Assert.assertEquals(account.getBalance(), contractAddressBBalance);
    contractAddressBalance = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(contractAddressBalance, 1000000000);
  }

  @Test(enabled = false, description = "unstake with reward balance", groups = {"contract", "daily"})
  public void tvmUnstakeTest006WithRewardBalance() {
    PublicMethod
        .sendcoin(testAddress001, 1120_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/unStake001.sol";
  String contractName = "unStakeTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 1000000000L, 100, null,
            testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methodStr = "Stake(address,uint256)";
  String argsStr = "\"" + Base58.encode58Check(witnessAddress4) + "\"," + 10000000;
    long balanceBefore = PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance();
  String txid = PublicMethod
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 1);
    Account account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
    long balanceAfter = account.getBalance();
    Assert.assertEquals(balanceAfter, balanceBefore - 10000000);
    long frozenBalance = account.getFrozen(0).getFrozenBalance();
  byte[] voteAddress = account.getVotes(0).getVoteAddress().toByteArray();
    long voteCount = account.getVotes(0).getVoteCount();
    Assert.assertEquals(voteCount, 10);
    Assert.assertEquals(voteAddress, witnessAddress4);
    Assert.assertEquals(frozenBalance, 10000000);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    methodStr = "rewardBalance(address)";
    argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"";
    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr, false, 0, 0, "0",
            0, testAddress001, testKey001, blockingStubFull);
    Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));
    org.junit.Assert.assertEquals(0, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));

    methodStr = "withdrawReward()";
    txid = PublicMethod
        .triggerContract(contractAddress, methodStr, "", false, 0, maxFeeLimit, testAddress001,
            testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 0);
    account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
    long balanceAfter2 = account.getBalance();
    Assert.assertEquals(balanceAfter, balanceAfter2);

    methodStr = "unStake2()";
    txid = PublicMethod
        .triggerContract(contractAddress, methodStr, "", false, 0, maxFeeLimit, testAddress001,
            testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 0);
    account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
  int frozenCount = account.getFrozenCount();
  int votesCount = account.getVotesCount();
    Assert.assertEquals(0, frozenCount);
    Assert.assertEquals(0, votesCount);
    Assert.assertEquals(account.getBalance(), balanceBefore);

    methodStr = "rewardBalance(address)";
    argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"";
    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr, false, 0, 0, "0",
            0, testAddress001, testKey001, blockingStubFull);
    transaction = transactionExtention.getTransaction();
    result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));
    org.junit.Assert.assertEquals(0, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(testAddress001, testKey001, testFoundationAddress, blockingStubFull);  }

}
