package org.tron.stresstest.dispatch.creator.freeze;
import org.tron.common.utils.Configuration;
import java.math.BigInteger;
import org.tron.program.FullNode;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Setter;
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
public class FreezeEnergyCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
  //2万的账号
  //private String ownerAddress = "TRxiyR3cJPwyMMpq3WQQF7xiRkNDLkyd9X";

  //18万的账号
  //private String ownerAddress = "TRxhePptGctYfCpxFCsLLAHUr1iShFkGC1";

  //private String ownerAddress = commonOwnerAddress;
  private long frozenBalance = 1000000L;
  private long frozenDuration = 3L;
  private AtomicInteger resourceCode = new AtomicInteger(0);
  private String delegateAddress = delegateResourceAddress;
  //private String privateKey = commonOwnerPrivateKey;
  //2万的账号
  //private String privateKey = "a79a37a3d868e66456d76b233cb894d664b75fd91861340f3843db05ab3a8c66";
  
  //18万的账号
  //private String privateKey = "1fe1d91bbe3ac4ac5dc9866c157ef7615ec248e3fd4f7d2b49b0428da5e046b2";


  @Override
  protected Protocol.Transaction create() {
    String privateKey = Configuration.getByPath("stress.conf")
        .getString("witness.key" + (resourceCode.get() / 3000000 + 1)); 

   String receiverAddress = FullNode.accountQueue.poll();
   byte[] ownerAddressBytes = getAddress(privateKey);
   //byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);
    Random rand = new Random();
    Integer randNum = rand.nextInt(1000000) + 1000000;
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    Contract.FreezeBalanceContract contract = createFreezeBalanceContract(ownerAddressBytes, randNum, frozenDuration, resourceCode.getAndAdd(1) % 2, receiverAddress);
    Protocol.Transaction transaction = createTransaction(contract, ContractType.FreezeBalanceContract);
    FullNode.accountQueue.offer(receiverAddress);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }


  public byte[] getAddress(String privateKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(privateKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    return ecKey.getAddress();
  }


}
