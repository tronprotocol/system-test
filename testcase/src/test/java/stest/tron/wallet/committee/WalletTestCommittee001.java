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
import org.tron.api.GrpcAPI.PaginatedMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletSolidityGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestCommittee001 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  //Witness 47.93.9.236  //Witness 47.93.33.201
  //Witness 123.56.10.6
  //Wtiness 39.107.80.135
  //Witness 47.93.184.2
  private final byte[] witness001Address = PublicMethod.getFinalAddress(witnessKey);
  private final byte[] witness002Address = PublicMethod.getFinalAddress(witnessKey2);
  private final byte[] witness003Address = PublicMethod.getFinalAddress(witnessKey3);
  private final byte[] witness004Address = PublicMethod.getFinalAddress(witnessKey4);
  private final byte[] witness005Address = PublicMethod.getFinalAddress(witnessKey5);
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

  @Test(groups = {"smoke"})
  public void testListProposals() {
    //List proposals
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
  final Integer beforeProposalCount = listProposals.get().getProposalsCount();
  //CreateProposal
    final long now = System.currentTimeMillis();
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(0L, 1000000L);
    PublicMethod.createProposal(witness001Address, witnessKey, proposalMap, blockingStubFull);
  //List proposals
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
    Integer afterProposalCount = listProposals.get().getProposalsCount();
    Assert.assertTrue(beforeProposalCount + 1 == afterProposalCount);
    logger.info(Long.toString(listProposals.get().getProposals(0).getCreateTime()));
    logger.info(Long.toString(now));
  //Assert.assertTrue(listProposals.get().getProposals(0).getCreateTime() >= now);
    Assert.assertTrue(listProposals.get().getProposals(0).getParametersMap().equals(proposalMap));
  //getProposalListPaginated
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(0);
    pageMessageBuilder.setLimit(1);
    ProposalList paginatedProposalList = blockingStubFull
        .getPaginatedProposalList(pageMessageBuilder.build());
    Assert.assertTrue(paginatedProposalList.getProposalsCount() >= 1);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}

