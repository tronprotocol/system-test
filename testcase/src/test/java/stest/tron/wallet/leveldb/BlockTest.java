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
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;




public class BlockTest {
  private static String database = Configuration.getByPath("testng.conf")
      .getString("leveldbParams.databasePath");
  private static final String PATH = database + "/block";
  private static final String BLOCK_INDEX_PATH = database + "/block-index";
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

  /**
   * constructor.
   */
  public Block getBlockByNumber(long number) {
    try {
      byte[] blockHash = getBlockHash(number);
      Block block = Block.parseFrom(db.get(blockHash));
      System.out.println(block.toString());
      return block;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */
  public static byte[] getBlockHash(long number) {
    DBFactory factory = new Iq80DBFactory();
    File file = new File(BLOCK_INDEX_PATH);
    Options options = new Options();
    try {
      DB db = factory.open(file, options);
      byte[] hash = db.get(ByteArray.fromLong(number));
      db.close();
      return hash;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Block getNowBlock() {
    long nowNum = getTotal();
    return getBlockByNumber(nowNum);
  }

  @Test
  public void getAllBlock() {
    try {
      Snapshot snapshot = db.getSnapshot();
      ReadOptions readOptions = new ReadOptions().fillCache(false);
      readOptions.snapshot(snapshot);
      DBIterator it = db.iterator(readOptions);

      while (it.hasNext()) {
        Map.Entry<byte[], byte[]> entry = (Map.Entry<byte[], byte[]>) it
            .next();
        String key = ByteArray.toHexString(ByteString.copyFrom(entry.getKey()).toByteArray());
        String value =  Block.parseFrom(entry.getValue()).toString();
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

  /**
   * constructor.
   */
  public long getTotal() {
    ReadOptions readOptions = new ReadOptions().fillCache(false);
    try (DBIterator iterator = db.iterator(readOptions)) {
      long total = 0;
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        total++;
      }
      return total;
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * constructor.
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
