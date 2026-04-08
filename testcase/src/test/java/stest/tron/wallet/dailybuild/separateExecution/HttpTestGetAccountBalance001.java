package stest.tron.wallet.dailybuild.separateExecution;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Retry;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class HttpTestGetAccountBalance001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(0);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] assetOwnerAddress = ecKey2.getAddress();
  String assetOwnerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] randomAddress = ecKey3.getAddress();
  Long amount = 2048000000L;
  String txid;
  Long sendcoinBlockNumber;
  String sendcoinBlockHash;
  Long deployContractBlockNumber;
  String deployContractBlockHash;
  Long fee;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true, description = "Deploy smart contract by http")
  public void test01DeployContractForTest() {
    HttpMethod.waitToProduceOneBlock(httpnode);
    PublicMethod.printAddress(assetOwnerKey);
    txid = HttpMethod.sendCoin(httpnode, fromAddress, assetOwnerAddress, amount, "", testKey002);
    HttpMethod.waitToProduceOneBlock(httpnode);
    txid = HttpMethod.sendCoin(httpnode, assetOwnerAddress, randomAddress,
        amount / 1000000L, "", assetOwnerKey);
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getTransactionInfoById(httpnode, txid);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    sendcoinBlockNumber = responseContent.getLong("blockNumber");
    Assert.assertTrue(sendcoinBlockNumber > 0);

    response = HttpMethod.getBlockByNum(httpnode, sendcoinBlockNumber);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    sendcoinBlockHash = responseContent.getString("blockID");

    String contractName = "transferTokenContract";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractTrcToken001_transferTokenContract");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractTrcToken001_transferTokenContract");
    txid = HttpMethod
        .deployContractGetTxid(httpnode, contractName, abi, code, 1000000L, 1000000000L, 100,
            11111111111111L, 0L, 0, 0L, assetOwnerAddress, assetOwnerKey);

    HttpMethod.waitToProduceOneBlock(httpnode);
    logger.info(txid);

    response = HttpMethod.getTransactionInfoById(httpnode, txid);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    fee = responseContent.getLong("fee");
    deployContractBlockNumber = responseContent.getLong("blockNumber");
    String receiptString = responseContent.getString("receipt");
    Assert
        .assertEquals(HttpMethod.parseStringContent(receiptString).getString("result"), "SUCCESS");

    response = HttpMethod.getBlockByNum(httpnode, deployContractBlockNumber);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    deployContractBlockHash = responseContent.getString("blockID");
  }

  /**
   * constructor.
   */
  @Test(enabled = true, priority=2, description = "Get account balance by http", groups = {"daily", "serial"})
  public void test01GetAccountBalance() {
    response = HttpMethod.getAccountBalance(httpnode, assetOwnerAddress,
        sendcoinBlockNumber, sendcoinBlockHash);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);
    final Long beforeBalance = responseContent.getLong("balance");

    response = HttpMethod.getAccountBalance(httpnode, assetOwnerAddress,
        deployContractBlockNumber, deployContractBlockHash);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);
    Long afterBalance = responseContent.getLong("balance");

    Assert.assertTrue(beforeBalance - afterBalance == fee);


    response = HttpMethod.getAccountBalance(httpnode, assetOwnerAddress,
        deployContractBlockNumber, deployContractBlockHash);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, priority=2, description = "Get block balance by http", groups = {"daily", "serial"})
  public void test02GetBlockBalance() {
    response = HttpMethod.getBlockBalance(httpnode,
        sendcoinBlockNumber, sendcoinBlockHash);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);
    Assert.assertEquals(sendcoinBlockNumber, responseContent.getJSONObject("block_identifier")
        .getLong("number"));
    JSONObject transactionObject = responseContent.getJSONArray("transaction_balance_trace")
        .getJSONObject(0);
    Assert.assertEquals(transactionObject.getString("type"), "TransferContract");
    Assert.assertTrue(Math.abs(transactionObject.getJSONArray("operation")
        .getJSONObject(0).getLong("amount")) == 100000L);
    Assert.assertTrue(Math.abs(transactionObject.getJSONArray("operation")
        .getJSONObject(1).getLong("amount")) == (amount / 1000000L + 1000000L));
    Assert.assertTrue(Math.abs(transactionObject.getJSONArray("operation")
        .getJSONObject(2).getLong("amount")) == amount / 1000000L);

    response = HttpMethod.getBlockBalance(httpnode,
        deployContractBlockNumber, deployContractBlockHash);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);

    Integer transactionIndex = 0;
    for(int i = 0; i < responseContent.getJSONArray("transaction_balance_trace").size();i++) {
      transactionObject = responseContent.getJSONArray("transaction_balance_trace").getJSONObject(i);
      if(transactionObject.getString("transaction_identifier").equalsIgnoreCase(txid)) {
        transactionIndex = i;
        break;
      }
    }

    transactionObject = responseContent.getJSONArray("transaction_balance_trace").getJSONObject(transactionIndex);
    Assert.assertEquals(transactionObject.getString("transaction_identifier"), txid);
    Assert.assertEquals(transactionObject.getString("type"), "CreateSmartContract");
    Assert.assertTrue(transactionObject.getJSONArray("operation")
        .getJSONObject(0).getLong("amount") == -fee);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, retryAnalyzer = Retry.class, priority=2, description = "Get burn trx by http", groups = {"daily", "serial"})
  public void test03GetBurnTrx() {

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] assetOwnerAddress = ecKey2.getAddress();
    String assetOwnerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    HttpMethod.sendCoin(httpnode, fromAddress, assetOwnerAddress, amount, "", testKey002);
    HttpMethod.waitToProduceOneBlock(httpnode);
    final Long beforeBurnTrxAmount = HttpMethod.getBurnTrx(httpnode);
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] receiverAddress = ecKey3.getAddress();

    String txid = HttpMethod.sendCoin(httpnode, assetOwnerAddress, receiverAddress,
        amount - 2003000L, "", assetOwnerKey);
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    //HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    Long afterBurnTrxAmount = HttpMethod.getBurnTrx(httpnode);
    logger.info(afterBurnTrxAmount + "  :   " + beforeBurnTrxAmount);
    Assert.assertTrue(afterBurnTrxAmount - beforeBurnTrxAmount == 1100000L);

    Assert.assertEquals(afterBurnTrxAmount, HttpMethod.getBurnTrxFromSolidity(httpSolidityNode));
    Assert.assertEquals(afterBurnTrxAmount, HttpMethod.getBurnTrxFromPbft(httpPbftNode));
  }

  /**
   * constructor.
   */
  @Test(enabled = false, priority=2, description = "Get receipt root by http", groups = {"daily", "serial"})
  public void test04GetReceiptRootByHttp() {
    response = HttpMethod.getBlockByNum(httpnode, sendcoinBlockNumber);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    String receiptsRoot = responseContent.getJSONObject("block_header").getJSONObject("raw_data")
        .getString("receiptsRoot");
    Assert.assertNotEquals(receiptsRoot,
        "0000000000000000000000000000000000000000000000000000000000000000");
    Assert.assertFalse(receiptsRoot.isEmpty());

  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethod.disConnect();
  }
}