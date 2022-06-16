package stest.tron.wallet.leveldb;


import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol;
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


public class AccountTest {
  private static final String PATH = "/Users/wangzihe/Downloads/output-directory-test/database/account";
  private static final Charset CHARSET = Charset.forName("utf-8");
  private static final File FILE = new File(PATH);



  @Test
  public void readFromSnapshotTest() {
    DBFactory factory = new Iq80DBFactory();
    File file = new File(PATH);
    Options options = new Options();
    DB db = null;
    try {
      db = factory.open(file, options);

      Snapshot snapshot = db.getSnapshot();

      ReadOptions readOptions = new ReadOptions();

      readOptions.fillCache(false);
      readOptions.snapshot(snapshot);

      DBIterator it = db.iterator(readOptions);
      while (it.hasNext()) {
        Map.Entry<byte[], byte[]> entry = (Map.Entry<byte[], byte[]>) it
            .next();
        String key = ByteArray.toHexString(ByteString.copyFrom(entry.getKey()).toByteArray());
        String value =  Protocol.Account.parseFrom(entry.getValue()).toString();
//        System.out.println(key + "  :  " + value);
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


}
