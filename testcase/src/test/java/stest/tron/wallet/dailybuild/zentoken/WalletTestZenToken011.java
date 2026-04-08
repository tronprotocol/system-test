package stest.tron.wallet.dailybuild.zentoken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class WalletTestZenToken011 extends TronBaseTest {

  private static ByteString assetAccountId = null;  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo;
  String sendShieldAddress;
  String receiverShieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  DecryptNotes notes;
  String memo;
  Note sendNote;
  Note receiverNote;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelSolidity1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity1 = null;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliditynode1 = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethod.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long costTokenAmount = 10 * zenTokenFee;
  private Long sendTokenAmount = 8 * zenTokenFee;
  private String txid;
  private Optional<TransactionInfo> infoById;
  private Optional<Transaction> byId;

  /**
   * constructor.
   */


  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(foundationZenTokenKey);
    PublicMethod.printAddress(zenTokenOwnerKey);    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    channelSolidity1 = ManagedChannelBuilder.forTarget(soliditynode1)
        .usePlaintext()
        .build();
    blockingStubSolidity1 = WalletSolidityGrpc.newBlockingStub(channelSolidity1);

    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Args.setFullNodeAllowShieldedTransaction(true);


  }

  @Test(enabled = false, description = "Test get new shield address ", groups = {"daily", "shield"})
  public void test1Shield2ShieldTransaction() {
    sendShieldAddress = blockingStubFull.getNewShieldedAddress(GrpcAPI.EmptyMessage.newBuilder()
        .build()).getPaymentAddress();
    memo = "Shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(zenTokenOwnerAddress, sendTokenAmount, null,
        null, shieldOutList, null, 0, zenTokenOwnerKey, blockingStubFull));
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
            PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress, zenTokenOwnerKey, blockingStubFull);    if (channelSolidity1 != null) {
      channelSolidity1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}