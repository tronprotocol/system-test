package stest.tron.wallet.dailybuild.manual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class WalletTestNode001 extends TronBaseTest {  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

  }


  @Test(enabled = true, description = "List all nodes", groups = {"daily"})
  public void testGetAllNode() {
    GrpcAPI.NodeList nodeList = blockingStubFull
        .listNodes(GrpcAPI.EmptyMessage.newBuilder().build());
    GrpcAPI.NodeList nodeList1 = blockingStubFull1
        .listNodes(GrpcAPI.EmptyMessage.newBuilder().build());
    Integer times = 0;
    while (nodeList.getNodesCount() == 0 && times++ < 5) {
      nodeList = blockingStubFull
          .listNodes(GrpcAPI.EmptyMessage.newBuilder().build());
      nodeList1 = blockingStubFull1
          .listNodes(GrpcAPI.EmptyMessage.newBuilder().build());
      if (nodeList.getNodesCount() != 0 || nodeList1.getNodesCount() != 0) {
        break;
      }
      PublicMethod.waitProduceNextBlock(blockingStubFull);
    }
    //Assert.assertTrue(nodeList.getNodesCount() != 0 || nodeList1.getNodesCount() != 0);

    for (Integer j = 0; j < nodeList.getNodesCount(); j++) {
      //Assert.assertTrue(nodeList.getNodes(j).hasAddress());
  //Assert.assertFalse(nodeList.getNodes(j).getAddress().getHost().isEmpty());
  //Assert.assertTrue(nodeList.getNodes(j).getAddress().getPort() < 65535);
  //logger.info(ByteArray.toStr(nodeList.getNodes(j).getAddress().getHost().toByteArray()));
    }
    logger.info("get listnode succesuflly");
  //Improve coverage.
    GrpcAPI.NodeList newNodeList = blockingStubFull
        .listNodes(GrpcAPI.EmptyMessage.newBuilder().build());
    nodeList.equals(nodeList);
    nodeList.equals(newNodeList);
    nodeList.getNodesList();
    nodeList.hashCode();
    nodeList.isInitialized();

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


