package stest.tron.wallet.sophia;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.CommonParameter;
import stest.tron.wallet.common.client.utils.JsonFormat;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Sha256Hash;

@Slf4j
public class CrosschainStress {
  private final String fundKey = "787d15ee13f6e2fae9f7212015239ff920fb24b7df69d8ef6f558198b49975f0";
  private final byte[] fundAddress = PublicMethed.getFinalAddress(fundKey);

  private final String receiverKey = "7400E3D0727F8A61041A8E8BF86599FE5597CE19DE451E59AED07D60967A5E25";
  private final byte[] receiverAddress = PublicMethed.getFinalAddress(receiverKey);

  private final String testKey006 = "b8d3915ac8add213f14d2b642d034310694507305e863a3643784048fc448fd2";
  private final byte[] fromAddress6 = PublicMethed.getFinalAddress(testKey006);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

//  private ManagedChannel channelFull = null;
//  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = "47.252.3.238:50051";  //nile test
  byte[] transferTokenContractAddress = PublicMethed.decode58Check("TBDEjEpW72xJorhANbbozHcwKTAXAvtLUS");

  private AtomicInteger count = new AtomicInteger(0);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
//    channelFull = ManagedChannelBuilder.forTarget(fullnode)
//        .usePlaintext(true)
//        .build();
//    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

  }

  @Test(enabled = true)
  public void test(){
    double d = Math.random();
    int i = (int)(d*100);
    System.out.println(i);
    d = Math.random();
    i = (int)(d*100);
    System.out.println(i);
    d = Math.random();
    i = (int)(d*100);
    System.out.println(i);
  }

  @Test(enabled = true, threadPoolSize = 30,invocationCount = 30)
  public void depositFromTron1() throws InterruptedException{
    System.out.println("start:  "+System.currentTimeMillis());
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    String param =
        "\"TXTNcgJHD9GPfpiTbSG2VGtfdfii9VcpEr\",\"TEmgWCx4FzPFFF9VwNXmXdCTSKz54spbbn\"," +
            "\"0x0000000000000000000000000000000000000000000000000000000000000";
    String param1 =
        "\"TXTNcgJHD9GPfpiTbSG2VGtfdfii9VcpEr\",\"TQ5H3HGyYv1nWe4btfCjV1tS6feo4287sU\"," +
            "\"0x0000000000000000000000000000000000000000000000000000000000000";
    String param2 = "\"TXTNcgJHD9GPfpiTbSG2VGtfdfii9VcpEr\"";
    String param3 = "";
    String param4 = "";
    while(count.get()<5000){
      double d = Math.random();
      int tem = (int)(d*1000);
      if(tem < 10){
        param3 = param+ tem +"10\"";
        param4 = param1 + tem +"10\"";
      }else if(tem < 100){
        param3 = param+ tem +"1\"";
        param4 = param1 + tem +"1\"";
      }else {
        param3 = param+ tem +"\"";
        param4 = param1 + tem +"\"";
      }

      count.addAndGet(1);
      PublicMethed.triggerContract(transferTokenContractAddress,
          "depositFor(address,address,bytes)",
          param3, false, 0, 1000000000L, "0",
          0, fromAddress6, testKey006, blockingStubFull);
      PublicMethed.triggerContract(transferTokenContractAddress,
          "depositFor(address,address,bytes)",
          param4, false, 0, 1000000000L, "0",
          0, fromAddress6, testKey006, blockingStubFull);
      PublicMethed.triggerContract(transferTokenContractAddress,
          "depositEtherFor(address)",
          param2, false, tem, 1000000000L, "0",
          0, fromAddress6, testKey006, blockingStubFull);

    }
    System.out.println("end:  "+System.currentTimeMillis());
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }

  private static int randomInt(int minInt, int maxInt) {
    return (int) Math.round(Math.random() * (maxInt - minInt) + minInt);
  }


  @Test(enabled = true, threadPoolSize = 6,invocationCount = 6)
  public void depositFromTron() throws InterruptedException{
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    String param =
        "\"TH48niZfbwHMyqZwEB8wmHfzcvR8ZzJKC6\",\"TStSix5JFp1seawo6W7VGpfZng3rJjdWge\"," +
            "\"0x0000000000000000000000000000000000000000000000000000000000000001\"";
    String param1 =
        "\"TH48niZfbwHMyqZwEB8wmHfzcvR8ZzJKC6\",\"TE16oEyG9WY7zLVGbSzVbEtd3X5cUNjEic\"," +
            "\"0x0000000000000000000000000000000000000000000000000000000000000001\"";
    String param2 = "\"TH48niZfbwHMyqZwEB8wmHfzcvR8ZzJKC6\"";
    while(count.get()<20000){
      count.addAndGet(1);
      System.out.println("-----count: " +count.get());
      PublicMethed.triggerContract(transferTokenContractAddress,
          "depositFor(address,address,bytes)",
          param, false, 0, 1000000000L, "0",
          0, fromAddress6, testKey006, blockingStubFull);
      PublicMethed.triggerContract(transferTokenContractAddress,
          "depositFor(address,address,bytes)",
          param1, false, 0, 1000000000L, "0",
          0, fromAddress6, testKey006, blockingStubFull);
      PublicMethed.triggerContract(transferTokenContractAddress,
          "depositEtherFor(address)",
          param2, false, 1, 1000000000L, "0",
          0, fromAddress6, testKey006, blockingStubFull);
    }
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }

  @Test(enabled = true)
  public void tttScanBlockTest() throws Exception{
    Long startNum = 20450668L;
    Long endNum = 20450698L;
    long trxCount = 0;
    long erc20Count = 0;
    long mintErc20Count = 0;
    String erc20 = "418A4D1994232110A97D4FA137B006733F5FE31C8D";//7496241BBD16907736A5BE6C592B0BDF441FBE5B
    String mintErc20 = "414A301CE9135F9AF86D75E7A7E42ECCADBCFA155C";
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    while (startNum <= endNum) {
      logger.info("scan block num:" + startNum);
      builder.setNum(startNum);
      List<Transaction> transactionList = blockingStubFull
          .getBlockByNum(builder.build()).getTransactionsList();
      for (Transaction transaction : transactionList) {
        String txid = ByteArray.toHexString(Sha256Hash
            .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));
        Optional<Transaction> transbyid = PublicMethed.getTransactionById(txid, blockingStubFull);
        String type = transbyid.get().getRawData().getContract(0).getType().toString();
        if("TriggerSmartContract".equals(type) ){
          SmartContractOuterClass.TriggerSmartContract contract = transbyid.get().getRawData().getContract(0).getParameter().unpack(SmartContractOuterClass.TriggerSmartContract.class);
          if("41ebae50590810b05d4b403f13766f213518edef65".equals(ByteArray.toHexString(contract.getOwnerAddress().toByteArray()))){
            if(ByteArray.toHexString(contract.getData().toByteArray()).contains("4faa8a26")){ //depositTrx

              trxCount += contract.getCallValue();
//              System.out.println("-------depositTrx: " + trxCount);
            }else if(ByteArray.toHexString(contract.getData().toByteArray()).contains("e3dec8fb")){
              if (ByteArray.toHexString(contract.getData().toByteArray()).contains(erc20.toLowerCase())){ //deposit erc20
                erc20Count += ByteArray.toLong(contract.getData().substring(132).toByteArray());
//                System.out.println("------deposit erc20: " + erc20Count);
              }else if (ByteArray.toHexString(contract.getData().toByteArray()).contains(mintErc20.toLowerCase())){ //deposit minterc20
                mintErc20Count += ByteArray.toLong(contract.getData().substring(132).toByteArray());
//                System.out.println("------deposit minterc20: " + mintErc20Count);
              }
            }
          }
        }
      }
      startNum++;
    }

    System.out.println("-------depositTrx: " + trxCount);
    System.out.println("------deposit erc20: " + erc20Count);
    System.out.println("------deposit minterc20: " + mintErc20Count);
  }


  @Test(enabled = true)
  public void tttScanBlockTest222() throws Exception{
    Long startNum = 20450699L;
    Long endNum = 20450729L;
    long trxCount = 0;
    long erc20Count = 0;
    long mintErc20Count = 0;
    String erc20 = "418A4D1994232110A97D4FA137B006733F5FE31C8D";//7496241BBD16907736A5BE6C592B0BDF441FBE5B
    String mintErc20 = "414A301CE9135F9AF86D75E7A7E42ECCADBCFA155C";
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    while (startNum <= endNum) {
      logger.info("scan block num:" + startNum);
      builder.setNum(startNum);
      List<Transaction> transactionList = blockingStubFull
          .getBlockByNum(builder.build()).getTransactionsList();
      for (Transaction transaction : transactionList) {
        String txid = ByteArray.toHexString(Sha256Hash
            .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));
        Optional<Transaction> transbyid = PublicMethed.getTransactionById(txid, blockingStubFull);
        String type = transbyid.get().getRawData().getContract(0).getType().toString();
        if("TriggerSmartContract".equals(type) ){
          SmartContractOuterClass.TriggerSmartContract contract = transbyid.get().getRawData().getContract(0).getParameter().unpack(SmartContractOuterClass.TriggerSmartContract.class);
          if("41ebae50590810b05d4b403f13766f213518edef65".equals(ByteArray.toHexString(contract.getOwnerAddress().toByteArray()))){
            if(ByteArray.toHexString(contract.getData().toByteArray()).contains("4faa8a26")){ //depositTrx

              trxCount += contract.getCallValue();
//              System.out.println("-------depositTrx: " + trxCount);
            }else if(ByteArray.toHexString(contract.getData().toByteArray()).contains("e3dec8fb")){
              if (ByteArray.toHexString(contract.getData().toByteArray()).contains(erc20.toLowerCase())){ //deposit erc20
                erc20Count += ByteArray.toLong(contract.getData().substring(132).toByteArray());
//                System.out.println("------deposit erc20: " + erc20Count);
              }else if (ByteArray.toHexString(contract.getData().toByteArray()).contains(mintErc20.toLowerCase())){ //deposit minterc20
                mintErc20Count += ByteArray.toLong(contract.getData().substring(132).toByteArray());
//                System.out.println("------deposit minterc20: " + mintErc20Count);
              }
            }
          }
        }
      }
      startNum++;
    }

    System.out.println("-------depositTrx: " + trxCount);
    System.out.println("------deposit erc20: " + erc20Count);
    System.out.println("------deposit minterc20: " + mintErc20Count);
  }

  @Test(enabled = true)
  public void tttScanBlockTest333() throws Exception{
    Long startNum = 20450730L;
    Long endNum = 20450760L;
    long trxCount = 0;
    long erc20Count = 0;
    long mintErc20Count = 0;
    String erc20 = "418A4D1994232110A97D4FA137B006733F5FE31C8D";//7496241BBD16907736A5BE6C592B0BDF441FBE5B
    String mintErc20 = "414A301CE9135F9AF86D75E7A7E42ECCADBCFA155C";
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    while (startNum <= endNum) {
      logger.info("scan block num:" + startNum);
      builder.setNum(startNum);
      List<Transaction> transactionList = blockingStubFull
          .getBlockByNum(builder.build()).getTransactionsList();
      for (Transaction transaction : transactionList) {
        String txid = ByteArray.toHexString(Sha256Hash
            .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));
        Optional<Transaction> transbyid = PublicMethed.getTransactionById(txid, blockingStubFull);
        String type = transbyid.get().getRawData().getContract(0).getType().toString();
        if("TriggerSmartContract".equals(type) ){
          SmartContractOuterClass.TriggerSmartContract contract = transbyid.get().getRawData().getContract(0).getParameter().unpack(SmartContractOuterClass.TriggerSmartContract.class);
          if("41ebae50590810b05d4b403f13766f213518edef65".equals(ByteArray.toHexString(contract.getOwnerAddress().toByteArray()))){
            if(ByteArray.toHexString(contract.getData().toByteArray()).contains("4faa8a26")){ //depositTrx

              trxCount += contract.getCallValue();
//              System.out.println("-------depositTrx: " + trxCount);
            }else if(ByteArray.toHexString(contract.getData().toByteArray()).contains("e3dec8fb")){
              if (ByteArray.toHexString(contract.getData().toByteArray()).contains(erc20.toLowerCase())){ //deposit erc20
                erc20Count += ByteArray.toLong(contract.getData().substring(132).toByteArray());
//                System.out.println("------deposit erc20: " + erc20Count);
              }else if (ByteArray.toHexString(contract.getData().toByteArray()).contains(mintErc20.toLowerCase())){ //deposit minterc20
                mintErc20Count += ByteArray.toLong(contract.getData().substring(132).toByteArray());
//                System.out.println("------deposit minterc20: " + mintErc20Count);
              }
            }
          }
        }
      }
      startNum++;
    }

    System.out.println("-------depositTrx: " + trxCount);
    System.out.println("------deposit erc20: " + erc20Count);
    System.out.println("------deposit minterc20: " + mintErc20Count);
  }

  @Test(enabled = true)
  public void tttScanBlockTest444() throws Exception{
    Long startNum = 20450761L;
    Long endNum = 20450790L;
    long trxCount = 0;
    long erc20Count = 0;
    long mintErc20Count = 0;
    String erc20 = "418A4D1994232110A97D4FA137B006733F5FE31C8D";//7496241BBD16907736A5BE6C592B0BDF441FBE5B
    String mintErc20 = "414A301CE9135F9AF86D75E7A7E42ECCADBCFA155C";
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    while (startNum <= endNum) {
      logger.info("scan block num:" + startNum);
      builder.setNum(startNum);
      List<Transaction> transactionList = blockingStubFull
          .getBlockByNum(builder.build()).getTransactionsList();
      for (Transaction transaction : transactionList) {
        String txid = ByteArray.toHexString(Sha256Hash
            .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));
        Optional<Transaction> transbyid = PublicMethed.getTransactionById(txid, blockingStubFull);
        String type = transbyid.get().getRawData().getContract(0).getType().toString();
        if("TriggerSmartContract".equals(type) ){
          SmartContractOuterClass.TriggerSmartContract contract = transbyid.get().getRawData().getContract(0).getParameter().unpack(SmartContractOuterClass.TriggerSmartContract.class);
          if("41ebae50590810b05d4b403f13766f213518edef65".equals(ByteArray.toHexString(contract.getOwnerAddress().toByteArray()))){
            if(ByteArray.toHexString(contract.getData().toByteArray()).contains("4faa8a26")){ //depositTrx

              trxCount += contract.getCallValue();
//              System.out.println("-------depositTrx: " + trxCount);
            }else if(ByteArray.toHexString(contract.getData().toByteArray()).contains("e3dec8fb")){
              if (ByteArray.toHexString(contract.getData().toByteArray()).contains(erc20.toLowerCase())){ //deposit erc20
                erc20Count += ByteArray.toLong(contract.getData().substring(132).toByteArray());
//                System.out.println("------deposit erc20: " + erc20Count);
              }else if (ByteArray.toHexString(contract.getData().toByteArray()).contains(mintErc20.toLowerCase())){ //deposit minterc20
                mintErc20Count += ByteArray.toLong(contract.getData().substring(132).toByteArray());
//                System.out.println("------deposit minterc20: " + mintErc20Count);
              }
            }
          }
        }
      }
      startNum++;
    }

    System.out.println("-------depositTrx: " + trxCount);
    System.out.println("------deposit erc20: " + erc20Count);
    System.out.println("------deposit minterc20: " + mintErc20Count);
  }

  @Test(enabled = true)
  public void scanBlockRpc() throws Exception{
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Long startNum = 20074895L;
    Long endNum = 20075547L;
    int erc20Count =0;
    int minterc20Count =0;
    int trxCount = 0;

    NumberMessage.Builder builder = NumberMessage.newBuilder();
    String contractAddresses = "TGBzckLfFKDMcrWmU2yFvmXZxsDoMUsqxB";
    String erc20 = "7496241BBD16907736A5BE6C592B0BDF441FBE5B";//7496241BBD16907736A5BE6C592B0BDF441FBE5B
    String mintErc20 = "2C3C280BE638733D84EF79A60461B1A8EC5F48CE";
    String callerAdd = "TH48niZfbwHMyqZwEB8wmHfzcvR8ZzJKC6";
    String rootToken;
    String temValue;

    while (startNum <= endNum) {
      logger.info("scan block num:" + startNum);
      builder.setNum(startNum);
      List<Transaction>  transactionList = blockingStubFull
          .getBlockByNum(builder.build()).getTransactionsList();
      for (Transaction transaction : transactionList) {
        String txid = ByteArray.toHexString(Sha256Hash
            .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));
        Optional<Transaction> transbyid = PublicMethed.getTransactionById(txid,blockingStubFull);

        JSONObject jsonTransaction =
            JSONObject.parseObject(JsonFormat.printToString(transbyid.get(), true));

        if ( JSONPath.read(jsonTransaction.toJSONString(),"$.ret[0].contractRet") != null
            && JSONPath.read(jsonTransaction.toJSONString(),"$.ret[0].contractRet").toString().equals("SUCCESS")
            && JSONPath.read(jsonTransaction.toJSONString(),"$.raw_data.contract[0].type") != null
            && JSONPath.read(jsonTransaction.toJSONString(),"$.raw_data.contract[0].type").toString().equals("TriggerSmartContract")
            && JSONPath.read(jsonTransaction.toJSONString(),"$.raw_data.contract[0].parameter.value") != null){
//          System.out.println("------deposit");
          temValue = JSONPath.read(jsonTransaction.toJSONString(),"$.raw_data.contract[0].parameter.value").toString().toLowerCase();
//          System.out.println("======= value: "+ temValue);
          if (temValue.contains("4faa8a26")){ //depositTrx
            trxCount++;
          }else if(temValue.contains("e3dec8fb")){
            if(temValue.contains(erc20.toLowerCase())){ //erc20
              erc20Count++;
            }else if(temValue.contains(mintErc20.toLowerCase())){ //minterc20
              minterc20Count++;
            }
          }
        }
      }
      startNum++;
    }
    logger.info("erc20Count:" + erc20Count);
    logger.info("minterc20Count:" + minterc20Count);
    logger.info("trxCount:" + trxCount);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {

//    if (channelFull != null) {
//      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
//    }

  }
}

