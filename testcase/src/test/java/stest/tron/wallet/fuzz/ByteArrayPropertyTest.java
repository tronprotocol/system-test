package stest.tron.wallet.fuzz;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import static org.junit.jupiter.api.Assertions.*;
import stest.tron.wallet.common.client.utils.ByteArray;

class ByteArrayPropertyTest {

  @Property(tries = 10000)
  void hexRoundtrip(@ForAll byte[] input) {
    String hex = ByteArray.toHexString(input);
    byte[] result = ByteArray.fromHexString(hex);
    assertArrayEquals(input, result);
  }

  @Property(tries = 5000)
  void hexStringIsLowercase(@ForAll byte[] input) {
    String hex = ByteArray.toHexString(input);
    assertEquals(hex, hex.toLowerCase());
  }

  @Property(tries = 5000)
  void hexStringHasEvenLength(@ForAll byte[] input) {
    String hex = ByteArray.toHexString(input);
    assertEquals(0, hex.length() % 2);
  }

  @Property(tries = 5000)
  void longRoundtrip(@ForAll @LongRange(min = 0) long value) {
    byte[] bytes = ByteArray.fromLong(value);
    long result = ByteArray.toLong(bytes);
    assertEquals(value, result);
  }

  @Property(tries = 5000)
  void intRoundtrip(@ForAll @IntRange(min = 0) int value) {
    byte[] bytes = ByteArray.fromInt(value);
    int result = ByteArray.toInt(bytes);
    assertEquals(value, result);
  }
}
