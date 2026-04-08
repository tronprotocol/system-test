package stest.tron.wallet.common.client.utils;

import org.tron.api.WalletGrpc;

/**
 * Factory for creating funded test accounts.
 *
 * <p>Reduces boilerplate in test {@code @BeforeClass} methods. Instead of:
 * <pre>{@code
 * ECKey ecKey = new ECKey(Utils.getRandom());
 * byte[] addr = ecKey.getAddress();
 * String key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
 * PublicMethod.sendcoin(addr, amount, foundation, foundationKey, stub);
 * PublicMethod.waitProduceNextBlock(stub);
 * }</pre>
 *
 * <p>Use:
 * <pre>{@code
 * TestAccount test = TestAccountFactory.funded(amount, foundation, foundationKey, stub);
 * // test.address, test.privateKey, test.ecKey are all available
 * }</pre>
 */
public final class TestAccountFactory {

  private TestAccountFactory() {
    // utility class
  }

  /**
   * Creates a new random account funded with the specified TRX amount.
   *
   * @param amountSun  TRX amount in sun to transfer from foundation
   * @param foundation foundation account address (source of funds)
   * @param foundationKey foundation account private key
   * @param stub       gRPC blocking stub
   * @return a funded TestAccount ready for use in tests
   */
  public static TestAccount funded(long amountSun,
      byte[] foundation, String foundationKey,
      WalletGrpc.WalletBlockingStub stub) {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] address = ecKey.getAddress();
    String privateKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethod.sendcoin(address, amountSun, foundation, foundationKey, stub);
    PublicMethod.waitProduceNextBlock(stub);
    return new TestAccount(ecKey, address, privateKey);
  }

  /**
   * Creates a new random account without funding.
   * Useful for negative tests or when funding is done separately.
   */
  public static TestAccount unfunded() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] address = ecKey.getAddress();
    String privateKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    return new TestAccount(ecKey, address, privateKey);
  }

  /**
   * Holds a test account's key material.
   */
  public static class TestAccount {
    public final ECKey ecKey;
    public final byte[] address;
    public final String privateKey;

    TestAccount(ECKey ecKey, byte[] address, String privateKey) {
      this.ecKey = ecKey;
      this.address = address;
      this.privateKey = privateKey;
    }
  }
}
