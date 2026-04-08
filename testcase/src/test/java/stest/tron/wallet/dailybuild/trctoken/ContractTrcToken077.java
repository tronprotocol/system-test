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
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class ContractTrcToken077 extends TronBaseTest {


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

  @Test(enabled = false, groups = {"contract", "daily"})
  public void testAddress001() {
    PublicMethod
        .sendcoin(grammarAddress, 100000000000L, foundationAddress, foundationKey,
            blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/contractTrcToken077.sol";
  String contractName = "trcToken077";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String deployTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, testKeyForGrammarAddress,
            grammarAddress, blockingStubFull);
    Optional<TransactionInfo> deployInfo = PublicMethod
        .getTransactionInfoById(deployTxid, blockingStubFull);
    contractAddress = deployInfo.get().getContractAddress().toByteArray();
    logger.info("Deploy energy is " + deployInfo.get().getReceipt().getEnergyUsageTotal());
  String txid = "";
    txid = PublicMethod.triggerContract(contractAddress,
        "addressTest()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
    logger.info("Trigger energy is " + infoById.get().getReceipt().getEnergyUsageTotal());

  }

  @Test(enabled = true, description = "The value of address is not at the beginning of 41", groups = {"contract", "daily"})
  public void testAddress002() {
    PublicMethod
        .sendcoin(grammarAddress, 100000000000L, foundationAddress, foundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String contractName = "trcToken077";
  String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600"
        + "080fd5b5060b0806100396000396000f3fe6080604052348015600f57600080fd5b50d38015601b57600080"
        + "fd5b50d28015602757600080fd5b5060043610605c577c01000000000000000000000000000000000000000"
        + "0000000000000000060003504636241c1d881146061575b600080fd5b60676079565b604080519182525190"
        + "81900360200190f35b60405130908190529056fea165627a7a723058207b9b52e71420f2fa4cb55ffd55641"
        + "355ec84e09d6d4545c629dde7cc01d74a100029";
  String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"addressTest\",\"outputs\":[{\"name"
        + "\":\"addressValue\",\"type\":\"bytes32\"}],\"payable\":false,\"stateMutability\":\"nonp"
        + "ayable\",\"type\":\"function\"}]";
  String deploytxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, testKeyForGrammarAddress,
            grammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> deployById = PublicMethod
        .getTransactionInfoById(deploytxid, blockingStubFull);
    contractAddress = deployById.get().getContractAddress().toByteArray();
    logger.info("infoById:" + deployById);
  String txid = "";
    txid = PublicMethod.triggerContract(contractAddress,
        "addressTest()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);

    Assert.assertNotNull(infoById);
  byte[] a = infoById.get().getContractResult(0).toByteArray();
  byte[] b = subByte(a, 11, 1);
  byte[] c = subByte(a, 0, 11);
  byte[] e = "41".getBytes();
  byte[] d = subByte(a, 12, 20);

    logger.info("a:" + ByteArray.toHexString(a));

    logger.info("b:" + ByteArray.toHexString(b));
    logger.info("c:" + ByteArray.toHexString(c));

    logger.info("d:" + ByteArray.toHexString(d));

    logger.info("41" + ByteArray.toHexString(d));
  String exceptedResult = "41" + ByteArray.toHexString(d);
  String realResult = ByteArray.toHexString(b);
    Assert.assertEquals(realResult, "00");
    Assert.assertNotEquals(realResult, "41");

    Assert.assertEquals(exceptedResult, ByteArray.toHexString(contractAddress));

  }


  /**
   * constructor.
   */
  public byte[] subByte(byte[] b, int off, int length) {
    byte[] b1 = new byte[length];
    System.arraycopy(b, off, b1, 0, length);
    return b1;

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(grammarAddress, testKeyForGrammarAddress, foundationAddress,
        blockingStubFull);  }
}
