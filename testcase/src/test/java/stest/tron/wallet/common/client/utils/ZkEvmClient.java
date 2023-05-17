package stest.tron.wallet.common.client.utils;
import com.alibaba.fastjson.JSON;
import io.reactivex.Flowable;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import jnr.ffi.Struct.BYTE;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.BatchRequest;
import org.web3j.protocol.core.BatchResponse;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.events.Notification;
import org.web3j.utils.Numeric;
import stest.tron.wallet.common.client.WalletClient;

@Slf4j
public class ZkEvmClient {
  private static volatile Web3j web3j;
  private static volatile Admin admin;

  public static String nileFoundationKey = "acd7dd9093ae00a66dd9676b5d59531bc18f4b9ad7934226360de57768d6cbd8";
  public static String nileBridgeAddress = "TU458zUkKGn6WZsD2EEXW5pweAj1kdgKkL";
  public static String zkEvmBridgeAddress = "0xBe43Fc5Cb5Ff02C063C6CbeE1B23eB090E5966bb";
  public static String balanceOfEncode = "0x70a08231";
  public static String zeroAddressInZkEvm = "0x0000000000000000000000000000000000000000";
  public static String zeroAddressInNile = "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb";
  //public static String targetAddress = "0xEBAE50590810B05D4B403F13766F213518EDEF65";
  //public static String targetContract = "0xf999F7C9E427A6cFc2feF0fca050277339DFCFE3";
  public static String receiveAddress = "";
  public static int netTron = 0;
  public static int netZkEvm = 1;
  public static String usdtAddressInTron = "0xECa9bC828A3005B9a3b909f2cc5c2a54794DE05F";
  public static String bttAddressInTron = "0xd5A8752a5Fc64A1cE09Df7B195df6b669d09271F";
  public static String usddAddressInTron = "0x4a3a5dd265bd974B4DE0Bbe33FAa7EFb8b7b87e8";
  public static String maticAddressInTron = "0x9A19d9982407E17128099f2846021b17935C6DEE";

  public static String usdtAddressInZkEvm = "0xf999F7C9E427A6cFc2feF0fca050277339DFCFE3";
  public static String bttAddressInZkEvm = "0x4643BC0B3310cC3aDb9f8f39AfF719bE9870930E";
  public static String usddAddressInZkEvm = "0x57cD5165De482D45a8aB008950A2582f57292Fef";
  public static String maticAddressInZkEvm = "0x7a6d59A607D2E585Fc9138c80b772374EFab0A89";
  public static String tokenTypeTrx = "TRX";
  public static String tokenTypeErc20 = "ERC20";

  private ZkEvmClient() {
  }

  public static Web3j getClient() {
    if (web3j == null) {
      Class var0 = ZkEvmClient.class;
      synchronized(ZkEvmClient.class) {
        if (web3j == null) {

          web3j = Web3j.build(new HttpService("http://54.160.226.39:8123"));
        }
      }
    }

    return web3j;
  }

  static {
    try {
      System.out.println(">>>>>>>> web3JClientHost:" + " http://54.160.226.39:8123");
    } catch (Exception var4) {
      var4.printStackTrace();
    }

  }

  public static String get64LengthStr(String str) {
    if(str.length() >= 64) {
      return str;
    }
    if(str.substring(0,2).equalsIgnoreCase("0x")) {
      str = str.substring(2);
    }
    while (str.length()!=64) {
      str = "0" + str;
    }

    return str;

  }

  public static String[] stringToStringArray(String src, int length) {
    //检查参数是否合法
    if (null == src || src.equals("")) {
      return null;
    }

    if (length <= 0) {
      return null;
    }
    int n = (src.length() + length - 1) / length; //获取整个字符串可以被切割成字符子串的个数
    String[] split = new String[n];
    for (int i = 0; i < n; i++) {
      if (i < (n - 1)) {
        split[i] = src.substring(i * length, (i + 1) * length);
      } else {
        split[i] = src.substring(i * length);
      }
    }
    return split;
  }


  public static void transferErc20(String erc20TokenAddress, String toAddress, BigInteger amount, String privateKey) throws  Exception{
    Integer chainId = 202305;
    Integer gasPriceStep = 10;

    Credentials credentials = Credentials.create(privateKey);
    BigInteger nonce = getNonce(credentials.getAddress());
    BigInteger gasPrice = gasPrice();
    Function function = new Function("transfer", Arrays.<Type>asList(new Address(toAddress),new Uint256(amount)),
        Arrays.asList(new TypeReference<Utf8String>() {
        }));
    String encodedFunction = FunctionEncoder.encode(function);
    BigInteger gasLimit = getEstimateGas(erc20TokenAddress,credentials.getAddress(),encodedFunction);
    RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, erc20TokenAddress, BigInteger.ZERO, encodedFunction);
    byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, chainId,credentials);
    String hexValue = Numeric.toHexString(signMessage);
    EthSendTransaction ethSendTransaction = (EthSendTransaction) ZkEvmClient.getClient().ethSendRawTransaction(hexValue).sendAsync().get();


    Integer retryTimes = 0;
    while(!JSON.toJSONString(ethSendTransaction).contains("transactionHash") && retryTimes++ < 5) {
      Thread.sleep(500);
      gasPrice = gasPrice().multiply(new BigInteger("100").add(new BigInteger(String.valueOf(retryTimes * gasPriceStep)))).divide(new BigInteger("100"));
      System.out.println("Gas price :" + gasPrice);
      rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, erc20TokenAddress, BigInteger.ZERO, encodedFunction);
      signMessage = TransactionEncoder.signMessage(rawTransaction, chainId,credentials);
      hexValue = Numeric.toHexString(signMessage);
      ethSendTransaction = (EthSendTransaction) ZkEvmClient.getClient().ethSendRawTransaction(hexValue).sendAsync().get();
      logger.info(JSON.toJSONString(ethSendTransaction));
    }


    retryTimes = 0;
    EthGetTransactionReceipt ethGetTransactionReceipt = ZkEvmClient.getClient().ethGetTransactionReceipt(ethSendTransaction.getTransactionHash()).send();
    while (retryTimes++ <= 30) {
      try {
        String status = ethGetTransactionReceipt.getTransactionReceipt().get().getStatus();
        if(status.equalsIgnoreCase("0x1")) {
          System.out.println("ToAddress:" + toAddress + "  , " + ethSendTransaction.getTransactionHash());
          break;
        }
      } catch (Exception e) {
        logger.info(ethSendTransaction.getTransactionHash() + " 暂时没上链, 重试次数：" + retryTimes);
      }
      Thread.sleep(3000);
      ethGetTransactionReceipt = ZkEvmClient.getClient().ethGetTransactionReceipt(ethSendTransaction.getTransactionHash()).send();

    }

  }


  public static String approve(String erc20Address, String spenderAddress ,String privateKey) throws Exception {
    Credentials credentials = Credentials.create(privateKey);
    BigInteger nonce = getNonce(credentials.getAddress());
    BigInteger gasPrice = gasPrice();
    Function function = new Function("approve", Arrays.<Type>asList(
        new Address(spenderAddress), new Uint256(BigInteger.valueOf(9000000000000000000L))),
        Arrays.asList(new TypeReference<Utf8String>() {
        })
    );

    String encodedFunction = FunctionEncoder.encode(function);

    BigInteger gasLimit = BigInteger.valueOf(3000000L);
    RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, erc20Address, BigInteger.ZERO, encodedFunction);
    byte[] signMessage = TransactionEncoder.signMessage(rawTransaction,credentials);
    String hexValue = Numeric.toHexString(signMessage);
    EthSendTransaction ethSendTransaction = (EthSendTransaction) ZkEvmClient.getClient().ethSendRawTransaction(hexValue).sendAsync().get();
    return ethSendTransaction.getTransactionHash();


  }

  public static String bridgeAsset(String triggerContractAddress, BigInteger destinationNetwork,
      String destinationAddress, BigInteger amount, String erc20TokenAddress,
      Boolean forceUpdateGlobalExitRoot,String permitData, BigInteger value, String privateKey) throws  Exception{
    Integer chainId = 202305;
    Integer gasPriceStep = 10;

    Credentials credentials = Credentials.create(privateKey);
    BigInteger nonce = getNonce(credentials.getAddress());
    BigInteger gasPrice = gasPrice();
    byte[] utfEncoded = permitData.getBytes(StandardCharsets.UTF_8);


    Function function = new Function("bridgeAsset", Arrays.<Type>asList(
        new Uint32(destinationNetwork), new Address(destinationAddress),new Uint256(amount),new Address(erc20TokenAddress),
        new Bool(forceUpdateGlobalExitRoot),new DynamicBytes("".getBytes())),
        Collections.<TypeReference<?>>emptyList()
/*        Arrays.asList(new TypeReference<Utf8String>() {
        })*/
    );
    String encodedFunction = FunctionEncoder.encode(function);
    //BigInteger gasLimit = getEstimateGas(triggerContractAddress, credentials.getAddress(), encodedFunction);
    BigInteger gasLimit = BigInteger.valueOf(3000000L);
    RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, triggerContractAddress, value, encodedFunction);
    byte[] signMessage = TransactionEncoder.signMessage(rawTransaction,credentials);
    String hexValue = Numeric.toHexString(signMessage);
    EthSendTransaction ethSendTransaction = (EthSendTransaction) ZkEvmClient.getClient().ethSendRawTransaction(hexValue).sendAsync().get();


    Integer retryTimes = 0;
    while(!JSON.toJSONString(ethSendTransaction).contains("transactionHash") && retryTimes++ < 5) {
      Thread.sleep(500);
      gasPrice = gasPrice().multiply(new BigInteger("100").add(new BigInteger(String.valueOf(retryTimes * gasPriceStep)))).divide(new BigInteger("100"));
      System.out.println("Gas price :" + gasPrice);
      rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, triggerContractAddress, value, encodedFunction);
      signMessage = TransactionEncoder.signMessage(rawTransaction,credentials);
      hexValue = Numeric.toHexString(signMessage);
      ethSendTransaction = (EthSendTransaction) ZkEvmClient.getClient().ethSendRawTransaction(hexValue).sendAsync().get();
      logger.info(JSON.toJSONString(ethSendTransaction));
    }


    retryTimes = 0;
    EthGetTransactionReceipt ethGetTransactionReceipt = ZkEvmClient.getClient().ethGetTransactionReceipt(ethSendTransaction.getTransactionHash()).send();
    while (retryTimes++ <= 30) {
      try {
        String status = ethGetTransactionReceipt.getTransactionReceipt().get().getStatus();
        if(status.equalsIgnoreCase("0x1")) {
          System.out.println("Deposit txid :" + ethSendTransaction.getTransactionHash());
          break;
        }
        if(status.equalsIgnoreCase("0x0")) {
          System.out.println("Deposit txid revert:" + ethSendTransaction.getTransactionHash());
          break;
        }
      } catch (Exception e) {
        logger.info(ethSendTransaction.getTransactionHash() + " 暂时没上链, 重试次数：" + retryTimes);
      }
      Thread.sleep(3000);
      ethGetTransactionReceipt = ZkEvmClient.getClient().ethGetTransactionReceipt(ethSendTransaction.getTransactionHash()).send();

    }
    return ethSendTransaction.getTransactionHash();

  }


  public static BigInteger getEstimateGas(String contractAddress, String userAddress, String encodedFunction) throws Exception {
    EthEstimateGas ethEstimateGas = ZkEvmClient.getClient().ethEstimateGas(
            Transaction.createEthCallTransaction(userAddress, contractAddress, encodedFunction))
        .sendAsync().get();
    return ethEstimateGas.getAmountUsed().multiply(new BigInteger("150")).divide(new BigInteger("100"));
  }



  public static BigInteger getNonce(String address) throws IOException {
    return ((EthGetTransactionCount)ZkEvmClient.getClient().ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send()).getTransactionCount();
  }

  public static BigInteger gasPrice() throws ExecutionException, InterruptedException {
    EthGasPrice ethGasPrice = (EthGasPrice)ZkEvmClient.getClient().ethGasPrice().sendAsync().get();
    return ethGasPrice == null ? null : ethGasPrice.getGasPrice();
  }

  public static String getConvertAddress(String address) {
    if(!address.substring(0,2).equalsIgnoreCase("0x")
    && !address.substring(0,1).equalsIgnoreCase("T")) {
      logger.info("Address format wrong, please check");
      return null;
    }

    if(address.substring(0,2).equalsIgnoreCase("0x")) {
      String hexAddress = "41" + address.substring(2);
      return Base58.encode58Check(ByteArray.fromHexString(hexAddress));
    }

    String hexAddress = ByteArray.toHexString(WalletClient.decodeFromBase58Check(address));
    return "0x" + hexAddress.substring(2);

  }




}
