package stest.tron.wallet.dailybuild.transaction;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestTransactionMemo extends TronBaseTest {
  private static final long sendAmount = 10000000L;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] memoAddress = ecKey1.getAddress();
  String memoKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  //if send coin with value 10000000000L, then the script's max byte size is 511790
  private int maxScriptByteSize = 511790 - 128; // 475

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(memoKey);    Assert.assertTrue(PublicMethod.sendcoin(memoAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    

  }

  @Test(enabled = false, description = "Transaction with memo should be pay memo fee", groups = {"daily"})
  public void test01TransactionMemo() {
    Long memoFee = PublicMethod.getProposalMemoFee(blockingStubFull);
    logger.info("MemoFee:" + memoFee);
  String memo = "PAY FEE";
  Long sendAmount = 1L;
    Account account = PublicMethod.queryAccount(memoAddress,blockingStubFull);
  final Long beforeBalance = account.getBalance();
  String txid = PublicMethod.sendcoinWithMemoGetTransactionId(foundationAddress,sendAmount,memo,
        memoAddress,memoKey,blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ;
  String dataMemo = PublicMethod.getTransactionById(txid,blockingStubFull)
        .get().getRawData().getData().toStringUtf8();
    logger.info(dataMemo);
    Assert.assertEquals(dataMemo,memo);

    account = PublicMethod.queryAccount(memoAddress,blockingStubFull);
  final Long afterBalance = account.getBalance();
    Assert.assertEquals(beforeBalance - afterBalance, sendAmount + memoFee);
  Long transactionFee = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get().getFee();
  Long freeNet = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get().getReceipt().getNetUsage();
    Assert.assertEquals(transactionFee,memoFee);
  String txidWithNoMemo = PublicMethod.sendcoinWithMemoGetTransactionId(foundationAddress,sendAmount,null,
        memoAddress,memoKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long noMemoFreeNet = PublicMethod.getTransactionInfoById(txidWithNoMemo,blockingStubFull).get().getReceipt().getNetUsage();
    logger.info("freeNet:" + freeNet);
    logger.info("noMemoFreeNet:" + noMemoFreeNet);
    Assert.assertTrue(noMemoFreeNet + 9 == freeNet);


  }

  @Test(enabled = false, description = "transaction's max size is 500*1024", groups = {"daily"})
  public void test02TransactionMaxSize() {
    Assert.assertTrue(PublicMethod.sendcoinWithScript(memoAddress, 10000000000L,
        foundationAddress, foundationKey, maxScriptByteSize, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Code = TOO_BIG_TRANSACTION_ERROR; Message = Transaction size is too big
    Assert.assertFalse(PublicMethod.sendcoinWithScript(memoAddress, 10000000000L,
        foundationAddress, foundationKey, maxScriptByteSize + 1, blockingStubFull));
  }

  @Test(enabled = true, description = "transaction's max size is 1500 when create a new account (proposal 82)", groups = {"daily"})
  public void test02TransactionMaxSizeCreateNewAccount() {
    maxScriptByteSize = 1357;
  ECKey key = new ECKey(Utils.getRandom());
    memoAddress = key.getAddress();
    PublicMethod.printAddress(ByteArray.toHexString(key.getPrivKeyBytes()));
    Assert.assertTrue(PublicMethod.sendcoinWithScript(memoAddress, 10000000000L,
        foundationAddress, foundationKey, maxScriptByteSize, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  ECKey key1 = new ECKey(Utils.getRandom());
    memoAddress = key1.getAddress();
    PublicMethod.printAddress(ByteArray.toHexString(key1.getPrivKeyBytes()));
  //Code = TOO_BIG_TRANSACTION_ERROR; Message = Transaction size is too big
    Assert.assertFalse(PublicMethod.sendcoinWithScript(memoAddress, 10000000000L,
        foundationAddress, foundationKey, maxScriptByteSize + 1, blockingStubFull));
  }


  

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(memoAddress, memoKey, foundationAddress, blockingStubFull);  }
}


