package stest.tron.wallet.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;

import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class WalletTestAccount001 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();  //just test key
  private final String invalidTestKey =
      "592BB6C9BB255409A6A45EFD18E9A74FECDDCCE93A40D96B70FBE334E6361E36";  private ManagedChannel channelSolidity = null;  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;  /**
   * constructor.
   */
  @BeforeClass
  public void beforeClass() {  }

  @Test
  public void testqueryaccountfromfullnode() {
    initSolidityChannel();
    //Query success, get the right balance,bandwidth and the account name.
    Account queryResult = queryAccount(foundationKey2, blockingStubFull);
    /*    Account queryResult = PublicMethod.queryAccountByAddress(fromAddress,blockingStubFull);
    logger.info(ByteArray.toStr(queryResult.getAccountName().toByteArray()));
    logger.info(Long.toString(queryResult.getBalance()));
    logger.info(ByteArray.toStr(queryResult.getAddress().toByteArray()));*/
    Assert.assertTrue(queryResult.getBalance() > 0);
    //Assert.assertTrue(queryResult.getBandwidth() >= 0);
    Assert.assertTrue(queryResult.getAccountName().toByteArray().length > 0);
    Assert.assertFalse(queryResult.getAddress().isEmpty());

    //Query failed
    Account invalidQueryResult = queryAccount(invalidTestKey, blockingStubFull);
    Assert.assertTrue(invalidQueryResult.getAccountName().isEmpty());
    Assert.assertTrue(invalidQueryResult.getAddress().isEmpty());

    //Improve coverage.
    queryResult.hashCode();
    queryResult.getSerializedSize();
    queryResult.equals(queryResult);
    queryResult.equals(invalidQueryResult);
  }

  @Test
  public void testqueryaccountfromsoliditynode() {
    //Query success, get the right balance,bandwidth and the account name.
    Account queryResult = solidityqueryAccount(foundationKey2, blockingStubSolidity);
    Assert.assertTrue(queryResult.getBalance() > 0);
    //Assert.assertTrue(queryResult.getBandwidth() >= 0);
    Assert.assertTrue(queryResult.getAccountName().toByteArray().length > 0);
    Assert.assertFalse(queryResult.getAddress().isEmpty());

    //Query failed
    Account invalidQueryResult = solidityqueryAccount(invalidTestKey, blockingStubSolidity);
    Assert.assertTrue(invalidQueryResult.getAccountName().isEmpty());
    Assert.assertTrue(invalidQueryResult.getAddress().isEmpty());

  }
  /**
   * constructor.
   */

  public Account queryAccount(String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
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
    logger.info(Integer.toString(ecKey.getAddress().length));

    //PublicMethod.AddPreFix();
    logger.info(Integer.toString(ecKey.getAddress().length));
    System.out.println("address ====== " + ByteArray.toHexString(ecKey.getAddress()));
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
    //return grpcQueryAccount(address,blockingStubFull);
  }

  /**
   * constructor.
   */

  public Account solidityqueryAccount(String priKey,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
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
    //byte[] address = PublicMethod.AddPreFix(ecKey.getAddress());
    return grpcQueryAccountSolidity(ecKey.getAddress(), blockingStubSolidity);
    //return grpcQueryAccountSolidity(address,blockingStubSolidity);
  }

  /**
   * constructor.
   */

  public String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  /**
   * constructor.
   */

  public Account grpcQueryAccount(byte[] address,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    //address = PublicMethod.AddPreFix(address);
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */

  public Account grpcQueryAccountSolidity(byte[] address,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    //address = PublicMethod.AddPreFix(address);
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubSolidity.getAccount(request);
  }

  /**
   * constructor.
   */

  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }
}

