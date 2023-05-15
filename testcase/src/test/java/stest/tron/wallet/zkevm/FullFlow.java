package stest.tron.wallet.zkevm;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.TransactionInfo;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.ReadonlyTransactionManager;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ZkEvmClient;

@Slf4j
public class FullFlow {

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

  Long startTime;


  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    int i = 0;
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.printAddress(testKey);

    startTime = System.currentTimeMillis();
    //Deposit trx from nile to zkEvm
    String data = 1 + "," + "\"" + WalletClient.encode58Check(testAddress) + "\"" + "," + depositTrxAmount + "," + "\"" + ZkEvmClient.zeroAddressInNile + "\"" + ","
        + false + "," + "\"" + "\"";
    logger.info(data);
    String txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(ZkEvmClient.nileBridgeAddress),
        "bridgeAsset(uint32,address,uint256,address,bool,bytes)", data, false,
        depositTrxAmount, 5000000000L, PublicMethed.getFinalAddress(ZkEvmClient.nileFoundationKey), ZkEvmClient.nileFoundationKey, blockingStubFull);

    logger.info("Deposit trx from nile to zkEvm txid: " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getReceipt()
        .getResultValue() == 1);


    //Deposit usdt contract
    String contractName = "shieldTrc20Token";

    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_shieldTrc20Token");
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_shieldTrc20Token");
    String constructorStr = "constructor(uint256,string,string)";
    Long totalSupply = 1000000000000000000L;
    data = totalSupply.toString() + "," + "\"TokenERC20\"" + "," + "\"zkEvmErc20\"";
    logger.info("data:" + data);
    txid  = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, constructorStr, data, "",
            5000000000L, 0L, 100, null,
            ZkEvmClient.nileFoundationKey, PublicMethed.getFinalAddress(ZkEvmClient.nileFoundationKey), blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("Deploy usdt contract txid: " + txid);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    ByteString contractAddressByteString = infoById.get().getContractAddress();
    byte[] contractAddressByte = infoById.get().getContractAddress().toByteArray();
    usdtErc20Contract = Base58.encode58Check(contractAddressByte);
    logger.info("Usdt contract address: " + usdtErc20Contract);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getReceipt()
        .getResultValue() == 1);

    //Approve bridge contract on usdt contract
    data = "\"" + ZkEvmClient.nileBridgeAddress + "\"" + "," + totalSupply.toString();
    txid = PublicMethed.triggerContract(contractAddressByte,
        "approve(address,uint256)", data, false,
        0, 5000000000L,PublicMethed.getFinalAddress(ZkEvmClient.nileFoundationKey),ZkEvmClient.nileFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("approve:" + txid);
    Assert.assertTrue(infoById.get().getReceipt().getResultValue() == 1);


    //Deposit usdt from nile to zkEvm(first deposit ,auto mapping)
    data = 1 + "," + "\"" + WalletClient.encode58Check(testAddress) + "\"" + "," + depositUsdtAmount + "," + "\"" + usdtErc20Contract + "\"" + ","
        + false + "," + "\"" + "\"";
    logger.info(data);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(ZkEvmClient.nileBridgeAddress),
        "bridgeAsset(uint32,address,uint256,address,bool,bytes)", data, false,
        0L, 5000000000L, PublicMethed.getFinalAddress(ZkEvmClient.nileFoundationKey), ZkEvmClient.nileFoundationKey, blockingStubFull);

    logger.info("Deposit usdt from nile to zkEvm txid: " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getReceipt()
        .getResultValue() == 1);

    //Deposit usdt from nile to zkEvm(second depoist ,exist mapping,ignore mapping)
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(ZkEvmClient.nileBridgeAddress),
        "bridgeAsset(uint32,address,uint256,address,bool,bytes)", data, false,
        0L, 5000000000L, PublicMethed.getFinalAddress(ZkEvmClient.nileFoundationKey), ZkEvmClient.nileFoundationKey, blockingStubFull);

    logger.info("Deposit usdt from nile to zkEvm txid: " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getReceipt()
        .getResultValue() == 1);

  }

  @Test(enabled = true)
  public void test01GetTrxBalanceFromZkEvm() throws Exception{

    BigInteger ethBalance = ZkEvmClient.getClient().ethGetBalance(ZkEvmClient.getConvertAddress(WalletClient.encode58Check(testAddress)),DefaultBlockParameterName.LATEST).send().getBalance();
    logger.info("Eth balance: " + ethBalance);

    int retry = 200;
    while (retry-- >= 0 && !ethBalance.equals(new BigInteger(String.valueOf(depositTrxAmount)).multiply(new BigInteger(String.valueOf(trxToZkEvmPrecision))))) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      ethBalance = ZkEvmClient.getClient().ethGetBalance(ZkEvmClient.getConvertAddress(WalletClient.encode58Check(testAddress)),DefaultBlockParameterName.LATEST).send().getBalance();
      logger.info("Eth balance: " + ethBalance);
    }

    Assert.assertTrue(retry > 0);
    Assert.assertEquals(ethBalance,new BigInteger(String.valueOf(depositTrxAmount)).multiply(new BigInteger(String.valueOf(trxToZkEvmPrecision))));
    logger.info("Trx deposit auto to zkEvm cost time: " + (System.currentTimeMillis() - startTime));
  }


  @Test(enabled = true)
  public void test02GetUsdtBalanceFromZkEvm() throws Exception{


    ReadonlyTransactionManager readonlyTransactionManager = new ReadonlyTransactionManager(ZkEvmClient.getClient(),ZkEvmClient.getConvertAddress(WalletClient.encode58Check(testAddress)));
    String userInfo = readonlyTransactionManager.sendCall(ZkEvmClient.getConvertAddress(usdtErc20Contract), ZkEvmClient.balanceOfEncode + ZkEvmClient.get64LengthStr(ZkEvmClient.getConvertAddress(WalletClient.encode58Check(testAddress))), DefaultBlockParameterName.LATEST);
    String[] array = ZkEvmClient.stringToStringArray(userInfo.substring(2), 64);
    Long usdtBalance = array.length > 0 ?Long.parseLong(array[0],16) : 0L;
    logger.info("USDT balance: " + usdtBalance);

    int retry = 200;
    while (retry-- >= 0 && !usdtBalance.equals(new BigInteger(String.valueOf(depositUsdtAmount)))) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      userInfo = readonlyTransactionManager.sendCall(ZkEvmClient.getConvertAddress(usdtErc20Contract), ZkEvmClient.balanceOfEncode + ZkEvmClient.get64LengthStr(ZkEvmClient.getConvertAddress(WalletClient.encode58Check(testAddress))), DefaultBlockParameterName.LATEST);
      array = ZkEvmClient.stringToStringArray(userInfo.substring(2), 64);
      usdtBalance = array.length > 0 ?Long.parseLong(array[0],16) : 0L;
      logger.info("USDT balance: " + usdtBalance);
      logger.info("Usdt deposit auto to zkEvm cost time: " + (System.currentTimeMillis() - startTime));
    }

    Assert.assertTrue(retry > 0);
    Assert.assertEquals(usdtBalance,new BigInteger(String.valueOf(depositUsdtAmount)).multiply(new BigInteger("2")));


  }




  @Test(enabled = true)
  public void test03DepositAssetFromZkEvmToNile() throws Exception{
    String url = "http://54.160.226.39:8080/tokenwrapped?orig_token_addr=" + ZkEvmClient.getConvertAddress(usdtErc20Contract) + "&orig_net=0";
    JSONObject jsonObject = HttpMethed.parseResponseContent(HttpMethed.createConnectForGet(url));

    HttpMethed.printJsonContent(jsonObject);

    String zkEvmErc20MappingAddress = jsonObject.getJSONObject("tokenwrapped").getString("wrapped_token_addr");
    logger.info(zkEvmErc20MappingAddress);


    //Deposit trx from zkEvm to Nile
    ZkEvmClient.bridgeAsset(zkEvmErc20MappingAddress, new BigInteger("1"),
        ZkEvmClient.getConvertAddress(WalletClient.encode58Check(testAddress)), new BigInteger(String.valueOf(depositUsdtAmount)).divide(new BigInteger("2")),
        ZkEvmClient.zeroAddressInZkEvm, false, "", testKey);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Deposit usdt from zkEvm to Nile
    ZkEvmClient.bridgeAsset(zkEvmErc20MappingAddress, new BigInteger("1"),
        ZkEvmClient.getConvertAddress(WalletClient.encode58Check(testAddress)), new BigInteger(String.valueOf(depositUsdtAmount)).divide(new BigInteger("2")),
        zkEvmErc20MappingAddress, false, "", testKey);









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

}


