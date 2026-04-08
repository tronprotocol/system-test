package stest.tron.wallet.fuzz;

import net.jqwik.api.*;
import static org.junit.jupiter.api.Assertions.*;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.Sha256Hash;

class ECKeyPropertyTest {

  @Property(tries = 200)
  void signAndRecoverPublicKey(@ForAll byte[] message) {
    Assume.that(message.length > 0);
    byte[] messageHash = Sha256Hash.hash(true, message);
    ECKey key = new ECKey();
    ECKey.ECDSASignature sig = key.sign(messageHash);

    byte[] recoveredPubKey = ECKey.recoverPubBytesFromSignature(
        sig.v - 27, sig, messageHash);
    assertNotNull(recoveredPubKey);
    assertArrayEquals(key.getPubKey(), recoveredPubKey);
  }

  @Property(tries = 500)
  void generatedKeyHasValidAddress() {
    ECKey key = new ECKey();
    byte[] address = key.getAddress();
    assertEquals(21, address.length, "Address must be 21 bytes");
    assertEquals((byte) 0x41, address[0], "Address must start with 0x41");
  }

  @Property(tries = 500)
  void privateKeyIs32Bytes() {
    ECKey key = new ECKey();
    byte[] privKey = key.getPrivKeyBytes();
    assertEquals(32, privKey.length, "Private key must be 32 bytes");
  }

  @Property(tries = 200)
  void differentKeysProduceDifferentAddresses() {
    ECKey key1 = new ECKey();
    ECKey key2 = new ECKey();
    assertFalse(java.util.Arrays.equals(key1.getAddress(), key2.getAddress()));
  }
}
