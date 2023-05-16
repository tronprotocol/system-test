package stest.tron.wallet.zkevm;

import bridge.v1.Zkevm.Deposit;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.TransactionInfo;
import org.web3j.abi.datatypes.Int;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.tx.ReadonlyTransactionManager;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;
import stest.tron.wallet.common.client.utils.PublicMethodForZkEvm;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ZkEvmClient;

@Slf4j
public class ClaimAssetInNile {

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

  private String zkEvmErc20MappingAddress;
  private Long depositTrxAmount = 1000000000L;
  private Long depositUsdtAmount = 1000000L;
  private Long trxToZkEvmPrecision = 1000000000000L;


  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
/*
    FullFlow.testKey = "de148b99895b0c514e19d738c0a346db8c951af4ac1fd5a740e8a6777aea6057";
    FullFlow.testAddress = PublicMethed.getFinalAddress(FullFlow.testKey);
    FullFlow.zkEvmDepositTrxTxid = "0x1a39356a939a354319d05b1f214968e4d7c3c6d01234766e01f80b4f1d4f09dc";
    FullFlow.zkEvmDepositUsdtTxid = "0xf1c895d0601ffeaf8b492d52ff00fde2d0f3fc6009700efad1796c4ed004fa7f";
    FullFlow.usdtErc20Contract = "TEcVs79mi4yovj5Q3md3CzDyLFafT8FYLg";
    FullFlow.depositUsdtFromZkEvmToNileAmount = BigInteger.ONE;
    FullFlow.depositTrxFromZkEvmToNileAmount = new BigInteger(trxToZkEvmPrecision.toString());
*/

    PublicMethed.sendcoin(FullFlow.testAddress,500000000L,PublicMethed.getFinalAddress(ZkEvmClient.nileFoundationKey),
        ZkEvmClient.nileFoundationKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = true, priority = 2)
  public void test01ClaimTrxInNile() throws Exception{


    List<Deposit> depositList = PublicMethodForZkEvm
        .getBridges(PublicMethodForZkEvm.getETHAddress(FullFlow.testAddress),0,30).getDepositsList();
    Integer retryTimes = 300;
    while (depositList.size() < 5 && retryTimes-- > 0) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      depositList = PublicMethodForZkEvm
          .getBridges(PublicMethodForZkEvm.getETHAddress(FullFlow.testAddress),0,30).getDepositsList();
    }
    Assert.assertTrue(retryTimes > 0);

    Long trxDepositCnt = -1L;
    Long usdtDepositCnt = -1L;
    String trxMainExitRoot;
    String trxRollupExitRoot;

    for(Deposit deposit : depositList) {
      if(deposit.getNetworkId() == 1 && deposit.getOrigAddr().equalsIgnoreCase(ZkEvmClient.zeroAddressInZkEvm)) {
        trxDepositCnt = deposit.getDepositCnt();
      }
      if(deposit.getNetworkId() == 1 && deposit.getOrigAddr().equalsIgnoreCase(FullFlow.usdtErc20Contract)) {
        usdtDepositCnt = deposit.getDepositCnt();
      }
    }

    ProtocolStringList protocolStringList = PublicMethodForZkEvm.getProof(1,trxDepositCnt.intValue()).getProof().getMerkleProofList();

    StringBuffer argsStr = new StringBuffer();
    argsStr.append("[\"");
    for(String merkleProof : protocolStringList) {
      argsStr.append(merkleProof);
      argsStr.append("\",\"");
    }
    argsStr.delete(argsStr.length()-2,argsStr.length());
    argsStr.append("],");
    argsStr.append(trxDepositCnt);
    argsStr.append(",\"");
    argsStr.append(PublicMethodForZkEvm.getProof(1,trxDepositCnt.intValue()).getProof().getMainExitRoot()).append("\",\"");
    argsStr.append(PublicMethodForZkEvm.getProof(1,trxDepositCnt.intValue()).getProof().getRollupExitRoot()).append("\",");
    //originNetwork id : 1
    argsStr.append(0).append(",\"");
    //originTokenAddress
    argsStr.append(ZkEvmClient.zeroAddressInNile).append("\",");
    //destinationNetwork id : 0
    argsStr.append(0).append(",");
    //destinationAddress
    argsStr.append("\"").append(Base58.encode58Check(FullFlow.testAddress)).append("\",");
    //Amount
    argsStr.append(FullFlow.depositTrxFromZkEvmToNileAmount).append(",\"\"");

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


  @Test(enabled = true, priority = 2)
  public void test02ClaimUsdtInNile() throws Exception{


    List<Deposit> depositList = PublicMethodForZkEvm
        .getBridges(PublicMethodForZkEvm.getETHAddress(FullFlow.testAddress),0,30).getDepositsList();
    Integer retryTimes = 300;
    while (depositList.size() < 5 && retryTimes-- > 0) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      depositList = PublicMethodForZkEvm
          .getBridges(PublicMethodForZkEvm.getETHAddress(FullFlow.testAddress),0,30).getDepositsList();
    }
    Assert.assertTrue(retryTimes > 0);
    Long usdtDepositCnt = -1L;
    String trxMainExitRoot;
    String trxRollupExitRoot;

    for(Deposit deposit : depositList) {
      if(deposit.getNetworkId() == 1 && deposit.getOrigAddr().equalsIgnoreCase(PublicMethodForZkEvm.getETHAddress(WalletClient.decodeFromBase58Check(FullFlow.usdtErc20Contract)))) {
        usdtDepositCnt = deposit.getDepositCnt();
      }
    }

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
    argsStr.append(FullFlow.usdtErc20Contract).append("\",");
    //destinationNetwork id : 0
    argsStr.append(0).append(",");
    //destinationAddress
    argsStr.append("\"").append(Base58.encode58Check(FullFlow.testAddress)).append("\",");
    //Amount
    argsStr.append(FullFlow.depositUsdtFromZkEvmToNileAmount).append(",\"\"");

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


