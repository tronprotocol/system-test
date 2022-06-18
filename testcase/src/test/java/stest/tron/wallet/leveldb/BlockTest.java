package stest.tron.wallet.leveldb;


import com.google.protobuf.ByteString;
import org.iq80.leveldb.*;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.utils.ByteArray;

import java.io.File;
import java.io.IOException;
import java.util.Map;


public class BlockTest {
  private static final String PATH = "/Users/sophiawang/Documents/需求/java-tron/读取account库账户标志/output-directory-test/database/block";
  private static final String BLOCK_INDEX_PATH = "/Users/sophiawang/Documents/需求/java-tron/读取account库账户标志/output-directory-test/database/block-index";
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

  public Block getNowBlock(){
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
