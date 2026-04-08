package stest.tron.wallet.fuzz;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import static org.junit.jupiter.api.Assertions.*;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.protos.Protocol.Transaction;

/**
 * Fuzz testing for gRPC/protobuf message parsing.
 *
 * <p>Feeds random bytes into protobuf parsers to verify they handle
 * malformed input gracefully (throw exceptions, not crash).
 */
class GrpcMessageFuzzTest {

  @Property(tries = 2000)
  void randomBytesAsTransactionShouldNotCrash(@ForAll byte[] randomBytes) {
    try {
      Transaction tx = Transaction.parseFrom(randomBytes);
      // If parsing succeeds, the result should be a valid protobuf object
      assertNotNull(tx);
    } catch (InvalidProtocolBufferException e) {
      // Expected for random bytes
    }
  }

  @Property(tries = 2000)
  void randomBytesAsBytesMessageShouldNotCrash(@ForAll byte[] randomBytes) {
    try {
      BytesMessage msg = BytesMessage.parseFrom(randomBytes);
      assertNotNull(msg);
    } catch (InvalidProtocolBufferException e) {
      // Expected
    }
  }

  @Property(tries = 2000)
  void randomBytesAsNumberMessageShouldNotCrash(@ForAll byte[] randomBytes) {
    try {
      NumberMessage msg = NumberMessage.parseFrom(randomBytes);
      assertNotNull(msg);
    } catch (InvalidProtocolBufferException e) {
      // Expected
    }
  }

  @Property(tries = 1000)
  void bytesMessageRoundTrip(@ForAll @Size(max = 1024) byte[] data) {
    BytesMessage original = BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom(data))
        .build();

    try {
      BytesMessage parsed = BytesMessage.parseFrom(original.toByteArray());
      assertArrayEquals(data, parsed.getValue().toByteArray(),
          "BytesMessage should roundtrip correctly");
    } catch (InvalidProtocolBufferException e) {
      fail("Valid BytesMessage should always parse: " + e.getMessage());
    }
  }

  @Property(tries = 1000)
  void numberMessageRoundTrip(@ForAll long number) {
    NumberMessage original = NumberMessage.newBuilder()
        .setNum(number)
        .build();

    try {
      NumberMessage parsed = NumberMessage.parseFrom(original.toByteArray());
      assertEquals(number, parsed.getNum(),
          "NumberMessage should roundtrip correctly");
    } catch (InvalidProtocolBufferException e) {
      fail("Valid NumberMessage should always parse: " + e.getMessage());
    }
  }

  @Property(tries = 500)
  void transactionWithRandomRawBytes(@ForAll @Size(max = 4096) byte[] rawBytes) {
    Transaction.Builder txBuilder = Transaction.newBuilder();
    try {
      Transaction.raw rawData = Transaction.raw.parseFrom(rawBytes);
      txBuilder.setRawData(rawData);
      Transaction tx = txBuilder.build();
      assertNotNull(tx);
    } catch (InvalidProtocolBufferException e) {
      // Expected for random bytes
    }
  }
}
