package tron.trident.stakeV2;

import org.testng.annotations.Test;
import org.tron.trident.core.utils.ByteArray;
import org.tron.trident.core.utils.Sha256Hash;
import org.tron.trident.proto.Chain.Transaction;
import org.tron.trident.proto.Response;
import tron.trident.utils.TestBase;


public class TestNewStake extends TestBase {

  @Test(enabled = true)
  public void test01FreezeBalanceV2() throws Exception {
    Response.TransactionExtention transactionExtention = wrapper.freezeBalanceV2(owner,100000000L,resourceCode);

    Transaction transaction = wrapper.signTransaction(transactionExtention);
    ;
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("freeze:" + broadcast);
    Thread.sleep(3000);
  }


  @Test(enabled = true)
  public void test02UnFreezeBalanceV2() throws Exception {
    Response.TransactionExtention transactionExtention = wrapper.unfreezeBalanceV2(owner,1000000L,resourceCode);

    Transaction transaction = wrapper.signTransaction(transactionExtention);
    ;
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("unfreeze:" + broadcast);
  }


  @Test(enabled = true)
  public void test03DelegateResource() throws Exception {
    long canDelegatedMaxSize = wrapper.getCanDelegatedMaxSize(owner, resourceCode);
    Response.TransactionExtention transactionExtention = wrapper.delegateResource(owner,2000000L,resourceCode,receiverAddress,false);

    Transaction transaction = wrapper.signTransaction(transactionExtention);
    ;
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("delegate resource :" + broadcast);
  }


  @Test(enabled = true)
  public void test04unDelegateResource() throws Exception {
    Response.TransactionExtention transactionExtention = wrapper.undelegateResource(owner,1000000L,resourceCode,receiverAddress);

    Transaction transaction = wrapper.signTransaction(transactionExtention);
    System.out.println(ByteArray.toHexString(Sha256Hash.hash(true, transaction.getRawData().toByteArray())));
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("unDelegate resource :" + broadcast);
  }


  @Test(enabled = true)
  public void test05WithdrawUnfreezeExpireAmount() throws Exception {

    Long canWithdrawUnfreezeAmount =  wrapper.getCanWithdrawUnfreezeAmount(owner);
    Long canUnfreezeCount = wrapper.getAvailableUnfreezeCount(owner);
    System.out.println("canWithdrawUnfreezeAmount " + canWithdrawUnfreezeAmount);
    System.out.println("canUnfreezeCount " + canUnfreezeCount);
    Response.TransactionExtention transactionExtention = wrapper.withdrawExpireUnfreeze(owner);

    Transaction transaction = wrapper.signTransaction(transactionExtention);
    ;
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("withdraw unfreeze expire amount:" + broadcast);


  }


  @Test(enabled = false)
  public void test06GetDelegateResourceV2() throws Exception {
    //Thread.sleep(60000);
    Response.DelegatedResourceAccountIndex delegatedResourceAccountIndex = wrapper.getDelegatedResourceAccountIndexV2(owner);
    Response.DelegatedResourceList delegatedResourceList = wrapper.getDelegatedResourceV2(owner,receiverAddress);
    int i = 0;


  }





}
