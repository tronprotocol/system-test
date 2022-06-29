package stest.tron.wallet.common.client.utils;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import stest.tron.wallet.common.client.Configuration;

public class MongoBase {

  public static MongoDatabase mongoDatabase;

  private String mongoNode =
      Configuration.getByPath("testng.conf").getStringList("mongonode.ip.list").get(0);
  private MongoClient mongoClient;

  /** constructor. */
  @BeforeSuite(enabled = true, description = "Create new mongo client")
  public void createMongoDBConnection() throws Exception {
    try {
      MongoCredential credential =
          MongoCredential.createCredential("root", "dailybuild", "123456".toCharArray());
      mongoClient = new MongoClient(new ServerAddress(mongoNode), Arrays.asList(credential));
      mongoDatabase = mongoClient.getDatabase("dailybuild");
      System.out.println("Connect to database successfully");
      int times = 7;
      Assert.assertTrue(backupCollection("block", times));
      Assert.assertTrue(backupCollection("contractevent", times));
      Assert.assertTrue(backupCollection("solidity", times));
      Assert.assertTrue(backupCollection("solidityevent", times));
      Assert.assertTrue(backupCollection("transaction", times));
      System.out.println("Backup collection  successfully");
      /* mongoDatabase.getCollection("block").drop();
      mongoDatabase.getCollection("contractevent").drop();
      mongoDatabase.getCollection("solidity").drop();
      mongoDatabase.getCollection("solidityevent").drop();
      mongoDatabase.getCollection("transaction").drop();*/

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean backupCollection(String collectionName, Integer times) {
    for (int i = times; i >= 0; i--) {
      if (i == times) {
        renameCollection(collectionName + "_" + i, null);
        continue;
      }

      if (i == 0) {
        renameCollection(collectionName, collectionName + "_1");
        break;
      }

      renameCollection(collectionName + "_" + i, collectionName + "_" + (i + 1));
    }
    return true;
  }

  private DBCollection renameCollection(String oldCollection, String newCollection) {
    System.out.println("111:" + oldCollection);
    System.out.println("2222:" + newCollection);
    DB db = mongoClient.getDB("dailybuild");

    if (!db.collectionExists(oldCollection)) {
      return null;
    }
    if (db.collectionExists(newCollection)) {
      mongoDatabase.getCollection(newCollection).drop();
    }
    if (null == newCollection) {
      mongoDatabase.getCollection(oldCollection).drop();
      return null;
    }
    DBCollection dbCollection = db.getCollection(oldCollection);
    return dbCollection.rename(newCollection, true);
  }
}
