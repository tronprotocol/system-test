package stest.tron.wallet.fuzz;

import net.jqwik.api.*;
import static org.junit.jupiter.api.Assertions.*;
import stest.tron.wallet.common.client.utils.Base58;

class Base58PropertyTest {

  @Property(tries = 10000)
  void encodeDecodeRoundtrip(@ForAll byte[] input) {
    String encoded = Base58.encode(input);
    byte[] decoded = Base58.decode(encoded);
    assertArrayEquals(input, decoded);
  }

  @Property(tries = 5000)
  void encodedStringContainsOnlyBase58Chars(@ForAll byte[] input) {
    String encoded = Base58.encode(input);
    String alphabet = new String(Base58.ALPHABET);
    for (char c : encoded.toCharArray()) {
      assertTrue(alphabet.indexOf(c) >= 0,
          "Encoded string contains non-Base58 character: " + c);
    }
  }

  @Property(tries = 5000)
  void encodedLengthIsNonNegative(@ForAll byte[] input) {
    String encoded = Base58.encode(input);
    assertTrue(encoded.length() >= 0);
  }
}
