package stest.tron.wallet.dailybuild.trctoken;

import io.grpc.ManagedChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j

public class ContractTrcToken078 extends TronBaseTest {

  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] internalTxsAddress = ecKey1.getAddress();
  String testKeyForinternalTxsAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  String priKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;

  /**
   * constructor.
   */
  public static String byte2HexStr(byte[] b, int offset, int length) {
    String stmp = "";
    StringBuilder sb = new StringBuilder("");
    for (int n = offset; n < offset + length && n < b.length; n++) {
      stmp = Integer.toHexString(b[n] & 0xFF);
      sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
    }
    return sb.toString().toUpperCase().trim();
  }


  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(testKeyForinternalTxsAddress);    logger.info(Long.toString(PublicMethod.queryAccount(foundationKey, blockingStubFull)
        .getBalance()));
  }

  @Test(enabled = true, description = "Origin test call", groups = {"contract", "daily"})
  public void testOriginCall001() {
    PublicMethod
        .sendcoin(internalTxsAddress, 100000000000L, foundationAddress, foundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/contractTrcToken078.sol";
  String contractName = "callerContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById : " + infoById);
    contractAddress = infoById.get().getContractAddress().toByteArray();
  String filePath1 = "./src/test/resources/soliditycode/contractTrcToken078.sol";
  String contractName1 = "calledContract";
    HashMap retMap1 = PublicMethod.getBycodeAbi(filePath1, contractName1);
  String code1 = retMap1.get("byteCode").toString();
  String abi1 = retMap1.get("abI").toString();

    txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById : " + infoById);
  byte[] contractAddress1;
    contractAddress1 = infoById.get().getContractAddress().toByteArray();
  String filePath2 = "./src/test/resources/soliditycode/contractTrcToken078.sol";
  String contractName2 = "c";
    HashMap retMap2 = PublicMethod.getBycodeAbi(filePath2, contractName2);
  String code2 = retMap2.get("byteCode").toString();
  String abi2 = retMap2.get("abI").toString();

    txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
  byte[] contractAddress2 = infoById.get().getContractAddress().toByteArray();
    logger.info("infoById : " + infoById);
  String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";
  String txid2 = "";
    txid2 = PublicMethod.triggerContract(contractAddress,
        "sendToB2(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethod.getTransactionInfoById(txid2, blockingStubFull);
    logger.info("Trigger InfobyId: " + infoById2);
    Account info1 = PublicMethod.queryAccount(internalTxsAddress, blockingStubFull);
    AccountResourceMessage resourceInfo1 = PublicMethod.getAccountResource(internalTxsAddress,
        blockingStubFull);
    logger.info("getEnergyUsed  " + resourceInfo1.getEnergyUsed());
    logger.info("getEnergyLimit  " + resourceInfo1.getEnergyLimit());
    Assert.assertTrue(infoById2.get().getResultValue() == 0);


  }

  @Test(enabled = true, description = "Origin test delegatecall", groups = {"contract", "daily"})
  public void testOriginDelegatecall001() {
    PublicMethod
        .sendcoin(internalTxsAddress, 100000000000L, foundationAddress, foundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/contractTrcToken078.sol";
  String contractName = "callerContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById : " + infoById);
    contractAddress = infoById.get().getContractAddress().toByteArray();
  String filePath1 = "./src/test/resources/soliditycode/contractTrcToken078.sol";
  String contractName1 = "calledContract";
    HashMap retMap1 = PublicMethod.getBycodeAbi(filePath1, contractName1);
  String code1 = retMap1.get("byteCode").toString();
  String abi1 = retMap1.get("abI").toString();
    txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById : " + infoById);
  byte[] contractAddress1;
    contractAddress1 = infoById.get().getContractAddress().toByteArray();
  String filePath2 = "./src/test/resources/soliditycode/contractTrcToken078.sol";
  String contractName2 = "c";
    HashMap retMap2 = PublicMethod.getBycodeAbi(filePath2, contractName2);
  String code2 = retMap2.get("byteCode").toString();
  String abi2 = retMap2.get("abI").toString();
    txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById : " + infoById);
  byte[] contractAddress2 = infoById.get().getContractAddress().toByteArray();
  String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";
  String txid2 = "";
    txid2 = PublicMethod.triggerContract(contractAddress,
        "sendToB(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethod.getTransactionInfoById(txid2, blockingStubFull);
    logger.info("infoById : " + infoById2);

    Assert.assertTrue(infoById2.get().getResultValue() == 0);


  }

  private List<String> getStrings(byte[] data) {
    int index = 0;
    List<String> ret = new ArrayList<>();
    while (index < data.length) {
      ret.add(byte2HexStr(data, index, 32));
      index += 32;
    }
    return ret;
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
    PublicMethod
        .freeResource(internalTxsAddress, testKeyForinternalTxsAddress, foundationAddress,
            blockingStubFull);  }
}
