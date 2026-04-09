package stest.tron.wallet.dailybuild.operationupdate;

import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Proposal;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class MultiSignProposalTest extends TronBaseTest {
  private static final long now = System.currentTimeMillis();
  private final String operations =
      Configuration.getByPath("testng.conf").getString("defaultParameter.operations");
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private long multiSignFee =
      Configuration.getByPath("testng.conf").getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee =
      Configuration.getByPath("testng.conf").getLong("defaultParameter.updateAccountPermissionFee");
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(0);

  /** constructor. */
  @BeforeClass
  public void beforeClass() {
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode).usePlaintext().build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test(
      enabled = true,
      groups = {"contract", "daily"})
  public void testMultiSignForProposal() {
    long needcoin = updateAccountPermissionFee + multiSignFee * 5;
    Assert.assertTrue(
        PublicMethod.sendcoin(
            witnessAddress2,
            needcoin + 10000000L,
            foundationAddress,
            foundationKey,
            blockingStubFull));

    ecKey1 = new ECKey(Utils.getRandom());
    manager1Address = ecKey1.getAddress();
    manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    manager2Address = ecKey2.getAddress();
    manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = witnessKey2;
    ownerKeyString[1] = foundationKey;

    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\""
            + ",\"threshold\":2,\"keys\":[{\"address\":\""
            + PublicMethod.getAddressString(witnessKey2)
            + "\","
            + "\"weight\":1},{\"address\":\""
            + PublicMethod.getAddressString(foundationKey)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":\"owner\",\"threshold\":1,"
            + "\"keys\":[{\"address\":\""
            + PublicMethod.getAddressString(witnessKey2)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0037e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\""
            + PublicMethod.getAddressString(manager1Key)
            + "\",\"weight\":1},"
            + "{\"address\":\""
            + PublicMethod.getAddressString(manager2Key)
            + "\",\"weight\":1}]}]} ";
    logger.info(accountPermissionJson);
    PublicMethodForMultiSign.accountPermissionUpdate(
        accountPermissionJson, witnessAddress2, witnessKey2, blockingStubFull, ownerKeyString);
    // Create a proposal

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    Long proposalValue = 819897L;
    proposalMap.put(0L, proposalValue);
    Assert.assertTrue(
        PublicMethodForMultiSign.createProposalWithPermissionId(
            witnessAddress2, witnessKey2, proposalMap, 0, blockingStubFull, ownerKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    // Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
    Integer proposalId = -1;
    for (Proposal proposal : listProposals.get().getProposalsList()) {
      for (Map.Entry<Long, Long> entry : proposal.getParametersMap().entrySet()) {
        if (entry.getValue().equals(proposalValue)) {
          proposalId = (int) proposal.getProposalId();
          break;
        }
      }
      if (proposalId != -1) {
        break;
      }
    }
    final Integer finalProposalId = proposalId;
    logger.info(Integer.toString(finalProposalId));

    Assert.assertTrue(
        PublicMethodForMultiSign.approveProposalWithPermission(
            witnessAddress2,
            witnessKey2,
            finalProposalId,
            true,
            0,
            blockingStubFull,
            ownerKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    // Delete proposal list after approve
    Assert.assertTrue(
        PublicMethodForMultiSign.deleteProposalWithPermissionId(
            witnessAddress2, witnessKey2, finalProposalId, 0, blockingStubFull, ownerKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Long balanceAfter = PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {}
}
