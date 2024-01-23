package stest.tron.wallet.onlinestress;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.AccountResource;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.WithdrawBalanceContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Sha256Hash;
import stest.tron.wallet.common.client.utils.Sha256Sm3Hash;

@Slf4j
public class TestStorageAndCpuStress {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key5");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("witness.key4");
  private final byte[] testAddress003 = PublicMethed.getFinalAddress(testKey003);

  private final String testKey004 = Configuration.getByPath("testng.conf")
      .getString("witness.key3");
  private final byte[] testAddress004 = PublicMethed.getFinalAddress(testKey004);
  ArrayList<String> txidList = new ArrayList<String>();
  Optional<TransactionInfo> infoById = null;
  Long beforeTime;
  Long afterTime;
  Long beforeBlockNum;
  Long afterBlockNum;
  Block currentBlock;
  Long currentBlockNum;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode =  "47.95.206.44:50051";
  private String fullnode1 = "47.95.206.44:50051";


  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKey002);
    PublicMethed.printAddress(testKey003);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
    currentBlock = blockingStubFull1.getNowBlock(EmptyMessage.newBuilder().build());
    beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    beforeTime = System.currentTimeMillis();
  }

  public static List<String> listForTxid = new ArrayList<>();
  public static HashMap<String,Integer> map = new HashMap<>();
  public static HashMap<Integer,String> witnessMap = new HashMap<>();

  @Test(enabled = true,threadPoolSize = 1, invocationCount = 1)
  public void scanTransaction() {
    getTxidList();
    witnessMap.clear();
    map.clear();
    witnessMap.put(5,"41F08012B4881C320EB40B80F1228731898824E09D");
    witnessMap.put(10,"41DF309FEF25B311E7895562BD9E11AAB2A58816D2");
    witnessMap.put(15,"41BB7322198D273E39B940A5A4C955CB7199A0CDEE");
    witnessMap.put(20,"412080D8A0364E82150DD5235CE7A61A7B40F3F9EF");
    witnessMap.put(25,"4173FC381D3E2AFEFCCED94A57D49520291C38AFBB");
    witnessMap.put(30,"41AF6146B0AD9EE8BBEE811D5858F3252666DFC90C");
    witnessMap.put(35,"41AF6A9D9C0636BD9DF74F687B90C6F44C471A6AB3");
    witnessMap.put(40,"41AF730429E4AB7BF7B53FB15ACB1D45EF5B22F463");
    witnessMap.put(45,"41AF4AEA1C4CBCFA681D98C354C142938381C99389");
    witnessMap.put(50,"41AF53DC31D9DE64DFF59A847125EFCA89D97BC86D");
    witnessMap.put(55,"41AF49468FA1BA966244D76F7D0139FC2CA751FAA5");
    witnessMap.put(60,"41AF5360256F958D2A922D160C429F13D432EFC22F");
    witnessMap.put(65,"41AF5EF33FD79FECB0419A5688035D7BCD3AEFE236");
    witnessMap.put(70,"41AF68F90ED62BA9F6F7A7EABA384E417551CF83E5");
    witnessMap.put(75,"41AF619F8CE75A9E95A19E851BEBE63E89FCB1826E");
    witnessMap.put(80,"41AF71E98F91515D7E5D5379837B9EEFD1AB4650D2");
    witnessMap.put(85,"41AF498B43EE098B26926798CFEAE1AB1154EF4430");
    witnessMap.put(90,"41AF536672333170CB0FBFA78819CD90A05537D872");
    witnessMap.put(95,"41AF5FAC2D62DD1F5C9892BA9D6593337ABBEAAACB");
    witnessMap.put(100,"41AF6981D4562E7B0A6C9E8F8C22D4CCCD03D2F39A");
    witnessMap.put(105,"41AF72A34243836238A533B7E77F3B2B29FD056B14");
    witnessMap.put(110,"41AF49C25D14AED36186B7C89AF405EF37A01EF23D");
    witnessMap.put(115,"41AF53BA37D394575CAD99A2A2C5BE56DEA0227C87");
    witnessMap.put(120,"41AF6A761C941AE2CDC75890D9900AC4B89B7EFCDD");
    witnessMap.put(125,"41AF72B56845F0C4D37388B6E6DC3601A0538ABA71");
    witnessMap.put(130,"41AF4ACF25C1E192285C9BA98522CB3CF20FFBE392");
    witnessMap.put(100000,"416C0214C9995C6F3A61AB23F0EB84B0CDE7FD9C7C");



    for (String txid : listForTxid) {

      long blockNum = PublicMethed.getTransactionInfoById(txid,blockingStubFull)
          .get().getBlockNumber();
      String witnessAddress = ByteArray.toHexString(PublicMethed
          .getBlock(blockNum,blockingStubFull).getBlockHeader().getRawData()
          .getWitnessAddress().toByteArray());

      map.put(witnessAddress.toLowerCase(), map.getOrDefault(witnessAddress,0) + 1);
      logger.info("end");
    }
// all keys below are for test
    int ms_5 = map.containsKey("41F08012B4881C320EB40B80F1228731898824E09D".toLowerCase())
        ? map.get("41F08012B4881C320EB40B80F1228731898824E09D".toLowerCase()) : 0 ;
    int ms_10 = map.containsKey("41DF309FEF25B311E7895562BD9E11AAB2A58816D2".toLowerCase())
        ? map.get("41DF309FEF25B311E7895562BD9E11AAB2A58816D2".toLowerCase()) : 0 ;
    int ms_15 = map.containsKey("41BB7322198D273E39B940A5A4C955CB7199A0CDEE".toLowerCase())
        ? map.get("41BB7322198D273E39B940A5A4C955CB7199A0CDEE".toLowerCase()) : 0 ;
    int ms_20 = map.containsKey("412080D8A0364E82150DD5235CE7A61A7B40F3F9EF".toLowerCase())
        ? map.get("412080D8A0364E82150DD5235CE7A61A7B40F3F9EF".toLowerCase()) : 0 ;
    int ms_25 = map.containsKey("4173FC381D3E2AFEFCCED94A57D49520291C38AFBB".toLowerCase())
        ? map.get("4173FC381D3E2AFEFCCED94A57D49520291C38AFBB".toLowerCase()) : 0 ;
    int ms_30 = map.containsKey("41AF6146B0AD9EE8BBEE811D5858F3252666DFC90C".toLowerCase())
        ? map.get("41AF6146B0AD9EE8BBEE811D5858F3252666DFC90C".toLowerCase()) : 0 ;
    int ms_35 = map.containsKey("41AF6A9D9C0636BD9DF74F687B90C6F44C471A6AB3".toLowerCase())
        ? map.get("41AF6A9D9C0636BD9DF74F687B90C6F44C471A6AB3".toLowerCase()) : 0 ;
    int ms_40 = map.containsKey("41AF730429E4AB7BF7B53FB15ACB1D45EF5B22F463".toLowerCase())
        ? map.get("41AF730429E4AB7BF7B53FB15ACB1D45EF5B22F463".toLowerCase()) : 0 ;
    int ms_45 = map.containsKey("41AF4AEA1C4CBCFA681D98C354C142938381C99389".toLowerCase())
        ? map.get("41AF4AEA1C4CBCFA681D98C354C142938381C99389".toLowerCase()) : 0 ;
    int ms_50 = map.containsKey("41AF53DC31D9DE64DFF59A847125EFCA89D97BC86D".toLowerCase())
        ? map.get("41AF53DC31D9DE64DFF59A847125EFCA89D97BC86D".toLowerCase()) : 0 ;
    int ms_55 = map.containsKey("41AF49468FA1BA966244D76F7D0139FC2CA751FAA5".toLowerCase())
        ? map.get("41AF49468FA1BA966244D76F7D0139FC2CA751FAA5".toLowerCase()) : 0 ;
    int ms_60 = map.containsKey("41AF5360256F958D2A922D160C429F13D432EFC22F".toLowerCase())
        ? map.get("41AF5360256F958D2A922D160C429F13D432EFC22F".toLowerCase()) : 0 ;
    int ms_65 = map.containsKey("41AF5EF33FD79FECB0419A5688035D7BCD3AEFE236".toLowerCase())
        ? map.get("41AF5EF33FD79FECB0419A5688035D7BCD3AEFE236".toLowerCase()) : 0 ;
    int ms_70 = map.containsKey("41AF68F90ED62BA9F6F7A7EABA384E417551CF83E5".toLowerCase())
        ? map.get("41AF68F90ED62BA9F6F7A7EABA384E417551CF83E5".toLowerCase()) : 0 ;
    int ms_75 = map.containsKey("41AF619F8CE75A9E95A19E851BEBE63E89FCB1826E".toLowerCase())
        ? map.get("41AF619F8CE75A9E95A19E851BEBE63E89FCB1826E".toLowerCase()) : 0 ;
    int ms_80 = map.containsKey("41AF71E98F91515D7E5D5379837B9EEFD1AB4650D2".toLowerCase())
        ? map.get("41AF71E98F91515D7E5D5379837B9EEFD1AB4650D2".toLowerCase()) : 0 ;
    int ms_85 = map.containsKey("41AF498B43EE098B26926798CFEAE1AB1154EF4430".toLowerCase())
        ? map.get("41AF498B43EE098B26926798CFEAE1AB1154EF4430".toLowerCase()) : 0 ;
    int ms_90 = map.containsKey("41AF536672333170CB0FBFA78819CD90A05537D872".toLowerCase())
        ? map.get("41AF536672333170CB0FBFA78819CD90A05537D872".toLowerCase()) : 0 ;
    int ms_95 = map.containsKey("41AF5FAC2D62DD1F5C9892BA9D6593337ABBEAAACB".toLowerCase())
        ? map.get("41AF5FAC2D62DD1F5C9892BA9D6593337ABBEAAACB".toLowerCase()) : 0 ;
    int ms_100 = map.containsKey("41AF6981D4562E7B0A6C9E8F8C22D4CCCD03D2F39A".toLowerCase())
        ? map.get("41AF6981D4562E7B0A6C9E8F8C22D4CCCD03D2F39A".toLowerCase()) : 0 ;
    int ms_105 = map.containsKey("41AF72A34243836238A533B7E77F3B2B29FD056B14".toLowerCase())
        ? map.get("41AF72A34243836238A533B7E77F3B2B29FD056B14".toLowerCase()) : 0 ;
    int ms_110 = map.containsKey("41AF49C25D14AED36186B7C89AF405EF37A01EF23D".toLowerCase())
        ? map.get("41AF49C25D14AED36186B7C89AF405EF37A01EF23D".toLowerCase()) : 0 ;
    int ms_115 = map.containsKey("41AF53BA37D394575CAD99A2A2C5BE56DEA0227C87".toLowerCase())
        ? map.get("41AF53BA37D394575CAD99A2A2C5BE56DEA0227C87".toLowerCase()) : 0 ;
    int ms_120 = map.containsKey("41AF6A761C941AE2CDC75890D9900AC4B89B7EFCDD".toLowerCase())
        ? map.get("41AF6A761C941AE2CDC75890D9900AC4B89B7EFCDD".toLowerCase()) : 0 ;
    int ms_125 = map.containsKey("41AF72B56845F0C4D37388B6E6DC3601A0538ABA71".toLowerCase())
        ? map.get("41AF72B56845F0C4D37388B6E6DC3601A0538ABA71".toLowerCase()) : 0 ;
    int ms_130 = map.containsKey("41AF4ACF25C1E192285C9BA98522CB3CF20FFBE392".toLowerCase())
        ? map.get("41AF4ACF25C1E192285C9BA98522CB3CF20FFBE392".toLowerCase()) : 0 ;
    int cms_sr = map.containsKey("416C0214C9995C6F3A61AB23F0EB84B0CDE7FD9C7C".toLowerCase())
        ? map.get("416C0214C9995C6F3A61AB23F0EB84B0CDE7FD9C7C".toLowerCase()) : 0 ;


    int g = 1;

  }

  private static void getTxidList() {
    String line=null;
    try {
      //BufferedReader bufferedReader=new BufferedReader(new FileReader(filePath));
      BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream("/Users/wangzihe/Documents/java-tron/framework/28108921L-28110620L.csv"),"utf-8"));

      //int i=0;
      while((line=bufferedReader.readLine())!=null){
        listForTxid.add(line.toLowerCase());

      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Test(enabled = true,threadPoolSize = 1, invocationCount = 1)
  public void scanBlock() {
    witnessMap.clear();
    map.clear();
    witnessMap.put(5,"41F08012B4881C320EB40B80F1228731898824E09D");
    witnessMap.put(10,"41DF309FEF25B311E7895562BD9E11AAB2A58816D2");
    witnessMap.put(15,"41BB7322198D273E39B940A5A4C955CB7199A0CDEE");
    witnessMap.put(20,"412080D8A0364E82150DD5235CE7A61A7B40F3F9EF");
    witnessMap.put(25,"4173FC381D3E2AFEFCCED94A57D49520291C38AFBB");
    witnessMap.put(30,"41AF6146B0AD9EE8BBEE811D5858F3252666DFC90C");
    witnessMap.put(35,"41AF6A9D9C0636BD9DF74F687B90C6F44C471A6AB3");
    witnessMap.put(40,"41AF730429E4AB7BF7B53FB15ACB1D45EF5B22F463");
    witnessMap.put(45,"41AF4AEA1C4CBCFA681D98C354C142938381C99389");
    witnessMap.put(50,"41AF53DC31D9DE64DFF59A847125EFCA89D97BC86D");
    witnessMap.put(55,"41AF49468FA1BA966244D76F7D0139FC2CA751FAA5");
    witnessMap.put(60,"41AF5360256F958D2A922D160C429F13D432EFC22F");
    witnessMap.put(65,"41AF5EF33FD79FECB0419A5688035D7BCD3AEFE236");
    witnessMap.put(70,"41AF68F90ED62BA9F6F7A7EABA384E417551CF83E5");
    witnessMap.put(75,"41AF619F8CE75A9E95A19E851BEBE63E89FCB1826E");
    witnessMap.put(80,"41AF71E98F91515D7E5D5379837B9EEFD1AB4650D2");
    witnessMap.put(85,"41AF498B43EE098B26926798CFEAE1AB1154EF4430");
    witnessMap.put(90,"41AF536672333170CB0FBFA78819CD90A05537D872");
    witnessMap.put(95,"41AF5FAC2D62DD1F5C9892BA9D6593337ABBEAAACB");
    witnessMap.put(100,"41AF6981D4562E7B0A6C9E8F8C22D4CCCD03D2F39A");
    witnessMap.put(105,"41AF72A34243836238A533B7E77F3B2B29FD056B14");
    witnessMap.put(110,"41AF49C25D14AED36186B7C89AF405EF37A01EF23D");
    witnessMap.put(115,"41AF53BA37D394575CAD99A2A2C5BE56DEA0227C87");
    witnessMap.put(120,"41AF6A761C941AE2CDC75890D9900AC4B89B7EFCDD");
    witnessMap.put(125,"41AF72B56845F0C4D37388B6E6DC3601A0538ABA71");
    witnessMap.put(130,"41AF4ACF25C1E192285C9BA98522CB3CF20FFBE392");
    witnessMap.put(100000,"416C0214C9995C6F3A61AB23F0EB84B0CDE7FD9C7C");


    Long startNum = 30855000L;
    Long endNum = 30858000L;

    Integer totalNum = 0;
    Integer successNum = 0;
    Integer failedNum = 0;
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    while (endNum >= startNum) {
      logger.info("scan block num:" + endNum);
      builder.setNum(endNum);
      Block block = blockingStubFull1.getBlockByNum(builder.build());
      List<Transaction>  transactionList = block.getTransactionsList();
      map.put(ByteArray.toHexString(block.getBlockHeader().getRawData().getWitnessAddress().toByteArray()).toLowerCase(), map.getOrDefault(ByteArray.toHexString(block.getBlockHeader().getRawData().getWitnessAddress().toByteArray()).toLowerCase(),0) + 1);
      Integer transactionNumInThisBlock = transactionList.size();
      totalNum = totalNum + transactionNumInThisBlock;
      for (Transaction transaction : transactionList) {
        String txid = ByteArray.toHexString(Sha256Hash.hash(true, transaction.getRawData().toByteArray()));
        //String writeData = ByteArray.toHexString(Sha256Hash.hash(true, transaction.getRawData().toByteArray()));
        //createRosettaApiCsvData.writeDataToCsvFile("txid-stressss.csv",txid);
        //System.out.println("Fee:" + PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getFee());
      }
/*      for (Transaction transaction : transactionList) {
        if (transaction.getRet(0).getContractRet().name().equals("SUCCESS")) {
          successNum++;
        } else {
          failedNum++;

          String writeData = ByteArray.toHexString(Sha256Hash.hash(true, transaction.getRawData().toByteArray()));
          logger.info(writeData);
          createRosettaApiCsvData.writeDataToCsvFile("28164160L-28167324L.csv",writeData);
        }
      }*/
      endNum--;
    }

    System.exit(1);
    logger.info("successNum:" + successNum);
    logger.info("failedNum:" + failedNum);
    logger.info("totalNum:" + totalNum);
    logger.info("Success rate:" + (double)failedNum / (double)totalNum);

    int ms_5 = map.containsKey("41F08012B4881C320EB40B80F1228731898824E09D".toLowerCase())
        ? map.get("41F08012B4881C320EB40B80F1228731898824E09D".toLowerCase()) : 0 ;
    int ms_10 = map.containsKey("41DF309FEF25B311E7895562BD9E11AAB2A58816D2".toLowerCase())
        ? map.get("41DF309FEF25B311E7895562BD9E11AAB2A58816D2".toLowerCase()) : 0 ;
    int ms_15 = map.containsKey("41BB7322198D273E39B940A5A4C955CB7199A0CDEE".toLowerCase())
        ? map.get("41BB7322198D273E39B940A5A4C955CB7199A0CDEE".toLowerCase()) : 0 ;
    int ms_20 = map.containsKey("412080D8A0364E82150DD5235CE7A61A7B40F3F9EF".toLowerCase())
        ? map.get("412080D8A0364E82150DD5235CE7A61A7B40F3F9EF".toLowerCase()) : 0 ;
    int ms_25 = map.containsKey("4173FC381D3E2AFEFCCED94A57D49520291C38AFBB".toLowerCase())
        ? map.get("4173FC381D3E2AFEFCCED94A57D49520291C38AFBB".toLowerCase()) : 0 ;
    int ms_30 = map.containsKey("41AF6146B0AD9EE8BBEE811D5858F3252666DFC90C".toLowerCase())
        ? map.get("41AF6146B0AD9EE8BBEE811D5858F3252666DFC90C".toLowerCase()) : 0 ;
    int ms_35 = map.containsKey("41AF6A9D9C0636BD9DF74F687B90C6F44C471A6AB3".toLowerCase())
        ? map.get("41AF6A9D9C0636BD9DF74F687B90C6F44C471A6AB3".toLowerCase()) : 0 ;
    int ms_40 = map.containsKey("41AF730429E4AB7BF7B53FB15ACB1D45EF5B22F463".toLowerCase())
        ? map.get("41AF730429E4AB7BF7B53FB15ACB1D45EF5B22F463".toLowerCase()) : 0 ;
    int ms_45 = map.containsKey("41AF4AEA1C4CBCFA681D98C354C142938381C99389".toLowerCase())
        ? map.get("41AF4AEA1C4CBCFA681D98C354C142938381C99389".toLowerCase()) : 0 ;
    int ms_50 = map.containsKey("41AF53DC31D9DE64DFF59A847125EFCA89D97BC86D".toLowerCase())
        ? map.get("41AF53DC31D9DE64DFF59A847125EFCA89D97BC86D".toLowerCase()) : 0 ;
    int ms_55 = map.containsKey("41AF49468FA1BA966244D76F7D0139FC2CA751FAA5".toLowerCase())
        ? map.get("41AF49468FA1BA966244D76F7D0139FC2CA751FAA5".toLowerCase()) : 0 ;
    int ms_60 = map.containsKey("41AF5360256F958D2A922D160C429F13D432EFC22F".toLowerCase())
        ? map.get("41AF5360256F958D2A922D160C429F13D432EFC22F".toLowerCase()) : 0 ;
    int ms_65 = map.containsKey("41AF5EF33FD79FECB0419A5688035D7BCD3AEFE236".toLowerCase())
        ? map.get("41AF5EF33FD79FECB0419A5688035D7BCD3AEFE236".toLowerCase()) : 0 ;
    int ms_70 = map.containsKey("41AF68F90ED62BA9F6F7A7EABA384E417551CF83E5".toLowerCase())
        ? map.get("41AF68F90ED62BA9F6F7A7EABA384E417551CF83E5".toLowerCase()) : 0 ;
    int ms_75 = map.containsKey("41AF619F8CE75A9E95A19E851BEBE63E89FCB1826E".toLowerCase())
        ? map.get("41AF619F8CE75A9E95A19E851BEBE63E89FCB1826E".toLowerCase()) : 0 ;
    int ms_80 = map.containsKey("41AF71E98F91515D7E5D5379837B9EEFD1AB4650D2".toLowerCase())
        ? map.get("41AF71E98F91515D7E5D5379837B9EEFD1AB4650D2".toLowerCase()) : 0 ;
    int ms_85 = map.containsKey("41AF498B43EE098B26926798CFEAE1AB1154EF4430".toLowerCase())
        ? map.get("41AF498B43EE098B26926798CFEAE1AB1154EF4430".toLowerCase()) : 0 ;
    int ms_90 = map.containsKey("41AF536672333170CB0FBFA78819CD90A05537D872".toLowerCase())
        ? map.get("41AF536672333170CB0FBFA78819CD90A05537D872".toLowerCase()) : 0 ;
    int ms_95 = map.containsKey("41AF5FAC2D62DD1F5C9892BA9D6593337ABBEAAACB".toLowerCase())
        ? map.get("41AF5FAC2D62DD1F5C9892BA9D6593337ABBEAAACB".toLowerCase()) : 0 ;
    int ms_100 = map.containsKey("41AF6981D4562E7B0A6C9E8F8C22D4CCCD03D2F39A".toLowerCase())
        ? map.get("41AF6981D4562E7B0A6C9E8F8C22D4CCCD03D2F39A".toLowerCase()) : 0 ;
    int ms_105 = map.containsKey("41AF72A34243836238A533B7E77F3B2B29FD056B14".toLowerCase())
        ? map.get("41AF72A34243836238A533B7E77F3B2B29FD056B14".toLowerCase()) : 0 ;
    int ms_110 = map.containsKey("41AF49C25D14AED36186B7C89AF405EF37A01EF23D".toLowerCase())
        ? map.get("41AF49C25D14AED36186B7C89AF405EF37A01EF23D".toLowerCase()) : 0 ;
    int ms_115 = map.containsKey("41AF53BA37D394575CAD99A2A2C5BE56DEA0227C87".toLowerCase())
        ? map.get("41AF53BA37D394575CAD99A2A2C5BE56DEA0227C87".toLowerCase()) : 0 ;
    int ms_120 = map.containsKey("41AF6A761C941AE2CDC75890D9900AC4B89B7EFCDD".toLowerCase())
        ? map.get("41AF6A761C941AE2CDC75890D9900AC4B89B7EFCDD".toLowerCase()) : 0 ;
    int ms_125 = map.containsKey("41AF72B56845F0C4D37388B6E6DC3601A0538ABA71".toLowerCase())
        ? map.get("41AF72B56845F0C4D37388B6E6DC3601A0538ABA71".toLowerCase()) : 0 ;
    int ms_130 = map.containsKey("41AF4ACF25C1E192285C9BA98522CB3CF20FFBE392".toLowerCase())
        ? map.get("41AF4ACF25C1E192285C9BA98522CB3CF20FFBE392".toLowerCase()) : 0 ;
    int cms_sr = map.containsKey("416C0214C9995C6F3A61AB23F0EB84B0CDE7FD9C7C".toLowerCase())
        ? map.get("416C0214C9995C6F3A61AB23F0EB84B0CDE7FD9C7C".toLowerCase()) : 0 ;

    int p = 0;

  }

  @Test(enabled = false, threadPoolSize = 1, invocationCount = 1)
  public void storageAndCpu() {
    Random rand = new Random();
    Integer randNum = rand.nextInt(30) + 1;
    randNum = rand.nextInt(4000);

    Long maxFeeLimit = 1000000000L;
    String contractName = "StorageAndCpu" + Integer.toString(randNum);
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestStorageAndCpu_storageAndCpu");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestStorageAndCpu_storageAndCpu");
    PublicMethed
        .freezeBalanceGetEnergy(fromAddress, 1000000000000L, 3, 1, testKey002, blockingStubFull);
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, testKey002, fromAddress, blockingStubFull);
    try {
      Thread.sleep(30000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    String txid;

    ChainParameters chainParameters = blockingStubFull
        .getChainParameters(EmptyMessage.newBuilder().build());
    Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);

    Integer i = 1;
    while (i++ < 8000) {
      String initParmes = "\"" + "930" + "\"";
      txid = PublicMethed.triggerContract(contractAddress,
          "testUseCpu(uint256)", "9100", false,
          0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
      txid = PublicMethed.triggerContract(contractAddress,
          "storage8Char()", "", false,
          0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
      //storage 9 EnergyUsageTotal is  211533, 10 is 236674, 5 is 110969,21 is 500000
      txid = PublicMethed.triggerContract(contractAddress,
          "testUseStorage(uint256)", "21", false,
          0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
      //logger.info("i is " +Integer.toString(i) + " " + txid);
      //txidList.add(txid);
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (i % 10 == 0) {
        chainParameters = blockingStubFull
            .getChainParameters(EmptyMessage.newBuilder().build());
        getChainParameters = Optional.ofNullable(chainParameters);
        logger.info(getChainParameters.get().getChainParameter(22).getKey());
        logger.info(Long.toString(getChainParameters.get().getChainParameter(22).getValue()));
        logger.info(getChainParameters.get().getChainParameter(23).getKey());
        logger.info(Long.toString(getChainParameters.get().getChainParameter(23).getValue()));

      }
    }
  }

  @Test(enabled = false, description = "Get asset issue net resource")
  public void test12GetAssetIssueNet() throws Exception {
    Long startNum = 29266108L;
    Long endNum = 29266208L;

    int energy = 0;
    int bandwidth = 0;
    int tronpower = 0;
    while (startNum++ <= endNum) {
      NumberMessage.Builder builder = NumberMessage.newBuilder();
      builder.setNum(startNum);
      Block block = blockingStubFull.getBlockByNum(builder.build());
      logger.info("Start to scan block :" + block.getBlockHeader().getRawData().getNumber());

      List<Transaction> transactionList = block.getTransactionsList();
      for (Transaction transaction : transactionList) {
        Any any = transaction.getRawData().getContract(0).getParameter();
        Integer contractType = transaction.getRawData().getContract(0).getType().getNumber();
        try {
          switch (contractType) {
            case 11:
              FreezeBalanceContract freezeBalanceContract = any.unpack(FreezeBalanceContract.class);
              int type = freezeBalanceContract.getResource().getNumber();
              if(type == 0) {
                bandwidth++;
              } else if(type == 1) {
                energy++;
              } else if(type == 2){
                tronpower++;
              }


            default:
              logger.info("Unknown type:" + contractType);
              continue;
          }
        } catch (Exception e) {

        }
      }
    }

    System.out.println("Bandwidth:" + bandwidth);
    System.out.println("Energy:" + energy);
    System.out.println("TronPower:" + tronpower);
  }


  public static Account account;
  public HashSet<ByteString> addressSet = new HashSet<>();
  public HashSet<ByteString> assetIssueSet = new HashSet<>();

  @Test(enabled = true, description = "Get asset issue net resource")
  public void test02GetAssetIssueNet() throws Exception{
    //compareDelegateResource("4C0056A3BD796094EA0677597E6E2152EE42E21F5DA289DF65E000");




    account = PublicMethed.queryAccount("7400E3D0727F8A61041A8E8BF86599FE5597CE19DE451E59AED07D60967A5E25",blockingStubFull);
    //扫描到28307530块了
    Long startNum = 29266108L;
    Long endNum = 29266208L;
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(startNum);
    int retryTimes = 0;
    HashSet<ByteString> set = new HashSet<>();
    while (startNum++ <= endNum) {
      //Block block = blockingStubFull412.getNowBlock(EmptyMessage.newBuilder().build());
      builder.setNum(startNum);
      Block block = blockingStubFull.getBlockByNum(builder.build());
      logger.info("Start to scan block :" + block.getBlockHeader().getRawData().getNumber());

      List<Transaction> transactionList = block.getTransactionsList();
      for (Transaction transaction : transactionList) {

        Any any = transaction.getRawData().getContract(0).getParameter();
        Integer contractType =  transaction.getRawData().getContract(0).getType().getNumber();


        try {
          switch (contractType) {
            case 1:
              TransferContract transferContract = any.unpack(TransferContract.class);
              set.add(transferContract.getOwnerAddress());
              //doCheck(transferContract.getOwnerAddress());
            case 2:
              TransferAssetContract transferAssetContract = any.unpack(TransferAssetContract.class);
              doCheck(transferAssetContract.getOwnerAddress());
              if(!addressSet.contains(transferAssetContract.getAssetName())) {
                Assert.assertEquals(PublicMethed.getAssetIssueById(ByteArray.toStr(transferAssetContract.getAssetName().toByteArray()), blockingStubFull),
                    PublicMethed.getAssetIssueById(ByteArray.toStr(transferAssetContract.getAssetName().toByteArray()), blockingStubFull));
                addressSet.add(transferAssetContract.getAssetName());
                logger.info("check token " + ByteArray.toStr(transferAssetContract.getAssetName().toByteArray()) + " successfully");
              }


            case 31:
              TriggerSmartContract triggerSmartContract = any.unpack(TriggerSmartContract.class);
              doCheck(triggerSmartContract.getOwnerAddress());
            case 13:
              WithdrawBalanceContract withdrawBalanceContract = any.unpack(WithdrawBalanceContract.class);
              doCheck(withdrawBalanceContract.getOwnerAddress());
            case 11:
              FreezeBalanceContract freezeBalanceContract = any.unpack(FreezeBalanceContract.class);
              doCheck(freezeBalanceContract.getOwnerAddress());
            case 0:
              AccountCreateContract accountCreateContract = any.unpack(AccountCreateContract.class);
              doCheck(accountCreateContract.getOwnerAddress());
/*            case 4:
              VoteWitnessContract voteWitnessContract = any.unpack(VoteWitnessContract.class);
              doCheck(voteWitnessContract.getOwnerAddress());*/
            case 12:
              UnfreezeBalanceContract unfreezeBalanceContract = any.unpack(UnfreezeBalanceContract.class);
              doCheck(unfreezeBalanceContract.getOwnerAddress());
            case 30:
              CreateSmartContract createSmartContract = any.unpack(CreateSmartContract.class);
              doCheck(createSmartContract.getOwnerAddress());
            case 46:
              AccountPermissionUpdateContract accountPermissionUpdateContract = any.unpack(AccountPermissionUpdateContract.class);
              doCheck(accountPermissionUpdateContract.getOwnerAddress());
            default:
              logger.info("Unknown type:" + contractType);
              continue;

          }
        } catch (Exception e) {

        }





      }
    }


  }


  @Test(enabled = true, description = "Get asset issue net resource")
  public void test03CatchContractAddressAndTopic() throws Exception{
    //compareDelegateResource("4C0056A3BD796094EA0677597E6E2152EE42E21F5DA289DF65E000");



    HashSet<String> contractAndTopicList = new HashSet<>();



    Long startNum = 34678216L;
    Long endNum = startNum + 10L;
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(startNum);
    int retryTimes = 0;
    HashSet<ByteString> set = new HashSet<>();
    while (startNum++ <= endNum) {
      logger.info("current block num:" + startNum);
      builder.setNum(startNum);
      TransactionInfoList transactionInfoList = blockingStubFull.getTransactionInfoByBlockNum(builder.build());


      for (TransactionInfo transactionInfo : transactionInfoList.getTransactionInfoList()) {
        if(!transactionInfo.getContractAddress().isEmpty() && transactionInfo.getLogCount() > 0) {

          for (TransactionInfo.Log log : transactionInfo.getLogList()) {
            try {
              contractAndTopicList.add(
                  ByteArray.toHexString(transactionInfo.getContractAddress().toByteArray()).substring(2)
              +
                      ",0x"
                      +  ByteArray.toHexString(log.getTopics(0).toByteArray())
              + ","
                  + ByteArray.toHexString(transactionInfo.getId().toByteArray())
              )
              ;
            } catch (Exception e) {

            }

          }
        }




      }
    }

    for (String contractAddressAndTopic : contractAndTopicList) {
      writeDataToCsvFile("eth_contractAddressAndTopic2.csv", contractAddressAndTopic);
    }



  }


  ConcurrentHashMap<ByteString,Integer> certificationCosts = new ConcurrentHashMap<>();
  Set<ByteString> concurrentHashSet = certificationCosts.newKeySet();
  private static HashSet<String> existAddress = new HashSet<>();
  List<Long> list1 = new ArrayList<>();

  private static AtomicLong blockNum = new AtomicLong(34988101L - 4000L);
  private static AtomicLong times = new AtomicLong(5);
  @Test(enabled = true, threadPoolSize = 10, invocationCount = 10)
  public void test07CreateAddressCsv() throws Exception{
    getNowAddressList();
    ManagedChannel channelFull = null;
    WalletGrpc.WalletBlockingStub blockingStubFull = null;
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    long blockNumCurrent = blockNum.getAndAdd(-200);
    int times = 200;
    while (times-->= 0) {
      if(concurrentHashSet.size() > 1000000) {
        break;
      }
      //list1.add(blockNumCurrent);
      builder.setNum(blockNumCurrent--);
      Block block = blockingStubFull.getBlockByNum(builder.build());
      logger.info("Start to scan block :" + block.getBlockHeader().getRawData().getNumber());

      List<Transaction> transactionList = block.getTransactionsList();
      for (Transaction transaction : transactionList) {

        Any any = transaction.getRawData().getContract(0).getParameter();
        Integer contractType =  transaction.getRawData().getContract(0).getType().getNumber();


        try {
          switch (contractType) {
            case 1:
              TransferContract transferContract = any.unpack(TransferContract.class);
              isExist(transferContract.getOwnerAddress());
              isExist(transferContract.getToAddress());
              break;
            case 2:
              TransferAssetContract transferAssetContract = any.unpack(TransferAssetContract.class);
              isExist(transferAssetContract.getOwnerAddress());
              isExist(transferAssetContract.getToAddress());
              break;
            case 31:
              TriggerSmartContract triggerSmartContract = any.unpack(TriggerSmartContract.class);
              isExist(triggerSmartContract.getContractAddress());
              isExist(triggerSmartContract.getOwnerAddress());
              break;
            case 13:
              WithdrawBalanceContract withdrawBalanceContract = any.unpack(WithdrawBalanceContract.class);

              isExist(withdrawBalanceContract.getOwnerAddress());
              break;
            case 11:
              FreezeBalanceContract freezeBalanceContract = any.unpack(FreezeBalanceContract.class);
              isExist(freezeBalanceContract.getOwnerAddress());
              break;
            case 0:
              AccountCreateContract accountCreateContract = any.unpack(AccountCreateContract.class);
              isExist(accountCreateContract.getOwnerAddress());
              isExist(accountCreateContract.getAccountAddress());
              break;
            case 12:
              UnfreezeBalanceContract unfreezeBalanceContract = any.unpack(UnfreezeBalanceContract.class);
              isExist(unfreezeBalanceContract.getOwnerAddress());
              break;
            case 30:
              CreateSmartContract createSmartContract = any.unpack(CreateSmartContract.class);
              isExist(createSmartContract.getOwnerAddress());
              break;
            case 46:
              AccountPermissionUpdateContract accountPermissionUpdateContract = any.unpack(AccountPermissionUpdateContract.class);
              isExist(accountPermissionUpdateContract.getOwnerAddress());
              break;
            case 4:
              VoteWitnessContract voteWitnessContract = any.unpack(VoteWitnessContract.class);
              isExist(voteWitnessContract.getOwnerAddress());
            default:
              logger.info("Unknown type:" + contractType);
              continue;

          }
        } catch (Exception e) {

        }






      }
    }




  }

  private static HashSet<String> getFileList(String fileName,HashSet<String> set) {
    String line=null;
    try {
      //BufferedReader bufferedReader=new BufferedReader(new FileReader(filePath));
      BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"utf-8"));

      //int i=0;
      while((line=bufferedReader.readLine())!=null){
        set.add(line);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return set;
  }




  private static void getNowAddressList() {
    String line=null;
    try {
      //BufferedReader bufferedReader=new BufferedReader(new FileReader(filePath));
      BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream("mainnet_newAddress.csv"),"utf-8"));

      //int i=0;
      while((line=bufferedReader.readLine())!=null){
        existAddress.add(line);
      }
      System.out.println(existAddress.size());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }



  public static void writeDataToCsvFile(String fileName,String writeData) {

    {
      try {
        File file = new File(fileName);

        if (!file.exists()) {
          file.createNewFile();
        }
        FileWriter fileWritter = new FileWriter(file.getName(), true);
        fileWritter.write(writeData + "\n");
        fileWritter.close();
        //System.out.println("finish");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }



  public void doCheck(ByteString address) throws Exception{
    if(addressSet.contains(address)) {
      //logger.info("skip :" + ByteArray.toHexString(address.toByteArray()));
      return;
    } else {
      addressSet.add(address);
    }
    logger.info("checking :" + ByteArray.toHexString(address.toByteArray()));
    compareTwoAddress(address);
    compareNet(address);
    compareAccountResource(address);
    return;

  }

  public void compareTwoAddress(ByteString address) {

    Assert.assertEquals(
        PublicMethed.queryAccount(address.toByteArray(),blockingStubFull).toBuilder().clearFreeAssetNetUsageV2()
            //.putAllFreeAssetNetUsageV2(account.getFreeAssetNetUsageV2Map())
            .setBalance(1L).setLatestOprationTime(1L).setAccountResource(AccountResource.newBuilder())
            .setFreeNetUsage(1L)
            .setNetUsage(1L)
            .clearAssetV2()
            .setLatestConsumeFreeTime(1L)
            .setLatestConsumeTime(1L)
            .setAllowance(1L)
            .clearAccountResource()
            //.clearOldVotePower()
            .build(),
        PublicMethed.queryAccount(address.toByteArray(),blockingStubFull).toBuilder().clearFreeAssetNetUsageV2()
            //.putAllFreeAssetNetUsageV2(account.getFreeAssetNetUsageV2Map())
            .setBalance(1L).setLatestOprationTime(1L).setAccountResource(AccountResource.newBuilder())
            .setFreeNetUsage(1L)
            .setNetUsage(1L)
            .setLatestConsumeFreeTime(1L)
            .setLatestConsumeTime(1L)
            .clearAssetV2()
            .setAllowance(1L)
            .clearAccountResource()
            .build()
    );

    //long vote413 = PublicMethed.queryAccount(address.toByteArray(),blockingStubFull413).getOldVotePower();
    //logger.info("checking :" + ByteArray.toHexString(address.toByteArray()));
    //logger.info("vote413:" + vote413);

    //Assert.assertTrue(vote413 == 0 || vote413 >= 1000000);
    //Assert.assertTrue(vote413 >= 0);



  }



  public void compareNet(ByteString address) {
    Assert.assertEquals(
        PublicMethed.getAccountNet(address.toByteArray(),blockingStubFull)
            .toBuilder().setTotalNetWeight(1L)
            .setNetUsed(1L)
            .setFreeNetUsed(1)
            .setNetLimit(1)
            .build()
        ,
        PublicMethed.getAccountNet(address.toByteArray(),blockingStubFull)
            .toBuilder().setTotalNetWeight(1L)
            .setNetUsed(1L)
            .setFreeNetUsed(1)
            .setNetLimit(1)
            .build()
    );
  }

  public void compareAccountResource(ByteString address) throws Exception{
/*    int notEqualTimes = 0;
    while (!isEqual(address)) {
      notEqualTimes++;
    }
    if(notEqualTimes >= 1) {
      logger.info("checking :" + ByteArray.toHexString(address.toByteArray()));
      logger.info("Not equal times:" + notEqualTimes);
    }*/



    Assert.assertEquals(
        PublicMethed.getAccountResource(address.toByteArray(),blockingStubFull)
            .toBuilder()
            .setFreeNetUsed(1L)
            .setEnergyUsed(1L)
            .setTotalEnergyWeight(1L)
            .setTotalNetWeight(1L)
            .setNetUsed(1L)
            .setNetLimit(1L)
            .setEnergyLimit(1L)
            .build()
        ,
        PublicMethed.getAccountResource(address.toByteArray(),blockingStubFull)
            .toBuilder()
            .setFreeNetUsed(1L)
            .setEnergyUsed(1L)
            .setNetUsed(1L)
            .setNetLimit(1L)
            .setTotalEnergyWeight(1L)
            .setTotalNetWeight(1L)
            .setEnergyLimit(1L)
            .build()
    );

  }

  public boolean isEqual(ByteString address) {
    return PublicMethed.getAccountResource(address.toByteArray(),blockingStubFull)
        .toBuilder()
        .setFreeNetUsed(1L)
        .setEnergyUsed(1L)
        .setTotalEnergyWeight(1L)
        .setTotalNetWeight(1L)
        .setNetUsed(1L)
        .setNetLimit(1L)
        .setEnergyLimit(1L)
        .build().equals(PublicMethed.getAccountResource(address.toByteArray(),blockingStubFull)
            .toBuilder()
            .setFreeNetUsed(1L)
            .setEnergyUsed(1L)
            .setTotalEnergyWeight(1L)
            .setTotalNetWeight(1L)
            .setNetUsed(1L)
            .setNetLimit(1L)
            .setEnergyLimit(1L)
            .build());

  }

  public void isExist(ByteString address1) {
    byte[] address = address1.toByteArray();
    byte[] hash0 = Sha256Sm3Hash.hash(address);
    byte[] hash1 = Sha256Sm3Hash.hash(hash0);
    byte[] checkSum = Arrays.copyOfRange(hash1, 0, 4);
    byte[] addchecksum = new byte[address.length + 4];
    System.arraycopy(address, 0, addchecksum, 0, address.length);
    System.arraycopy(checkSum, 0, addchecksum, address.length, 4);
    if(!existAddress.contains(Base58.encode(addchecksum))) {
     concurrentHashSet.add(address1);
    }
  }



  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    List<ByteString> list = new ArrayList<>(concurrentHashSet);
    for(ByteString target : list) {
      byte[] address = target.toByteArray();
      byte[] hash0 = Sha256Sm3Hash.hash(address);
      byte[] hash1 = Sha256Sm3Hash.hash(hash0);
      byte[] checkSum = Arrays.copyOfRange(hash1, 0, 4);
      byte[] addchecksum = new byte[address.length + 4];
      System.arraycopy(address, 0, addchecksum, 0, address.length);
      System.arraycopy(checkSum, 0, addchecksum, address.length, 4);
      writeDataToCsvFile("mainnet_newAddress.csv", Base58.encode(addchecksum));
    }
    Collections.sort(list1);


    int i = 1;
    /*
    afterTime = System.currentTimeMillis();
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    currentBlock = blockingStubFull1.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    afterBlockNum = currentBlock.getBlockHeader().getRawData().getNumber() + 2;
    Long blockNum = beforeBlockNum;
    Integer txsNum = 0;
    Integer topNum = 0;
    Integer totalNum = 0;
    Long energyTotal = 0L;
    String findOneTxid = "";

    NumberMessage.Builder builder = NumberMessage.newBuilder();
    while (blockNum <= afterBlockNum) {
      builder.setNum(blockNum);
      txsNum = blockingStubFull1.getBlockByNum(builder.build()).getTransactionsCount();
      totalNum = totalNum + txsNum;
      if (topNum < txsNum) {
        topNum = txsNum;
        findOneTxid = ByteArray.toHexString(Sha256Hash.hash(blockingStubFull1
            .getBlockByNum(builder.build()).getTransactionsList().get(2)
            .getRawData().toByteArray()));
        //logger.info("find one txid is " + findOneTxid);
      }

      blockNum++;
    }
    Long costTime = (afterTime - beforeTime - 31000) / 1000;
    logger.info("Duration block num is  " + (afterBlockNum - beforeBlockNum - 11));
    logger.info("Cost time are " + costTime);
    logger.info("Top block txs num is " + topNum);
    logger.info("Total transaction is " + (totalNum - 30));
    logger.info("Average Tps is " + (totalNum / costTime));

    infoById = PublicMethed.getTransactionInfoById(findOneTxid, blockingStubFull1);
    Long oneEnergyTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("EnergyTotal is " + oneEnergyTotal);
    logger.info("Average energy is " + oneEnergyTotal * (totalNum / costTime));
*/

    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}