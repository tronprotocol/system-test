package stest.tron.wallet.dailybuild.manual;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class ContractScenario015 extends TronBaseTest {  byte[] contractAddress1 = null;
  byte[] contractAddress2 = null;
  byte[] contractAddress3 = null;
  String txid = "";
  Optional<TransactionInfo> infoById = null;
  String contractName = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract014Address = ecKey1.getAddress();
  String contract014Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey2.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {  }

  @Test(enabled = true, description = "TRON TRC20 transfer token", groups = {"daily"})
  public void trc20Tron() {
    ecKey1 = new ECKey(Utils.getRandom());
    contract014Address = ecKey1.getAddress();
    contract014Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    receiverAddress = ecKey2.getAddress();
    receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethod.printAddress(contract014Key);
    PublicMethod.printAddress(receiverKey);

    Assert.assertTrue(PublicMethod.sendcoin(contract014Address, 1000000000L, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Deploy contract1, contract1 has a function to transaction 5 sun to target account
    String contractName = "TRON TRC20";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_Scenario015_TRC20_TRON");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_Scenario015_TRC20_TRON");
    txid = PublicMethod.deployContractAndGetTransactionInfoById(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, contract014Key, contract014Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    logger.info(txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contractAddress1 = infoById.get().getContractAddress().toByteArray();
  //Set SiringAuctionAddress to kitty core.
    String siringContractString = "\"" + Base58.encode58Check(foundationAddress) + "\"";
    txid = PublicMethod
        .triggerContract(contractAddress1, "balanceOf(address)", siringContractString,
            false, 0, 30000000L, contract014Address, contract014Key, blockingStubFull);
    logger.info(txid);

    siringContractString = "\"" + Base58.encode58Check(foundationAddress) + "\",\"" + 17 + "\"";
    txid = PublicMethod.triggerContract(contractAddress1, "transfer(address,uint256)",
        siringContractString, false, 0, 30000000L, contract014Address,
        contract014Key, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;

    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    siringContractString = "\"" + Base58.encode58Check(foundationAddress) + "\"";
    txid = PublicMethod
        .triggerContract(contractAddress1, "balanceOf(address)",
            siringContractString, false, 0, 30000000L, contract014Address,
            contract014Key, blockingStubFull);
    logger.info(txid);
  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(contract014Address, contract014Key, foundationAddress, blockingStubFull);  }
}


