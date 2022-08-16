package stest.tron.wallet.common.client.utils;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;
//import org.iq80.leveldb.impl.Iq80DBFactory;
import static org.fusesource.leveldbjni.JniDBFactory.factory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionRet;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import stest.tron.wallet.common.client.Configuration;


public class LeveldbBase {
  private static String database = Configuration.getByPath("testng.conf")
      .getString("leveldbParams.databasePath");
  private static String ACCOUNT_PATH = database + "/account";
  private static String ACCOUNT_ASSET_PATH = database + "/account-asset";  //地址+asseetid : 数量
  private static String ASSET_ISSUE_V2_PATH = database + "/asset-issue-v2";   //trc10资产的信息
  private static String TRANS_INDEX_PATH = database + "/trans";
  private static String TRANS_RET_STORE_PATH = database + "/transactionRetStore";
  private static String BLOCK_INDEX_PATH = database + "/block-index";
  private static String BLOCK_PATH = database + "/block";
  private static String RECENT_PATH = database + "/recent-transaction";
//  private static String RECENT_PATH = database + "/trans-cache";
  private DB accountDb;
  private DB accountAssetDb;
  private DB assetIssueV2Db;
  private DB transIndexDb;
  private DB transRetStoreDb;
  private DB blockIndexDb;
  private DB blockDb;
  private DB recentDb;
  List<DB> dbList = new ArrayList<>();

  /**
   * .
   */
  @BeforeSuite(enabled = true, description = "init db")
  public void initDb() {
//    DBFactory factory = new Iq80DBFactory();
    File accountFile = new File(ACCOUNT_PATH);
    File accountAssetFile = new File(ACCOUNT_ASSET_PATH);
    File assetIssueV2File = new File(ASSET_ISSUE_V2_PATH);
    File transIndexFile = new File(TRANS_INDEX_PATH);
    File transRetStoreFile = new File(TRANS_RET_STORE_PATH);
    File blockIndexFile = new File(BLOCK_INDEX_PATH);
    File blockFile = new File(BLOCK_PATH);
    File recentFile = new File(RECENT_PATH);
    Options options = new Options();
    try {
      accountDb = factory.open(accountFile, options);
      accountAssetDb = factory.open(accountAssetFile, options);
      assetIssueV2Db = factory.open(assetIssueV2File, options);
      transIndexDb = factory.open(transIndexFile, options);
      transRetStoreDb = factory.open(transRetStoreFile, options);
      blockIndexDb = factory.open(blockIndexFile, options);
      blockDb = factory.open(blockFile, options);
      recentDb = factory.open(recentFile, options);
      dbList.add(accountDb);
      dbList.add(accountAssetDb);
      dbList.add(assetIssueV2Db);
      dbList.add(transIndexDb);
      dbList.add(transRetStoreDb);
      dbList.add(blockIndexDb);
      dbList.add(blockDb);
      dbList.add(recentDb);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }



  /**
   * get account from db, address is base58.
   */
  public Account getAccountFromDb(String address) {
    try {
      Account account = Account.parseFrom(accountDb.get(PublicMethed.decode58Check(address)));
      return account;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   *.
   */
  public long getAddressBalance(String address) {
    try {
      long balance = Account.parseFrom(accountDb.get(PublicMethed.decode58Check(address)))
          .getBalance();
      return balance;
    } catch (IOException e) {
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * from account-asset.
   */
  //@Test
  public int getAddressAssetCount(String address, String assetId) {
    try {
      //String address = "TAsimLx1ZWc6epr5FjitmktrDqhJY2AZ5R";
      //String assetId = "1000005";
      String addressHex = ByteArray.toHexString(PublicMethed.decode58Check(address));
      String assetIdHex = ByteArray.toHexString(assetId.getBytes());
      String key = addressHex + assetIdHex;
      int amount = ByteArray.toInt(accountAssetDb.get(ByteArray.fromHexString(key)));
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
    try {
      AssetIssueContract assetIssue =  AssetIssueContract
          .parseFrom(assetIssueV2Db.get(assetId.getBytes()));
      return assetIssue;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */
  public Transaction getTransactionById(String txid) {
    try {
      byte[] transHash = getTransHash(txid);
      TransactionRet trans = TransactionRet.parseFrom(transRetStoreDb.get(transHash));
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
  public byte[] getTransHash(String txid) {
    try {
      byte[] hash = transIndexDb.get(ByteArray.fromHexString(txid));
      return hash;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * constructor.
   */
  public TransactionInfo getTransactionInfoById(String txid) {
    try {
      byte[] transHash = getTransHash(txid);
      TransactionRet trans = TransactionRet.parseFrom(transRetStoreDb.get(transHash));

      for (TransactionInfo info : trans.getTransactioninfoList()) {
        if (txid.equalsIgnoreCase(ByteArray.toHexString(info.getId().toByteArray()))) {
          return info;
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
  public Protocol.Block getBlockByNumber(long number) {
    try {
      byte[] blockHash = getBlockHash(number);
      Block block = Block.parseFrom(blockDb.get(blockHash));
      return block;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */
  public byte[] getBlockHash(long number) {
    try {
      byte[] hash = blockIndexDb.get(ByteArray.fromLong(number));
      return hash;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * constructor.
   */
  public long getTotal() {
    ReadOptions readOptions = new ReadOptions().fillCache(false);
    try (DBIterator iterator = blockDb.iterator(readOptions)) {
      long total = 0;
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        total++;
      }
      return total - 1;
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * constructor.
   */
  public long getRecenTotal() {
    ReadOptions readOptions = new ReadOptions().fillCache(false);
    try (DBIterator iterator = recentDb.iterator(readOptions)) {
      long total = 0;
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        total++;
      }
      System.out.println("1111111111111111111111--total: " + total);
      return total - 1;
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("error 1111111111111111111111--total:");
      return 0;
    }
  }

  public Block getNowBlock() {
    long nowNum = getTotal();
    return getBlockByNumber(nowNum);
  }

  /**
   * .
   */
  @AfterSuite
  public void destroyDb() {
    for (DB tem: dbList) {
      if (tem != null) {
        try {
          tem.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

  }


}
