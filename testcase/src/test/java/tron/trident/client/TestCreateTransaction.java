package tron.trident.client;

import com.beust.ah.A;
import org.tron.trident.proto.Response;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.Utils;
import tron.trident.utils.TestBase;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.Constant;
import org.tron.trident.core.exceptions.IllegalException;
import org.tron.trident.proto.Chain;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TestCreateTransaction extends TestBase {

  @Test(enabled = true, description = "test broadcastTransaction failed message issue-133")
  public void test01broadcastTransactionFailedMessage() throws IllegalException {
    ECKey ec = new ECKey(Utils.getRandom());
    byte[] toAddress = ec.getAddress();
    Response.TransactionExtention trxExt = wrapper.transfer(owner, Base58.encode58Check(toAddress), 1);
    Chain.Transaction newTrx = trxExt.getTransaction().toBuilder().setRawData(trxExt.getTransaction().getRawData().toBuilder().setExpiration(System.currentTimeMillis() - 5000000L).build()).build();
    Chain.Transaction signedTrx = wrapper.signTransaction(newTrx);
    try {
      wrapper.broadcastTransaction(signedTrx);
    } catch (RuntimeException e) {
      String msg = e.getMessage();
      //test issue-133 switch case Breakdown
      logger.info(msg);
      Assert.assertTrue(msg.contains("TRANSACTION_EXPIRATION_ERROR"));
    }
  }

  @Test(enabled = true, description = "test createTransaction check result code issue-138")
  public void test02CreateTransactionCheckResult() throws IllegalException {
    ECKey ec = new ECKey(Utils.getRandom());
    byte[] toAddress = ec.getAddress();
    Response.TransactionExtention trxExt = wrapper.transfer(owner, Base58.encode58Check(toAddress), 1);
    Assert.assertTrue(trxExt.getResult().getResult());
    Assert.assertTrue(trxExt.getResult().getCode() == Response.TransactionReturn.response_code.SUCCESS);
  }

}
