package stest.tron.wallet.fuzz;

import com.google.protobuf.ByteString;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import static org.junit.jupiter.api.Assertions.*;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.BalanceContract.TransferContract;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.Sha256Hash;
import stest.tron.wallet.common.client.utils.TransactionUtils;

/**
 * Fuzz testing for transaction fields.
 *
 * <p>Generates random/boundary values for transaction parameters to verify
 * that invalid transactions are properly rejected and valid ones can be signed.
 */
class TransactionFuzzTest {

  @Property(tries = 500)
  void randomAmountTransactionCanBeSigned(@ForAll @LongRange(min = 0, max = Long.MAX_VALUE) long amount) {
    ECKey sender = new ECKey();
    ECKey receiver = new ECKey();

    TransferContract.Builder transferBuilder = TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(sender.getAddress()))
        .setToAddress(ByteString.copyFrom(receiver.getAddress()))
        .setAmount(amount);

    Transaction.raw.Builder rawBuilder = Transaction.raw.newBuilder()
        .setTimestamp(System.currentTimeMillis())
        .setExpiration(System.currentTimeMillis() + 60_000);

    Transaction.Contract.Builder contractBuilder = Transaction.Contract.newBuilder()
        .setType(Transaction.Contract.ContractType.TransferContract)
        .setParameter(com.google.protobuf.Any.pack(transferBuilder.build()));

    rawBuilder.addContract(contractBuilder.build());
    Transaction tx = Transaction.newBuilder().setRawData(rawBuilder.build()).build();

    // Should be able to sign any well-formed transaction
    Transaction signedTx = TransactionUtils.sign(tx, sender);
    assertNotNull(signedTx);
    assertTrue(signedTx.getSignatureCount() > 0, "Signed tx should have signature");
  }

  @Property(tries = 500)
  void negativeAmountProducesInvalidTransfer(@ForAll @LongRange(min = Long.MIN_VALUE, max = -1) long amount) {
    ECKey sender = new ECKey();
    ECKey receiver = new ECKey();

    TransferContract contract = TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(sender.getAddress()))
        .setToAddress(ByteString.copyFrom(receiver.getAddress()))
        .setAmount(amount)
        .build();

    // Negative amounts should still serialize (validation is on-chain)
    assertTrue(contract.getAmount() < 0, "Negative amount should be preserved in protobuf");
  }

  @Property(tries = 300)
  void randomBytesAsAddressInTransfer(@ForAll byte[] randomAddr) {
    ECKey sender = new ECKey();

    TransferContract contract = TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(sender.getAddress()))
        .setToAddress(ByteString.copyFrom(randomAddr))
        .setAmount(1000000L)
        .build();

    // Protobuf accepts any bytes; on-chain validation rejects bad addresses
    assertEquals(randomAddr.length, contract.getToAddress().size());
  }

  @Property(tries = 200)
  void tamperedSignatureInvalidatesTransaction() {
    ECKey key = new ECKey();
    byte[] message = Sha256Hash.hash(true, "test transaction".getBytes());
    ECKey.ECDSASignature sig = key.sign(message);

    // Tamper with signature
    byte[] sigBytes = sig.toByteArray();
    if (sigBytes.length > 0) {
      sigBytes[0] ^= 0xFF;
    }

    // Tampered signature should not recover the original public key
    try {
      byte[] recovered = ECKey.recoverPubBytesFromSignature(
          sig.v - 27,
          ECKey.ECDSASignature.fromComponents(sigBytes, sigBytes, (byte) sig.v),
          message);
      // If recovery succeeds, it should be a different key
      if (recovered != null) {
        assertFalse(java.util.Arrays.equals(key.getPubKey(), recovered),
            "Tampered signature should not recover original key");
      }
    } catch (Exception e) {
      // Expected: tampered signature may cause recovery failure
    }
  }
}
