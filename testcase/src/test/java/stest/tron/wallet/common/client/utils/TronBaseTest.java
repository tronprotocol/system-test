package stest.tron.wallet.common.client.utils;

import com.typesafe.config.Config;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import stest.tron.wallet.common.client.Configuration;

/**
 * Abstract base class for TRON system tests.
 *
 * <p>Provides common infrastructure: gRPC channel management, foundation/witness account
 * loading, and node endpoint configuration. Subclasses get {@code channelFull} and
 * {@code blockingStubFull} initialized automatically before each test class runs.
 *
 * <p>Usage:
 * <pre>{@code
 * public class MyTest extends TronBaseTest {
 *   @BeforeClass
 *   public void beforeClass() {
 *     // channelFull and blockingStubFull are already available
 *     PublicMethod.sendcoin(..., blockingStubFull);
 *   }
 * }
 * }</pre>
 *
 * <p>For tests needing solidity/PBFT channels, call {@link #initSolidityChannel()}
 * or {@link #initPbftChannel()} in a subclass {@code @BeforeClass} method.
 */
@Slf4j
public abstract class TronBaseTest {

  // --------------- Configuration ---------------
  protected static final Config config = Configuration.getByPath("testng.conf");

  // --------------- Foundation Account 1 ---------------
  protected final String foundationKey = config.getString("foundationAccount.key1");
  protected final byte[] foundationAddress = PublicMethod.getFinalAddress(foundationKey);
  /** @deprecated Use {@code foundationAddress} instead. Kept for backward compatibility. */
  protected final byte[] fromAddress = foundationAddress;

  // --------------- Foundation Account 2 ---------------
  protected final String foundationKey2 = config.getString("foundationAccount.key2");
  protected final byte[] foundationAddress2 = PublicMethod.getFinalAddress(foundationKey2);
  /**
   * Backward-compatible alias. Despite the name suggesting key2, the original code
   * in most classes mapped testKey002 to foundationAccount.key1.
   */
  protected final String testKey002 = foundationKey;

  // --------------- Witness Accounts ---------------
  protected final String witnessKey = config.getString("witness.key1");
  protected final byte[] witnessAddress = PublicMethod.getFinalAddress(witnessKey);
  protected final String witnessKey2 = config.getString("witness.key2");
  protected final byte[] witnessAddress2 = PublicMethod.getFinalAddress(witnessKey2);
  protected final String witnessKey3 = config.getString("witness.key3");
  protected final byte[] witnessAddress3 = PublicMethod.getFinalAddress(witnessKey3);
  protected final String witnessKey4 = config.getString("witness.key4");
  protected final byte[] witnessAddress4 = PublicMethod.getFinalAddress(witnessKey4);
  protected final String witnessKey5 = config.getString("witness.key5");
  protected final byte[] witnessAddress5 = PublicMethod.getFinalAddress(witnessKey5);

  // --------------- Node Endpoints ---------------
  protected String fullnode = config.getStringList("fullnode.ip.list").get(0);

  // --------------- Full Node Channel (always initialized) ---------------
  protected ManagedChannel channelFull = null;
  protected WalletGrpc.WalletBlockingStub blockingStubFull = null;

  // --------------- Solidity Node Channel (optional) ---------------
  protected ManagedChannel channelSolidity = null;
  protected WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  // --------------- PBFT Node Channel (optional) ---------------
  protected ManagedChannel channelPbft = null;
  protected WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;

  // --------------- Common Parameters ---------------
  protected long maxFeeLimit = config.getLong("defaultParameter.maxFeeLimit");

  /**
   * Initializes the fullnode gRPC channel and stub.
   * Runs before any subclass {@code @BeforeClass} method.
   */
  @BeforeClass(alwaysRun = true)
  public void initChannels() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  /**
   * Shuts down all open gRPC channels.
   * Runs after any subclass {@code @AfterClass} method.
   */
  @AfterClass(alwaysRun = true)
  public void closeChannels() throws InterruptedException {
    shutdownChannel(channelFull);
    shutdownChannel(channelSolidity);
    shutdownChannel(channelPbft);
  }

  /**
   * Initializes the solidity node channel and stub.
   * Call this in a subclass {@code @BeforeClass} if solidity access is needed.
   */
  protected void initSolidityChannel() {
    String solidityNode = config.getStringList("solidityNode.ip.list").get(0);
    channelSolidity = ManagedChannelBuilder.forTarget(solidityNode)
        .usePlaintext()
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  /**
   * Initializes the PBFT node channel and stub.
   * Call this in a subclass {@code @BeforeClass} if PBFT access is needed.
   */
  protected void initPbftChannel() {
    String pbftNode = config.getStringList("solidityNode.ip.list").get(2);
    channelPbft = ManagedChannelBuilder.forTarget(pbftNode)
        .usePlaintext()
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);
  }

  /**
   * Returns the second fullnode endpoint if available, or throws SkipException.
   * Call this in {@code @BeforeClass} of multi-node tests to safely skip
   * when only a single node is running.
   *
   * @return the second fullnode endpoint string (e.g. "127.0.0.1:50052")
   * @throws SkipException if no second node is configured or it equals the first
   */
  protected String requireSecondFullnode() {
    List<String> nodes = config.getStringList("fullnode.ip.list");
    if (nodes.size() < 2) {
      throw new SkipException("Multi-node required but only 1 fullnode configured");
    }
    String node2 = nodes.get(1);
    if (node2.equals(nodes.get(0))) {
      throw new SkipException("Multi-node required but fullnode.ip.list[0] == [1]: " + node2);
    }
    return node2;
  }

  /**
   * Generates a new random ECKey and returns a key-address pair.
   *
   * @return array where [0] = hex private key, [1] = address bytes
   */
  protected static Object[] generateAccount() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    String key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    byte[] address = ecKey.getAddress();
    return new Object[]{key, address};
  }

  /**
   * Safely shuts down a gRPC channel with a 5-second timeout.
   */
  protected static void shutdownChannel(ManagedChannel channel) throws InterruptedException {
    if (channel != null) {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
