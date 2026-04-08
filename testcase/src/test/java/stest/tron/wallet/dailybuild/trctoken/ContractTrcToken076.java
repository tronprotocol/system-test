package stest.tron.wallet.dailybuild.trctoken;

import io.grpc.ManagedChannel;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class ContractTrcToken076 extends TronBaseTest {


  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress = ecKey1.getAddress();
  String testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(testKeyForGrammarAddress);    logger.info(Long.toString(PublicMethod.queryAccount(foundationKey, blockingStubFull)
        .getBalance()));
  }

  @Test(enabled = true, description = "Origin test ", groups = {"contract", "daily"})
  public void testDeployTransferTokenContract() {
    PublicMethod
        .sendcoin(grammarAddress, 100000000000L, foundationAddress, foundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/contractTrcToken076.sol";
  String contractName = "Test";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, testKeyForGrammarAddress,
            grammarAddress, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    contractAddress = infoById.get().getContractAddress().toByteArray();

    PublicMethod.triggerContract(contractAddress,
        "test()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    txid = PublicMethod.triggerContract(contractAddress,
        "getResult1()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    logger.info("infoById:" + infoById);
  Long returnnumber = ByteArray.toLong(ByteArray.fromHexString(ByteArray.toHexString(
        infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber == 1);

    txid = PublicMethod.triggerContract(contractAddress,
        "getResult2()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    logger.info("-------------------------");

    logger.info("infoById:" + infoById);
  Long returnnumber2 = ByteArray.toLong(ByteArray.fromHexString(
        ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber2 == 1);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(grammarAddress, testKeyForGrammarAddress, foundationAddress,
        blockingStubFull);  }

}
