package tron.trident.transaction;

import com.google.protobuf.ByteString;
import org.testng.annotations.Test;
import org.tron.trident.api.GrpcAPI.TransactionIdList;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.proto.Chain.Transaction;
import org.tron.trident.proto.Common.Permission;
import org.tron.trident.proto.Contract.AccountPermissionUpdateContract;
import org.tron.trident.proto.Response;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.Utils;
import tron.trident.utils.TestBase;

import java.util.HashMap;
import java.util.Random;

import static org.tron.trident.core.Constant.FULLNODE_NILE;
import static org.tron.trident.core.Constant.FULLNODE_NILE_SOLIDITY;


public class trxTransaction extends TestBase {

  @Test(enabled = true)
  public void test01SendCoin() throws Exception {

    Response.TransactionExtention transactionExtention = wrapper.transfer(owner,receiverAddress,1L);

    Transaction transaction = wrapper.signTransaction(transactionExtention);
    ;
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("TransferContract:" + broadcast);
  }



  @Test(enabled = true)
  public void test02VoteWitness() throws Exception {
    HashMap<String, String> votes = new HashMap<>();
    votes.put("TZBZ3LN7GqbCKzdghSWE5jKjZPJdbniMYk","1");
    votes.put("TYbgswVSQLXDyk3sYsHmxREEBbcZv4XBdA","2");
    Response.TransactionExtention transactionExtention = wrapper.voteWitness(owner,votes);
    Transaction transaction = wrapper.signTransaction(transactionExtention);
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Vote witness :" + broadcast);
  }




  @Test(enabled = true)
  public void test03CreateAccount() throws Exception {
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAddress = Base58.encode58Check(newAccount.getAddress());
    Response.TransactionExtention transactionExtention = wrapper.createAccount(owner, newAddress);
    Transaction transaction = wrapper.signTransaction(transactionExtention);
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Create account :" + broadcast);
  }



  @Test(enabled = true)
  public void test04UpdateAccountAndSetAccountId() throws Exception {
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAddress = Base58.encode58Check(newAccount.getAddress());
    Response.TransactionExtention transactionExtention0 =  wrapper.transfer(owner, newAddress, 10000000L);
    Transaction transaction0 = wrapper.signTransaction(transactionExtention0);
    wrapper.broadcastTransaction(transaction0);
    Thread.sleep(30000);
    ApiWrapper wrapper = new ApiWrapper(FULLNODE_NILE, FULLNODE_NILE_SOLIDITY, ByteArray.toHexString(newAccount.getPrivateKey()));
    String accountName = "updateAccount" + System.currentTimeMillis();
    Response.TransactionExtention transactionExtention = wrapper.updateAccount(newAddress,accountName);
    Transaction transaction = wrapper.signTransaction(transactionExtention);
    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Update account :" + broadcast);

    String accountId = "setAccountId" + System.currentTimeMillis();
    Response.TransactionExtention transactionExtention2 = wrapper.setAccountId2(accountId, newAddress);
    Transaction transaction2 = wrapper.signTransaction(transactionExtention2);
    String broadcast2 = wrapper.broadcastTransaction(transaction2);
    System.out.println("Set account id:" + broadcast2);
  }


  @Test(enabled = true)
  public void test06UpdateAccountPermission() throws Exception {
    Permission ownerPermission = wrapper.getAccount(owner).getOwnerPermission();
    Permission witnessPermission = wrapper.getAccount(owner).getWitnessPermission();

    Permission activePermission = wrapper.getAccount(owner).getActivePermission(0);

    Permission newActivePermission = activePermission.toBuilder().setPermissionName("trident").build();


    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();

    builder.setOwner(ownerPermission);
    builder.setWitness(witnessPermission);
    builder.addActives(0, newActivePermission);
    ByteString bsAddress = wrapper.parseAddress(owner);
    builder.setOwnerAddress(bsAddress);
    AccountPermissionUpdateContract contract = builder.build();
    Response.TransactionExtention transactionExtention = wrapper.accountPermissionUpdate(contract);
    Transaction transaction = wrapper.signTransaction(transactionExtention);

    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("Update account permission :" + broadcast);
  }



  @Test(enabled = true)
  public void test07getBlockByNum() throws Exception {
    Response.BlockExtention blockExtention = wrapper.getBlockByNum(35874297);
    blockExtention.getBlockid();

  }


  @Test(enabled = true)
  public void test08GetTransactionListFromPending() throws Exception {
    System.out.println("Get pending size:" + wrapper.getPendingSize());
    TransactionIdList transactionList = wrapper.getTransactionListFromPending();
    if(transactionList.getTxIdCount() > 0) {
      System.out.println("transactionIdList : \n" + transactionList.getTxIdList());
      System.out.println("Get transaction from pending:" + wrapper.getTransactionFromPending(transactionList.getTxId(0)));
    }
  }

  @Test
  public void test09GetBurnTrx() throws Exception {
    wrapper.getBurnTRX();
    System.out.println("Get burn trx:" + wrapper.getBurnTRX());
  }


  @Test
  public void test10GetBlockById() throws Exception {

    System.out.println("Get block by id:" + wrapper.getBlockById("000000000231b4bbe82686f7851b5e582dd31231a905486ae3e0740dba5804f8"));
  }


  @Test
  public void test11GetNextMaintenanceTime() throws Exception {

    System.out.println("GetNextMaintenanceTime:" + wrapper.getNextMaintenanceTime());
  }


  @Test(enabled = false)
  public void test12GetBlockBalance() throws Exception {
    System.out.println("Get block balance:\n" + wrapper.getBlockBalance("000000000231b4bbe82686f7851b5e582dd31231a905486ae3e0740dba5804f8",36811963));
  }









}
