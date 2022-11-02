package stest.tron.wallet.dailybuild.transaction;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class WalletTestTransactionMemo {
  private static final long sendAmount = 10000000L;
  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] memoAddress = ecKey1.getAddress();
  String memoKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(memoKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(memoAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    

  }

  @Test(enabled = true, description = "Transaction with memo should be pay memo fee")
  public void test01TransactionMemo() {
    String memo = "PAY FEE";
    Account account = PublicMethed.queryAccount(memoAddress,blockingStubFull);


    final Long beforeBalance = System.currentTimeMillis();

    String txid = PublicMethed.sendcoinWithMemoGetTransactionId(foundationAddress,1L,memo,
        memoAddress,memoKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ;
    String dataMemo = PublicMethed.getTransactionById(txid,blockingStubFull)
        .get().getRawData().getData().toStringUtf8();
    logger.info(dataMemo);
    Assert.assertEquals(dataMemo,memo);


  }
  

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(memoAddress, memoKey, foundationAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


