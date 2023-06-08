package tron.trident.transaction;

import org.testng.annotations.Test;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.proto.Chain.Transaction;
import org.tron.trident.proto.Response;
import tron.trident.utils.TestBase;

import java.util.HashMap;


public class trc10Transaction extends TestBase {

  @Test(enabled = true)
  public void test01CreateTrc10Token() throws Exception {
    HashMap<String, String> frozenSupply = new HashMap<>();
    frozenSupply.put("1","1");
    frozenSupply.put("2","2");
    frozenSupply.put("3","3");
    String tokenName = "trident-" + System.currentTimeMillis();
    Response.TransactionExtention transactionExtention = wrapper
        .createAssetIssue(owner, tokenName, tokenName, 10000000000000000L,1,1,
            System.currentTimeMillis() + 6000L, System.currentTimeMillis() + 9000000000000L,
            "url",1000L,2000L,6,frozenSupply,"description");

    Transaction transaction = wrapper.signTransaction(transactionExtention);
    ;
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Create trc10 token :" + broadcast);
  }



  @Test(enabled = true)
  public void test02TransferTrc10Token() throws Exception {
    Response.TransactionExtention transactionExtention = wrapper
        .transferTrc10(owner,receiverAddress,trc10TokenId,1L);

    Transaction transaction = wrapper.signTransaction(transactionExtention);


    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Transaction trc10 token :" + broadcast);
  }


  @Test(enabled = true)
  public void test03participateTrc10Token() throws Exception {
    wrapper = new ApiWrapper("grpc.nile.trongrid.io:50051",
        "grpc.nile.trongrid.io:50061", xiaofeiKey);

    String tokenOwner = owner;
    Response.TransactionExtention transactionExtention = wrapper
        .participateAssetIssue(tokenOwner,xiaofeiAddress,String.valueOf(trc10TokenId),1L);

    Transaction transaction = wrapper.signTransaction(transactionExtention);


    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Participate trc10 token :" + broadcast);

    wrapper = new ApiWrapper("grpc.nile.trongrid.io:50051",
        "grpc.nile.trongrid.io:50061", ownerKey);
  }



  @Test(enabled = true)
  public void test04UpdateTrc10Token() throws Exception {
    Response.TransactionExtention transactionExtention = wrapper
        .updateAsset(owner,"trident","tridenturl",20,1);

    Transaction transaction = wrapper.signTransaction(transactionExtention);


    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Update trc10 token :" + broadcast);
  }


  @Test(enabled = true)
  public void test05UnfreezeAssetTrc10Token() throws Exception {
    Response.TransactionExtention transactionExtention = wrapper
        .unfreezeAsset(owner);

    Transaction transaction = wrapper.signTransaction(transactionExtention);


    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Unfreeze trc10 token :" + broadcast);
  }

















}
