package stest.tron.wallet.zkevm;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.ReadonlyTransactionManager;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;
import stest.tron.wallet.common.client.utils.ZkEvmClient;

@Slf4j
public class ZkEvmGetAccountInfo {

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
  private ManagedChannel searchChannelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {


  }

  @Test(enabled = true)
  public void test01GetZkEvmUsdtBalance() throws Exception{

    String balanceOfEncode = "0x70a08231";
    String targetAddress = "0xEBAE50590810B05D4B403F13766F213518EDEF65";
    String targetContract = "0xf999F7C9E427A6cFc2feF0fca050277339DFCFE3";
    ReadonlyTransactionManager readonlyTransactionManager = new ReadonlyTransactionManager(ZkEvmClient.getClient(),targetAddress);
    String userInfo = readonlyTransactionManager.sendCall(targetContract, balanceOfEncode + ZkEvmClient.get64LengthStr(targetAddress), DefaultBlockParameterName.LATEST);
    String[] array = ZkEvmClient.stringToStringArray(userInfo.substring(2), 64);
    Long balance = Long.parseLong(array[0],16);
    logger.info("USDT balance: " + balance);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
  }

}


