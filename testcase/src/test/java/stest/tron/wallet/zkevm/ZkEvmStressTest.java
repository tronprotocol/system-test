package stest.tron.wallet.zkevm;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.ProtocolStringList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.ReadonlyTransactionManager;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethodForZkEvm;
import stest.tron.wallet.common.client.utils.Sha256Hash;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ZkEvmClient;

@Slf4j
public class ZkEvmStressTest {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey001);


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = "grpc.nile.trongrid.io:50051";
  private Long depositTrxAmount = 1000000L;
  private Long depositUsdtAmount = 1000000L;
  private Long trxToZkEvmPrecision = 1000000000000L;

  ECKey testEcKey = new ECKey(Utils.getRandom());
  byte[] testAddress = testEcKey.getAddress();
  String testKey = ByteArray.toHexString(testEcKey.getPrivKeyBytes());

  String usdtErc20Contract;

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    int i = 0;
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

  }
  @Test(enabled = false)
  public void test02GetUsdtBalanceFromZkEvm() throws Exception{

    String testAddress = "0xeff5dda00de11b0b8a90b024ecc4027dbb26680e";

    ReadonlyTransactionManager readonlyTransactionManager = new ReadonlyTransactionManager(ZkEvmClient.getClient(),testAddress);
    String userInfo = readonlyTransactionManager.sendCall("0xCdB95296Dda413B3593778de9631f86352C7c2a1", ZkEvmClient.balanceOfEncode + ZkEvmClient.get64LengthStr(testAddress), DefaultBlockParameterName.LATEST);
    String[] array = ZkEvmClient.stringToStringArray(userInfo.substring(2), 64);
    Long usdtBalance = array.length > 0 ?Long.parseLong(array[0],16) : 0L;
    logger.info("USDT balance: " + usdtBalance);


  }

  @Test(enabled = false)
  public void test03DepositAssetFromZkEvmToNile() throws Exception{
    String usdtErc20Contract = "TFWkh34ymJj841898nCjqSaqjJQFkFLtee";
    byte[] testAddress = WalletClient.decodeFromBase58Check("TUJ8dAeL3WsDGWqYdu5WY9P3PWJNZhCa6q");
    String testKey = "ac991ea195ca85df4b84d47578dbd79e664676f911f1f33a469a8e5e7fb60f36";






    String url = "http://54.160.226.39:8080/tokenwrapped?orig_token_addr=" + ZkEvmClient.getConvertAddress(usdtErc20Contract) + "&orig_net=0";
    JSONObject jsonObject = HttpMethed.parseResponseContent(HttpMethed.createConnectForGet(url));

    HttpMethed.printJsonContent(jsonObject);

    String zkEvmErc20MappingAddress = jsonObject.getJSONObject("tokenwrapped").getString("wrapped_token_addr");
    logger.info(zkEvmErc20MappingAddress);


    ReadonlyTransactionManager readonlyTransactionManager = new ReadonlyTransactionManager(ZkEvmClient.getClient(),ZkEvmClient.getConvertAddress(WalletClient.encode58Check(testAddress)));
    String userInfo = readonlyTransactionManager.sendCall(zkEvmErc20MappingAddress, ZkEvmClient.balanceOfEncode + ZkEvmClient.get64LengthStr(ZkEvmClient.getConvertAddress(WalletClient.encode58Check(testAddress))), DefaultBlockParameterName.LATEST);
    String[] array = ZkEvmClient.stringToStringArray(userInfo.substring(2), 64);
    Long usdtBalance = array.length > 0 ?Long.parseLong(array[0],16) : 0L;
    logger.info("USDT balance: " + usdtBalance);




    //Deposit trx from zkEvm to Nile
    ZkEvmClient.bridgeAsset(zkEvmErc20MappingAddress, new BigInteger("1"),
        ZkEvmClient.getConvertAddress(WalletClient.encode58Check(testAddress)), new BigInteger(String.valueOf(depositTrxAmount)).divide(new BigInteger("2")),
        ZkEvmClient.zeroAddressInZkEvm, false, "", BigInteger.ZERO,testKey);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Deposit usdt from zkEvm to Nile
    ZkEvmClient.bridgeAsset(zkEvmErc20MappingAddress, new BigInteger("1"),
        ZkEvmClient.getConvertAddress(WalletClient.encode58Check(testAddress)), new BigInteger(String.valueOf(depositUsdtAmount)).divide(new BigInteger("2")),
        zkEvmErc20MappingAddress, false, "", BigInteger.ZERO,testKey);




    int retryTime = 300;
    while (PublicMethed.queryAccount(testKey,blockingStubFull).getBalance() == 0 && retryTime-- > 0) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }

    Assert.assertTrue(retryTime > 0);
    Assert.assertEquals(PublicMethed.queryAccount(testKey,blockingStubFull).getBalance(),depositTrxAmount / 2);

    List<Object> parameters = Arrays.asList(usdtErc20Contract);
    String data = PublicMethed.parametersString(parameters);

    GrpcAPI.TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            WalletClient.decodeFromBase58Check(usdtErc20Contract),
            "balanceOf(address,uint256)",
            data,
            false,
            0,
            0,
            "0",
            0,
            testAddress,
            testKey,
            blockingStubFull);
    String hexBalance = Hex.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    long result = Long.parseLong(hexBalance, 16);
    Assert.assertEquals(depositUsdtAmount / 2 , result);



//cd586579
// 0000000000000000000000000000000000000000000000000000000000000001
// 000000000000000000000041c9068365e33930c73c57cf0cb7fcda333f5e3b99
// 00000000000000000000000000000000000000000000000000000000000f4240
// 0000000000000000000000413ccffa029ac765827a5eb4ed51adb8e81272bb2e
// 000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000

  }

  @Test(enabled = false, priority = 2)
  public void test02ClaimUsdtInNile() throws Exception{
    FullFlow.testKey = "";
    FullFlow.testAddress = PublicMethed.getFinalAddress(FullFlow.testKey);

    Long usdtDepositCnt = 422L;
    ProtocolStringList protocolStringList = PublicMethodForZkEvm.getProof(1,usdtDepositCnt.intValue()).getProof().getMerkleProofList();

    StringBuffer argsStr = new StringBuffer();
    argsStr.append("[\"");
    for(String merkleProof : protocolStringList) {
      argsStr.append(merkleProof);
      argsStr.append("\",\"");
    }
    argsStr.delete(argsStr.length()-2,argsStr.length());
    argsStr.append("],");
    argsStr.append(usdtDepositCnt);
    argsStr.append(",\"");
    argsStr.append(PublicMethodForZkEvm.getProof(1,usdtDepositCnt.intValue()).getProof().getMainExitRoot()).append("\",\"");
    argsStr.append(PublicMethodForZkEvm.getProof(1,usdtDepositCnt.intValue()).getProof().getRollupExitRoot()).append("\",");
    //originNetwork id : 1
    argsStr.append(0).append(",\"");
    //originTokenAddress
    argsStr.append("TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf").append("\",");
    //destinationNetwork id : 0
    argsStr.append(0).append(",");
    //destinationAddress
    argsStr.append("\"").append("TXTNcgJHD9GPfpiTbSG2VGtfdfii9VcpEr").append("\",");
    //Amount
    argsStr.append(9).append(",\"\"");

    System.out.println(argsStr.toString());




    byte[] consum = PublicMethed.decode58Check(ZkEvmClient.nileBridgeAddress);
    String methedStr = "claimAsset(bytes32[32],uint32,bytes32,bytes32,uint32,address,uint32,address,uint256,bytes)";
    //String argsStr = "[\"0x5a28ab5cbe4514f8350c63b48abb7ee428869fcf8999bebf6f7ce38cc6fdcec2\",\"0xe8601e1acc447b4aec089c5d44d710b5bdf8da59faf6b9c7b6314efc401c5482\",\"0x5cac0b9f05d21698436c8617ee3d2034237f5053eec5e875fe0137345b5d7c97\",\"0x39a4b097f89d3d929b5c99e2f8bd93b65f5d294c55a2400b1ff4ab686c26c8c0\",\"0x6082b0714d6e6cb92306fd35b13c422bb7b7f0de961691b243948a79e36288fe\",\"0xd7a4b69b096acef1292219abfb52f5a471bd6aeac57fe249a1e23db142696136\",\"0x42a82f349f326b8b93687a3c471347b4f0be3b8942c67788eb2644bedfabff6f\",\"0x1548d16470cc3e2102d7209758c9fb2c5969b15c07cf6783811104c8a6ee1080\",\"0x57e966cd91f7aff2c75d77d4abec624887d6203b8f3ce0d0ba7646fcb0bee3ff\",\"0xcefad4e508c098b9a7e1d8feb19955fb02ba9675585078710969d3440f5054e0\",\"0xf9dc3e7fe016e050eff260334f18a5d4fe391d82092319f5964f2e2eb7c1c3a5\",\"0xf8b13a49e282f609c317a833fb8d976d11517c571d1221a265d25af778ecf892\",\"0x3490c6ceeb450aecdc82e28293031d10c7d73bf85e57bf041a97360aa2c5d99c\",\"0xc1df82d9c4b87413eae2ef048f94b4d3554cea73d92b0f7af96e0271c691e2bb\",\"0x5c67add7c6caf302256adedf7ab114da0acfe870d449a3a489f781d659e8becc\",\"0xda7bce9f4e8618b6bd2f4132ce798cdc7a60e7e1460a7299e3c6342a579626d2\",\"0x2733e50f526ec2fa19a22b31e8ed50f23cd1fdf94c9154ed3a7609a2f1ff981f\",\"0xe1d3b5c807b281e4683cc6d6315cf95b9ade8641defcb32372f1c126e398ef7a\",\"0x5a2dce0a8a7f68bb74560f8f71837c2c2ebbcbf7fffb42ae1896f13f7c7479a0\",\"0xb46a28b6f55540f89444f63de0378e3d121be09e06cc9ded1c20e65876d36aa0\",\"0xc65e9645644786b620e2dd2ad648ddfcbf4a7e5b1a3a4ecfe7f64667a3f0b7e2\",\"0xf4418588ed35a2458cffeb39b93d26f18d2ab13bdce6aee58e7b99359ec2dfd9\",\"0x5a9c16dc00d6ef18b7933a6f8dc65ccb55667138776f7dea101070dc8796e377\",\"0x4df84f40ae0c8229d0d6069e5c8f39a7c299677a09d367fc7b05e3bc380ee652\",\"0xcdc72595f74c7b1043d0e1ffbab734648c838dfb0527d971b602bc216c9619ef\",\"0x0abf5ac974a1ed57f4050aa510dd9c74f508277b39d7973bb2dfccc5eeb0618d\",\"0xb8cd74046ff337f0a7bf2c8e03e10f642c1886798d71806ab1e888d9e5ee87d0\",\"0x838c5655cb21c6cb83313b5a631175dff4963772cce9108188b34ac87c81c41e\",\"0x662ee4dd2dd7b2bc707961b1e646c4047669dcb6584f0d8d770daf5d7e7deb2e\",\"0x388ab20e2573d171a88108e79d820e98f26c0b84aa8b2f4aa4968dbb818ea322\",\"0x93237c50ba75ee485f4c22adf2f741400bdf8d6a9cc7df7ecae576221665d735\",\"0x8448818bb4ae4562849e949e17ac16e0be16688e156b5cf15e098c627c0056a9\"],380,\"0xcafa2d79baf0be81cc9e368a65b71f0ec2f0a1de03c34240c0459aa36be68ce8\",\"0x31c00c24424a9f3f9e3d98d9b3bc5c68ea23b6c7c3fa7cffa7dc811fc907ded5\",0,\"TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf\",0,\"TXTNcgJHD9GPfpiTbSG2VGtfdfii9VcpEr\",10,\"\"";
    //System.out.println(argsStr);
    String txid = PublicMethed.triggerContract(consum, methedStr, argsStr.toString(),
        false, 0, 5000000000L, FullFlow.testAddress, FullFlow.testKey, blockingStubFull);


    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getReceipt()
        .getResultValue() == 1);


  }


  @Test(enabled = false, priority = 2)
  public void stressForNileToZkEvm() throws Exception {


    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.printAddress(testKey);

    int i = 0;
    while (i++ <= 20000) {
      ECKey testEcKey = new ECKey(Utils.getRandom());
      byte[] testAddress = testEcKey.getAddress();
      String testKey = ByteArray.toHexString(testEcKey.getPrivKeyBytes());

      //Deposit trx from nile to zkEvm
      String data = 1 + "," + "\"" + WalletClient.encode58Check(testAddress) + "\"" + "," + depositTrxAmount + "," + "\"" + ZkEvmClient.zeroAddressInNile + "\"" + ","
          + false + "," + "\"" + "\"";
      logger.info(data);
      String txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(ZkEvmClient.nileBridgeAddress),
          "bridgeAsset(uint32,address,uint256,address,bool,bytes)", data, false,
          depositTrxAmount, 5000000000L, PublicMethed.getFinalAddress(ZkEvmClient.nileFoundationKey), ZkEvmClient.nileFoundationKey, blockingStubFull);

      logger.info("Deposit trx from nile to zkEvm txid: " + txid);
/*
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      Assert.assertTrue(PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getReceipt()
          .getResultValue() == 1);
*/




      Long depositRealUsdtAmount = 1L;

      //deposit real usdt in nile
      String realUsdt58 = ZkEvmClient.getConvertAddress(ZkEvmClient.usdtAddressInTron);

      data = 1 + "," + "\"" + WalletClient.encode58Check(testAddress) + "\"" + "," + depositRealUsdtAmount + "," + "\"" + realUsdt58 + "\"" + ","
          + true + "," + "\"" + "\"";
      txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(ZkEvmClient.nileBridgeAddress),
          "bridgeAsset(uint32,address,uint256,address,bool,bytes)", data, false,
          0L, 5000000000L, PublicMethed.getFinalAddress(ZkEvmClient.nileFoundationKey), ZkEvmClient.nileFoundationKey, blockingStubFull);
      logger.info("Deposit real usdt from nile to zkEvm txid: " + txid);

      Thread.sleep(500L);

    }




  }


  @Test(enabled = false)
  public void testBridgeNotMissTransaction() throws Exception {
    Long scanStartBlock = 	36959733L;
    Long scanEndBlock = scanStartBlock - 20000L;
    String depositEncode = "cd586579";
    String claimEncode = "2cffd02e";
    for(long i = scanStartBlock; i >= scanEndBlock; i--) {
      NumberMessage.Builder builder = NumberMessage.newBuilder();
      builder.setNum(i);
      Block block = blockingStubFull.getBlockByNum(builder.build());
      logger.info("current block:" + i);


      List<Transaction> transactionList = block.getTransactionsList();
      for (Transaction transaction : transactionList) {

        Any any = transaction.getRawData().getContract(0).getParameter();
        Integer contractType =  transaction.getRawData().getContract(0).getType().getNumber();
        if(contractType.equals(31)) {
          TriggerSmartContract triggerSmartContract = any.unpack(TriggerSmartContract.class);
          if(Base58.encode58Check(triggerSmartContract.getContractAddress().toByteArray()).equalsIgnoreCase(ZkEvmClient.nileBridgeAddress)) {
            String txid = ByteArray.toHexString(Sha256Hash.hash(true,
                transaction.getRawData().toByteArray()));
            TransactionInfo transactionInfo = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
            if (ByteArray.toHexString(triggerSmartContract.getData().toByteArray()).substring(0,8)
                .equalsIgnoreCase(depositEncode) && transactionInfo
                .getResult().name().equalsIgnoreCase("SUCESS")) {
              //logger.info("Deposit txid:" + txid);
              //Assert.assertTrue(PublicMethodForZkEvm.getTransactionInfo(txid).getData().getTransaction().getSrcHashUrl().contains("nile.tronscan.org"));
              String[] array = ZkEvmClient.stringToStringArray(ByteArray.toHexString(triggerSmartContract.getData().toByteArray()).substring(8),64);
              writeDataToCsvFile("zkevm_stress.csv",txid + ","
                  + ZkEvmClient.getConvertAddress("0x" + array[1].substring(24)) + ","
              + "0x" + array[1].substring(24));
            } else if (ByteArray.toHexString(triggerSmartContract.getData().toByteArray()).substring(0,8)
                .equalsIgnoreCase(claimEncode) && transactionInfo
                .getResult().name().equalsIgnoreCase("SUCESS")) {
              logger.info("Claim txid:" + txid);

              //Assert.assertTrue(!PublicMethodForZkEvm.getTransactionInfo(txid).getData().getTransaction().getSrcHashUrl().isEmpty());
            }
            }


          }
        }


    }
    System.exit(1);
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



  /**
   * constructor.
   */
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



}


