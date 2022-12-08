package org.tron.walletcli;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.walletserver.GrpcClient;
import org.tron.walletserver.WalletApi;

public class MyTask implements Runnable {

  private int taskNum;
  private Transaction trans;
  private GrpcClient client;
  private Boolean flag;

  public MyTask(Transaction trans, GrpcClient client) {
    this.trans = trans;
    this.client = client;
}
  @Override
  public void run() {
    client.broadcastTransaction(trans);
 }
}
