package stest.tron.wallet.fuzz;

import java.util.Arrays;
import net.jqwik.api.*;
import static org.junit.jupiter.api.Assertions.*;
import stest.tron.wallet.common.client.utils.Sha256Hash;

class Sha256PropertyTest {

  @Property(tries = 10000)
  void hashIsAlways32Bytes(@ForAll byte[] input) {
    Assume.that(input.length > 0);
    byte[] hash = Sha256Hash.hash(true, input);
    assertEquals(32, hash.length);
  }

  @Property(tries = 5000)
  void hashIsDeterministic(@ForAll byte[] input) {
    Assume.that(input.length > 0);
    byte[] hash1 = Sha256Hash.hash(true, input);
    byte[] hash2 = Sha256Hash.hash(true, input);
    assertArrayEquals(hash1, hash2);
  }

  @Property(tries = 5000)
  void differentInputsProduceDifferentHashes(@ForAll byte[] input1, @ForAll byte[] input2) {
    Assume.that(input1.length > 0 && input2.length > 0);
    Assume.that(!Arrays.equals(input1, input2));
    byte[] hash1 = Sha256Hash.hash(true, input1);
    byte[] hash2 = Sha256Hash.hash(true, input2);
    assertFalse(Arrays.equals(hash1, hash2));
  }

  @Property(tries = 5000)
  void doubleHashDiffersFromSingleHash(@ForAll byte[] input) {
    Assume.that(input.length > 0);
    byte[] singleHash = Sha256Hash.hash(true, input);
    byte[] doubleHash = Sha256Hash.hash(true, singleHash);
    assertFalse(Arrays.equals(singleHash, doubleHash));
  }
}
