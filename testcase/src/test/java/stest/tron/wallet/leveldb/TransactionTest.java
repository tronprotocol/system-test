package stest.tron.wallet.leveldb;


import com.google.protobuf.ByteString;
import org.iq80.leveldb.*;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionRet;
import stest.tron.wallet.common.client.utils.ByteArray;

import java.io.File;
import java.io.IOException;
import java.util.Map;


public class TransactionTest {
  private static final String PATH = "/Users/sophiawang/Documents/需求/java-tron/读取account库账户标志/output-directory-test/database/transactionRetStore";
//  private static final String PATH = "/Users/sophiawang/Documents/需求/java-tron/读取account库账户标志/output-directory-test/database/trans-cache";
//  private static final String PATH = "/Users/sophiawang/Documents/需求/java-tron/读取account库账户标志/output-directory-test/database/transactionRetStore";
  private static final String TRANS_INDEX_PATH = "/Users/sophiawang/Documents/需求/java-tron/读取account库账户标志/output-directory-test/database/trans";
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
  public void getTransactioninfoById() {
    try {
      byte[] blockHash = getTransHash("004e7d393fd5d1008747b59891a269699ebb3826445dbb0c256dd5b80a06eb7f");
      TransactionRet trans = TransactionRet.parseFrom(db.get(blockHash));
      System.out.println(trans.toString());
//      return account;
    } catch (IOException e) {
      e.printStackTrace();
//      return null;
    }
  }

  public static byte[] getTransHash(String txid) {
    DBFactory factory = new Iq80DBFactory();
    File file = new File(TRANS_INDEX_PATH);
    Options options = new Options();
    try {
      DB db = factory.open(file, options);
      byte[] hash = db.get(ByteArray.fromHexString(txid));
      db.close();
      return hash;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }


  @Test
  public void getAllTrans() {
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
        String value = ByteArray.toHexString(ByteString.copyFrom(entry.getValue()).toByteArray());
//        String value =  Protocol.TransactionRet.parseFrom(entry.getValue()).toString();
        System.out.println(key + "  :  " + value);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (db != null) {
        try {
          db.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
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
