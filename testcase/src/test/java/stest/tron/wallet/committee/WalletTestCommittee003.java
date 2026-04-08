package stest.tron.wallet.committee;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletSolidityGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestCommittee003 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  //Witness 47.93.33.201
  //Witness 123.56.10.6
  //Wtiness 39.107.80.135
  //Witness 47.93.184.2
  private final byte[] witness001Address = PublicMethod.getFinalAddress(witnessKey);
  //private final byte[] witness003Address = PublicMethod.getFinalAddress(witnessKey3);
  //private final byte[] witness004Address = PublicMethod.getFinalAddress(witnessKey4);
  //private final byte[] witness005Address = PublicMethod.getFinalAddress(witnessKey5);
  private final byte[] witness002Address = PublicMethod.getFinalAddress(witnessKey2);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test(enabled = true, groups = {"smoke"})
  public void testApproveProposal() {
    PublicMethod.sendcoin(witness001Address, 1000000L,
        toAddress, testKey003, blockingStubFull);
    PublicMethod.sendcoin(witness002Address, 1000000L,
        toAddress, testKey003, blockingStubFull);

    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(0L, 81000L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
  final Integer proposalId = listProposals.get().getProposalsCount();
    logger.info(Integer.toString(proposalId));

    Assert.assertTrue(PublicMethod.approveProposal(witness002Address, witnessKey2, proposalId,
        true, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Get proposal list after approve
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
    logger.info(Integer.toString(listProposals.get().getProposals(0).getApprovalsCount()));
    Assert.assertTrue(listProposals.get().getProposals(0).getApprovalsCount() == 1);
  //logger.info(Base58.encode58Check(witness002Address));
  //logger.info(Base58.encode58Check(listProposals.get().getProposals(0).
    // getApprovalsList().get(0).toByteArray()));
    Assert.assertTrue(Base58.encode58Check(witness002Address).equals(Base58.encode58Check(
        listProposals.get().getProposals(0).getApprovalsList().get(0).toByteArray())));
  //Failed to approve proposal when you already approval this proposal
    Assert.assertFalse(PublicMethod.approveProposal(witness002Address, witnessKey2, proposalId,
        true, blockingStubFull));
  //Success to change the option from true to false.
    Assert.assertTrue(PublicMethod.approveProposal(witness002Address, witnessKey2, proposalId,
        false, blockingStubFull));
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
    Assert.assertTrue(listProposals.get().getProposals(0).getApprovalsCount() == 0);
  //Failed to approvel proposal when you already approval this proposal
    Assert.assertFalse(PublicMethod.approveProposal(witness002Address, witnessKey2, proposalId,
        false, blockingStubFull));
  //Non witness can't approval proposal
    Assert.assertFalse(PublicMethod.approveProposal(toAddress, testKey003, proposalId,
        true, blockingStubFull));
  //Muti approval
    Assert.assertTrue(PublicMethod.approveProposal(witness001Address, witnessKey, proposalId,
        true, blockingStubFull));
    Assert.assertTrue(PublicMethod.approveProposal(witness002Address, witnessKey2, proposalId,
        true, blockingStubFull));
  //Assert.assertTrue(PublicMethod.approveProposal(witness003Address,witnessKey3,proposalId,
    //    true,blockingStubFull));
  //Assert.assertTrue(PublicMethod.approveProposal(witness004Address,witnessKey4,proposalId,
    //    true,blockingStubFull));
  //Assert.assertTrue(PublicMethod.approveProposal(witness005Address,witnessKey5,proposalId,
    //    true,blockingStubFull));
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
    Assert.assertTrue(listProposals.get().getProposals(0).getApprovalsCount() == 2);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}

