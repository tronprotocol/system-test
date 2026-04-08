/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package stest.tron.wallet.common.client.utils;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;

@Slf4j(topic = "capsule")
public class BlockCapsule implements ProtoCapsule<Block> {

  public boolean generatedByMyself = false;


  private BlockId blockId = new BlockId(Sha256Hash.ZERO_HASH, 0);

  private Block block;

  private StringBuilder toStringBuff = new StringBuilder();
  private boolean isSwitch;


  public boolean isSwitch() {
    return isSwitch;
  }

  public BlockCapsule setSwitch(boolean aSwitch) {
    isSwitch = aSwitch;
    return this;
  }







  public BlockId getBlockId() {
    if (blockId.equals(Sha256Hash.ZERO_HASH)) {
      blockId =
          new BlockId(Sha256Hash.of(CommonParameter.getInstance().isECKeyCryptoEngine(),
              this.block.getBlockHeader().getRawData().toByteArray()), getNum());
    }
    return blockId;
  }



  public void setAccountStateRoot(byte[] root) {
    BlockHeader.raw blockHeaderRaw =
        this.block.getBlockHeader().getRawData().toBuilder()
            .setAccountStateRoot(ByteString.copyFrom(root)).build();

    this.block = this.block.toBuilder().setBlockHeader(
        this.block.getBlockHeader().toBuilder().setRawData(blockHeaderRaw)).build();
  }

  /* only for genesis */
  public void setWitness(String witness) {
    BlockHeader.raw blockHeaderRaw =
        this.block.getBlockHeader().getRawData().toBuilder().setWitnessAddress(
            ByteString.copyFrom(witness.getBytes())).build();

    this.block = this.block.toBuilder().setBlockHeader(
        this.block.getBlockHeader().toBuilder().setRawData(blockHeaderRaw)).build();
  }

  public Sha256Hash getMerkleRoot() {
    return Sha256Hash.wrap(this.block.getBlockHeader().getRawData().getTxTrieRoot());
  }

  public Sha256Hash getAccountRoot() {
    if (this.block.getBlockHeader().getRawData().getAccountStateRoot() != null
        && !this.block.getBlockHeader().getRawData().getAccountStateRoot().isEmpty()) {
      return Sha256Hash.wrap(this.block.getBlockHeader().getRawData().getAccountStateRoot());
    }
    return Sha256Hash.ZERO_HASH;
  }

  public ByteString getWitnessAddress() {
    return this.block.getBlockHeader().getRawData().getWitnessAddress();
  }

  public boolean isMerkleRootEmpty() {
    return this.block.getBlockHeader().getRawData().getTxTrieRoot().toByteArray().length == 0;
  }

  @Override
  public byte[] getData() {
    return this.block.toByteArray();
  }

  @Override
  public Block getInstance() {
    return this.block;
  }

  public Sha256Hash getParentHash() {
    return Sha256Hash.wrap(this.block.getBlockHeader().getRawData().getParentHash());
  }

  public BlockId getParentBlockId() {
    return new BlockId(getParentHash(), getNum() - 1);
  }

  public ByteString getParentHashStr() {
    return this.block.getBlockHeader().getRawData().getParentHash();
  }

  public long getNum() {
    return this.block.getBlockHeader().getRawData().getNumber();
  }

  public long getTimeStamp() {
    return this.block.getBlockHeader().getRawData().getTimestamp();
  }

  public boolean hasWitnessSignature() {
    return !getInstance().getBlockHeader().getWitnessSignature().isEmpty();
  }



  public static class BlockId extends Sha256Hash {

    private long num;

    public BlockId() {
      super(Sha256Hash.ZERO_HASH.getBytes());
      num = 0;
    }

    public BlockId(Sha256Hash blockId) {
      super(blockId.getBytes());
      byte[] blockNum = new byte[8];
      System.arraycopy(blockId.getBytes(), 0, blockNum, 0, 8);
      num = Longs.fromByteArray(blockNum);
    }

    /**
     * Use {@link #wrap(byte[])} instead.
     */
    public BlockId(Sha256Hash hash, long num) {
      super(num, hash);
      this.num = num;
    }

    public BlockId(byte[] hash, long num) {
      super(num, hash);
      this.num = num;
    }

    public BlockId(ByteString hash, long num) {
      super(num, hash.toByteArray());
      this.num = num;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || (getClass() != o.getClass() && !(o instanceof Sha256Hash))) {
        return false;
      }
      return Arrays.equals(getBytes(), ((Sha256Hash) o).getBytes());
    }

    public String getString() {
      return "Num:" + num + ",ID:" + super.toString();
    }

    @Override
    public String toString() {
      return super.toString();
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

    @Override
    public int compareTo(Sha256Hash other) {
      if (other.getClass().equals(BlockId.class)) {
        long otherNum = ((BlockId) other).getNum();
        return Long.compare(num, otherNum);
      }
      return super.compareTo(other);
    }

    public long getNum() {
      return num;
    }
  }
}
