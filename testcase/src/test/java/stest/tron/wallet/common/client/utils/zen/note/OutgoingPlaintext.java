package stest.tron.wallet.common.client.utils.zen.note;


import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import stest.tron.wallet.common.client.utils.exception.ZksnarkException;
import stest.tron.wallet.common.client.utils.zen.ZenChainParams;
import stest.tron.wallet.common.client.utils.zen.note.NoteEncryption.Encryption;
import stest.tron.wallet.common.client.utils.zen.note.NoteEncryption.Encryption.OutPlaintext;

@AllArgsConstructor
public class OutgoingPlaintext {

  @Getter
  @Setter
  private byte[] pkD;
  @Getter
  @Setter
  private byte[] esk;

  private static OutgoingPlaintext decode(Encryption.OutPlaintext outPlaintext) {
    byte[] data = outPlaintext.getData();
    OutgoingPlaintext ret = new OutgoingPlaintext(new byte[ZenChainParams.ZC_JUBJUB_SCALAR_SIZE],
        new byte[ZenChainParams.ZC_JUBJUB_POINT_SIZE]);
    System.arraycopy(data, 0, ret.pkD, 0, ZenChainParams.ZC_JUBJUB_SCALAR_SIZE);
    System.arraycopy(data, ZenChainParams.ZC_JUBJUB_SCALAR_SIZE, ret.esk, 0, ZenChainParams.ZC_JUBJUB_POINT_SIZE);
    return ret;
  }

  public static Optional<OutgoingPlaintext> decrypt(Encryption.OutCiphertext ciphertext, byte[] ovk,
      byte[] cv, byte[] cm, byte[] epk) throws ZksnarkException {
    Optional<OutPlaintext> pt = Encryption
        .attemptOutDecryption(ciphertext, ovk, cv, cm, epk);
    if (!pt.isPresent()) {
      return Optional.empty();
    }
    OutgoingPlaintext ret = OutgoingPlaintext.decode(pt.get());
    return Optional.of(ret);
  }

  private OutPlaintext encode() {
    OutPlaintext ret = new OutPlaintext();
    ret.setData(new byte[ZenChainParams.ZC_OUTPLAINTEXT_SIZE]);
    // ZC_OUTPLAINTEXT_SIZE = (ZC_JUBJUB_POINT_SIZE + ZC_JUBJUB_SCALAR_SIZE)
    System.arraycopy(pkD, 0, ret.getData(), 0, ZenChainParams.ZC_JUBJUB_SCALAR_SIZE);
    System.arraycopy(esk, 0, ret.getData(), ZenChainParams.ZC_JUBJUB_SCALAR_SIZE, ZenChainParams.ZC_JUBJUB_POINT_SIZE);
    return ret;
  }

  /**
   * encrypt plain_out with ock to c_out, use NoteEncryption.epk
   */
  public Encryption.OutCiphertext encrypt(byte[] ovk, byte[] cv, byte[] cm, NoteEncryption enc)
      throws ZksnarkException {
    OutPlaintext pt = this.encode();
    return enc.encryptToOurselves(ovk, cv, cm, pt);
  }
}
