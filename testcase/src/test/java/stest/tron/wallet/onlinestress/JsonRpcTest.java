package stest.tron.wallet.onlinestress;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.BalanceContract.DelegateResourceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.BalanceContract.UnDelegateResourceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.WithdrawBalanceContract;
import org.tron.protos.contract.BalanceContract.WithdrawExpireUnfreezeContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.CommonParameter;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.JsonRpcBase;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Sha256Hash;
import stest.tron.wallet.common.client.utils.Utils;


@Slf4j

public class JsonRpcTest extends JsonRpcBase {
  private JSONObject responseContent;
  private HttpResponse response;

  ECKey getBalanceECKey = new ECKey(Utils.getRandom());
  byte[] getBalanceTestAddress = getBalanceECKey.getAddress();
  String getBalanceTestKey = ByteArray.toHexString(getBalanceECKey.getPrivKeyBytes());

  private final String foundationKey001 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private  Long sendAmount = 20000000L;
  private final Long transferAmount = 2L;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    //fullnode = "47.94.243.150:50051";
    fullnode = "39.106.55.169:50051";
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.printAddress(getBalanceTestKey);
  }


  public void doCheck(ByteString address) throws Exception {
    if (addressSet.contains(address)) {
      return;
    } else {
      addressSet.add(address);
      if(addressSet.size() % 100 == 0) {
        logger.info("Set size : " + addressSet.size());
      }
    }
    logger.info("checking :" + WalletClient.encode58Check(address.toByteArray()));
    checkTrxBalance(address);
    //checkTrc10Balance(address);
    return;

  }

  public void checkTrc10Balance(ByteString address) throws Exception {
    JsonArray params = new JsonArray();
    params.add(ByteArray.toHexString(address.toByteArray()));
    params.add("0x" + Long.toHexString(blockNum));
    JsonObject requestBody = getJsonRpcBody("tron_getAssets", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    JSONArray jsonArray = responseContent.getJSONArray("result");
    for(int i = 0; i < jsonArray.size();i++) {
      Long tokenId = Long.parseLong(jsonArray.getJSONObject(i).getString("key").substring(2),16);
      Long assertBalance = Long.parseLong(jsonArray.getJSONObject(i).getString("value").substring(2),16);

      Long balanceFromGetAccount = PublicMethed.getAssetIssueValue(address.toByteArray(),ByteString.copyFromUtf8(tokenId.toString()),blockingStubFull);
      Assert.assertEquals(assertBalance,balanceFromGetAccount);
    }

  }

  public void compareBalance(ByteString userAddress,ByteString contractAddress) throws Exception {
    String paramString = "\"" + Base58.encode58Check(userAddress.toByteArray()) + "\"";
    BigInteger constantBalance = new BigInteger(ByteArray.toHexString(PublicMethed.triggerConstantContractForExtention(contractAddress.toByteArray(),
        "balanceOf(address)",paramString,
        false,0,1000000000L, "0", 0,userAddress.toByteArray(),
        foundationKey001,blockingStubFull).getConstantResult(0).toByteArray()),16);


    String addressParam =
        "000000000000000000000000"
            + ByteArray.toHexString(userAddress.toByteArray()).substring(2); // [0,3)

    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(userAddress.toByteArray()));
    param.addProperty("to", ByteArray.toHexString(contractAddress.toByteArray()));
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    //balanceOf(address) keccak encode
    param.addProperty("data", "0x70a08231" + addressParam);


    JsonArray params = new JsonArray();
    params.add(param);
    params.add("0x" + Long.toHexString(blockNum));

    JsonObject requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String balance = responseContent.getString("result").substring(2);
    BigInteger jsonrpcBalanceOf = new BigInteger(balance, 16);
    Assert.assertEquals(jsonrpcBalanceOf,constantBalance);



  }

  public void checkSmartContractBalanceOf(ByteString userAddress, ByteString contractAddress,String txid) throws Exception {
    TransactionInfo transactionInfo = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    if(transactionInfo.getLogCount() == 1) {
      String topic = ByteArray.toHexString(transactionInfo.getLog(0).getTopics(0).toByteArray());
      if(topic.equalsIgnoreCase("ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef")
      ) {
        byte[] fromAddress = ByteArray.fromHexString("41" + ByteArray.toHexString(transactionInfo.getLog(0).getTopics(1).toByteArray()).substring(24));
        byte[] toAddress = ByteArray.fromHexString("41" + ByteArray.toHexString(transactionInfo.getLog(0).getTopics(2).toByteArray()).substring(24));

        compareBalance(ByteString.copyFrom(fromAddress),contractAddress);
        compareBalance(ByteString.copyFrom(toAddress),contractAddress);





      }
    }

  }

  public void checkTrxBalance(ByteString address) throws Exception {
    JsonArray params = new JsonArray();
    params.add(ByteArray.toHexString(address.toByteArray()));
    params.add("latest");
    JsonObject requestBody = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(noStateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String balance = responseContent.getString("result").substring(2);
    Long latestBalance = Long.parseLong(balance, 16);

    params = new JsonArray();
    params.add(ByteArray.toHexString(address.toByteArray()));
    params.add("0x" + Long.toHexString(blockNum));
    requestBody = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long stateTreeBalance = Long.parseLong(balance, 16);
    Assert.assertEquals(stateTreeBalance,latestBalance);

    Assert.assertEquals((long)stateTreeBalance,PublicMethed.queryAccount(address.toByteArray(),blockingStubFull).getBalance());
  }

  //public String noStateTreeNode = "47.94.243.150:50545";
  public String noStateTreeNode = "39.106.55.169:50545";
  //public String stateTreeNode = "39.106.110.245:50545";
  public String stateTreeNode = "39.106.55.169:50546";
  public HashSet<ByteString> addressSet = new HashSet<>();
  public Long blockNum = 6000L;

  @Test(enabled = true, description = "State tree with eth_getBalance")
  public void test01StateTreeWithEthGetBalance() throws Exception {



    Long startNum = blockNum;
    //Long endNum = startNum - 20000;;
    Long endNum = 0L;
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(startNum);
    HashSet<ByteString> set = new HashSet<>();
    while (startNum-- >= endNum) {
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
              doCheck(transferContract.getOwnerAddress());
              doCheck(transferContract.getToAddress());
              break;
            case 2:
              TransferAssetContract transferAssetContract = any.unpack(TransferAssetContract.class);
              doCheck(transferAssetContract.getOwnerAddress());
              doCheck(transferAssetContract.getToAddress());
              break;
            case 31:
              TriggerSmartContract triggerSmartContract = any.unpack(TriggerSmartContract.class);
              doCheck(triggerSmartContract.getOwnerAddress());
              doCheck(triggerSmartContract.getContractAddress());
              checkSmartContractBalanceOf(triggerSmartContract.getOwnerAddress(),triggerSmartContract.getContractAddress(),
                  ByteArray.toHexString(
                      Sha256Hash.hash(
                          CommonParameter.getInstance().isECKeyCryptoEngine(),
                          transaction.getRawData().toByteArray())));
              break;
            case 13:
              WithdrawBalanceContract withdrawBalanceContract
                  = any.unpack(WithdrawBalanceContract.class);
              doCheck(withdrawBalanceContract.getOwnerAddress());
              break;
            case 11:
              FreezeBalanceContract freezeBalanceContract = any.unpack(FreezeBalanceContract.class);
              doCheck(freezeBalanceContract.getOwnerAddress());
              break;
            case 0:
              AccountCreateContract accountCreateContract = any.unpack(AccountCreateContract.class);
              doCheck(accountCreateContract.getOwnerAddress());
              break;
               case 4:
              VoteWitnessContract voteWitnessContract = any.unpack(VoteWitnessContract.class);
              doCheck(voteWitnessContract.getOwnerAddress());
            case 12:
              UnfreezeBalanceContract unfreezeBalanceContract
                  = any.unpack(UnfreezeBalanceContract.class);
              doCheck(unfreezeBalanceContract.getOwnerAddress());
              break;
            case 30:
              CreateSmartContract createSmartContract = any.unpack(CreateSmartContract.class);
              doCheck(createSmartContract.getOwnerAddress());
              break;
            case 46:
              AccountPermissionUpdateContract accountPermissionUpdateContract
                  = any.unpack(AccountPermissionUpdateContract.class);
              doCheck(accountPermissionUpdateContract.getOwnerAddress());
              break;
            case 54:
              FreezeBalanceV2Contract freezeBalanceV2Contract
                  = any.unpack(FreezeBalanceV2Contract.class);
              doCheck(freezeBalanceV2Contract.getOwnerAddress());
              break;
            case 55:
              UnfreezeBalanceV2Contract unfreezeBalanceV2Contract
                  = any.unpack(UnfreezeBalanceV2Contract.class);
              doCheck(unfreezeBalanceV2Contract.getOwnerAddress());
              break;
            case 56:
              WithdrawExpireUnfreezeContract withdrawExpireUnfreezeContract
                  = any.unpack(WithdrawExpireUnfreezeContract.class);
              doCheck(withdrawExpireUnfreezeContract.getOwnerAddress());
              break;
            case 57:
              DelegateResourceContract delegateResourceContract
                  = any.unpack(DelegateResourceContract.class);
              doCheck(delegateResourceContract.getOwnerAddress());
              doCheck(delegateResourceContract.getReceiverAddress());
              break;
            case 58:
              UnDelegateResourceContract unDelegateResourceContract
                  = any.unpack(UnDelegateResourceContract.class);
              doCheck(unDelegateResourceContract.getOwnerAddress());
              doCheck(unDelegateResourceContract.getReceiverAddress());
              break;
            default:
              logger.info("Unknown type:" + contractType);
              continue;

          }
        } catch (Exception e) {
          e.printStackTrace();

        }





      }
    }



  }


  @Test(enabled = false, description = "State tree with tron_getToken10")
  public void test02StateTreeWithTronGetToken10() throws Exception {
    Assert.assertTrue(PublicMethed.transferAsset(getBalanceTestAddress, jsonRpcAssetId.getBytes(),sendAmount,
        jsonRpcOwnerAddress,jsonRpcOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    final Long beforeBalance = sendAmount;
    final Long beforeBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethed.transferAsset(getBalanceTestAddress, jsonRpcAssetId.getBytes(),transferAmount,
        jsonRpcOwnerAddress,jsonRpcOwnerKey, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    final Long afterBalance = sendAmount + transferAmount;
    final Long afterBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();


    //Assert before trc10 balance
    JsonArray params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(getBalanceTestAddress).substring(2));
    params.add("0x" + Long.toHexString(beforeBlockNumber));
    JsonObject requestBody = getJsonRpcBody("tron_getToken10", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String balance = responseContent.getString("result").substring(2);
    Long assertBalance = Long.parseLong(balance, 16);
    Assert.assertEquals(assertBalance,beforeBalance);


    //Assert after balance
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(getBalanceTestAddress).substring(2));
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("tron_getToken10", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    assertBalance = Long.parseLong(balance, 16);
    Assert.assertEquals(assertBalance,afterBalance);



    //State tree not open didn't support block number
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(getBalanceTestAddress).substring(2));
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("tron_getToken10", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String wrongMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(wrongMessage,"QUANTITY not supported, just support TAG as latest");
  }


  @Test(enabled = false, description = "State tree with eth_call")
  public void test03StateTreeWithEthCall() throws Exception {
    String selector = "transfer(address,uint256)";
    String addressParam =
        "000000000000000000000000"
            + ByteArray.toHexString(getBalanceTestAddress).substring(2); // [0,3)

    String transferValueParam = "0000000000000000000000000000000000000000000000000000000000000100";
    String paramString = addressParam + transferValueParam;
    trc20Txid =
        PublicMethed.triggerContract(
            ByteArray.fromHexString(trc20AddressHex),
            selector,
            paramString,
            true,
            0,
            maxFeeLimit,
            "0",
            0,
            jsonRpcOwnerAddress,
            jsonRpcOwnerKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(trc20Txid,blockingStubFull).get()
        .getLogCount() == 1);


    final Long beforeBalance = Long.parseLong("100",16);
    final Long beforeBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    trc20Txid =
        PublicMethed.triggerContract(
            ByteArray.fromHexString(trc20AddressHex),
            selector,
            paramString,
            true,
            0,
            maxFeeLimit,
            "0",
            0,
            jsonRpcOwnerAddress,
            jsonRpcOwnerKey,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(trc20Txid,blockingStubFull).get()
        .getLogCount() == 1);

    final Long afterBalance = beforeBalance + beforeBalance;
    final Long afterBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();


    //Assert before trc20 balance
    JsonObject param = new JsonObject();
    HttpMethed.waitToProduceOneBlock(httpFullNode);
    param.addProperty("from", ByteArray.toHexString(getBalanceTestAddress));
    param.addProperty("to", trc20AddressHex);
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    //balanceOf(address) keccak encode
    param.addProperty("data", "0x70a08231" + addressParam);


    JsonArray params = new JsonArray();
    params.add(param);
    params.add("0x" + Long.toHexString(beforeBlockNumber));

    JsonObject requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String balance = responseContent.getString("result").substring(2);
    Long assertBalance = Long.parseLong(balance, 16);
    Assert.assertEquals(assertBalance,beforeBalance);


    //Assert after balance
    params = new JsonArray();
    params.add(param);
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    assertBalance = Long.parseLong(balance, 16);
    Assert.assertEquals(assertBalance,afterBalance);



    //State tree not open didn't support block number
    params = new JsonArray();
    params.add(param);
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String wrongMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(wrongMessage,"QUANTITY not supported, just support TAG as latest");
  }


  @Test(enabled = false, description = "State tree with eth_getCode")
  public void test04StateTreeWithEthGetCode() throws Exception {
    String getCodeFromGetContract = ByteArray
        .toHexString(PublicMethed.getContract(selfDestructAddressByte,blockingStubFull)
            .getBytecode().toByteArray());

    logger.info("Get contract bytecode: " + getCodeFromGetContract);

    JsonArray params = new JsonArray();
    params.add(ByteArray.toHexString(selfDestructAddressByte));
    params.add("latest");

    JsonObject requestBody = getJsonRpcBody("eth_getCode", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String getCodeFromLatest= responseContent.getString("result").substring(2);
    logger.info("Latest getCode:" + getCodeFromLatest);

    //Assert.assertEquals(getCodeFromJsonRpc,getCodeFromGetContract);



    final Long beforeBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    txid =
        PublicMethed.triggerContract(
            selfDestructAddressByte,
            "kill()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            jsonRpcOwnerAddress,
            jsonRpcOwnerKey,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertEquals(PublicMethed.getTransactionInfoById(txid,blockingStubFull).get()
        .getReceipt().getResult(), contractResult.SUCCESS);
    final Long afterBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();


    //Assert before selfDestruct eth_getCode
    params = new JsonArray();
    params.add(ByteArray.toHexString(selfDestructAddressByte));
    params.add("0x" + Long.toHexString(beforeBlockNumber));

    requestBody = getJsonRpcBody("eth_getCode", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String getCodeFromBeforeKill = responseContent.getString("result").substring(2);
    logger.info("Before kill : " + getCodeFromBeforeKill);

    Assert.assertEquals(getCodeFromBeforeKill,getCodeFromLatest);


    //Assert after self destruct
    params = new JsonArray();
    params.add(ByteArray.toHexString(selfDestructAddressByte));
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_getCode", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String getCodeFromAfterKill = responseContent.getString("result");
    logger.info("After kill : " + getCodeFromAfterKill);
    Assert.assertEquals(getCodeFromAfterKill,"0x");



    //State tree not open didn't support block number
    params = new JsonArray();
    params.add(ByteArray.toHexString(selfDestructAddressByte));
    params.add("0x" + Long.toHexString(beforeBlockNumber));
    requestBody = getJsonRpcBody("eth_getCode", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String wrongMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(wrongMessage,"QUANTITY not supported, just support TAG as latest");
  }

  @Test(enabled = false, description = "State tree with eth_getStorageAt")
  public void test05StateTreeWithEthGetStorageAt() throws Exception {
    JsonArray params = new JsonArray();
    params.add(contractAddressFrom58);
    params.add("0x2");
    params.add("latest");
    JsonObject requestBody = getJsonRpcBody("eth_getStorageAt", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String result = responseContent.getString("result").substring(2);
    long beforePos2 = Long.parseLong(result, 16);
    logger.info("beforePos2:" + beforePos2);
    final Long beforeBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();

    txid =
        PublicMethed.triggerContract(
            ByteArray.fromHexString(contractAddressFrom58),
            "changePos2()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            jsonRpcOwnerAddress,
            jsonRpcOwnerKey,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertEquals(PublicMethed.getTransactionInfoById(txid,blockingStubFull).get()
        .getReceipt().getResult(), contractResult.SUCCESS);

    final Long afterBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();


    //Assert before pos2 eth_getStorageAt
    params = new JsonArray();
    params.add(contractAddressFrom58);
    params.add("0x2");
    params.add("0x" + Long.toHexString(beforeBlockNumber));

    requestBody = getJsonRpcBody("eth_getStorageAt", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    Long beforeNumberEthGetStorageAt = Long.parseLong(responseContent.getString("result").substring(2),16);
    logger.info("Before change pos2 : " + beforeNumberEthGetStorageAt);

    Assert.assertEquals((long)beforeNumberEthGetStorageAt,beforePos2);


    //Assert after change pos2
    params = new JsonArray();
    params.add(contractAddressFrom58);
    params.add("0x2");
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_getStorageAt", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    Long afterNumberEthGetStorageAt = Long.parseLong(responseContent.getString("result").substring(2),16);

    Assert.assertEquals((long)afterNumberEthGetStorageAt,2);



    //State tree not open didn't support block number
    params = new JsonArray();
    params.add(contractAddressFrom58);
    params.add("0x2");
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_getStorageAt", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String wrongMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(wrongMessage,"QUANTITY not supported, just support TAG as latest");
  }







  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
