package stest.tron.wallet.dailybuild.manual;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.WalletExtensionGrpc;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.BalanceContract.TransferContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TransactionUtils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestTransfer002 extends TronBaseTest {  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  private WalletExtensionGrpc.WalletExtensionBlockingStub blockingStubExtension = null;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }


  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    blockingStubExtension = WalletExtensionGrpc.newBlockingStub(channelSolidity);
  }

  @Test(enabled = false, groups = {"daily"})
  public void testGetTotalTransaction() {
    NumberMessage beforeGetTotalTransaction = blockingStubFull
        .totalTransaction(GrpcAPI.EmptyMessage.newBuilder().build());
    logger.info(Long.toString(beforeGetTotalTransaction.getNum()));
  Long beforeTotalTransaction = beforeGetTotalTransaction.getNum();
    Assert.assertTrue(PublicMethod.sendcoin(toAddress, 1000000, foundationAddress,
        foundationKey, blockingStubFull));
    NumberMessage afterGetTotalTransaction = blockingStubFull
        .totalTransaction(GrpcAPI.EmptyMessage.newBuilder().build());
    logger.info(Long.toString(afterGetTotalTransaction.getNum()));
  Long afterTotalTransaction = afterGetTotalTransaction.getNum();
    Assert.assertTrue(afterTotalTransaction - beforeTotalTransaction > 0);
  //Improve coverage.
    afterGetTotalTransaction.equals(beforeGetTotalTransaction);
    afterGetTotalTransaction.equals(afterGetTotalTransaction);
    afterGetTotalTransaction.hashCode();
    afterGetTotalTransaction.isInitialized();
    afterGetTotalTransaction.getSerializedSize();
    afterGetTotalTransaction.getDefaultInstanceForType();
    afterGetTotalTransaction.getParserForType();
    afterGetTotalTransaction.getUnknownFields();


  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(toAddress, testKey003, foundationAddress, blockingStubFull);  }

  /**
   * constructor.
   */
  public Boolean sendcoin(byte[] to, long amount, byte[] owner, String priKey) {

    //String priKey = foundationKey;
  ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    Account search = queryAccount(ecKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    return response.getResult();
  }

  /**
   * constructor.
   */
  public Account queryAccount(ECKey ecKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    if (ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
  byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  /**
   * constructor.
   */
  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */
  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }
}


