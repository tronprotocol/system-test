package stest.tron.wallet.leveldb;


import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.utils.ByteArray;
import com.google.protobuf.ByteString;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.Snapshot;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.iq80.leveldb.impl.Iq80DBFactory;
import stest.tron.wallet.common.client.utils.PublicMethed;


public class AccountTest {
  private static final String PATH = "/Users/sophiawang/Documents/需求/java-tron/读取account库账户标志/output-directory-test/database/account";
  private static final Charset CHARSET = Charset.forName("utf-8");
  private static final File FILE = new File(PATH);
  private DB db;

  @BeforeClass
  public void initDb(){
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
        String value =  Account.parseFrom(entry.getValue()).toString();
        System.out.println(key + "  :  " + value);
      }
    } catch (IOException e) {
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
   * get account from db, address is base58
   * @param address
   * @return
   * @throws Exception
   */
  public Account getAccountFromDb(String address) {
    try {
      Account account = Account.parseFrom(db.get(PublicMethed.decode58Check(address)));
      System.out.println(account.toString());
      return account;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   *
   * @param address
   * @return
   */
  public long getAddressBalance(String address) {
    try {

      long balance = Account.parseFrom(db.get(PublicMethed.decode58Check(address))).getBalance();
      System.out.println(balance);
      return balance;
    } catch (IOException e) {
      e.printStackTrace();
      return 0;
    }
  }



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
