package stest.tron.wallet.dailybuild.tvmnewcommand.tvmFreeze;

import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
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
public class FreezeContractTest002 extends TronBaseTest {

  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey2.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private long freezeEnergyUseage;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {    if(PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {      throw new SkipException("Skipping freezeV2 test case");
    }


    Assert.assertTrue(PublicMethod.sendcoin(testAddress001,2000_000000L,
        testFoundationAddress,testFoundationKey,blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(testAddress002,10_000000L,
        testFoundationAddress,testFoundationKey,blockingStubFull));
  String filePath = "src/test/resources/soliditycode/freezeContract001.sol";
  String contractName = "TestFreeze";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 100_000000L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "contract freeze over balance", groups = {"contract", "daily"})
  void FreezeContract001() {

    AccountResourceMessage account002_before = PublicMethod
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_before = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    Long freezeCount = contractAccount_before.getBalance() + 1;
  String methedStr = "freeze(address,uint256,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"," + freezeCount + "," + "1";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethod
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_before.getEnergyLimit : " + account002_before.getEnergyLimit());
    logger.info("account002_after.getEnergyLimit : " + account002_after.getEnergyLimit());
    Assert.assertEquals(account002_before.getEnergyLimit(), account002_after.getEnergyLimit());
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy(),
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance(),
        contractAccount_after.getBalance());

    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    freezeEnergyUseage = info.getReceipt().getEnergyUsageTotal();

    Assert.assertEquals(code.FAILED,info.getResult());
    Assert.assertEquals(contractResult.REVERT,info.getReceipt().getResult());

  }

  @Test(enabled = true, description = "contract freeze amount < 1 TRX", groups = {"contract", "daily"})
  void FreezeContract002() {

    Account account002_before = PublicMethod
        .queryAccount(testAddress002,blockingStubFull);
    Account contractAccount_before = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    Long freezeCount = 999999L;
  String methedStr = "freeze(address,uint256,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"," + freezeCount + "," + "1";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account account002_after = PublicMethod
        .queryAccount(testAddress002,blockingStubFull);
    Account contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);
    logger.info("account002_before.getAccountResource : " + account002_before.getAccountResource());
    logger.info("account002_after.getAccountResource : " + account002_after.getAccountResource());

    Assert.assertEquals(
        account002_before.getAccountResource().getAcquiredDelegatedFrozenBalanceForEnergy(),
        account002_after.getAccountResource().getAcquiredDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy(),
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance(),
        contractAccount_after.getBalance());


    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    freezeEnergyUseage = info.getReceipt().getEnergyUsageTotal();

    Assert.assertEquals(code.FAILED,info.getResult());
    Assert.assertEquals(contractResult.REVERT,info.getReceipt().getResult());

  }

  @Test(enabled = true, description = "contract transfer all balance, then freeze", groups = {"contract", "daily"})
  void FreezeContract003() {

    AccountResourceMessage account002_before = PublicMethod
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_before = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    Long freezeCount = contractAccount_before.getBalance();
  String methedStr = "freezeAndSend(address,uint256,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"," + freezeCount + "," + "1";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethod
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_before.getEnergyLimit : " + account002_before.getEnergyLimit());
    logger.info("account002_after.getEnergyLimit : " + account002_after.getEnergyLimit());
    Assert.assertEquals(account002_before.getEnergyLimit(), account002_after.getEnergyLimit());
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy(),
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance(),
        contractAccount_after.getBalance());

    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    freezeEnergyUseage = info.getReceipt().getEnergyUsageTotal();

    Assert.assertEquals(code.FAILED,info.getResult());
    Assert.assertEquals(contractResult.REVERT,info.getReceipt().getResult());
  }

  @Test(enabled = true, description = "contract freeze to ger Net", groups = {"contract", "daily"})
  void FreezeContract004() {

    AccountResourceMessage account002_before = PublicMethod
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_before = PublicMethod.queryAccount(contractAddress,blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    Long freezeCount = 1_000000L;
  String methedStr = "freeze(address,uint256,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"," + freezeCount + "," + "0";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethod
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_after = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_before.getNetLimit : " + account002_before.getNetLimit());
    logger.info("account002_after.getNetLimit : " + account002_after.getNetLimit());
    Assert.assertTrue(account002_before.getNetLimit() < account002_after.getNetLimit());
    Assert.assertEquals(contractAccount_before
            .getDelegatedFrozenBalanceForBandwidth() + freezeCount,
        contractAccount_after.getDelegatedFrozenBalanceForBandwidth());
    Assert.assertEquals(contractAccount_before.getBalance() - freezeCount,
        contractAccount_after.getBalance());
  }

  @Test(enabled = true, description = "contract freeze to ger Net", groups = {"contract", "daily"})
  void FreezeContract005() {
    long startTime = System.currentTimeMillis() - 6 * 1000;
  // freeze(address payable receiver, uint amount, uint res)
    Long freezeCount = 1000000L;
  String methedStr = "freeze(address,uint256,uint256)";
  String argsStr = "\"" + Base58.encode58Check(testAddress001) + "\"," + freezeCount + "," + "1";
  String txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // freeze(address payable receiver, uint amount, uint res)
    String ExpireTimeMethedStr = "getExpireTime(address,uint256)";
  String ExpireTimeArgsStr = "\"" + Base58.encode58Check(testAddress001) + "\"" + ",1";
    TransactionExtention extention = PublicMethod
        .triggerConstantContractForExtention(contractAddress,ExpireTimeMethedStr,ExpireTimeArgsStr,
            false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
  Long ExpireTime1 = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime1: " + ExpireTime1);

    txid = PublicMethod.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    freezeEnergyUseage = info.getReceipt().getEnergyUsageTotal();
    Assert.assertEquals(code.SUCESS,info.getResult());
    Assert.assertEquals(contractResult.SUCCESS,info.getReceipt().getResult());

    extention = PublicMethod
        .triggerConstantContractForExtention(contractAddress,ExpireTimeMethedStr,ExpireTimeArgsStr,
            false,0,maxFeeLimit,"#",0, testAddress001, testKey001, blockingStubFull);
  Long ExpireTime2 = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime2: " + ExpireTime2);

    Assert.assertTrue(ExpireTime2 * 1000 <= info.getBlockTimeStamp());
    Assert.assertTrue(ExpireTime2 * 1000 > startTime);
  }


}
