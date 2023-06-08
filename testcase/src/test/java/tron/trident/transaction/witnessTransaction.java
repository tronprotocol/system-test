package tron.trident.transaction;

import org.testng.annotations.Test;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.proto.Chain.Transaction;
import org.tron.trident.proto.Response;
import tron.trident.utils.TestBase;

import java.util.HashMap;


public class witnessTransaction extends TestBase {




  @Test(enabled = true)
  public void test01UpdateBroker() throws Exception {
    Response.TransactionExtention transactionExtention = wrapper.updateBrokerage(owner,4);
    Transaction transaction = wrapper.signTransaction(transactionExtention);
    ;
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Update broker id:" + broadcast);
  }


  @Test
  public void test02CreateWitness() throws Exception {
    wrapper = new ApiWrapper("grpc.nile.trongrid.io:50051",
        "grpc.nile.trongrid.io:50061", "1523fe602624680e4457691929d24d56336d6ad065c8bb0d2ea617f720b72dc7");
    Response.TransactionExtention transactionExtention = wrapper.createWitness("TVg4bhV72tfHy5mmnuRQGwsJXxL7RgNPkD","trident create witness");
    Transaction transaction = wrapper.signTransaction(transactionExtention);
    ;
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Create witness id:" + broadcast);

  }


  @Test
  public void test03UpdateWitness() throws Exception {
    wrapper = new ApiWrapper("grpc.nile.trongrid.io:50051",
        "grpc.nile.trongrid.io:50061", "1523fe602624680e4457691929d24d56336d6ad065c8bb0d2ea617f720b72dc7");
    Response.TransactionExtention transactionExtention = wrapper.updateWitness("TVg4bhV72tfHy5mmnuRQGwsJXxL7RgNPkD","update trident create witness");
    Transaction transaction = wrapper.signTransaction(transactionExtention);
    ;
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Update witness id:" + broadcast);

  }





  @Test
  public void test04WithdrawBalance() throws Exception {
    wrapper = new ApiWrapper("grpc.nile.trongrid.io:50051",
        "grpc.nile.trongrid.io:50061", "7400E3D0727F8A61041A8E8BF86599FE5597CE19DE451E59AED07D60967A5E25");
    Response.TransactionExtention transactionExtention = wrapper.withdrawBalance("TKpJUP4CCymphdug1XmGzDGDmGXZjLyf29");
    Transaction transaction = wrapper.signTransaction(transactionExtention);
    ;
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Withdraw balance id:" + broadcast);

  }


  @Test
  public void test05Proposal() throws Exception {
    Integer proposalCount = wrapper.listProposals().getProposalsCount();

    System.out.println("proposal count: " + proposalCount);
    HashMap<Long,Long> hashMap = new HashMap<>();
    hashMap.put(0L,1000000L);
    Response.TransactionExtention transactionExtention = wrapper.proposalCreate(owner,hashMap);
    Transaction transaction = wrapper.signTransaction(transactionExtention);
    ;
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Create proposal id:" + broadcast);




    transactionExtention = wrapper.approveProposal(owner,proposalCount,true);
    transaction = wrapper.signTransaction(transactionExtention);
    ;
    broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Approval proposal id:" + broadcast);


    transactionExtention = wrapper.deleteProposal(owner,proposalCount);
    transaction = wrapper.signTransaction(transactionExtention);
    ;
    broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Delete proposal id:" + broadcast);


  }









}
