package org.tron.stresstest.dispatch.creator.freeze;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Configuration;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;


@Setter
public class DelegateResourceCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
  public static AtomicLong createFreezeBalanceV2Count = new AtomicLong();


  @Override
  protected Protocol.Transaction create() {
    createFreezeBalanceV2Count.incrementAndGet();

    String privateKey = Configuration.getByPath("stress.conf")
        .getString("witness.key" + (createFreezeBalanceV2Count.get() % 27 + 1));

    String receiverKey = Configuration.getByPath("stress.conf")
        .getString("witness.key" + (new Random().nextInt(27) + 1));
    while (receiverKey == privateKey) {
      receiverKey = Configuration.getByPath("stress.conf")
          .getString("witness.key" + (new Random().nextInt(27) + 1));
    }


    Contract.DelegateResourceContract contract;
    if(createFreezeBalanceV2Count.get() % 2 == 0L) {
      TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
      contract = delegateResourceContract(getAddress(privateKey),
          new Random().nextInt(500000) + 1000000,
          0,getAddress(receiverKey));
    } else {
      TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
      contract = delegateResourceContract(getAddress(privateKey),
          new Random().nextInt(500000) + 1000000,
          1,getAddress(receiverKey));
    }

    Protocol.Transaction transaction = createTransaction(contract, ContractType.DelegateResourceContract);
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

