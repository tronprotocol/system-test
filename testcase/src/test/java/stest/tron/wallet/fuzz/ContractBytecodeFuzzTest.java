package stest.tron.wallet.fuzz;

import com.google.protobuf.ByteString;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import static org.junit.jupiter.api.Assertions.*;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import stest.tron.wallet.common.client.utils.ECKey;

/**
 * Fuzz testing for smart contract bytecode.
 *
 * <p>Generates random bytecode sequences and verifies that the protobuf
 * message construction handles arbitrary data without crashing.
 */
class ContractBytecodeFuzzTest {

  @Property(tries = 1000)
  void randomBytecodeCanBePackaged(@ForAll @Size(max = 24576) byte[] bytecode) {
    ECKey owner = new ECKey();

    SmartContract.Builder contractBuilder = SmartContract.newBuilder()
        .setOriginAddress(ByteString.copyFrom(owner.getAddress()))
        .setBytecode(ByteString.copyFrom(bytecode))
        .setName("FuzzContract")
        .setConsumeUserResourcePercent(100);

    CreateSmartContract createContract = CreateSmartContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(owner.getAddress()))
        .setNewContract(contractBuilder.build())
        .build();

    // Should not throw; protobuf accepts any bytes as bytecode
    assertNotNull(createContract);
    assertEquals(bytecode.length, createContract.getNewContract().getBytecode().size());
  }

  @Property(tries = 500)
  void emptyBytecodeIsAcceptedByProtobuf() {
    ECKey owner = new ECKey();

    SmartContract contract = SmartContract.newBuilder()
        .setOriginAddress(ByteString.copyFrom(owner.getAddress()))
        .setBytecode(ByteString.EMPTY)
        .setName("EmptyContract")
        .build();

    assertEquals(0, contract.getBytecode().size());
  }

  @Property(tries = 500)
  void contractNameWithSpecialChars(@ForAll @StringLength(max = 256) String name) {
    ECKey owner = new ECKey();

    SmartContract contract = SmartContract.newBuilder()
        .setOriginAddress(ByteString.copyFrom(owner.getAddress()))
        .setBytecode(ByteString.copyFrom(new byte[]{0x60, 0x00}))
        .setName(name)
        .build();

    assertEquals(name, contract.getName());
  }

  @Property(tries = 300)
  void abiWithRandomEntryCount(@ForAll @IntRange(min = 0, max = 50) int entryCount) {
    ABI.Builder abiBuilder = ABI.newBuilder();
    for (int i = 0; i < entryCount; i++) {
      abiBuilder.addEntrys(ABI.Entry.newBuilder()
          .setName("func" + i)
          .setType(ABI.Entry.EntryType.Function)
          .build());
    }

    ABI abi = abiBuilder.build();
    assertEquals(entryCount, abi.getEntrysCount());
  }
}
