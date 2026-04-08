package stest.tron.wallet.fulltest;

import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class CreateAsset extends TronBaseTest {

  private static final String tooLongDescription =
      "1qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqa"
          + "zxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvq"
          + "azxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcxswedcv";
  private static final String tooLongUrl =
      "qaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqasw1qazxswedcvqazxswedcv"
          + "qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedc"
          + "vqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqaz"
          + "xswedcvqazxswedcvqazxswedcwedcv";
  private static final long now = System.currentTimeMillis();
  private static final long sendAmount = 1025000000L;
  private static final long netCostMeasure = 200L;
  private static String name = "c_" + Long.toString(now);
  //testng001、testng002、testng003、testng004 only for test, do not worry
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final String testKey003 =
      "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  long totalSupply = now;
  Long freeAssetNetLimit = 30000L;
  Long publicFreeAssetNetLimit = 30000L;
  String description = "f";
  String url = "h";
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset016Address = ecKey1.getAddress();
  String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    logger.info(testKeyForAssetIssue016);
    logger.info(transferAssetCreateKey);  }

  //@Test(enabled = false, groups = {"full"})
  @Test(enabled = false, threadPoolSize = 20, invocationCount = 20, groups = {"full"})
  public void createAssetissue() throws InterruptedException {

    ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset016Address = ecKey1.getAddress();
  String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Account fromAccountInfo = PublicMethod.queryAccount(testKey002, blockingStubFull);
  //Assert.assertTrue(PublicMethod.freezeBalance(foundationAddress,100000000, 3, testKey002,
    //  blockingStubFull));

    Integer i = 0;
  //GrpcAPI.AssetIssueList assetIssueList = blockingStubFull
    // .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
  //logger.info(Integer.toString(assetIssueList.getAssetIssueCount()));
    Boolean ret = false;
    Boolean transRet = false;
    Boolean updateRet = false;
    Boolean participateRet = false;
    Random rand = new Random();
    Integer randNum;

    while (fromAccountInfo.getBalance() > 1025000000) {
      randNum = rand.nextInt(4);      fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
          .get(randNum);      PublicMethod
          .sendcoin(asset016Address, sendAmount, foundationAddress, testKey002, blockingStubFull);
  Long start = System.currentTimeMillis() + 2000;
  Long end = System.currentTimeMillis() + 1000000000;
      name = "c_" + Long.toString(System.currentTimeMillis());
      totalSupply = now;

      ret = PublicMethod
          .createAssetIssue(asset016Address, name, totalSupply, 1, 1, start, end, 1, description,
              url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L, testKeyForAssetIssue016,
              blockingStubFull);

      if (ret) {
        updateRet = PublicMethod
            .updateAsset(asset016Address, tooLongDescription.getBytes(), tooLongUrl.getBytes(),
                4000L, 4000L,
                testKeyForAssetIssue016, blockingStubFull);
        if (updateRet) {
          logger.info("update succesfully");
        }
        logger.info(Integer.toString(i++));
  //assetIssueList = blockingStubFull
        //   .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
  //logger.info("assetissue num is " + Integer.toString(assetIssueList.getAssetIssueCount()));
        try {
          randNum = rand.nextInt(10000) + 3000;
          Thread.sleep(randNum);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        transRet = PublicMethod.transferAsset(toAddress, name.getBytes(),
            1L, asset016Address, testKeyForAssetIssue016, blockingStubFull);
        participateRet = PublicMethod
            .participateAssetIssue(asset016Address, name.getBytes(), 1L, toAddress, testKey003,
                blockingStubFull);
        if (participateRet) {
          logger.info("participate success");
        }
        logger.info(testKeyForAssetIssue016);
        if (transRet) {
          logger.info("transfer success");
        }

      }
      ecKey1 = new ECKey(Utils.getRandom());
      asset016Address = ecKey1.getAddress();
      testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
      fromAccountInfo = PublicMethod.queryAccount(testKey002, blockingStubFull);
      ret = false;
      updateRet = false;
      participateRet = false;
      if (channelFull != null) {        try {
          //randNum = rand.nextInt(10000) + 3000;
          Thread.sleep(6000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

      }


    }
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    /*    if (channelFull != null) {    }*/
  }
}


