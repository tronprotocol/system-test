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
  //if send coin with value 10000000000L, then the script's max byte size is 511790
  private int maxScriptByteSize = 511790;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(memoKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(memoAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    

  }

  @Test(enabled = true, description = "Transaction with memo should be pay memo fee")
  public void test01TransactionMemo() {
    Long memoFee = PublicMethed.getProposalMemoFee(blockingStubFull);
    logger.info("MemoFee:" + memoFee);
    String memo = "PAY FEE";
    Long sendAmount = 1L;
    Account account = PublicMethed.queryAccount(memoAddress,blockingStubFull);


    final Long beforeBalance = account.getBalance();

    String txid = PublicMethed.sendcoinWithMemoGetTransactionId(foundationAddress,sendAmount,memo,
        memoAddress,memoKey,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ;
    String dataMemo = PublicMethed.getTransactionById(txid,blockingStubFull)
        .get().getRawData().getData().toStringUtf8();
    logger.info(dataMemo);
    Assert.assertEquals(dataMemo,memo);

    account = PublicMethed.queryAccount(memoAddress,blockingStubFull);
    final Long afterBalance = account.getBalance();
    Assert.assertEquals(beforeBalance - afterBalance, sendAmount + memoFee);

    Long transactionFee = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getFee();
    Long freeNet = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getReceipt().getNetUsage();
    Assert.assertEquals(transactionFee,memoFee);


    String txidWithNoMemo = PublicMethed.sendcoinWithMemoGetTransactionId(foundationAddress,sendAmount,null,
        memoAddress,memoKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long noMemoFreeNet = PublicMethed.getTransactionInfoById(txidWithNoMemo,blockingStubFull).get().getReceipt().getNetUsage();
    logger.info("freeNet:" + freeNet);
    logger.info("noMemoFreeNet:" + noMemoFreeNet);
    Assert.assertTrue(noMemoFreeNet + 9 == freeNet);





  }

  @Test(enabled = true, description = "transaction's max size is 500*1024")
  public void test02TransactionMaxSize() {
    Assert.assertTrue(PublicMethed.sendcoinWithScript(memoAddress, 10000000000L,
        foundationAddress, foundationKey, maxScriptByteSize, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Code = TOO_BIG_TRANSACTION_ERROR; Message = Transaction size is too big
    Assert.assertFalse(PublicMethed.sendcoinWithScript(memoAddress, 10000000000L,
        foundationAddress, foundationKey, maxScriptByteSize + 1, blockingStubFull));
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


