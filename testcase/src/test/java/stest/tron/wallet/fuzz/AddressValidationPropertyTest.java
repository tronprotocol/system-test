package stest.tron.wallet.fuzz;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import static org.junit.jupiter.api.Assertions.*;
import stest.tron.wallet.common.client.utils.Base58;

class AddressValidationPropertyTest {

  @Property(tries = 10000)
  void randomBytesAreAlmostNeverValidAddresses(@ForAll byte[] randomBytes) {
    Assume.that(randomBytes.length != 21 || randomBytes[0] != (byte) 0xa0);
    // Skip if it happens to be a valid address (21 bytes with 0xa0 prefix)
    assertFalse(Base58.addressValid(randomBytes));
  }

  @Property(tries = 5000)
  void correctlyFormedAddressIsValid(@ForAll @Size(20) byte[] body) {
    byte[] address = new byte[21];
    address[0] = (byte) 0xa0;
    System.arraycopy(body, 0, address, 1, 20);
    assertTrue(Base58.addressValid(address));
  }

  @Property(tries = 5000)
  void wrongLengthIsAlwaysInvalid(@ForAll byte[] input) {
    Assume.that(input.length != 21);
    assertFalse(Base58.addressValid(input));
  }

  @Property(tries = 5000)
  void wrongPrefixIsAlwaysInvalid(@ForAll @Size(20) byte[] body) {
    byte[] address = new byte[21];
    address[0] = (byte) 0x00;
    System.arraycopy(body, 0, address, 1, 20);
    assertFalse(Base58.addressValid(address));
  }

  @Property
  void nullAndEmptyAreInvalid() {
    assertFalse(Base58.addressValid(null));
    assertFalse(Base58.addressValid(new byte[0]));
  }
}
