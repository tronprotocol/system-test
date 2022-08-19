package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class CreateReplayTestUsedAccount {
  private final String foundationKey =
      "a79a37a3d868e66456d76b233cb894d664b75fd91861340f3843db05ab3a8c66";
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);


  private String fullnode = "47.94.243.150:50051";
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  public  static String fileName = "random200Account";
  public static HashMap<String,String> accountMap = new HashMap<>();

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    getAccountList();


  }

  @Test(enabled = false)
  public void create200TestAccountToCsv() {

    for(int i = 0; i < 200;i++) {
      ECKey randomEckey = new ECKey(Utils.getRandom());
      String randomKey = ByteArray.toHexString(randomEckey.getPrivKeyBytes());
      byte[] randomAddressByte = randomEckey.getAddress();
      String randomAddress = WalletClient.encode58Check(randomAddressByte);

      writeDataToCsvFile("random200Account",randomKey + "," + randomAddress);
    }
  }


  @Test(enabled = false)
  public void sendCoinTo200TestAccount() {
    for(String key : accountMap.keySet()) {
      String address = accountMap.get(key);
      String txid = PublicMethed.sendcoinGetTransactionId(WalletClient.decode58Check(address),100000000000000L,foundationAddress,foundationKey,blockingStubFull);
      try {
        Thread.sleep(100);
      } catch (Exception e) {

      }
    }
  }


  @Test(enabled = true)
  public void create200TokenOneByOneAndDispatchTo200Account() {
    int i = 0;
    for(String key : accountMap.keySet()) {
      logger.info("当前个数：" + i++);
      String address = accountMap.get(key);
      if (!PublicMethed.queryAccount(key,blockingStubFull).getAssetIssuedID().toStringUtf8().contains("100")) {

        long start = System.currentTimeMillis() + 10000;
        long end = System.currentTimeMillis() + 1000000000;
        PublicMethed.createAssetIssueGetTxid(WalletClient.decode58Check(address), "replay_" + System.currentTimeMillis(), "replay_" + System.currentTimeMillis(),100000000000000000L,
            1, 1, start, end, 1, "wwwwww", "wwwwwwww", 0L, 0L, 1L, 1L, key, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
      } else {
        continue;
      }

      Account getAssetIdFromThisAccount;
      getAssetIdFromThisAccount = PublicMethed.queryAccount(WalletClient.decode58Check(address), blockingStubFull);
      ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();


      for(String toKey : accountMap.keySet()) {
        if(toKey.equalsIgnoreCase(key)) {
          continue;
        }
        String toAddress = accountMap.get(toKey);
        Assert.assertTrue(PublicMethed.transferAsset(WalletClient.decode58Check(toAddress), assetAccountId.toByteArray(),
            100000L, WalletClient.decode58Check(address), key, blockingStubFull));
        try {
          Thread.sleep(20);
        } catch (Exception e) {

        }
      }
    }
  }



  @Test(enabled = true)
  public void sendTrc20To200Account() {

    String trc20OwnerKey = "44FE180410D7BF05E41388A881C3C5566C6667840116EC25C6FC924CE678FC4A";
    String trc20OwnerAddress = "TRRJbGw8BC8S5ueuium2aTBuRrLnkytnUi";

    List<String> contractList = new ArrayList<>();
    getContractList(contractList);



    for(int i = contractList.size()-1; i >= 0;i--) {
      logger.info("索引到:" + i);
    //for(int i = 0; i < contractList.size();i++) {
      for(String key : accountMap.keySet()) {
        String address = accountMap.get(key);

        String initParmes = "\"" + address + "\",\"10000\"";


        String txid = PublicMethed.triggerContract(
                WalletClient.decodeFromBase58Check(contractList.get(i)),
                "transfer(address,uint256)",
            initParmes,
                false,
                0,
                1000000000L,
                WalletClient.decode58Check(trc20OwnerAddress),
                trc20OwnerKey,
                blockingStubFull);

        try {
          Thread.sleep(10);
        } catch (Exception e) {

        }
      }

    }


  }



  @Test(enabled = true)
  public void sendTrc20To200Contract() {
    //Every contract receive 50 trc10 token

    AtomicInteger contractIndex = new AtomicInteger();


    List<String> contractList = new ArrayList<>();
    getContractList(contractList);

    int times = 0;
    for(String key : accountMap.keySet()) {
      if(times++ == 50) {
        break;
      }
      String address = accountMap.get(key);


      Account getAssetIdFromThisAccount;
      getAssetIdFromThisAccount = PublicMethed.queryAccount(WalletClient.decode58Check(address), blockingStubFull);
      ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();


      for(String contractAddress : contractList) {

        Assert.assertTrue(PublicMethed.transferAsset(WalletClient.decode58Check(contractAddress), assetAccountId.toByteArray(),
            100000L, WalletClient.decode58Check(address), key, blockingStubFull));
        try {
          Thread.sleep(20);
        } catch (Exception e) {

        }
      }
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


  private static void getAccountList() {
    String line=null;
    try {
      //BufferedReader bufferedReader=new BufferedReader(new FileReader(filePath));
      BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"utf-8"));

      //int i=0;
      while((line=bufferedReader.readLine())!=null){
        String[] accountInforamtion = line.split(",");
        accountMap.put(accountInforamtion[0],accountInforamtion[1]);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  private static void getContractList(List<String> list) {
    String line=null;
    try {
      //BufferedReader bufferedReader=new BufferedReader(new FileReader(filePath));
      BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream("trc20Contract.csv"),"utf-8"));

      //int i=0;
      while((line=bufferedReader.readLine())!=null){
        list.add(line);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }



}

