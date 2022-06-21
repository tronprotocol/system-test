package stest.tron.wallet.leveldb;

import com.google.protobuf.ByteString;
import java.io.File;
import java.io.IOException;
import java.util.List;
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
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionRet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.Sha256Sm3Hash;


public class TransactionTest {
  private static String database = Configuration.getByPath("testng.conf")
      .getString("leveldbParams.databasePath");
  private static String PATH = database + "/transactionRetStore";
  private static String TRANS_INDEX_PATH = database + "/trans";
  private static String BLOCK_INDEX_PATH = database + "/block-index";
  private static String BLOCK_PATH = database + "/block";
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
  public Transaction getTransactionById(String txid) {
    try {
      byte[] transHash = getTransHash(txid);
      TransactionRet trans = TransactionRet.parseFrom(db.get(transHash));
      long blockNumber = trans.getBlockNumber();
      Block block = getBlockByNumber(blockNumber);
      List<Transaction> transList = block.getTransactionsList();
      for (Transaction tem: transList) {
        String txID = ByteArray.toHexString(Sha256Sm3Hash.hash(tem.getRawData().toByteArray()));
        if (txid.equals(txID)) {
          return tem;
        }
      }
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */
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

  /**
   * constructor.
   */
  public TransactionRet getTransactionInfoById(String txid) {
    try {
      byte[] transHash = getTransHash(txid);
      TransactionRet trans = TransactionRet.parseFrom(db.get(transHash));
      System.out.println(trans.toString());
      return trans;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */
  public Protocol.Block getBlockByNumber(long number) {
    DBFactory factory = new Iq80DBFactory();
    File file = new File(BLOCK_PATH);
    Options options = new Options();
    try {
      DB db = factory.open(file, options);
      byte[] blockHash = getBlockHash(number);
      Block block = Block.parseFrom(db.get(blockHash));
      db.close();
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
        //String value =  Protocol.TransactionRet.parseFrom(entry.getValue()).toString();
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
