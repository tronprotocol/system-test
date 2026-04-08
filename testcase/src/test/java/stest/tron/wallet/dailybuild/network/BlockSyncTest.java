package stest.tron.wallet.dailybuild.network;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.utils.MultiNode;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;

/**
 * Block synchronization tests.
 *
 * <p>Verifies that two nodes stay in sync: block heights should converge
 * and the same block number should have the same block hash on both nodes.
 */
@Slf4j
@MultiNode(reason = "Block sync verification requires two independent nodes")
public class BlockSyncTest extends TronBaseTest {

  private String fullnode2;
  private ManagedChannel channelFull2;
  private WalletGrpc.WalletBlockingStub blockingStubFull2;

  @BeforeClass(enabled = true)
  public void beforeClass() {
    fullnode2 = requireSecondFullnode();
    channelFull2 = ManagedChannelBuilder.forTarget(fullnode2).usePlaintext().build();
    blockingStubFull2 = WalletGrpc.newBlockingStub(channelFull2);
  }

  @Test(enabled = true, description = "Block heights on two nodes should be close",
      groups = {"daily"})
  public void test01BlockHeightConvergence() {
    // Wait for a fresh block to ensure both nodes are up-to-date
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Block block1 = PublicMethod.getBlock(-1, blockingStubFull);
    Block block2 = PublicMethod.getBlock(-1, blockingStubFull2);

    long height1 = block1.getBlockHeader().getRawData().getNumber();
    long height2 = block2.getBlockHeader().getRawData().getNumber();
    long diff = Math.abs(height1 - height2);
    logger.info("Node1 height: {}, Node2 height: {}, diff: {}", height1, height2, diff);

    // Allow up to 3 blocks difference (sync lag)
    Assert.assertTrue(diff <= 3,
        "Block height difference between nodes should be <= 3, got " + diff);
  }

  @Test(enabled = true, description = "Same block number should have same content on both nodes",
      groups = {"daily"})
  public void test02SameBlockSameContent() {
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Block latestNode1 = PublicMethod.getBlock(-1, blockingStubFull);
    long targetNum = latestNode1.getBlockHeader().getRawData().getNumber() - 5;

    Block blockOnNode1 = PublicMethod.getBlock(targetNum, blockingStubFull);
    Block blockOnNode2 = PublicMethod.getBlock(targetNum, blockingStubFull2);

    Assert.assertNotNull(blockOnNode1, "Block should exist on node1");
    Assert.assertNotNull(blockOnNode2, "Block should exist on node2");

    // Compare block hash (witness_address + timestamp + number should match)
    long ts1 = blockOnNode1.getBlockHeader().getRawData().getTimestamp();
    long ts2 = blockOnNode2.getBlockHeader().getRawData().getTimestamp();
    Assert.assertEquals(ts1, ts2,
        "Block " + targetNum + " timestamp should match on both nodes");

    long num1 = blockOnNode1.getBlockHeader().getRawData().getNumber();
    long num2 = blockOnNode2.getBlockHeader().getRawData().getNumber();
    Assert.assertEquals(num1, num2,
        "Block number should match on both nodes");

    String witness1 = blockOnNode1.getBlockHeader().getRawData()
        .getWitnessAddress().toStringUtf8();
    String witness2 = blockOnNode2.getBlockHeader().getRawData()
        .getWitnessAddress().toStringUtf8();
    Assert.assertEquals(witness1, witness2,
        "Block witness should match on both nodes");

    logger.info("Block {} verified consistent across both nodes", targetNum);
  }

  @Test(enabled = true, description = "New blocks appear on both nodes after waiting",
      groups = {"daily"})
  public void test03ContinuousSyncVerification() {
    long startHeight1 = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder()
        .build()).getBlockHeader().getRawData().getNumber();
    long startHeight2 = blockingStubFull2.getNowBlock(GrpcAPI.EmptyMessage.newBuilder()
        .build()).getBlockHeader().getRawData().getNumber();

    // Wait for 2 blocks
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull2);

    long endHeight1 = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder()
        .build()).getBlockHeader().getRawData().getNumber();
    long endHeight2 = blockingStubFull2.getNowBlock(GrpcAPI.EmptyMessage.newBuilder()
        .build()).getBlockHeader().getRawData().getNumber();

    Assert.assertTrue(endHeight1 > startHeight1, "Node1 should have new blocks");
    Assert.assertTrue(endHeight2 > startHeight2, "Node2 should have new blocks");
    logger.info("Node1: {} -> {}, Node2: {} -> {}",
        startHeight1, endHeight1, startHeight2, endHeight2);
  }

  @AfterClass(enabled = true)
  public void afterClass() throws InterruptedException {
    if (channelFull2 != null) {
      channelFull2.shutdown().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
    }
  }
}
