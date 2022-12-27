package stest.tron.wallet.dailybuild.freezeV2;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class DelegateResourceV2TimestampTest {
  private static final long sendAmount = 100000000L;
  private static final long frozenAmount = 10000000L;
  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozen1Address = ecKey1.getAddress();
  String frozen1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] receiver3Address = ecKey4.getAddress();
  String receiver3Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiver1Address = ecKey2.getAddress();
  String receiver1Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] receiver2Address = ecKey3.getAddress();
  String receiver2Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
      .get(0);
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFullSolidity = null;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception{
    PublicMethed.printAddress(frozen1Key);

    PublicMethed.printAddress(receiver1Key);
    PublicMethed.printAddress(receiver2Key);
    PublicMethed.printAddress(receiver3Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);


    if(!PublicMethed.freezeV2ProposalIsOpen(blockingStubFull)) {
      if (channelFull != null) {
        channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      }
      throw new SkipException("Skipping freezeV2 test case");
    }
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubFullSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);


    Assert.assertTrue(PublicMethed.sendcoin(frozen1Address, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));

    Assert.assertTrue(PublicMethed.sendcoin(receiver1Address, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiver2Address, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiver3Address, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethed.freezeBalanceV2(frozen1Address,frozenAmount,0,
        frozen1Key,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceV2(receiver1Address,frozenAmount,0,
        receiver1Key,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceV2(receiver2Address,frozenAmount,0,
        receiver2Key,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceV2(receiver3Address,frozenAmount,0,
        receiver3Key,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethed.delegateResourceV2(frozen1Address,frozenAmount / 10,
        0,receiver1Address,frozen1Key,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.delegateResourceV2(frozen1Address,frozenAmount / 10,
        0,receiver2Address,frozen1Key,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.delegateResourceV2(frozen1Address,frozenAmount / 10,
        0,receiver3Address,frozen1Key,blockingStubFull));

    Assert.assertTrue(PublicMethed.delegateResourceV2(receiver1Address,frozenAmount / 10,
        0,frozen1Address,receiver1Key,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.delegateResourceV2(receiver2Address,frozenAmount / 10,
        0,frozen1Address,receiver2Key,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.delegateResourceV2(receiver3Address,frozenAmount / 10,
        0,frozen1Address,receiver3Key,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "GetDelegateResource to account sort by timestamp")
  public void test01GetDelegateResourceToAccountTimestamp() {

    List<ByteString> toAccountList = PublicMethed.getDelegatedResourceAccountIndex(frozen1Address,blockingStubFull).get().getToAccountsList();
    //query solidity
    List<ByteString> toAccountListSolidity = PublicMethed.getDelegatedResourceAccountIndexV2Solidity(frozen1Address, blockingStubFullSolidity).get().getToAccountsList();
    Assert.assertEquals(toAccountListSolidity,toAccountList);
    Assert.assertTrue(toAccountList.size() == 3);
    Assert.assertEquals(toAccountList.get(0).toByteArray(),receiver1Address);
    Assert.assertEquals(toAccountList.get(1).toByteArray(),receiver2Address);
    Assert.assertEquals(toAccountList.get(2).toByteArray(),receiver3Address);


    Assert.assertTrue(PublicMethed.delegateResourceV2(frozen1Address,frozenAmount / 10,
        0,receiver1Address,frozen1Key,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    toAccountList = PublicMethed.getDelegatedResourceAccountIndex(frozen1Address,blockingStubFull).get().getToAccountsList();
    //query solidity
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubFullSolidity);
    toAccountListSolidity = PublicMethed.getDelegatedResourceAccountIndexV2Solidity(frozen1Address, blockingStubFullSolidity).get().getToAccountsList();
    Assert.assertEquals(toAccountListSolidity,toAccountList);

    Assert.assertTrue(toAccountList.size() == 3);
    Assert.assertEquals(toAccountList.get(0).toByteArray(),receiver2Address);
    Assert.assertEquals(toAccountList.get(1).toByteArray(),receiver3Address);
    Assert.assertEquals(toAccountList.get(2).toByteArray(),receiver1Address);


  }


  @Test(enabled = true, description = "GetDelegateResource from account sort by timestamp")
  public void test02GetDelegateResourceFromAccountTimestamp() {

    List<ByteString> fromAccountList = PublicMethed.getDelegatedResourceAccountIndex(frozen1Address,blockingStubFull).get().getFromAccountsList();
    //query solidity
    List<ByteString> fromAccountListSolidity = PublicMethed.getDelegatedResourceAccountIndexV2Solidity(frozen1Address, blockingStubFullSolidity).get().getToAccountsList();
    Assert.assertEquals(fromAccountListSolidity,fromAccountList);
    Assert.assertTrue(fromAccountList.size() == 3);
    Assert.assertEquals(fromAccountList.get(0).toByteArray(),receiver1Address);
    Assert.assertEquals(fromAccountList.get(1).toByteArray(),receiver2Address);
    Assert.assertEquals(fromAccountList.get(2).toByteArray(),receiver3Address);


    Assert.assertTrue(PublicMethed.delegateResourceV2(receiver1Address,frozenAmount / 10,
        0,frozen1Address,receiver1Key,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    fromAccountList = PublicMethed.getDelegatedResourceAccountIndex(frozen1Address,blockingStubFull).get().getToAccountsList();
    //querySolidity
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubFullSolidity);
    fromAccountListSolidity = PublicMethed.getDelegatedResourceAccountIndexV2Solidity(frozen1Address, blockingStubFullSolidity).get().getToAccountsList();
    Assert.assertEquals(fromAccountListSolidity,fromAccountList);
    Assert.assertTrue(fromAccountList.size() == 3);
    Assert.assertEquals(fromAccountList.get(0).toByteArray(),receiver2Address);
    Assert.assertEquals(fromAccountList.get(1).toByteArray(),receiver3Address);
    Assert.assertEquals(fromAccountList.get(2).toByteArray(),receiver1Address);


  }


  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(frozen1Address, frozen1Key, foundationAddress, blockingStubFull);
    PublicMethed.freedResource(receiver1Address, receiver1Key, foundationAddress, blockingStubFull);
    PublicMethed.freedResource(receiver2Address, receiver2Key, foundationAddress, blockingStubFull);
    PublicMethed.freedResource(receiver3Address, receiver3Key, foundationAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


