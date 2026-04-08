package stest.tron.wallet.dailybuild.transaction;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.TransactionIdList;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Sha256Hash;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.Utils;


@Slf4j

public class TransactionPendingQuery001 extends TronBaseTest {
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey1.getAddress();
  final String receiverKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  String txid = null;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    initPbftChannel();
  }

  @Test(enabled = true, description = "Test get pending size", groups = {"daily"})
  public void test01GetPendingSize() {
    long pendingSizeInFullNode = 0;
  int retryTimes = 100;
    while (pendingSizeInFullNode == 0 && retryTimes-- > 0) {
      PublicMethod.sendcoin(receiverAddress,1L,foundationAddress,foundationKey,blockingStubFull);
      if (retryTimes % 5 == 0) {
        pendingSizeInFullNode = blockingStubFull
            .getPendingSize(EmptyMessage.newBuilder().build()).getNum();
      }
    }
    Assert.assertNotEquals(pendingSizeInFullNode,0);
  }


  @Test(enabled = true, description = "Test get pending transaction list", groups = {"daily"})
  public void test02GetPendingTransactionList() {
    int retryTimes = 100;
    TransactionIdList transactionList = blockingStubFull
        .getTransactionListFromPending(EmptyMessage.newBuilder().build());
    while (transactionList.getTxIdCount() == 0 && retryTimes-- > 0) {
      PublicMethod.sendcoin(receiverAddress,1L,foundationAddress,foundationKey,blockingStubFull);
      if (retryTimes % 5 == 0) {
        transactionList = blockingStubFull
            .getTransactionListFromPending(EmptyMessage.newBuilder().build());
      }
    }
    Assert.assertNotEquals(transactionList.getTxIdCount(),0);

    txid = transactionList.getTxId(0);

    logger.info("txid:" + txid);

  }


  @Test(enabled = true, description = "Test transaction from pending", groups = {"daily"})
  public void test03GetTransactionFromPending() {
    Transaction transaction = PublicMethod.getTransactionFromPending(txid,blockingStubFull).get();
    Assert.assertEquals(ByteArray.toHexString(Sha256Hash
        .hash(true, transaction.getRawData().toByteArray())),txid);
  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.unFreezeBalance(receiverAddress, receiverKey, 1, receiverAddress,
            blockingStubFull);
    PublicMethod.freeResource(receiverAddress, receiverKey, foundationAddress, blockingStubFull);
  }

}
