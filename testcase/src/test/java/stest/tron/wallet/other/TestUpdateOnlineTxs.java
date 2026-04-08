package stest.tron.wallet.other;

import io.grpc.ManagedChannel;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.utils.PublicMethod;

import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class TestUpdateOnlineTxs extends TronBaseTest {  private byte[] transferTokenContractAddress = null;

  //just test key
  private String dev001Key = "8132dd4fe6c1140d60fadd69eab3975831fe681e4b28705f78c89b94d44af2f4";
  private byte[] dev001Address = PublicMethod.getFinalAddress(dev001Key);

  byte[] contractD = null;
  byte[] create2Address;

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 300100_000_000L,
            fromAddress, foundationKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "")
  public void sunswapV1() {
    //    String usddContractAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";
    //    String methodSign = "tokenToTokenSwapInput(uint256,uint256,uint256,uint256,address)";
    //    boolean hex = false;
    //    long deadline = System.currentTimeMillis() / 1000+2000;
    //    String param = "20000000,1,1,1654923588,\"" + usddContractAddress + "\"";
    //    long feeLimit = 1000000000L;
    //    String exchangeAdd = "TQn9Y2khEsLJW1ChVWFMSMeRDow5KcbLSE";
    //    String txid1 = PublicMethod.triggerContract(PublicMethod.decode58Check(exchangeAdd),
    //    methodSign, param,
    //        false, 0, feeLimit, dev001Address, dev001Key, blockingStubFull);
    //    Optional<Protocol.TransactionInfo> info1 =
    //        PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
    //    System.out.println(info1.get().toString());

    String methodSign = "trxToTokenSwapInput(uint256,uint256)";
    boolean hex = false;
    long deadline = System.currentTimeMillis() / 1000 + 3000;
    String param = "100000,1654933319";
    long feeLimit = 1000000000L;
    String exchangeAdd = "TQn9Y2khEsLJW1ChVWFMSMeRDow5KcbLSE";  //usdt
    String txid1 = PublicMethod.triggerContract(PublicMethod.decode58Check(exchangeAdd),
        methodSign, param, false, 50000000, maxFeeLimit, dev001Address,
        dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info1 =
        PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
    System.out.println(info1.toString());
  }

  @Test(enabled = true, description = "trx swap usdd")
  public void sunswapV11() {
    String methodSign = "trxToTokenSwapInput(uint256,uint256)";
    String param = "1091242047502,1654933319";
    String exchangeAdd = "TSJWbBJAS8HgQCMJfY5drVwYDa7JBAm6Es"; //usdd
    String txid1 = PublicMethod.triggerContract(PublicMethod.decode58Check(exchangeAdd),
        methodSign, param, false, 50000000, maxFeeLimit, dev001Address,
        dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info1 =
        PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
    System.out.println(info1.toString());
  }

  @Test(enabled = true, description = "")
  public void sunswapV2() {
    /*String jstContractAddress = "TKzxdSv2FZKQrEqkKVgp5DcwEXBEKMg2Ax";
    String methodSign = "swapExactETHForTokens(uint256,address,address,uint256)";
    boolean hex = false;
    long deadline = System.currentTimeMillis() / 1000;
    String param = "\"80447716923213891\",[\"\",\"\"],\"1\",\""
        + deadline + "\",\"" + jstContractAddress + "\"";
    long feeLimit = 1000000000L;*/
  }

  @Test(enabled = true)
  public void sunio2PoolAddliq() {
    String methedStr = "add_liquidity(uint256[2],uint256)";
    String argsStr = "[1000000,1000000000000000000],1";
    String pool = "TAUGwRhmCP518Bm4VBqv7hDun9fg8kYjC4";  //old 2pool
    String txid = PublicMethod.triggerContract(PublicMethod.decode58Check(pool), methedStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info.toString());
  }

  @Test(enabled = true)
  public void sunio2PoolDeposit() {
    String methedStr = "deposit(uint256)";
    String argsStr = "\"99308209820\"";
    String gague = "TBSRZYLZ2pguF3EqLz86Kt9hZ4eqKEQMSY";  //old 2pool
    String txid = PublicMethod.triggerContract(PublicMethod.decode58Check(gague),
        methedStr, argsStr, false, 0, maxFeeLimit, dev001Address,
        dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info.toString());
  }

  @Test(enabled = true)
  public void justlendBorrow() {
    String methedStr = "borrow(uint256)";
    String argsStr = "1313828980000"; //USDD
    // String argsStr = "1";
    String jusdd = "TX7kybeP6UwTBRHLNPYmswFESHfyjm9bAS";
    // String jUSDT = "TXJgMdjVX5dKiQaUi9QobwNxtSQaFqccvd";
    String txid = PublicMethod.triggerContract(PublicMethod.decode58Check(jusdd),
        methedStr, argsStr, false, 0, maxFeeLimit, dev001Address,
        dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info.toString());

  }

  @Test(enabled = true)
  public void justlendMint() {
    String methedStr = "mint()";
    String argsStr = "10000000";
    String jtrx = "TE2RzoSV3wFK99w6J9UnnZ4vLfXYoxvRwP"; //USDD
    String txid = PublicMethod.triggerContract(PublicMethod.decode58Check(jtrx), methedStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info.toString());

  }

  //
  @Test(enabled = true)
  public void justlendRepayBorrow() {
    String methedStr = "repayBorrow(uint256)";
    String argsStr = "1000000000000";
    String jtrx = "TSXv71Fy5XdL3Rh2QfBoUu3NAaM4sMif8R"; //tusd
    String txid = PublicMethod.triggerContract(PublicMethod.decode58Check(jtrx), methedStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info.toString());

  }

  @Test(enabled = true)
  public void bttcDepositJst() {
    String methedStr = "depositFor(address,address,bytes)";
    String argsStr = "\"TH48niZfbwHMyqZwEB8wmHfzcvR8ZzJKC6\","
        + "\"TCFLL5dx5ZJdKnWuesXxi1VPwjLVmWZZy9\","
        + "\"0x000000000000000000000000000000000000000000000000000000e8d4a51000\"";
    String rootChainManager = "TDgrSuii9e7HLfY1DhEBxkcFa3vrLgS3Gx"; //tusd
    String txid = PublicMethod.triggerContract(PublicMethod.decode58Check(rootChainManager),
        methedStr, argsStr, false, 0, maxFeeLimit,
        dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info.toString());

  }

  @Test(enabled = true)
  public void approve() {
    String methedStr = "approve(address,uint256)";
    String add = "\"TBSRZYLZ2pguF3EqLz86Kt9hZ4eqKEQMSY\"";
    String argsStr = add + ",-1";
    //    String token = "TPYmHEhy5n8TCEfYGqW2rPxsghSfzghPDn";//usdd
    //    String token = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";//usdt
    String token = "TXcJ6pCEGKeLEYXrVnLhqpCVuKfV6wgsfC"; //old2pool
    String txid = PublicMethod.triggerContract(PublicMethod.decode58Check(token),
        methedStr, argsStr, false, 0, maxFeeLimit, dev001Address,
        dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info.toString());

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    //    PublicMethod.freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}

