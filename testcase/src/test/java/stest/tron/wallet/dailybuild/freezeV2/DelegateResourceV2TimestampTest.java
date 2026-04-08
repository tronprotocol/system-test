package stest.tron.wallet.dailybuild.freezeV2;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletSolidityGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

  /**
 *
 */
@Slf4j
public class DelegateResourceV2TimestampTest extends TronBaseTest {
  private static final long sendAmount = 100000000L;
  private static final long frozenAmount = 10000000L;
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
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
      .get(0);
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFullSolidity = null;
  private String pbftnode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
          .get(2);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    initPbftChannel();
    initSolidityChannel();
    PublicMethod.printAddress(frozen1Key);

    PublicMethod.printAddress(receiver1Key);
    PublicMethod.printAddress(receiver2Key);
    PublicMethod.printAddress(receiver3Key);    if (!PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {      throw new SkipException("Skipping freezeV2 test case");
    }
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubFullSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelPbft = ManagedChannelBuilder.forTarget(pbftnode)
        .usePlaintext()
        .build();
    Assert.assertTrue(PublicMethod.sendcoin(frozen1Address, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));

    Assert.assertTrue(PublicMethod.sendcoin(receiver1Address, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(receiver2Address, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(receiver3Address, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod.freezeBalanceV2(frozen1Address, frozenAmount, 0,
        frozen1Key, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceV2(receiver1Address, frozenAmount, 0,
        receiver1Key, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceV2(receiver2Address, frozenAmount, 0,
        receiver2Key, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceV2(receiver3Address, frozenAmount, 0,
        receiver3Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod.delegateResourceV2(frozen1Address, frozenAmount / 10,
        0, receiver1Address, frozen1Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.delegateResourceV2(frozen1Address, frozenAmount / 10,
        0, receiver2Address, frozen1Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.delegateResourceV2(frozen1Address, frozenAmount / 10,
        0, receiver3Address, frozen1Key, blockingStubFull));

    Assert.assertTrue(PublicMethod.delegateResourceV2(receiver1Address, frozenAmount / 10,
        0, frozen1Address, receiver1Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.delegateResourceV2(receiver2Address, frozenAmount / 10,
        0, frozen1Address, receiver2Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.delegateResourceV2(receiver3Address, frozenAmount / 10,
        0, frozen1Address, receiver3Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "GetDelegateResource to account sort by timestamp", groups = {"daily", "staking"})
  public void test01GetDelegateResourceToAccountTimestamp() {

    List<ByteString> toAccountList = PublicMethod.getDelegatedResourceAccountIndex(
        frozen1Address,
        blockingStubFull
    ).get().getToAccountsList();
  //query solidity
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubFullSolidity);
    List<ByteString> toAccountListSolidity =
        PublicMethod.getDelegatedResourceAccountIndexV2Solidity(
            frozen1Address,
            blockingStubFullSolidity
        ).get().getToAccountsList();
    Assert.assertEquals(toAccountListSolidity, toAccountList);
  //query pbft
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubPbft);
    List<ByteString> toAccountListPbft =
        PublicMethod.getDelegatedResourceAccountIndexV2Solidity(
            frozen1Address,
            blockingStubPbft
        ).get().getToAccountsList();
    Assert.assertEquals(toAccountListPbft, toAccountList);

    Assert.assertTrue(toAccountList.size() == 3);
    Assert.assertEquals(toAccountList.get(0).toByteArray(), receiver1Address);
    Assert.assertEquals(toAccountList.get(1).toByteArray(), receiver2Address);
    Assert.assertEquals(toAccountList.get(2).toByteArray(), receiver3Address);


    Assert.assertTrue(PublicMethod.delegateResourceV2(frozen1Address, frozenAmount / 10,
        0, receiver1Address, frozen1Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);


    toAccountList = PublicMethod.getDelegatedResourceAccountIndex(
        frozen1Address,
        blockingStubFull
    ).get().getToAccountsList();
    Assert.assertTrue(toAccountList.size() == 3);
    Assert.assertEquals(toAccountList.get(0).toByteArray(), receiver2Address);
    Assert.assertEquals(toAccountList.get(1).toByteArray(), receiver3Address);
    Assert.assertEquals(toAccountList.get(2).toByteArray(), receiver1Address);


  }


  @Test(enabled = true, description = "GetDelegateResource from account sort by timestamp", groups = {"daily", "staking"})
  public void test02GetDelegateResourceFromAccountTimestamp() {

    List<ByteString> fromAccountList = PublicMethod.getDelegatedResourceAccountIndex(
        frozen1Address,
        blockingStubFull
    ).get().getFromAccountsList();
  //query solidity
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubFullSolidity);
    List<ByteString> fromAccountListSolidity =
        PublicMethod.getDelegatedResourceAccountIndexV2Solidity(
        frozen1Address,
        blockingStubFullSolidity
        ).get().getFromAccountsList();
    Assert.assertEquals(fromAccountListSolidity, fromAccountList);
  //query pbft
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubPbft);
    List<ByteString> fromAccountListPbft =
        PublicMethod.getDelegatedResourceAccountIndexV2Solidity(
            frozen1Address,
            blockingStubPbft
        ).get().getFromAccountsList();
    Assert.assertEquals(fromAccountListPbft, fromAccountList);

    Assert.assertTrue(fromAccountList.size() == 3);
    Assert.assertEquals(fromAccountList.get(0).toByteArray(), receiver1Address);
    Assert.assertEquals(fromAccountList.get(1).toByteArray(), receiver2Address);
    Assert.assertEquals(fromAccountList.get(2).toByteArray(), receiver3Address);


    Assert.assertTrue(
        PublicMethod.delegateResourceV2(
        receiver1Address,
        frozenAmount / 10,
        0,
        frozen1Address,
        receiver1Key,
        blockingStubFull
        )
    );
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    fromAccountList = PublicMethod.getDelegatedResourceAccountIndex(
        frozen1Address,
        blockingStubFull
    ).get().getToAccountsList();
    Assert.assertTrue(fromAccountList.size() == 3);
    Assert.assertEquals(fromAccountList.get(0).toByteArray(), receiver2Address);
    Assert.assertEquals(fromAccountList.get(1).toByteArray(), receiver3Address);
    Assert.assertEquals(fromAccountList.get(2).toByteArray(), receiver1Address);


  }


  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(frozen1Address, frozen1Key, foundationAddress, blockingStubFull);
    PublicMethod.freeResource(receiver1Address, receiver1Key, foundationAddress, blockingStubFull);
    PublicMethod.freeResource(receiver2Address, receiver2Key, foundationAddress, blockingStubFull);
    PublicMethod.freeResource(receiver3Address, receiver3Key, foundationAddress, blockingStubFull);  }
}


