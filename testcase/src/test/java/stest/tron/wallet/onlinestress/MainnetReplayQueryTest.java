package stest.tron.wallet.onlinestress;

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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.DiversifierMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExpandedSpendingKeyMessage;
import org.tron.api.GrpcAPI.IncomingViewingKeyDiversifierMessage;
import org.tron.api.GrpcAPI.IncomingViewingKeyMessage;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.PaymentAddressMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.api.GrpcAPI.ViewingKeyMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.CommonParameter;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Sha256Hash;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.zen.address.DiversifierT;


@Slf4j
public class MainnetReplayQueryTest {

  public final String foundationAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  public final byte[] foundationAccountAddress = PublicMethed.getFinalAddress(foundationAccountKey);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = "47.94.243.150:50051";//014
  //private String fullnode = "10.40.10.244:50051";//014

  AtomicLong atomicLong = new AtomicLong();

  private static Long replayTimes = 500000L;

  String[] trc20Contract = new String[]{
      //"TNUC9Qb1rRpS5CbWLmNMxXBjyFoydXjWFR",//WTRX
      //"TN3W4H6rK2ce4vX9YnFQHwKENnHjoxb3m9",//BTC
      //"THb4CqiFdwNHsWsQCs4JhzwjMWys4aqCbF",//ETH
      "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",//USDT
      "TPYmHEhy5n8TCEfYGqW2rPxsghSfzghPDn",//USDD
      "TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8",//USDC
      "TLa2f6VPqDgRE67v1736s7bJ8Ray5wYjU7",//WIN
      //"TMwFHYXLJaRUPeW6421aqXL4ZEzPRFGkGT",//USDJ
      //"TUpMhErZL2fhh4sVNULAbNKLokS4GjC1F4",//TUSD
      "TCFLL5dx5ZJdKnWuesXxi1VPwjLVmWZZy9",//JST
      "TSSMHYeV2uE9qYH95DqyoCuNCzEL1NvU3S"//SUN
  };






  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    getAccountList();
  }

  @Test(enabled = true, threadPoolSize = 20, invocationCount = 20)
  public void test01MainnetReplayQueryTest() throws InterruptedException {
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Integer threadId = Integer.valueOf(Thread.currentThread().getName().split("-")[2]);
    threadId = getQueryIndex(threadId);

    switch (threadId) {
      case 0:
      //GetTransactionInfoByBlockNum,getTransactionInfoByid
        getTransactionInfo(blockingStubFull);
      case 1:
      //Get account
        getAccountBalance(blockingStubFull);
        break;
      case 2:
      //Get trc10 balance
        getAccountTrc10Balance(blockingStubFull);
        break;
      case 3:
      //GetTransactionById,getblock
        getTransaction(blockingStubFull);
        break;
      default:
      //Trc20 balanceOf
        queryTrc20ContractBalanceOf(blockingStubFull);
        break;
    }








  }


  HashSet<String> set = new HashSet<>();
  public void queryTrc20ContractBalanceOf(WalletGrpc.WalletBlockingStub blockingStubFull) {
    for(int index = 0; index < replayTimes;index++) {
      Integer queryAccountIndex = index % accountList.size();
      String paramStr = "\"" + accountList.get(queryAccountIndex) + "\"";
      for(int i = 0; i < trc20Contract.length;i++) {
        TransactionExtention transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(trc20Contract[i]), "balanceOf(address)",
                paramStr, false, 0, 0, "0", 0,
                foundationAccountAddress, foundationAccountKey, blockingStubFull);


        System.out.println(Hex.toHexString(transactionExtention
            .getConstantResult(0).toByteArray()));

 /*      if(!"0000000000000000000000000000000000000000000000000000000000000000".equalsIgnoreCase(Hex.toHexString(transactionExtention
          .getConstantResult(0).toByteArray()))) {
        set.add(queryAddress);

       if(set.size() == 300) {
          for(String address : set) {
            writeDataToCsvFile("useAccount.csv",address);
          }
          System.exit(1);
        }
      }*/
      }
    }



  }

  public static void getAccountBalance(WalletGrpc.WalletBlockingStub blockingStubFull) {
    for(int index = 0; index < replayTimes;index++) {
      Integer queryAccountIndex = index % accountList.size();
      System.out.println(PublicMethed.queryAccount(WalletClient.decodeFromBase58Check(accountList.get(queryAccountIndex)),blockingStubFull).getBalance());
    }
  }

  public static void getTransactionInfo(WalletGrpc.WalletBlockingStub blockingStubFull) {
    for(int m = 0; m < replayTimes;m++) {
      Long currentBlockNum = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build()).getBlockHeader().getRawData().getNumber();
      TransactionInfoList transactionInfoList = PublicMethed.getTransactionInfoByBlockNum(currentBlockNum,blockingStubFull).get();
      for(int i = 0; i < transactionInfoList.getTransactionInfoCount();i++) {
        if(i > 20) {
          break;
        }
        System.out.println("TransactionInfo timestamp:" + PublicMethed.getTransactionInfoById(ByteArray.toHexString(transactionInfoList.getTransactionInfo(i).getId().toByteArray()),blockingStubFull)
            .get().getBlockTimeStamp());
      }
    }
  }


  public static void getTransaction(WalletGrpc.WalletBlockingStub blockingStubFull) {
    for(int m = 0; m < replayTimes;m++) {
      Block block = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
      for(int i = 0; i < block.getTransactionsCount();i++) {
        if(i > 20) {
          break;
        }
        String txid = ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                block.getTransactions(i).getRawData().toByteArray()));
        System.out.println("TransactionByid sig count: " + PublicMethed.getTransactionById(txid,blockingStubFull).get().getSignatureCount());
      }
    }



  }



  public static void getAccountTrc10Balance(WalletGrpc.WalletBlockingStub blockingStubFull) {
    for(int index = 0; index < replayTimes;index++) {
      Integer queryAccountIndex = index % accountList.size();
      PublicMethed.getAssetBalanceByAssetId(
          ByteString.copyFromUtf8("1002000"), WalletClient.decodeFromBase58Check(accountList.get(queryAccountIndex)), blockingStubFull);
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


  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


  public static int getQueryIndex(Integer threadId) {
    return threadId % 10;
  }


  public static List<String> accountList = new ArrayList<>();
  private static void getAccountList() {
    String line = null;
    try {
      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(new FileInputStream(System.getProperty("user.dir") + '/' + "src/test/resources/replay_account.csv"),"utf-8"));

      //int i=0;
      while ((line = bufferedReader.readLine()) != null) {
        accountList.add(line);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}