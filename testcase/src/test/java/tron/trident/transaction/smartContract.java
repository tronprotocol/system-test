package tron.trident.transaction;

import org.testng.annotations.Test;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.Utf8String;
import org.tron.trident.abi.datatypes.generated.Uint256;
import org.tron.trident.core.transaction.TransactionBuilder;
import org.tron.trident.core.utils.ByteArray;
import org.tron.trident.proto.Chain.Transaction;
import org.tron.trident.proto.Response;
import tron.trident.utils.TestBase;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class smartContract extends TestBase {

  @Test(enabled = true)
  public void test01TriggerConstantContract() throws Exception {
    List<org.tron.trident.abi.datatypes.Type> list = new ArrayList<>();
    list.add(new Address(owner));
    Function function = new Function("balanceOf", list,
        Arrays.asList(new TypeReference<Utf8String>() {
        }));
    Response.TransactionExtention transactionExtention = wrapper.constantCall(owner,usdtContract,function);

    ;
    System.out.println(Long.parseLong(ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()),16));
    System.out.println(transactionExtention);
  }


  @Test(enabled = true)
  public void test02TriggerContract() throws Exception {
    List<org.tron.trident.abi.datatypes.Type> list = new ArrayList<>();
    list.add(new Address(receiverAddress));
    list.add(new Uint256(new BigInteger("1")));
    Function function = new Function("transfer", list,
        Arrays.asList(new TypeReference<Utf8String>() {
        }));
    TransactionBuilder transactionExtention = wrapper.triggerCall(owner,usdtContract,function);
    transactionExtention.setFeeLimit(1000000000L);

    Transaction transaction = wrapper.signTransaction(transactionExtention.getTransaction());

    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("TriggerContract :" + broadcast);
  }













}
