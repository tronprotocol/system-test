package tron.trident.utils;

import org.testng.annotations.BeforeSuite;
import org.tron.trident.core.ApiWrapper;

import static org.tron.trident.core.Constant.FULLNODE_NILE;
import static org.tron.trident.core.Constant.FULLNODE_NILE_SOLIDITY;

public class TestBase {
  public static ApiWrapper wrapper;
  public static Integer resourceCode = 1;
  public static String owner = "TP4ZaSZJ3Zu8eHYPo6iJUWKf1xzHrZQpzL";
  //public static String receiverAddress = "TKpJUP4CCymphdug1XmGzDGDmGXZjLyf29";
  //keys just for test
  public static String receiverAddress = "TH48niZfbwHMyqZwEB8wmHfzcvR8ZzJKC6";
  public static String ownerKey = "95385e000a2ea68fdc339bfa7d2157a3df1feb8306a6e84b80e5c6830a4db396";
  public static String testPriKey = "8132dd4fe6c1140d60fadd69eab3975831fe681e4b28705f78c89b94d44af2f4";
  public static String testAddress = "TH48niZfbwHMyqZwEB8wmHfzcvR8ZzJKC6";

  public static String usdtContract = "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf";
  public static Integer trc10TokenId = 1005057;


  @BeforeSuite
  public void beforeClass() {

    //solidity:"grpc.nile.trongrid.io:50061"

    wrapper = new ApiWrapper(FULLNODE_NILE,
        FULLNODE_NILE_SOLIDITY, ownerKey);

    //wrapper = ApiWrapper.ofNile(ownerKey);
  }
}
