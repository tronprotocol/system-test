package stest.tron.wallet.common.client.utils;

import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import java.util.Arrays;
import org.testng.annotations.BeforeSuite;
import stest.tron.wallet.common.client.Configuration;

public class MongoBase {

  public static MongoDatabase mongoDatabase;


  private String mongoNode = Configuration.getByPath("testng.conf")
      .getStringList("mongonode.ip.list").get(0);



  /** constructor. */
  @BeforeSuite(enabled = true, description = "Create new mongo client")
  public void createMongoDBConnection() throws Exception {
    try{
      MongoCredential credential = MongoCredential.createCredential("root", "dailybuild", "123456".toCharArray());
      MongoClient mongoClient = new MongoClient(new ServerAddress(mongoNode), Arrays.asList(credential));
      mongoDatabase = mongoClient.getDatabase("dailybuild");
      System.out.println("Connect to database successfully");
      mongoDatabase.getCollection("block").drop();
      mongoDatabase.getCollection("contractevent").drop();
      mongoDatabase.getCollection("solidity").drop();
      mongoDatabase.getCollection("solidityevent").drop();
      mongoDatabase.getCollection("transaction").drop();
    }catch(Exception e){
      e.printStackTrace();
    }
  }

}
