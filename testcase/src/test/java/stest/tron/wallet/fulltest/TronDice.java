package stest.tron.wallet.fulltest;

import java.util.ArrayList;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
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
public class TronDice extends TronBaseTest {  byte[] contractAddress;
  Long maxFeeLimit = 1000000000L;
  Optional<TransactionInfo> infoById = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract008Address = ecKey1.getAddress();
  String contract008Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ArrayList<String> txidList = new ArrayList<String>();
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contract008Key);    PublicMethod.printAddress(foundationKey);
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contract008Address,
        blockingStubFull);
  }

  @Test(enabled = true, threadPoolSize = 30, invocationCount = 30, groups = {"full"})
  public void tronDice() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] tronDiceAddress = ecKey1.getAddress();
  String tronDiceKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethod
        .sendcoin(tronDiceAddress, 100000000000L, foundationAddress, foundationKey, blockingStubFull);
  String contractName = "TronDice";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TronDice_tronDice");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TronDice_tronDice");
  byte[] contractAddress = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 1000000000L, 100, null, tronDiceKey, tronDiceAddress, blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Assert.assertTrue(smartContract.getAbi() != null);
  String txid;

    for (Integer i = 0; i < 100; i++) {
      String initParmes = "\"" + "10" + "\"";
      txid = PublicMethod.triggerContract(contractAddress,
          "rollDice(uint256)", initParmes, false,
          1000000, maxFeeLimit, tronDiceAddress, tronDiceKey, blockingStubFull);
      logger.info(txid);
      txidList.add(txid);

      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Integer successTimes = 0;
    Integer failedTimes = 0;
    Integer totalTimes = 0;
    for (String txid1 : txidList) {
      totalTimes++;
      infoById = PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
      if (infoById.get().getBlockNumber() > 3523732) {
        logger.info("blocknum is " + infoById.get().getBlockNumber());
        successTimes++;
      } else {
        failedTimes++;
      }
    }
    logger.info("Total times is " + totalTimes.toString());
    logger.info("success times is " + successTimes.toString());
    logger.info("failed times is " + failedTimes.toString());
    logger.info("success percent is " + successTimes / totalTimes);  }
}


