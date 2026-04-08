package stest.tron.wallet.fuzz;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import static org.junit.jupiter.api.Assertions.*;
import stest.tron.wallet.common.client.utils.ByteArray;

class AmountBoundaryPropertyTest {

  @Property(tries = 5000)
  void longToByteArrayAndBack(@ForAll long value) {
    byte[] bytes = ByteArray.fromLong(value);
    assertEquals(8, bytes.length, "Long should serialize to 8 bytes");
    long recovered = ByteArray.toLong(bytes);
    assertEquals(value, recovered);
  }

  @Property(tries = 5000)
  void intToByteArrayAndBack(@ForAll int value) {
    byte[] bytes = ByteArray.fromInt(value);
    assertEquals(4, bytes.length, "Int should serialize to 4 bytes");
    int recovered = ByteArray.toInt(bytes);
    assertEquals(value, recovered);
  }

  @Property(tries = 1000)
  void trxAmountsPreserved(@ForAll @LongRange(min = 0, max = 100_000_000_000_000_000L) long sunAmount) {
    byte[] bytes = ByteArray.fromLong(sunAmount);
    long recovered = ByteArray.toLong(bytes);
    assertEquals(sunAmount, recovered, "TRX amount in sun should roundtrip");
  }

  @Property(tries = 5000)
  void subArrayBoundary(@ForAll @Size(min = 1, max = 100) byte[] input) {
    int mid = input.length / 2;
    byte[] sub = ByteArray.subArray(input, 0, mid);
    assertEquals(mid, sub.length);
    for (int i = 0; i < mid; i++) {
      assertEquals(input[i], sub[i]);
    }
  }
}
