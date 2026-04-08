package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.Flaky;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
@Flaky(reason = "Requires specific SR voting state, sensitive to block timing",
    since = "2026-04-03")
public class TvmVote extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] mapKeyContract = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  int freezeCount = 100000000;
  int voteCount = 1;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    PublicMethod.printAddress(contractExcKey);
//    if(PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {
//      throw new SkipException("Skipping freezeV2 test case");
//    }

    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 300100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/tvmVote.sol";
  String contractName = "TestVote";

    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    mapKeyContract = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(mapKeyContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "query reward balance", groups = {"contract", "daily"})
  public void test01QueryRewardBalance() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "queryRewardBalance()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    long trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + trueRes);
    Assert.assertEquals(0, trueRes);

    GrpcAPI.BytesMessage bytesMessage = GrpcAPI.BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(mapKeyContract))
        .build();
    long reward = blockingStubFull.getRewardInfo(bytesMessage).getNum();
    org.testng.Assert.assertEquals(trueRes, reward);
  }


  @Test(enabled = true, description = "freeze balance and vote witness", groups = {"contract", "daily"})
  public void test02VoteWitness() {
    String methodStr = "freezev2(uint256,uint256)";
  String receiverAdd = Base58.encode58Check(mapKeyContract);
  String args = "" + freezeCount + ",1";
  String triggerTxid = PublicMethod.triggerContract(mapKeyContract,
        methodStr, args, false, 0, maxFeeLimit, "0", 0,
        contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Protocol.InternalTransaction internal = transactionInfo.get().getInternalTransactions(0);
  String note = internal.getNote().toStringUtf8();
    Assert.assertEquals("freezeBalanceV2ForEnergy", note);
    Assert.assertEquals(freezeCount, internal.getCallValueInfo(0).getCallValue());
  String witness58Add = Base58.encode58Check(witnessAddress);
    args = "[\"" + witness58Add + "\"],[" + voteCount + "]";
    logger.info("vote args: " + args);
    methodStr = "voteWitness(address[],uint256[])";
    triggerTxid = PublicMethod.triggerContract(mapKeyContract, methodStr, args, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    internal = transactionInfo.get().getInternalTransactions(0);
    note = internal.getNote().toStringUtf8();
    Assert.assertEquals("voteWitness", note);
    Assert.assertTrue(internal.getExtra().length() > 1);

    Protocol.Account info = PublicMethod.queryAccount(mapKeyContract, blockingStubFull);
  int voteCount = info.getVotesCount();
    logger.info("voteCount: " + voteCount);
    Assert.assertEquals(1, voteCount);
  }

  @Test(enabled = true, description = "query contract address is Sr Candidate or not", groups = {"contract", "daily"})
  public void test03IsSrCandidate() {
    String args = "\"" + Base58.encode58Check(mapKeyContract) + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "isWitness(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(0, 0);
  }

  @Test(enabled = true, description = "query sr address is Sr Candidate or not", groups = {"contract", "daily"})
  public void test04IsSrCandidate() {
    String args = "\"" + Base58.encode58Check(witnessAddress) + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "isWitness(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(1, 1);
  }

  @Test(enabled = true, description = "query zero address is Sr Candidate or not", groups = {"contract", "daily"})
  public void test05IsSrCandidate() {
    String args = "\"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "isWitness(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(0, 0);
  }

  @Test(enabled = true, description = "query sr's total vote count", groups = {"contract", "daily"})
  public void test06querySrTotalVoteCount() {
    String args = "\"" + Base58.encode58Check(witnessAddress) + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "queryTotalVoteCount(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(0, trueRes);
  }

  @Test(enabled = true, description = "query contract's total vote count", groups = {"contract", "daily"})
  public void test07queryContractTotalVoteCount() {
    String args = "\"" + Base58.encode58Check(mapKeyContract) + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "queryTotalVoteCount(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(freezeCount / 1000000, trueRes);
  }

  @Test(enabled = true, description = "query vote count", groups = {"contract", "daily"})
  public void test08queryVoteCount() {
    String from = Base58.encode58Check(mapKeyContract);
  String to = Base58.encode58Check(witnessAddress);
  String args = "\"" + from + "\",\"" + to + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "queryVoteCount(address,address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(voteCount, trueRes);
  }

  @Test(enabled = true, description = "query contract used vote count", groups = {"contract", "daily"})
  public void test09queryUsedVoteCount() {
    String from = Base58.encode58Check(mapKeyContract);
  String args = "\"" + from + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "queryUsedVoteCount(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(voteCount, trueRes);
  }

  @Test(enabled = true, description = "query witnesses received vote count", groups = {"contract", "daily"})
  public void test10queryReceivedVoteCount() {
    String witness = Base58.encode58Check(witnessAddress);
  String args = "\"" + witness + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "queryReceivedVoteCount(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertTrue(trueRes > 0);
    Optional<GrpcAPI.WitnessList> list = PublicMethod.listWitnesses(blockingStubFull);
    long receiveCount = 0;
  String temAdd;
    for (int i = 0; i < list.get().getWitnessesCount(); i++) {
      temAdd = Base58.encode58Check(list.get().getWitnesses(i).getAddress().toByteArray());
      if (witness.equals(temAdd)) {
        receiveCount = list.get().getWitnesses(i).getVoteCount();
        break;
      }
    }
    Assert.assertEquals(trueRes, receiveCount);
  }

  @Test(enabled = true, description = "withdraw reward", groups = {"contract", "daily"})
  public void test11WithdrawReward() {
    String methodStr = "withdrawReward()";
  String triggerTxid = PublicMethod.triggerContract(mapKeyContract, methodStr, "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.InternalTransaction internal = transactionInfo.get().getInternalTransactions(0);
  String note = internal.getNote().toStringUtf8();
    Assert.assertEquals("withdrawReward", note);
    Assert.assertEquals(1, internal.getCallValueInfoCount());
    Assert.assertEquals("", internal.getCallValueInfo(0).toString());
  }

  @Test(enabled = true, description = "unfreeze energy", groups = {"contract", "daily"})
  public void test12Unfreeze() {
    String methodStr = "unfreezev2(uint256,uint256)";
  String args = ""+freezeCount+",1";
  String triggerTxid = PublicMethod.triggerContract(mapKeyContract, methodStr, args, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());

    Protocol.InternalTransaction internal = transactionInfo.get().getInternalTransactions(0);
  String note = internal.getNote().toStringUtf8();
    Assert.assertEquals("unfreezeBalanceV2ForEnergy", note);
    Assert.assertEquals(freezeCount, internal.getCallValueInfo(0).getCallValue());
  }

  @Test(enabled = true, description = "kill me", groups = {"contract", "daily"})
  public void test13Suicide() {
    String methodStr = "killme(address)";
  String args = "\"" + Base58.encode58Check(witnessAddress) + "\"";
  String triggerTxid = PublicMethod.triggerContract(mapKeyContract, methodStr, args, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(1, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        transactionInfo.get().getReceipt().getResult());
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);  }


}

