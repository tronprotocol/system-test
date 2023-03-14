package stest.tron.wallet.dailybuild.tvmnewcommand.tvmFreeze;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.code;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;
import zmq.socket.pubsub.Pub;


@Slf4j
public class NewFreezeContractTest {

  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethed.getFinalAddress(testFoundationKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;
  String contractAddress58;
  private byte[] contractAddressD;
  private byte[] create2AddBytes;
  private String create2Add58;
  private String create2Add41;



  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey2.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


  long delegatebleNet = 0;
  long delegatebleEnergy = 0;
  Long maxNet = Long.valueOf(0);
  Long maxEnergy = Long.valueOf(0);
  private long frozenAmount = 0L;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKey001);
    PublicMethed.printAddress(testKey002);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    Assert.assertTrue(PublicMethed.sendcoin(testAddress001, 100000_000000L,
        testFoundationAddress, testFoundationKey, blockingStubFull));

    Assert.assertTrue(PublicMethed.sendcoin(testAddress002, 20000_000000L,
        testFoundationAddress, testFoundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/newFreezeContract.sol";
    String contractName = "NewFreezeV2";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 100000000L,
            100, null, testKey001,
            testAddress001, blockingStubFull);
    contractAddress58 = Base58.encode58Check(contractAddress);
    contractName = "D";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractAddressD = PublicMethed
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, testKey001,
            testAddress001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed
        .getContract(contractAddress, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    smartContract = PublicMethed.getContract(contractAddressD, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
  }


  @Test(enabled = true, description = "freezebalancev2 amount cant not be 0,-1,"
      + " or number bigger than balance")
  void test01FreezeAmountException() {
    AccountResourceMessage contractResourceBefore = PublicMethed
        .getAccountResource(contractAddress, blockingStubFull);
    Account contractAccountBefore = PublicMethed.queryAccount(contractAddress, blockingStubFull);
    String methedStr = "freezeBalanceV2(uint256,uint256)";
    String argsStr = "0,0";
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);

    argsStr = "0,1";
    String txid1 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);

    argsStr = "-1,1";
    String txid2 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);

    argsStr = contractAccountBefore.getBalance() + 1 + ",1";
    String txid3 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    Assert.assertEquals(info.getReceipt().getResult(), contractResult.REVERT);
    info = PublicMethed.getTransactionInfoById(txid1, blockingStubFull).get();
    Assert.assertEquals(info.getReceipt().getResult(), contractResult.REVERT);
    info = PublicMethed.getTransactionInfoById(txid2, blockingStubFull).get();
    Assert.assertEquals(info.getReceipt().getResult(), contractResult.REVERT);
    info = PublicMethed.getTransactionInfoById(txid3, blockingStubFull).get();
    Assert.assertEquals(info.getReceipt().getResult(), contractResult.REVERT);

    AccountResourceMessage contractResourceAfter = PublicMethed
        .getAccountResource(contractAddress, blockingStubFull);
    Account contractAccountAfter = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    logger.info("contractResourceBefore.getEnergyLimit : "
        + contractResourceBefore.getEnergyLimit());
    logger.info("contractResourceAfter.getEnergyLimit : "
        + contractResourceAfter.getEnergyLimit());
    Assert.assertTrue(contractResourceBefore.getEnergyLimit()
        == contractResourceAfter.getEnergyLimit());

    Assert.assertEquals(contractAccountBefore.getBalance(),
        contractAccountAfter.getBalance());

  }

  @Test(enabled = true, description = "freeze 5trx v2 net and 5trx v2 energy")
  void test02FreezeContract() {
    frozenAmount = 10000000L;
    long temAmt = frozenAmount + 1000000;
    String methedStr = "freezeBalanceV2(uint256,uint256)";
    String argsStr = temAmt + ",0";
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    argsStr = frozenAmount + ",1";
    String txid1 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());

    info = PublicMethed.getTransactionInfoById(txid1, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());

    HashMap<String, Long> map = getResource(contractAddress58, blockingStubFull);
    Assert.assertEquals(temAmt, map.get("netFrozenBalanceV2").longValue());
    Assert.assertEquals(frozenAmount, map.get("energyFrozenBalanceV2").longValue());
  }

  @Test(enabled = true, description = "unfreeze 1trx to test withdraw expired unfreeze")
  void test03UnfreezeToWithdraw() {
    String methedStr = "unfreezeBalanceV2(uint256,uint256)";
    String argsStr = "1000000,";
    long size = getAvailableUnfreezeV2Size(contractAddress, contractAddress58);
    Assert.assertEquals(32, size);
    long expireUnfreezeBalance = getExpireUnfreezeBalanceV2(contractAddress, contractAddress58);
    Assert.assertEquals(0, expireUnfreezeBalance);
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr + 0,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("unfreeze 1trx info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
  }

  @Test(enabled = true, description = "cannot delegate resource to self")
  void test04CannotDelegateToSelf() {

    String methedStr = "delegateResource(uint256,uint256,address)";
    String argsStr = frozenAmount + ",0,\"" + contractAddress58 + "\"";   //delegate net
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    argsStr = frozenAmount + ",1,\"" + contractAddress58 + "\"";   //delegate energy
    String txid1 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("delegate net to self: " + info.toString());
    Assert.assertEquals(code.FAILED, info.getResult());
    Assert.assertEquals(contractResult.REVERT, info.getReceipt().getResult());

    info = PublicMethed.getTransactionInfoById(txid1, blockingStubFull).get();
    logger.info("delegate energy to self: " + info.toString());
    Assert.assertEquals(code.FAILED, info.getResult());
    Assert.assertEquals(contractResult.REVERT, info.getReceipt().getResult());
    HashMap<String, Long> map = getResource(contractAddress58, blockingStubFull);
    Assert.assertEquals(frozenAmount, map.get("netFrozenBalanceV2").longValue());
    Assert.assertEquals(frozenAmount, map.get("energyFrozenBalanceV2").longValue());


  }

  @Test(enabled = true, description = "cannot delegate resource to contract")
  void test05CannotDelegateToContract() {
    String filePath = "src/test/resources/soliditycode/onlyCreate2Address.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    byte[] newContract = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    String methedStr = "delegateResource(uint256,uint256,address)";
    String argsStr = frozenAmount + ",0,\"" + contractAddress58 + "\"";   //delegate net
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    argsStr = frozenAmount + ",1,\"" + contractAddress58 + "\"";   //delegate energy
    String txid1 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("delegate net to contract: " + info.toString());
    Assert.assertEquals(TransactionInfo.code.FAILED, info.getResult());
    Assert.assertEquals(contractResult.REVERT, info.getReceipt().getResult());

    info = PublicMethed.getTransactionInfoById(txid1, blockingStubFull).get();
    logger.info("delegate energy to contract: " + info.toString());
    Assert.assertEquals(TransactionInfo.code.FAILED, info.getResult());
    Assert.assertEquals(contractResult.REVERT, info.getReceipt().getResult());
    HashMap<String, Long> map = getResource(contractAddress58, blockingStubFull);
    Assert.assertEquals(frozenAmount, map.get("netFrozenBalanceV2").longValue());
    Assert.assertEquals(frozenAmount, map.get("energyFrozenBalanceV2").longValue());

  }

  @Test(enabled = true, description = "delegate all net and energy to normal address")
  void test06DelegateNetAndEnergy() {
    long delegatebleNet = getDelegatableResource(contractAddress58, contractAddress58, 0);
    long delegatebleEnergy = getDelegatableResource(contractAddress58, contractAddress58, 1);
    Assert.assertEquals(frozenAmount, delegatebleNet);
    Assert.assertEquals(frozenAmount, delegatebleEnergy);

    String receiver = Base58.encode58Check(testAddress002);
    String methedStr = "delegateResource(uint256,uint256,address)";
    String argsStr = frozenAmount + ",0,\"" + receiver + "\"";   //delegate net
    logger.info("methedStr: " + methedStr);
    logger.info("argsStr: " + argsStr);
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    argsStr = frozenAmount + ",1,\"" + receiver + "\"";   //delegate energy
    String txid1 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("delegate net info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    info = PublicMethed.getTransactionInfoById(txid1, blockingStubFull).get();
    logger.info("delegate energy info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    delegatebleNet = getDelegatableResource(contractAddress58, contractAddress58, 0);
    delegatebleEnergy = getDelegatableResource(contractAddress58, contractAddress58, 1);
    Assert.assertEquals(0, delegatebleNet);
    Assert.assertEquals(0, delegatebleEnergy);

    getTotalDelegatedResource(contractAddress58, contractAddress58);
    long resourceV2Net = getResourceV2(contractAddress58, receiver, "0", contractAddress);
    long resourceV2Energy = getResourceV2(contractAddress58, receiver, "1", contractAddress);
    logger.info("resourceV2Net: " + resourceV2Net);
    logger.info("resourceV2Energy: " + resourceV2Energy);
    Assert.assertEquals(frozenAmount, resourceV2Net);
    Assert.assertEquals(frozenAmount, resourceV2Energy);
    getTotalResource(contractAddress58, contractAddress58);
    getTotalResource(receiver, contractAddress58);
    getTotalAcquiredResource(contractAddress, receiver);

  }

  @Test(enabled = true, description = "can not unfreeze delegated resource")
  void test07CannotUnfreeze() {
    long unfreezableNet = getUnfreezableBalanceV2(contractAddress, contractAddress58, "0");
    long unfreezableEnergy = getUnfreezableBalanceV2(contractAddress, contractAddress58, "1");
    Assert.assertEquals(0, unfreezableNet);
    Assert.assertEquals(0, unfreezableEnergy);
    String methedStr = "unfreezeBalanceV2(uint256,uint256)";
    String argsStr = frozenAmount + ",0";
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    String argsStr1 = frozenAmount + ",1";
    String txid1 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr1,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("unfreeze delegated net: " + info.toString());
    Assert.assertEquals(code.FAILED, info.getResult());
    Assert.assertEquals(contractResult.REVERT, info.getReceipt().getResult());
    info = PublicMethed.getTransactionInfoById(txid1, blockingStubFull).get();
    logger.info("unfreeze delegated energy: " + info.toString());
    Assert.assertEquals(code.FAILED, info.getResult());
    Assert.assertEquals(contractResult.REVERT, info.getReceipt().getResult());
  }

  @Test(enabled = true, description = "undelegate with wrong amount or address")
  void test08UndelegateException() {

    String methedStr = "unDelegateResource(uint256,uint256,address)";
    String receiver = Base58.encode58Check(testAddress002);
    long amount = frozenAmount + 1;
    String argsStr = amount + ",1,\"" + receiver + "\"";
    String txid0 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);

    argsStr = amount + ",0,\"" + receiver + "\"";
    String txid1 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);

    //undelegate with wrong address
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] testAddress003 = ecKey3.getAddress();
    receiver = Base58.encode58Check(testAddress003);
    argsStr = frozenAmount + ",1,\"" + receiver + "\"";
    String txid2 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info0 = PublicMethed.getTransactionInfoById(txid0, blockingStubFull).get();
    logger.info("info0: " + info0.toString());
    Assert.assertEquals(code.FAILED, info0.getResult());
    Assert.assertEquals(contractResult.REVERT, info0.getReceipt().getResult());

    TransactionInfo info1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull).get();
    logger.info("info1: " + info1.toString());
    Assert.assertEquals(code.FAILED, info1.getResult());
    Assert.assertEquals(contractResult.REVERT, info1.getReceipt().getResult());

    TransactionInfo info2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull).get();
    logger.info("info0: " + info2.toString());
    Assert.assertEquals(code.FAILED, info2.getResult());
    Assert.assertEquals(contractResult.REVERT, info2.getReceipt().getResult());

  }

  @Test(enabled = true, description = "normal account consume energy and bandwidth")
  void test09ConsumeResource() {
    String methedStr = "deploy(uint256)";
    String argsStr = "123";
    for(int i = 0; i < 3; i++){
      PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
          false, 0, maxFeeLimit, testAddress002, testKey002, blockingStubFull);
    }
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    checkUnDelegateResource(contractAddress, Base58.encode58Check(testAddress002),
        "0", frozenAmount);
    checkUnDelegateResource(contractAddress, Base58.encode58Check(testAddress002),
        "1", frozenAmount);
  }

  @Test(enabled = true, description = "undelegate net and energy success")
  void test10UndelegateResource() {
    //todo : calculate usage (contract and receiver)

    String methedStr = "unDelegateResource(uint256,uint256,address)";
    String receiver = Base58.encode58Check(testAddress002);

    String argsStr = frozenAmount + ",1,\"" + receiver + "\"";
    String txid0 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);

    argsStr = frozenAmount + ",0,\"" + receiver + "\"";
    String txid1 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info0 = PublicMethed.getTransactionInfoById(txid0, blockingStubFull).get();
    logger.info("undelegate energy info0: " + info0.toString());
    Assert.assertEquals(code.SUCESS, info0.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info0.getReceipt().getResult());

    TransactionInfo info1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull).get();
    logger.info("undelegate bandwidth info1: " + info1.toString());
    Assert.assertEquals(code.SUCESS, info1.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info1.getReceipt().getResult());

    long resourceV2Net = getResourceV2(contractAddress58, receiver, "0", contractAddress);
    long resourceV2Energy = getResourceV2(contractAddress58, receiver, "1", contractAddress);
    logger.info("resourceV2Net: " + resourceV2Net);
    logger.info("resourceV2Energy: " + resourceV2Energy);
    Assert.assertEquals(0, resourceV2Net);
    Assert.assertEquals(0, resourceV2Energy);
    resourceUsage(contractAddress58, contractAddress);
    //todo : calculate usage (contract and receiver)
  }

  @Test(enabled = true, description = "delegate resource when usage>0")
  void test11DelegateWithUsage() {
    delegatebleNet = getDelegatableResource(contractAddress58, contractAddress58, 0);
    maxNet = PublicMethed.getCanDelegatedMaxSize(contractAddress, 0, blockingStubFull)
        .get().getMaxSize();
    delegatebleEnergy = getDelegatableResource(contractAddress58, contractAddress58, 1);
    maxEnergy = PublicMethed.getCanDelegatedMaxSize(contractAddress, 1, blockingStubFull)
        .get().getMaxSize();
    logger.info("delegatebleNet: " + delegatebleNet + "  maxNet: " + maxNet
        + "\n delegatebleEnergy: " + delegatebleEnergy + "  maxEnergy: " + maxEnergy);
    logger.info(PublicMethed.queryAccount(contractAddress,blockingStubFull).toString());
    logger.info(PublicMethed.getAccountResource(contractAddress,blockingStubFull).toString());
    String receiver = Base58.encode58Check(testAddress002);
    String methedStr = "delegateResource(uint256,uint256,address)";
    String argsStr = delegatebleNet + ",0,\"" + receiver + "\"";   //delegate net

    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    argsStr = delegatebleEnergy + ",1,\"" + receiver + "\"";   //delegate energy
    String txid1 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("delegate with usage net info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    info = PublicMethed.getTransactionInfoById(txid1, blockingStubFull).get();
    logger.info("delegate with usage energy info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    long delegatebleNetAfter = getDelegatableResource(contractAddress58, contractAddress58, 0);
    long delegatebleEnergyAfter = getDelegatableResource(contractAddress58, contractAddress58, 1);
    Assert.assertTrue(delegatebleNetAfter < 200);
    Assert.assertTrue(delegatebleEnergyAfter < 200);

    getTotalDelegatedResource(contractAddress58, contractAddress58);
    long resourceV2Net = getResourceV2(contractAddress58, receiver, "0", contractAddress);
    long resourceV2Energy = getResourceV2(contractAddress58, receiver, "1", contractAddress);
    logger.info("resourceV2Net: " + resourceV2Net + "\nresourceV2Energy: " + resourceV2Energy);

    Assert.assertEquals(delegatebleNet, resourceV2Net);
    Assert.assertEquals(delegatebleEnergy, resourceV2Energy);
    getTotalResource(contractAddress58, contractAddress58);
    getTotalResource(receiver, contractAddress58);
    getTotalAcquiredResource(contractAddress, receiver);

  }

  @Test(enabled = true, description = "normal account consume energy and bandwidth again")
  void test12ConsumeResourceAgain() {
    String methedStr = "deploy(uint256)";
    String argsStr = "123";
    for(int i = 0; i < 3; i++){
      PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
          false, 0, maxFeeLimit, testAddress002, testKey002, blockingStubFull);
    }
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    checkUnDelegateResource(contractAddress, Base58.encode58Check(testAddress002), "0", 
        delegatebleNet / 2);
    checkUnDelegateResource(contractAddress, Base58.encode58Check(testAddress002), "1", 
        delegatebleEnergy / 2);
  }

  @Test(enabled = true, description = "")
  void test13CannotKillWithDelegate() {
    String methedStr = "killme(address)";
    String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"";
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("kill me revert WithDelegate txid: " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    Assert.assertEquals(code.FAILED, info.getResult());
    Assert.assertEquals(contractResult.REVERT, info.getReceipt().getResult());

  }

  @Test(enabled = true, description = "undelegate net and energy success again")
  void test14UndelegateResourceAgain() {
    //todo : calculate usage (contract and receiver)

    String methedStr = "unDelegateResource(uint256,uint256,address)";
    String receiver = Base58.encode58Check(testAddress002);

    PublicMethed.freezeBalanceV2(testAddress002, maxNet, 0, testKey002, blockingStubFull);
    PublicMethed.freezeBalanceV2(testAddress002, maxEnergy,
        1, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String argsStr = delegatebleEnergy + ",1,\"" + receiver + "\"";
    String txid0 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);

    argsStr = delegatebleNet + ",0,\"" + receiver + "\"";
    String txid1 = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info0 = PublicMethed.getTransactionInfoById(txid0, blockingStubFull).get();
    logger.info("undelegate again energy info0: " + info0.toString());
    Assert.assertEquals(code.SUCESS, info0.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info0.getReceipt().getResult());

    TransactionInfo info1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull).get();
    logger.info("undelegate again bandwidth info1: " + info1.toString());
    Assert.assertEquals(code.SUCESS, info1.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info1.getReceipt().getResult());

    long resourceV2Net = getResourceV2(contractAddress58, receiver, "0", contractAddress);
    long resourceV2Energy = getResourceV2(contractAddress58, receiver, "1", contractAddress);
    logger.info("resourceV2Net: " + resourceV2Net);
    logger.info("resourceV2Energy: " + resourceV2Energy);
    Assert.assertEquals(0, resourceV2Net);
    Assert.assertEquals(0, resourceV2Energy);
    resourceUsage(contractAddress58, contractAddress);
    //todo : calculate usage (contract and receiver)
  }

  @Test(description = "")
  void test15Create2ContractFreezeDelegate() {
    create2NewFreezeContract();
    onlyFreeze(create2AddBytes, frozenAmount * 2);
    onlyDelegate(create2AddBytes, testAddress002, 0, frozenAmount);
    onlyDelegate(create2AddBytes, testAddress002, 1, frozenAmount);
    test09ConsumeResource();
    onlyUndelegate(create2AddBytes, testAddress002, 0, frozenAmount);
    onlyUndelegate(create2AddBytes, testAddress002, 1, frozenAmount);
  }

  @Test(description = "vote and resource transfer to receiver if contract destroy")
  void test16KillCreate2Add() {
    String witnessTT1 = "TT1smsmhxype64boboU8xTuNZVCKP1w6qT";
    String witnessTB4 = "TB4B1RMhoPeivkj4Hebm6tttHjRY9yQFes";
    String args = "[\"" + witnessTT1 + "\",\"" + witnessTB4 + "\"],[2,2]";
    voteWitness(create2AddBytes, args);

    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] testAddress = ecKey.getAddress();
    String testKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    long balanceCon = PublicMethed.queryAccount(create2AddBytes, blockingStubFull).getBalance();
    AccountResourceMessage resource = 
        PublicMethed.getAccountResource(create2AddBytes, blockingStubFull);
    long netUsedCon = resource.getNetUsed();
    long energyUsedCon = resource.getEnergyUsed();
    String txid = killCon(create2AddBytes, testAddress);
    TransactionInfo info0 = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("kill info0: " + info0.toString());
    Assert.assertEquals(code.SUCESS, info0.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info0.getReceipt().getResult());

    resource = PublicMethed.getAccountResource(testAddress, blockingStubFull);
    long netUsedReceiver = resource.getNetUsed();
    long energyUsedReceiver = resource.getEnergyUsed();

    logger.info("netUsedCon: " + netUsedCon + "  energyUsedCon: " + energyUsedCon
        + "\nnetUsedReceiver: " + netUsedReceiver + "  energyUsedReceiver: " + energyUsedReceiver);
    Account receiverAccount = PublicMethed.queryAccount(testAddress, blockingStubFull);
    long balanceReceiver = receiverAccount.getBalance();
    Assert.assertEquals(balanceCon, balanceReceiver);
    List<Protocol.Vote> li = receiverAccount.getVotesList();
    Assert.assertEquals(0, li.size());

    String str = PublicMethed.queryAccount(create2AddBytes, blockingStubFull).toString();
    Assert.assertEquals("", str);
    Assert.assertEquals(frozenAmount * 2, receiverAccount.getFrozenV2(0).getAmount());
    Assert.assertEquals(frozenAmount * 2, receiverAccount.getFrozenV2(1).getAmount());
    Assert.assertTrue(receiverAccount.getNetUsage() > 0);
    Assert.assertTrue(receiverAccount.getAccountResource().getEnergyUsage() > 0);
  }

  @Test(enabled = true, description = "")
  void test17withdrawExpireUnfreeze() {
    long expireUnfreezeBalance = getExpireUnfreezeBalanceV2(contractAddress, contractAddress58);
    Assert.assertEquals(1000000, expireUnfreezeBalance);
    long balanceBefore = PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance();
    String methedStr = "withdrawExpireUnfreeze()";
    String argsStr = "#";
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    long withdrawAmount =
        ByteArray.toLong(info.getContractResult(0).toByteArray());
    logger.info("withdrawAmount: " + withdrawAmount);
    Assert.assertEquals(expireUnfreezeBalance, withdrawAmount);

    long balanceAfter = PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore + "  balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceAfter, balanceBefore + withdrawAmount);
  }

  @Test(description = "Unfreeze Trigger Portial Unvote")
  void test18UnfreezeTriggerPortialUnvote() {
    String witnessTT1 = "TT1smsmhxype64boboU8xTuNZVCKP1w6qT";
    String witnessTB4 = "TB4B1RMhoPeivkj4Hebm6tttHjRY9yQFes";
    String args = "[\"" + witnessTT1 + "\",\"" + witnessTB4 + "\"],[10,10]";
    voteWitness(contractAddress, args);
    String methedStr = "unfreezeBalanceV2(uint256,uint256)";
    String argsStr = "10000000,0";
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("unfreeze  10trx to unvote info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
    Account receiverAccount = PublicMethed.queryAccount(contractAddress, blockingStubFull);
    List<Protocol.Vote> li = receiverAccount.getVotesList();
    Assert.assertEquals(2, li.size());
    Assert.assertEquals(5, li.get(0).getVoteCount());
    Assert.assertEquals(5, li.get(1).getVoteCount());
  }

  @Test(enabled = true, description = "unfreeze list's max size is 32")
  void test19UnfreezeToMaxSize() {
    String methedStr = "unfreezeBalanceV2(uint256,uint256)";
    String argsStr = ",1";
    for (int i = 0; i < 33; i++) {
      String argsStr1 = i + argsStr;
      PublicMethed.triggerContract(contractAddress, methedStr, argsStr1,
          false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    }
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    long size = getAvailableUnfreezeV2Size(contractAddress, contractAddress58);
    Assert.assertEquals(0, size);
    long expireUnfreezeBalance = getExpireUnfreezeBalanceV2(contractAddress, contractAddress58);
    Assert.assertEquals(0, expireUnfreezeBalance);
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, 1 + argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("unfreeze more than 32 info0: " + info.toString());
    Assert.assertEquals(code.FAILED, info.getResult());
    Assert.assertEquals(contractResult.REVERT, info.getReceipt().getResult());
  }

  @Test(enabled = true, description = "")
  void test20CannotKillWithUnfreeze() {
    String methedStr = "killme(address)";
    String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"";
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("kill me revert WithUnfreeze txid: " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    Assert.assertEquals(code.FAILED, info.getResult());
    Assert.assertEquals(contractResult.REVERT, info.getReceipt().getResult());

  }

  @Test(enabled = true, description = "cancel all unfreeze")
  void test21CancelUnfreeze() {
    long size = getAvailableUnfreezeV2Size(contractAddress, contractAddress58);
    Assert.assertEquals(0, size);
    String methedStr = "cancelAllUnfreezeV2()";
    String argsStr = "#";
    String txid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("cancel all unfreeze info0: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());

    size = getAvailableUnfreezeV2Size(contractAddress, contractAddress58);
    Assert.assertEquals(32, size);
  }

  @Test(description = "")
  void test22GetChainParameters() {
    HashMap<String, Long> map = getResource(contractAddress58, blockingStubFull);
    long netTotalLimit = map.get("netTotalLimit");
    long netTotalWeight = map.get("netTotalWeight");
    long energyTotalLimit = map.get("energyTotalLimit");
    long energyTotalWeight = map.get("energyTotalWeight");
    String methedStr = "getChainParameters()";
    String argsStr = "#";

    TransactionExtention exten = PublicMethed.triggerConstantContractForExtention(
        contractAddress, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    byte[] res = exten.getConstantResult(0).toByteArray();

    long totalNetLimit =
        ByteArray.toLong(ByteArray.subArray(res, 0, 32));
    long totalNetWeight =
        ByteArray.toLong(ByteArray.subArray(res, 32, 64));
    long totalEnergyCurrentLimit =
        ByteArray.toLong(ByteArray.subArray(res, 64, 96));
    long totalEnergyWeight =
        ByteArray.toLong(ByteArray.subArray(res, 96, 128));
    long unfreezeDelayDays =
        ByteArray.toLong(ByteArray.subArray(res, 128, 160));
    logger.info("totalNetLimit: " + totalNetLimit + "  \ntotalNetWeight: " + totalNetWeight
        + "   \ntotalEnergyCurrentLimit: " + totalEnergyCurrentLimit 
        + "  \ntotalEnergyWeight: " + totalEnergyWeight
        + "  \nunfreezeDelayDays: " + unfreezeDelayDays);

    Assert.assertEquals(netTotalLimit, totalNetLimit);
    Assert.assertEquals(netTotalWeight, totalNetWeight);
    Assert.assertEquals(energyTotalLimit, totalEnergyCurrentLimit);
    Assert.assertEquals(energyTotalWeight, totalEnergyWeight);
    Assert.assertEquals(1, unfreezeDelayDays);
  }

  String killCon(byte[] con, byte[] receiverAdd) {
    String receiver = Base58.encode58Check(receiverAdd);
    String methedStr = "killme(address)";
    String argsStr = "\"" + receiver + "\"";

    String txid = PublicMethed.triggerContract(con, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    return txid;

  }


  public void voteWitness(byte[] con, String args) {
    logger.info("vote args: " + args);
    String methodStr = "voteWitness(address[],uint256[])";

    String triggerTxid = PublicMethed.triggerContract(con, methodStr, args, false,
        0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull).get();
    Assert.assertEquals(0, transactionInfo.getResultValue());
    Assert.assertEquals(contractResult.SUCCESS,
        transactionInfo.getReceipt().getResult());
    logger.info("transactionInfo:  " + transactionInfo.toString());
  }


  void create2NewFreezeContract() {
    String methedStr = "getPredictedAddress(bytes32)";
    String argsStr = "1232";
    GrpcAPI.TransactionExtention transactionExtention = 
        PublicMethed.triggerConstantContractForExtention(contractAddressD, methedStr, argsStr,
        false, 0, maxFeeLimit, "0", 0, testFoundationAddress, testFoundationKey, blockingStubFull);

    logger.info("getPredictedAddress transactionExtention: " + transactionExtention.toString());
    create2Add41 = "41" + ByteArray.toHexString(transactionExtention.getConstantResult(0)
        .toByteArray()).substring(24);
    create2AddBytes = ByteArray.fromHexString(create2Add41);
    create2Add58 = Base58.encode58Check(create2AddBytes);
    PublicMethed.sendcoin(create2AddBytes, 100000000, testFoundationAddress, testFoundationKey, blockingStubFull);
    methedStr = "createDSalted(bytes32)";
    PublicMethed.triggerContract(contractAddressD, methedStr, argsStr,
        false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(create2AddBytes, blockingStubFull);
//    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
  }

  void onlyFreeze(byte[] con, long amount) {
    String methedStr = "freezeBalanceV2(uint256,uint256)";
    String argsStr = amount + ",0";
    String txid = PublicMethed.triggerContract(con, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    argsStr = amount + ",1";
    String txid1 = PublicMethed.triggerContract(con, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());

    info = PublicMethed.getTransactionInfoById(txid1, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
  }

  void onlyDelegate(byte[] con, byte[] receiverAdd, int type, long amount) {
    String receiver = Base58.encode58Check(receiverAdd);
    String methedStr = "delegateResource(uint256,uint256,address)";
    String argsStr = amount + "," + type + ",\"" + receiver + "\"";   //delegate net
    logger.info("methedStr: " + methedStr);
    logger.info("argsStr: " + argsStr);
    String txid = PublicMethed.triggerContract(con, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("delegate net info: " + info.toString());
    Assert.assertEquals(code.SUCESS, info.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info.getReceipt().getResult());
  }

  void onlyUndelegate(byte[] con, byte[] receiverAdd, int type, long amount) {
    String methedStr = "unDelegateResource(uint256,uint256,address)";
    String receiver = Base58.encode58Check(receiverAdd);

    String argsStr = amount + "," + type + ",\"" + receiver + "\"";
    String txid0 = PublicMethed.triggerContract(con, methedStr, argsStr,
        false, 0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info0 = PublicMethed.getTransactionInfoById(txid0, blockingStubFull).get();
    logger.info("undelegate energy info0: " + info0.toString());
    Assert.assertEquals(code.SUCESS, info0.getResult());
    Assert.assertEquals(contractResult.SUCCESS, info0.getReceipt().getResult());
  }


  //  @Test(description = "test Available UnfreezeV2 Size")
  long getAvailableUnfreezeV2Size(byte[] con, String queryAdd) {
    String methedStr = "getAvailableUnfreezeV2Size(address)";
    String argsStr = "\"" + queryAdd + "\"";

    TransactionExtention exten = PublicMethed.triggerConstantContractForExtention(
        con, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);

    long unfreezeSize = ByteArray.toLong(exten.getConstantResult(0).toByteArray());
//    logger.info("unfreezeSize: " + unfreezeSize);
    return unfreezeSize;
  }

  //  @Test(description = "get how much sun can be unfreezed")
  public long getUnfreezableBalanceV2(byte[] con, String queryAdd, String type) {

    String methedStr = "getUnfreezableBalanceV2(address,uint256)";
    String argsStr = "\"" + queryAdd + "\"," + type;

    TransactionExtention exten = PublicMethed.triggerConstantContractForExtention(
        con, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    long unfreezableBalance =
        ByteArray.toLong(exten.getConstantResult(0).toByteArray());
    logger.info("unfreezableBalanceNet: " + unfreezableBalance);
    return unfreezableBalance;
  }

  //  @Test(description = "")
  long getExpireUnfreezeBalanceV2(byte[] con, String queryAdd) {
    String methedStr = "getExpireUnfreezeBalanceV2(address,uint256)";
    String argsStr = "\"" + queryAdd + "\"," + System.currentTimeMillis() / 1000;
    logger.info("argsStr: " + argsStr);
    TransactionExtention exten = PublicMethed.triggerConstantContractForExtention(
        con, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);

    long ExpireUnfreezeBalance =
        ByteArray.toLong(exten.getConstantResult(0).toByteArray());
    logger.info("ExpireUnfreezeBalance: " + ExpireUnfreezeBalance);
    return ExpireUnfreezeBalance;
  }

  //  @Test(description = "(limit-use)/limit*freezeAmount")
  public long getDelegatableResource(String con, String queryAdd, int type) {
    String methedStr = "getDelegatableResource(address,uint256)";
    String argsStr = "\"" + queryAdd + "\"," + type;
    byte[] contract = PublicMethed.decode58Check(con);
    TransactionExtention exten = PublicMethed.triggerConstantContractForExtention(
        contract, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    byte[] res = exten.getConstantResult(0).toByteArray();
    long delegatableResource =
        ByteArray.toLong(ByteArray.subArray(res, 0, 32));
    logger.info("delegatable " + type + " : " + delegatableResource);
    return delegatableResource;

  }

  //  @Test(description = "equal with api wallet/getdelegatedresourcev2")
  long getResourceV2(String fromAdd, String receiveAdd, String type, byte[] con) {

    String methedStr = "getResourceV2(address,address,uint256)";
    String argsStr = "\"" + receiveAdd + "\",\"" + fromAdd + "\"," + type;

    TransactionExtention exten = PublicMethed.triggerConstantContractForExtention(
        con, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    byte[] res = exten.getConstantResult(0).toByteArray();
    long delegatedResource =
        ByteArray.toLong(ByteArray.subArray(res, 0, 32));
    logger.info("getResourceV2  delegatedResource: " + delegatedResource);

    return delegatedResource;
  }

  //  @Test(description = "how much can be undelegate")
  void checkUnDelegateResource(byte[] con, String receiveAdd, String type, long amount) {
//    String amount = "1";
    String methedStr = "checkUnDelegateResource(address,uint256,uint256)";
    String argsStr = "\"" + receiveAdd + "\"," + amount + "," + type;

    TransactionExtention exten = PublicMethed.triggerConstantContractForExtention(
        con, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    byte[] res = exten.getConstantResult(0).toByteArray();
    long cleanAmount =
        ByteArray.toLong(ByteArray.subArray(res, 0, 32));
    long dirtyAmount =
        ByteArray.toLong(ByteArray.subArray(res, 32, 64));
    long returnAmount = cleanAmount + dirtyAmount;

    HashMap<String, Long> map = getResource(receiveAdd, blockingStubFull);
    long frozenBalanceV1 = 0;
    long frozenBalanceV2 = 0;
    long acquiredBalanceV1 = 0;
    long acquiredBalanceV2 = 0;
    long used = 0;
    long totalWeight = 0;
    long totalLimit = 0;
    if (type.equalsIgnoreCase("0")) {
      frozenBalanceV1 = map.get("netFrozenBalanceV1");
      frozenBalanceV2 = map.get("netFrozenBalanceV2");
      acquiredBalanceV1 = map.get("netAcquiredBalanceV1");
      acquiredBalanceV2 = map.get("netAcquiredBalanceV2");
      used = map.get("netUsed");
      totalWeight = map.get("netTotalWeight");
      totalLimit = map.get("netTotalLimit");
    } else {
      frozenBalanceV1 = map.get("energyFrozenBalanceV1");
      frozenBalanceV2 = map.get("energyFrozenBalanceV2");
      acquiredBalanceV1 = map.get("energyAcquiredBalanceV1");
      acquiredBalanceV2 = map.get("energyAcquiredBalanceV2");
      used = map.get("energyUsed");
      totalWeight = map.get("energyTotalWeight");
      totalLimit = map.get("energyTotalLimit");
    }

    long sumNetFrozen = frozenBalanceV1 + frozenBalanceV2 + acquiredBalanceV1 + acquiredBalanceV2;
    long recycleNet = Long.valueOf(amount) * used / sumNetFrozen ;
    long dirtySun = recycleNet * totalWeight * 1000000 / totalLimit ;
    long cleanSun = Long.valueOf(amount) - dirtySun;

    logger.info("clean amount net: " + cleanAmount + "dirty amount net: " + dirtyAmount + ": sum: "
        + returnAmount + " compute cleanSun: " + cleanSun + "  compute dirtySun: " + dirtySun);
    Assert.assertEquals(amount, returnAmount);


  }

  //  @Test(description = "")
  void resourceUsage(String queryAdd, byte[] con) {
    String methedStr = "getResourceUsage(address,uint256)";
    String argsStr = "\"" + queryAdd + "\",0";

    TransactionExtention exten = PublicMethed.triggerConstantContractForExtention(
        con, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    byte[] res = exten.getConstantResult(0).toByteArray();
    long usageAmount =
        ByteArray.toLong(ByteArray.subArray(res, 0, 32));

    HashMap<String, Long> map = getResource(queryAdd, blockingStubFull);
    long netTotalWeight = map.get("netTotalWeight");
    long netTotalLimit = map.get("netTotalLimit");
    long netUsed = map.get("netUsed");
    long energyTotalWeight = map.get("energyTotalWeight");
    long energyTotalLimit = map.get("energyTotalLimit");
    long energyUsed = map.get("energyUsed");
    logger.info(netTotalWeight + " : " + netTotalLimit + " : " + netUsed + "  : " + energyTotalWeight
        + " : " + energyTotalLimit + " : " + energyUsed);
    logger.info("ResourceUsage net: " + usageAmount + "   :  "
        + netUsed * netTotalWeight / netTotalLimit * 1000000);

    argsStr = "\"" + queryAdd + "\",1";
    exten = PublicMethed.triggerConstantContractForExtention(
        con, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    res = exten.getConstantResult(0).toByteArray();
    usageAmount =
        ByteArray.toLong(ByteArray.subArray(res, 0, 32));

    logger.info("ResourceUsage energy: " + usageAmount + "   :  " + energyUsed * energyTotalWeight / energyTotalLimit * 1000000);

  }

  //  @Test(description = "return freeze plus acquired")
  void getTotalResource(String queryAdd, String con) {
    HashMap<String, Long> map = getResource(queryAdd, blockingStubFull);
    long netFrozenBalanceV1 = map.get("netFrozenBalanceV1");
    long netFrozenBalanceV2 = map.get("netFrozenBalanceV2");
    long netAcquiredBalanceV1 = map.get("netAcquiredBalanceV1");
    long netAcquiredBalanceV2 = map.get("netAcquiredBalanceV2");
    long energyFrozenBalanceV1 = map.get("energyFrozenBalanceV1");
    long energyFrozenBalanceV2 = map.get("energyFrozenBalanceV2");
    long energyAcquiredBalanceV1 = map.get("energyAcquiredBalanceV1");
    long energyAcquiredBalanceV2 = map.get("energyAcquiredBalanceV2");
    String methedStr = "getTotalResource(address,uint256)";
    String argsStr = "\"" + queryAdd + "\",0";
    byte[] contract = PublicMethed.decode58Check(con);

    TransactionExtention exten = PublicMethed.triggerConstantContractForExtention(
        contract, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    byte[] res = exten.getConstantResult(0).toByteArray();
    long resourceAmount =
        ByteArray.toLong(ByteArray.subArray(res, 0, 32));

    logger.info("resourceAmount net: " + resourceAmount + "  netAcquiredBalanceV1: " + netAcquiredBalanceV1
        + "   netAcquiredBalanceV2: " + netAcquiredBalanceV2
        + " netFrozenBalanceV1: " + netFrozenBalanceV1 + " netFrozenBalanceV2: " + netFrozenBalanceV2);
    Assert.assertEquals(resourceAmount, netAcquiredBalanceV1 + netAcquiredBalanceV2
        + netFrozenBalanceV1 + netFrozenBalanceV2);

    argsStr = "\"" + queryAdd + "\",1";
    exten = PublicMethed.triggerConstantContractForExtention(
        contract, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    res = exten.getConstantResult(0).toByteArray();
    resourceAmount =
        ByteArray.toLong(ByteArray.subArray(res, 0, 32));


    logger.info("resourceAmount energy: " + resourceAmount
        + "  energyFrozenBalanceV1: " + energyFrozenBalanceV1 + "energyFrozenBalanceV2: "
        + energyFrozenBalanceV2 + " energyAcquiredBalanceV1: " + energyAcquiredBalanceV1
        + " energyAcquiredBalanceV2: " + energyAcquiredBalanceV2);
    Assert.assertEquals(resourceAmount, energyFrozenBalanceV1 + energyFrozenBalanceV2
        + energyAcquiredBalanceV1 + energyAcquiredBalanceV2);

  }

  //  @Test(description = "get the amount that has been delegated to others")
  void getTotalDelegatedResource(String queryAdd, String con) {
    String methedStr = "getTotalDelegatedResource(address,uint256)";
    String argsStr = "\"" + queryAdd + "\",0";

    byte[] contract = PublicMethed.decode58Check(con);

    TransactionExtention exten = PublicMethed.triggerConstantContractForExtention(
        contract, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    byte[] res = exten.getConstantResult(0).toByteArray();
    long delegatedAmount =
        ByteArray.toLong(ByteArray.subArray(res, 0, 32));
    long accountDelegateV2 = PublicMethed.queryAccount(PublicMethed.decode58Check(queryAdd), blockingStubFull)
        .getDelegatedFrozenV2BalanceForBandwidth();
    long accountDelegateV1 = PublicMethed.queryAccount(PublicMethed.decode58Check(queryAdd), blockingStubFull)
        .getDelegatedFrozenBalanceForBandwidth();
    logger.info("delegatedAmount net: " + delegatedAmount + "    accountDelegateV2: " + accountDelegateV2
        + " accountDelegateV1: " + accountDelegateV1);
    Assert.assertEquals(delegatedAmount, accountDelegateV2 + accountDelegateV1);

    argsStr = "\"" + queryAdd + "\",1";
    exten = PublicMethed.triggerConstantContractForExtention(
        contract, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    res = exten.getConstantResult(0).toByteArray();
    delegatedAmount =
        ByteArray.toLong(ByteArray.subArray(res, 0, 32));
    accountDelegateV2 = PublicMethed.queryAccount(PublicMethed.decode58Check(queryAdd), blockingStubFull)
        .getAccountResource().getDelegatedFrozenV2BalanceForEnergy();
    accountDelegateV1 = PublicMethed.queryAccount(PublicMethed.decode58Check(queryAdd), blockingStubFull)
        .getAccountResource().getDelegatedFrozenBalanceForEnergy();
    logger.info("delegatedAmount energy: " + delegatedAmount + "    accountDelegateV2: " + accountDelegateV2
        + " accountDelegateV1: " + accountDelegateV1);
    Assert.assertEquals(delegatedAmount, accountDelegateV2 + accountDelegateV1);
  }


  void getTotalAcquiredResource(byte[] con, String queryAdd) {
    String methedStr = "getTotalAcquiredResource(address,uint256)";
    String argsStr = "\"" + queryAdd + "\",0";
    TransactionExtention exten = PublicMethed.triggerConstantContractForExtention(
        con, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    byte[] res = exten.getConstantResult(0).toByteArray();
    long acquiredAmount =
        ByteArray.toLong(ByteArray.subArray(res, 0, 32));
    HashMap<String, Long> map = getResource(queryAdd, blockingStubFull);
    long accountAcquiredV1 = map.get("netAcquiredBalanceV1");
    long accountAcquiredV2 = map.get("netAcquiredBalanceV2");
//    long accountAcquiredV2 = PublicMethed.queryAccount(PublicMethed.decode58Check(queryAdd),blockingStubFull).getAcquiredDelegatedFrozenV2BalanceForBandwidth();
//    long accountAcquiredV1 = PublicMethed.queryAccount(PublicMethed.decode58Check(queryAdd),blockingStubFull).getAcquiredDelegatedFrozenBalanceForBandwidth();
    logger.info("acquiredAmount net: " + acquiredAmount + "    accountAcquiredV2: " + accountAcquiredV2 + "  accountAcquiredV1: " + accountAcquiredV1);
    Assert.assertEquals(acquiredAmount, accountAcquiredV2 + accountAcquiredV1);


    argsStr = "\"" + queryAdd + "\",1";
    exten = PublicMethed.triggerConstantContractForExtention(
        con, methedStr, argsStr, false, 0, maxFeeLimit,
        "#", 0, testFoundationAddress, testFoundationKey, blockingStubFull);
    res = exten.getConstantResult(0).toByteArray();
    acquiredAmount =
        ByteArray.toLong(ByteArray.subArray(res, 0, 32));
    accountAcquiredV1 = map.get("energyAcquiredBalanceV1");
    accountAcquiredV2 = map.get("energyAcquiredBalanceV2");
    logger.info("acquiredAmount energy: " + acquiredAmount + "    accountAcquiredV2: " + accountAcquiredV2 + "  accountAcquiredV1: " + accountAcquiredV1);
    Assert.assertEquals(acquiredAmount, accountAcquiredV2 + accountAcquiredV1);

  }


  public HashMap<String, Long> getResource(String address, WalletGrpc.WalletBlockingStub blockingStub) {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(PublicMethed.decode58Check(address), blockingStub);
    Account account = PublicMethed.queryAccount(PublicMethed.decode58Check(address), blockingStub);

    long netFrozenBalanceV1 = account.getFrozenCount() != 0 ? account.getFrozen(0).getFrozenBalance() : 0;
    List<Account.FreezeV2> liV2 = account.getFrozenV2List();
    long netFrozenBalanceV2 = 0;
    for (Account.FreezeV2 tmp : liV2) {
      if (tmp.getType().getNumber() == 0) {
        netFrozenBalanceV2 = tmp.getAmount();
        break;
      }
    }
    long netAcquiredBalanceV1 = account.getAcquiredDelegatedFrozenBalanceForBandwidth();
    long netAcquiredBalanceV2 = account.getAcquiredDelegatedFrozenV2BalanceForBandwidth();

    long energyFrozenBalanceV1 = account.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance();
    long energyFrozenBalanceV2 = 0;
    for (Account.FreezeV2 tmp : liV2) {
      if (tmp.getType().getNumber() == 1) {
        energyFrozenBalanceV2 = tmp.getAmount();
        break;
      }
    }
    long energyAcquiredBalanceV1 = account.getAccountResource().getAcquiredDelegatedFrozenBalanceForEnergy();
    long energyAcquiredBalanceV2 = account.getAccountResource().getAcquiredDelegatedFrozenV2BalanceForEnergy();
    long netTotalWeight = accountResource.getTotalNetWeight();
    long netTotalLimit = accountResource.getTotalNetLimit();
    long netUsed = accountResource.getNetUsed();
    long energyTotalWeight = accountResource.getTotalEnergyWeight();
    long energyTotalLimit = accountResource.getTotalEnergyLimit();
    long energyUsed = accountResource.getEnergyUsed();
    HashMap<String, Long> map = new HashMap<>();
    map.put("netFrozenBalanceV1", netFrozenBalanceV1);
    map.put("netFrozenBalanceV2", netFrozenBalanceV2);
    map.put("netAcquiredBalanceV1", netAcquiredBalanceV1);
    map.put("netAcquiredBalanceV2", netAcquiredBalanceV2);
    map.put("energyFrozenBalanceV1", energyFrozenBalanceV1);
    map.put("energyFrozenBalanceV2", energyFrozenBalanceV2);
    map.put("energyAcquiredBalanceV1", energyAcquiredBalanceV1);
    map.put("energyAcquiredBalanceV2", energyAcquiredBalanceV2);
    map.put("netTotalWeight", netTotalWeight);
    map.put("netTotalLimit", netTotalLimit);
    map.put("netUsed", netUsed);
    map.put("energyTotalWeight", energyTotalWeight);
    map.put("energyTotalLimit", energyTotalLimit);
    map.put("energyUsed", energyUsed);
    return map;
  }

//  public void printGetAccount(byte[] address, WalletGrpc.WalletBlockingStub full) {
//    logger.info("----getaccount begin --address" + Base58.encode58Check(address));
//    logger.info(System.currentTimeMillis() + "");
//    logger.info(PublicMethed.queryAccount(address, full).toString());
//    logger.info(PublicMethed.getAccountResource(address, full).toString());
//    logger.info("----getaccount end----");
//  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
