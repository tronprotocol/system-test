package stest.tron.wallet.leveldb;


import com.google.protobuf.ByteString;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.Snapshot;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethed;


public class AssetIssueTest {
  private static String database = Configuration.getByPath("testng.conf")
      .getString("leveldbParams.databasePath");
  private static String ACCOUNT_ASSET_PATH = database + "/account-asset";  //地址+asseetid : 数量
  private static String PATH = database + "/asset-issue"; // discard
  private static String ASSET_ISSUE_V2_PATH = database + "/asset-issue-v2";   //trc10资产的信息
  private DB db;

  /**
   * constructor.
   */
  @BeforeClass
  public void initDb() {
    DBFactory factory = new Iq80DBFactory();
    File file = new File(PATH);
    Options options = new Options();
    try {
      db = factory.open(file, options);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void readFromSnapshotTest() {
    try {
      Snapshot snapshot = db.getSnapshot();
      ReadOptions readOptions = new ReadOptions();
      readOptions.fillCache(false);
      readOptions.snapshot(snapshot);
      DBIterator it = db.iterator(readOptions);
      while (it.hasNext()) {
        Map.Entry<byte[], byte[]> entry = (Map.Entry<byte[], byte[]>) it
            .next();
        String key = ByteArray.toHexString(ByteString.copyFrom(entry.getKey()).toByteArray());
        //String value = ByteArray.toHexString(ByteString.copyFrom(entry.getValue()).toByteArray());
        String value = AssetIssueContract.parseFrom(entry.getValue()).getId();
        //String value = AccountAsset.parseFrom(ByteString.copyFrom(entry.getValue())).toString();
        System.out.println(key + "  :  " + value);
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if (db != null) {
        try {
          db.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }


  /**
   * from account-asset.
   */
  //@Test
  public int getAddressAssetCount(String address, String assetId) {
    DBFactory factory = new Iq80DBFactory();
    File file = new File(ACCOUNT_ASSET_PATH);
    Options options = new Options();
    try {
      DB db = factory.open(file, options);
      //String address = "TAsimLx1ZWc6epr5FjitmktrDqhJY2AZ5R";
      //String assetId = "1000005";
      String addressHex = ByteArray.toHexString(PublicMethed.decode58Check(address));
      String assetIdHex = ByteArray.toHexString(assetId.getBytes());
      String key = addressHex + assetIdHex;
      //System.out.println(key);
      int amount = ByteArray.toInt(db.get(ByteArray.fromHexString(key)));
      System.out.println(amount);
      db.close();
      return amount;
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * read from  asset-issue-v2.
   */
  public AssetIssueContract getAssetIssueById(String assetId) {
    //String assetId = "1000005";
    DBFactory factory = new Iq80DBFactory();
    File file = new File(ASSET_ISSUE_V2_PATH);
    Options options = new Options();
    try {
      DB db = factory.open(file, options);
      AssetIssueContract assetIssue =  AssetIssueContract.parseFrom(db.get(assetId.getBytes()));
      System.out.println(assetIssue.toString());
      db.close();
      return assetIssue;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * .
   */
  @AfterClass
  public void destroyDb() {
    if (db != null) {
      try {
        db.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


}
