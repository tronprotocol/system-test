package org.tron.stresstest.dispatch.creator.account;

import com.google.protobuf.ByteString;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.program.FullNode;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.dispatch.creator.transfer.AbstractTransferTransactionCreator;

@Setter
public class WithdrawAllowanceTransactionCreator extends AbstractTransferTransactionCreator implements GoodCaseTransactonCreator {

  private String ownerAddress = commonOwnerAddress;
  private String privateKey = commonOwnerPrivateKey;




/*
  @Override
  protected Protocol.Transaction create() {
    String[] array = FullNode.withdrawAllawanceAccountList.poll().split(" ");
    byte[] ownerAddressBytes = ByteArray.fromHexString(array[1].substring(0,array[1].length()-1));



    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    Contract.WithdrawBalanceContract contract = withdrawBalanceContract(ownerAddressBytes);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.WithdrawBalanceContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
*/

  @Override
  protected Protocol.Transaction create() {
     String ownerAddress = commonOwnerAddress;
     long amount = 1L;
     String privateKey = commonOwnerPrivateKey;

    String[] array = FullNode.withdrawAllawanceAccountList.poll().split(" ");
    byte[] ownerAddressBytes = ByteArray.fromHexString(array[1].substring(0,array[1].length()-1));



    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    Contract.TransferContract contract = Contract.TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(ownerAddress)))
        .setToAddress(ByteString.copyFrom(ownerAddressBytes))
        .setAmount(amount)
        .build();


    Protocol.Transaction transaction = createTransaction(contract, ContractType.TransferContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }

}
