package tron.trident.transaction;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.protos.Protocol;
import org.tron.trident.abi.FunctionEncoder;
import org.tron.trident.abi.TypeDecoder;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.Utf8String;
import org.tron.trident.abi.datatypes.generated.Uint256;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.contract.Contract;
import org.tron.trident.core.exceptions.IllegalException;
import org.tron.trident.core.transaction.TransactionBuilder;
import org.tron.trident.core.utils.ByteArray;
import org.tron.trident.proto.Chain.Transaction;
import org.tron.trident.proto.Common;
import org.tron.trident.proto.Response;
import org.tron.trident.proto.Response.EstimateEnergyMessage;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import tron.trident.utils.TestBase;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Slf4j
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
  public void test02ContractCallV2() throws Exception {
    List<org.tron.trident.abi.datatypes.Type> list = new ArrayList<>();
    Response.TransactionExtention transactionExtention = wrapper.constantCallV2("TXTNcgJHD9GPfpiTbSG2VGtfdfii9VcpEr","TDqjTkZ63yHB19w2n7vPm2qAkLHwn9fKKk","0x095ea7b300000000000000000000000097d515b421330c57c545d8dbe946ba7cad02dbb1ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");

    ;
    System.out.println(Long.parseLong(ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()),16));
    Assert.assertEquals(1,Long.parseLong(ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()),16));
    System.out.println(transactionExtention);
  }


  @Test(enabled = true)
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


  @Test(enabled = true)
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

  @Test(enabled = true)
  public void test06TypeDecoderTest() throws Exception {
    //decode Address
    String data =
        "a9059cbb000000008cad2cef099fcfa65b6907386224d796acd2ddb9af120c5196e0b1c40000000000000000000000000000000000000000000000000000000000e7ef00";
    Address address = TypeDecoder.decodeAddress(data.substring(8, 72));
    System.out.println(address.toString());

    //decode uint256
    String dataInt = "97837e4900000000000000000000000000000000000000000000000000000000000003e7";
    Uint256 paramInt = TypeDecoder.decodeNumeric(dataInt.substring(8, 72), Uint256.class);
    System.out.println(paramInt.getValue());
  }

  @Test(enabled = true)
  public void test07DeployContractDefault() throws Exception {
    String abi = "[{\"inputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"},{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"addToDynamicArray\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"addr\",\"outputs\":[{\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"}],\"name\":\"balances\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"boolean\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"dynamicArray\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"dynamicString\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"x\",\"type\":\"uint256\"}],\"name\":\"exampleFunction\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"pure\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"fixedArray\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"fixedBytes\",\"outputs\":[{\"internalType\":\"bytes32\",\"name\":\"\",\"type\":\"bytes32\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"payableAddr\",\"outputs\":[{\"internalType\":\"address payable\",\"name\":\"\",\"type\":\"address\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"person\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"name\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"age\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"bool\",\"name\":\"_boolean\",\"type\":\"bool\"}],\"name\":\"setBoolean\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"enum AllTypesExample.State\",\"name\":\"_state\",\"type\":\"uint8\"}],\"name\":\"setState\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"signedInteger\",\"outputs\":[{\"internalType\":\"int256\",\"name\":\"\",\"type\":\"int256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"state\",\"outputs\":[{\"internalType\":\"enum AllTypesExample.State\",\"name\":\"\",\"type\":\"uint8\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"unsignedInteger\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"address\",\"name\":\"_addr\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"_amount\",\"type\":\"uint256\"}],\"name\":\"updateBalance\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_name\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"_age\",\"type\":\"uint256\"}],\"name\":\"updatePerson\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]" ;
    String code = "5f805460ff1916600190811790915560649055606319600255600380546001600160a01b0319908116735b38da6a701c568545dcfcb03fcb875f56beddc490811790925560048054909116909117905564199a5e195960da1b60055560c0604052600760809081526664796e616d696360c81b60a052600690620000849082620002a5565b506007805460ff1916905560408051608081018252600591810191825264416c69636560d81b6060820152818152601e602082015290600e908190620000cb9082620002a5565b50602082015181600101555050348015620000e4575f80fd5b50d38015620000f1575f80fd5b50d28015620000fe575f80fd5b5060088054600181810183555f8390527ff3f7a9fe364faab93b216da50a3214154f22a0a2b415b23a84c8169e8b636ee391820181905582548082019093556002929091018290556040805160a0810182529182526020820192909252600391810191909152600460608201526005608082018190526200018291600991620001a7565b506003546001600160a01b03165f9081526010602052604090206103e890556200036d565b8260058101928215620001dd579160200282015b82811115620001dd578251829060ff16905591602001919060010190620001bb565b50620001eb929150620001ef565b5090565b5b80821115620001eb575f8155600101620001f0565b634e487b7160e01b5f52604160045260245ffd5b600181811c908216806200022e57607f821691505b6020821081036200024d57634e487b7160e01b5f52602260045260245ffd5b50919050565b601f821115620002a0575f81815260208120601f850160051c810160208610156200027b5750805b601f850160051c820191505b818110156200029c5782815560010162000287565b5050505b505050565b81516001600160401b03811115620002c157620002c162000205565b620002d981620002d2845462000219565b8462000253565b602080601f8311600181146200030f575f8415620002f75750858301515b5f19600386901b1c1916600185901b1785556200029c565b5f85815260208120601f198616915b828110156200033f578886015182559484019460019091019084016200031e565b50858210156200035d57878501515f19600388901b60f8161c191681555b5050505050600190811b01905550565b610849806200037b5f395ff3fe608060405234801561000f575f80fd5b50d3801561001b575f80fd5b50d28015610027575f80fd5b50600436106100fc575f3560e01c80632537f3551461010057806327e235e31461013057806356de96db1461015d5780635a0aa04f14610172578063767800de1461018757806377ec2b551461019a578063934bc29d146101b05780639a5009ac146101c35780639a9bdca7146101d6578063a883ba14146101e9578063c19d93fb146101fc578063c5b57bdb14610216578063c5c85fe314610232578063cef7e0de1461023b578063da5e1cfd14610244578063e0b1cccb14610286578063f8462a0f146102af578063f8d0de92146102cf575b5f80fd5b600454610113906001600160a01b031681565b6040516001600160a01b0390911681526020015b60405180910390f35b61014f61013e3660046104a1565b60106020525f908152604090205481565b604051908152602001610127565b61017061016b3660046104c1565b6102d8565b005b61017a6102ff565b6040516101279190610522565b600354610113906001600160a01b031681565b6101a261038b565b604051610127929190610534565b61014f6101be366004610555565b610421565b61014f6101d1366004610555565b610433565b61014f6101e4366004610555565b610449565b6101706101f7366004610580565b610468565b6007546102099060ff1681565b6040516101279190610642565b5f546102229060ff1681565b6040519015158152602001610127565b61014f60025481565b61014f60055481565b610170610252366004610555565b600880546001810182555f919091527ff3f7a9fe364faab93b216da50a3214154f22a0a2b415b23a84c8169e8b636ee30155565b610170610294366004610668565b6001600160a01b039091165f90815260106020526040902055565b6101706102bd366004610690565b5f805460ff1916911515919091179055565b61014f60015481565b6007805482919060ff191660018360028111156102f7576102f761062e565b021790555050565b6006805461030c906106af565b80601f0160208091040260200160405190810160405280929190818152602001828054610338906106af565b80156103835780601f1061035a57610100808354040283529160200191610383565b820191905f5260205f20905b81548152906001019060200180831161036657829003601f168201915b505050505081565b600e8054819061039a906106af565b80601f01602080910402602001604051908101604052809291908181526020018280546103c6906106af565b80156104115780601f106103e857610100808354040283529160200191610411565b820191905f5260205f20905b8154815290600101906020018083116103f457829003601f168201915b5050505050908060010154905082565b5f61042d8260026106e7565b92915050565b60098160058110610442575f80fd5b0154905081565b60088181548110610458575f80fd5b5f91825260209091200154905081565b600e6104748382610758565b50600f5550565b5f81356001600160a81b0381168114610492575f80fd5b6001600160a01b031692915050565b5f602082840312156104b1575f80fd5b6104ba8261047b565b9392505050565b5f602082840312156104d1575f80fd5b8135600381106104ba575f80fd5b5f81518084525f5b81811015610503576020818501810151868301820152016104e7565b505f602082860101526020601f19601f83011685010191505092915050565b602081525f6104ba60208301846104df565b604081525f61054660408301856104df565b90508260208301529392505050565b5f60208284031215610565575f80fd5b5035919050565b634e487b7160e01b5f52604160045260245ffd5b5f8060408385031215610591575f80fd5b82356001600160401b03808211156105a7575f80fd5b818501915085601f8301126105ba575f80fd5b8135818111156105cc576105cc61056c565b604051601f8201601f19908116603f011681019083821181831017156105f4576105f461056c565b8160405282815288602084870101111561060c575f80fd5b826020860160208301375f602093820184015298969091013596505050505050565b634e487b7160e01b5f52602160045260245ffd5b602081016003831061066257634e487b7160e01b5f52602160045260245ffd5b91905290565b5f8060408385031215610679575f80fd5b6106828361047b565b946020939093013593505050565b5f602082840312156106a0575f80fd5b813580151581146104ba575f80fd5b600181811c908216806106c357607f821691505b6020821081036106e157634e487b7160e01b5f52602260045260245ffd5b50919050565b808202811582820484141761042d57634e487b7160e01b5f52601160045260245ffd5b601f821115610753575f81815260208120601f850160051c810160208610156107305750805b601f850160051c820191505b8181101561074f5782815560010161073c565b5050505b505050565b81516001600160401b038111156107715761077161056c565b6107858161077f84546106af565b8461070a565b602080601f8311600181146107b8575f84156107a15750858301515b5f19600386901b1c1916600185901b17855561074f565b5f85815260208120601f198616915b828110156107e6578886015182559484019460019091019084016107c7565b508582101561080357878501515f19600388901b60f8161c191681555b5050505050600190811b0190555056fea26474726f6e582212201d929ead8f8e871bc0fd0bfb021a9288b515e1ba90b48a9deb7a4f99388d724b64736f6c63430008140033";
    String contractName = "TCtoken";
    Response.TransactionExtention ext = wrapper.deployContract(contractName, abi, code
        ,null,
        1000000000,
        100,
        1000000000,
        0,
        null,
        0
    );
    Transaction signedTransaction = wrapper.signTransaction(ext);
    try{
      String txId = wrapper.broadcastTransaction(signedTransaction);
      logger.info("txId: " + txId);
    }catch (Exception e){
      logger.info(e.toString());
      logger.info(e.getMessage());
    }
  }


  @Test(enabled = true)
  public void test08GetContractInfo(){
    String contractAddress = "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf";
    Response.SmartContractDataWrapper contractInfo =  wrapper.getContractInfo(contractAddress);
    long energyUsage = contractInfo.getContractState().getEnergyUsage();
    logger.info("abi: " + contractInfo.getSmartContract().getAbi());
    logger.info("energyUsage: " + energyUsage);
    Assert.assertTrue(energyUsage > 0);
    String contractAddressFromContractInfo = Base58.encode58Check(contractInfo.getSmartContract().getContractAddress().toByteArray());
    logger.info("address: " + contractAddressFromContractInfo);
    Assert.assertEquals(contractAddressFromContractInfo, contractAddress);
  }


  @Test(enabled = true)
  public void test09ClearContractABI() throws Exception {
    Response.TransactionExtention ext = wrapper.clearContractABI( owner,"TVLUCTYv9DMd3bdvGY35CZJ3h5zqS6wqnm");
    Transaction signedTransaction = wrapper.signTransaction(ext);
    try{
      String txId = wrapper.broadcastTransaction(signedTransaction);
      logger.info("txId: " + txId);
    }catch (Exception e){
      logger.info(e.toString());
      logger.info(e.getMessage());
    }
  }


  @Test(enabled = true)
  public void test010UpdateEnergyLimit() throws IllegalException {
    String contractAddress = "TLEzProjYoys4GVtmq9D4XqXYgeHrjAfaC";
    try {
      Response.TransactionExtention ext = wrapper.updateEnergyLimit(owner, contractAddress, 100L);
      Transaction signedTransaction = wrapper.signTransaction(ext);
      String txId = wrapper.broadcastTransaction(signedTransaction);
      logger.info("txId : " + txId);
      Assert.assertTrue(txId.length() > 1);
    } catch (Exception e){
      e.printStackTrace();
      logger.info(e.getMessage());
      Assert.assertTrue(false);
    }
  }


  @Test(enabled = true)
  public void test011updateSetting() throws IllegalException {
    String contractAddress = "TLEzProjYoys4GVtmq9D4XqXYgeHrjAfaC";
    try {
      Response.TransactionExtention ext = wrapper.updateSetting(owner, contractAddress, 1L);
      Transaction signedTransaction = wrapper.signTransaction(ext);
      String txId = wrapper.broadcastTransaction(signedTransaction);
      logger.info("txId : " + txId);
      Assert.assertTrue(txId.length() > 1);
    } catch (Exception e){
      e.printStackTrace();
      Assert.assertTrue(false);
    }
  }


  @Test(enabled = true)
  public void test012triggerContractAllParameters() throws IllegalException {
    String contractAddress = "TLEzProjYoys4GVtmq9D4XqXYgeHrjAfaC";
    try {
      List<org.tron.trident.abi.datatypes.Type> list = new ArrayList<>();
      Function function = new Function("payMeTRX", list,
          Arrays.asList(new TypeReference<Utf8String>() {
          }));
      Response.TransactionExtention ext = wrapper.triggerContract(owner, contractAddress, FunctionEncoder.encode(function), 1L, 25L,"1005057", 10L);


      Transaction signedTransaction = wrapper.signTransaction(ext);
      String txId = wrapper.broadcastTransaction(signedTransaction);
      logger.info("txId : " + txId);
      Assert.assertTrue(txId.length() > 1);
    } catch (Exception e){
      e.printStackTrace();
      Assert.assertTrue(false);
    }

  }


  @Test(enabled = true)
  public void test013triggerConstantContract() throws IllegalException {
    String contractAddress = "TLEzProjYoys4GVtmq9D4XqXYgeHrjAfaC";
    try {
      List<org.tron.trident.abi.datatypes.Type> list = new ArrayList<>();
      list.add(new Uint256(new BigInteger("2")));
      Function function = new Function("writeNumber", list,
          Arrays.asList(new TypeReference<Utf8String>() {
          }));
      Response.TransactionExtention ext = wrapper.triggerConstantContract(owner, contractAddress, function);
      logger.info("TransactionExtention : " + ext);
    } catch (Exception e){
      e.printStackTrace();
      Assert.assertTrue(false);
    }
  }

  @Test(enabled = true)
  public void test014triggerConstantContractCallData() throws IllegalException {
    String contractAddress = "TLEzProjYoys4GVtmq9D4XqXYgeHrjAfaC";
    try {
      Response.TransactionExtention ext = wrapper.triggerConstantContract(owner, contractAddress, "0x5637a79c0000000000000000000000000000000000000000000000000000000000000003");
      logger.info("TransactionExtention : " + ext);
    } catch (Exception e){
      e.printStackTrace();
      Assert.assertTrue(false);
    }
  }

  @Test(enabled = true)
  public void test015triggerConstantContractCallDataPayTRX() throws IllegalException {
    String contractAddress = "TLEzProjYoys4GVtmq9D4XqXYgeHrjAfaC";
    try {
      List<org.tron.trident.abi.datatypes.Type> list = new ArrayList<>();
      Function function = new Function("payMeTRX", list,
          Arrays.asList(new TypeReference<Utf8String>() {
          }));

      String callData = FunctionEncoder.encode(function);
      Response.TransactionExtention ext = wrapper.triggerConstantContract(owner, contractAddress, callData,0L, 25L,"1005057");
      logger.info("TransactionExtention : " + ext);
    } catch (Exception e){
      e.printStackTrace();
      Assert.assertTrue(false);
    }
  }



  @Test(enabled = true)
  public void test015triggerConstantContractCallDataPayTRXException() throws IllegalException {
    String contractAddress = "TLEzProjYoys4GVtmq9D4XqXYgeHrjAfaC";
    try {
      List<org.tron.trident.abi.datatypes.Type> list = new ArrayList<>();
      Function function = new Function("payMeTRX", list,
          Arrays.asList(new TypeReference<Utf8String>() {
          }));

      String callData = FunctionEncoder.encode(function);
      Response.TransactionExtention ext = wrapper.triggerConstantContract(owner, contractAddress, callData,0L, 25L,"1005057");
      logger.info("TransactionExtention : " + ext);
    } catch (Exception e){
      e.printStackTrace();
      Assert.assertTrue(false);
    }
  }

  @Test(enabled = true)
  public void test016EstimateEnergyV2AllParameters() throws IllegalException {
    String contractAddress = "TLEzProjYoys4GVtmq9D4XqXYgeHrjAfaC";
    try {
      List<org.tron.trident.abi.datatypes.Type> list = new ArrayList<>();
      Function function = new Function("payMeTRX", list,
          Arrays.asList(new TypeReference<Utf8String>() {
          }));



      String callData = FunctionEncoder.encode(function);
      EstimateEnergyMessage estM = wrapper.estimateEnergy(owner, contractAddress, callData,0L, 25L,"1005057");
      logger.info("TransactionExtention : " + estM);
    } catch (Exception e){
      e.printStackTrace();
      Assert.assertTrue(false);
    }
  }


  @Test(enabled = true)
  public void test17DeployContractAllParameters() throws Exception {
    String abi = "[{\"inputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"},{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"addToDynamicArray\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"addr\",\"outputs\":[{\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"}],\"name\":\"balances\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"boolean\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"dynamicArray\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"dynamicString\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"x\",\"type\":\"uint256\"}],\"name\":\"exampleFunction\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"pure\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"fixedArray\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"fixedBytes\",\"outputs\":[{\"internalType\":\"bytes32\",\"name\":\"\",\"type\":\"bytes32\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"payableAddr\",\"outputs\":[{\"internalType\":\"address payable\",\"name\":\"\",\"type\":\"address\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"person\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"name\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"age\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"bool\",\"name\":\"_boolean\",\"type\":\"bool\"}],\"name\":\"setBoolean\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"enum AllTypesExample.State\",\"name\":\"_state\",\"type\":\"uint8\"}],\"name\":\"setState\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"signedInteger\",\"outputs\":[{\"internalType\":\"int256\",\"name\":\"\",\"type\":\"int256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"state\",\"outputs\":[{\"internalType\":\"enum AllTypesExample.State\",\"name\":\"\",\"type\":\"uint8\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"unsignedInteger\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"address\",\"name\":\"_addr\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"_amount\",\"type\":\"uint256\"}],\"name\":\"updateBalance\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_name\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"_age\",\"type\":\"uint256\"}],\"name\":\"updatePerson\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "5f805460ff1916600190811790915560649055606319600255600380546001600160a01b0319908116735b38da6a701c568545dcfcb03fcb875f56beddc490811790925560048054909116909117905564199a5e195960da1b60055560c0604052600760809081526664796e616d696360c81b60a052600690620000849082620002a5565b506007805460ff1916905560408051608081018252600591810191825264416c69636560d81b6060820152818152601e602082015290600e908190620000cb9082620002a5565b50602082015181600101555050348015620000e4575f80fd5b50d38015620000f1575f80fd5b50d28015620000fe575f80fd5b5060088054600181810183555f8390527ff3f7a9fe364faab93b216da50a3214154f22a0a2b415b23a84c8169e8b636ee391820181905582548082019093556002929091018290556040805160a0810182529182526020820192909252600391810191909152600460608201526005608082018190526200018291600991620001a7565b506003546001600160a01b03165f9081526010602052604090206103e890556200036d565b8260058101928215620001dd579160200282015b82811115620001dd578251829060ff16905591602001919060010190620001bb565b50620001eb929150620001ef565b5090565b5b80821115620001eb575f8155600101620001f0565b634e487b7160e01b5f52604160045260245ffd5b600181811c908216806200022e57607f821691505b6020821081036200024d57634e487b7160e01b5f52602260045260245ffd5b50919050565b601f821115620002a0575f81815260208120601f850160051c810160208610156200027b5750805b601f850160051c820191505b818110156200029c5782815560010162000287565b5050505b505050565b81516001600160401b03811115620002c157620002c162000205565b620002d981620002d2845462000219565b8462000253565b602080601f8311600181146200030f575f8415620002f75750858301515b5f19600386901b1c1916600185901b1785556200029c565b5f85815260208120601f198616915b828110156200033f578886015182559484019460019091019084016200031e565b50858210156200035d57878501515f19600388901b60f8161c191681555b5050505050600190811b01905550565b610849806200037b5f395ff3fe608060405234801561000f575f80fd5b50d3801561001b575f80fd5b50d28015610027575f80fd5b50600436106100fc575f3560e01c80632537f3551461010057806327e235e31461013057806356de96db1461015d5780635a0aa04f14610172578063767800de1461018757806377ec2b551461019a578063934bc29d146101b05780639a5009ac146101c35780639a9bdca7146101d6578063a883ba14146101e9578063c19d93fb146101fc578063c5b57bdb14610216578063c5c85fe314610232578063cef7e0de1461023b578063da5e1cfd14610244578063e0b1cccb14610286578063f8462a0f146102af578063f8d0de92146102cf575b5f80fd5b600454610113906001600160a01b031681565b6040516001600160a01b0390911681526020015b60405180910390f35b61014f61013e3660046104a1565b60106020525f908152604090205481565b604051908152602001610127565b61017061016b3660046104c1565b6102d8565b005b61017a6102ff565b6040516101279190610522565b600354610113906001600160a01b031681565b6101a261038b565b604051610127929190610534565b61014f6101be366004610555565b610421565b61014f6101d1366004610555565b610433565b61014f6101e4366004610555565b610449565b6101706101f7366004610580565b610468565b6007546102099060ff1681565b6040516101279190610642565b5f546102229060ff1681565b6040519015158152602001610127565b61014f60025481565b61014f60055481565b610170610252366004610555565b600880546001810182555f919091527ff3f7a9fe364faab93b216da50a3214154f22a0a2b415b23a84c8169e8b636ee30155565b610170610294366004610668565b6001600160a01b039091165f90815260106020526040902055565b6101706102bd366004610690565b5f805460ff1916911515919091179055565b61014f60015481565b6007805482919060ff191660018360028111156102f7576102f761062e565b021790555050565b6006805461030c906106af565b80601f0160208091040260200160405190810160405280929190818152602001828054610338906106af565b80156103835780601f1061035a57610100808354040283529160200191610383565b820191905f5260205f20905b81548152906001019060200180831161036657829003601f168201915b505050505081565b600e8054819061039a906106af565b80601f01602080910402602001604051908101604052809291908181526020018280546103c6906106af565b80156104115780601f106103e857610100808354040283529160200191610411565b820191905f5260205f20905b8154815290600101906020018083116103f457829003601f168201915b5050505050908060010154905082565b5f61042d8260026106e7565b92915050565b60098160058110610442575f80fd5b0154905081565b60088181548110610458575f80fd5b5f91825260209091200154905081565b600e6104748382610758565b50600f5550565b5f81356001600160a81b0381168114610492575f80fd5b6001600160a01b031692915050565b5f602082840312156104b1575f80fd5b6104ba8261047b565b9392505050565b5f602082840312156104d1575f80fd5b8135600381106104ba575f80fd5b5f81518084525f5b81811015610503576020818501810151868301820152016104e7565b505f602082860101526020601f19601f83011685010191505092915050565b602081525f6104ba60208301846104df565b604081525f61054660408301856104df565b90508260208301529392505050565b5f60208284031215610565575f80fd5b5035919050565b634e487b7160e01b5f52604160045260245ffd5b5f8060408385031215610591575f80fd5b82356001600160401b03808211156105a7575f80fd5b818501915085601f8301126105ba575f80fd5b8135818111156105cc576105cc61056c565b604051601f8201601f19908116603f011681019083821181831017156105f4576105f461056c565b8160405282815288602084870101111561060c575f80fd5b826020860160208301375f602093820184015298969091013596505050505050565b634e487b7160e01b5f52602160045260245ffd5b602081016003831061066257634e487b7160e01b5f52602160045260245ffd5b91905290565b5f8060408385031215610679575f80fd5b6106828361047b565b946020939093013593505050565b5f602082840312156106a0575f80fd5b813580151581146104ba575f80fd5b600181811c908216806106c357607f821691505b6020821081036106e157634e487b7160e01b5f52602260045260245ffd5b50919050565b808202811582820484141761042d57634e487b7160e01b5f52601160045260245ffd5b601f821115610753575f81815260208120601f850160051c810160208610156107305750805b601f850160051c820191505b8181101561074f5782815560010161073c565b5050505b505050565b81516001600160401b038111156107715761077161056c565b6107858161077f84546106af565b8461070a565b602080601f8311600181146107b8575f84156107a15750858301515b5f19600386901b1c1916600185901b17855561074f565b5f85815260208120601f198616915b828110156107e6578886015182559484019460019091019084016107c7565b508582101561080357878501515f19600388901b60f8161c191681555b5050505050600190811b0190555056fea26474726f6e582212201d929ead8f8e871bc0fd0bfb021a9288b515e1ba90b48a9deb7a4f99388d724b64736f6c63430008140033";
    String contractName = "AllTypesExample";
    Response.TransactionExtention ext = wrapper.deployContract(contractName, abi, code,  (List)null, 1500000000L, 95L, 1500000000L, 0L, null, 0L);
    Transaction signedTransaction = wrapper.signTransaction(ext);
    try{
      String txId = wrapper.broadcastTransaction(signedTransaction);
      logger.info("txId: " + txId);
    }catch (Exception e){
      logger.info(e.toString());
      logger.info(e.getMessage());
    }

  }















}
