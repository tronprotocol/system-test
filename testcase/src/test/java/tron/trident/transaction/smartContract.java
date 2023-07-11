package tron.trident.transaction;

import org.testng.Assert;
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
import org.tron.trident.proto.Response.EstimateEnergyMessage;
import tron.trident.utils.TestBase;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class smartContract extends TestBase {

  @Test(enabled = false)
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


  @Test(enabled = false)
  public void test02ContractCallV2() throws Exception {
    List<org.tron.trident.abi.datatypes.Type> list = new ArrayList<>();
    Response.TransactionExtention transactionExtention = wrapper.constantCallV2("TXTNcgJHD9GPfpiTbSG2VGtfdfii9VcpEr","TDqjTkZ63yHB19w2n7vPm2qAkLHwn9fKKk","0x095ea7b300000000000000000000000097d515b421330c57c545d8dbe946ba7cad02dbb1ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");

    ;
    System.out.println(Long.parseLong(ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()),16));
    Assert.assertEquals(1,Long.parseLong(ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()),16));
    System.out.println(transactionExtention);
  }


  @Test(enabled = false)
  public void test03TriggerContract() throws Exception {
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


  @Test(enabled = false)
  public void test04TriggerContractV2() throws Exception {
    List<org.tron.trident.abi.datatypes.Type> list = new ArrayList<>();
    list.add(new Address(receiverAddress));
    list.add(new Uint256(new BigInteger("1")));
    Function function = new Function("transfer", list,
        Arrays.asList(new TypeReference<Utf8String>() {
        }));
    TransactionBuilder transactionExtention = wrapper.triggerCallV2(owner,usdtContract,"a9059cbb0000000000000000000000004DB7719251CE8BA74549BA35BBDC02418ECDE5950000000000000000000000000000000000000000000000000000000000000001");
    transactionExtention.setFeeLimit(1000000000L);

    Transaction transaction = wrapper.signTransaction(transactionExtention.getTransaction());

    String broadcast = wrapper.broadcastTransaction(transaction);
    System.out.println("TriggerContract : " + broadcast);
  }



  @Test(enabled = true)
  public void test05estimateEnergyAndEstimateEnergyV2() throws Exception {
    List<org.tron.trident.abi.datatypes.Type> list = new ArrayList<>();
    list.add(new Address(receiverAddress));
    list.add(new Uint256(new BigInteger("1")));
    Function function = new Function("transfer", list,
        Arrays.asList(new TypeReference<Utf8String>() {
        }));
    EstimateEnergyMessage estimateEnergyMessage = wrapper.estimateEnergy(owner,usdtContract,function);

    System.out.println("getEnergyRequired : " + estimateEnergyMessage.getEnergyRequired());;
    System.out.println("Result : " + estimateEnergyMessage.getResult().getResult());


     estimateEnergyMessage = wrapper.estimateEnergyV2(owner,usdtContract,"a9059cbb0000000000000000000000004DB7719251CE8BA74549BA35BBDC02418ECDE5950000000000000000000000000000000000000000000000000000000000000001");
    System.out.println("getEnergyRequiredV2 : " + estimateEnergyMessage.getEnergyRequired());;
    System.out.println("ResultV2 : " + estimateEnergyMessage.getResult().getResult());

  }















}
