package org.tron.stresstest.dispatch.creator.freeze;
import org.tron.program.FullNode;
import lombok.Setter;
import java.util.concurrent.atomic.AtomicInteger;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

@Setter
public class UnFreezeEnergyCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

  //private String ownerAddress = commonOwnerAddress;
  private String ownerAddress = "TRxiyR3cJPwyMMpq3WQQF7xiRkNDLkyd9X";
  //private int resourceCode = 1;
  private AtomicInteger resourceCode = new AtomicInteger(0);
  private String delegateAddress = delegateResourceAddress;
  //private String privateKey = commonOwnerPrivateKey;
    private String privateKey = "a79a37a3d868e66456d76b233cb894d664b75fd91861340f3843db05ab3a8c66";
  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    Contract.UnfreezeBalanceContract contract = createUnfreezeBalanceContract(ownerAddressBytes, resourceCode.getAndAdd(1) % 2, FullNode.accountQueue.poll());
    Protocol.Transaction transaction = createTransaction(contract, ContractType.UnfreezeBalanceContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
