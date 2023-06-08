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
public class CancelAllUnfreezeV2Creator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
  public static AtomicLong cancelAllUnfreezeV2Count = new AtomicLong();


  @Override
  protected Protocol.Transaction create() {
    cancelAllUnfreezeV2Count.incrementAndGet();

    String privateKey = Configuration.getByPath("stress.conf")
        .getString("witness.key" + (cancelAllUnfreezeV2Count.get() % 27 + 1));
    Contract.CancelAllUnfreezeV2Contract contract;
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    contract = cancelAllUnfreezeV2Contract(getAddress(privateKey));

    Protocol.Transaction transaction = createTransaction(contract, ContractType.CancelAllUnfreezeV2Contract);
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

