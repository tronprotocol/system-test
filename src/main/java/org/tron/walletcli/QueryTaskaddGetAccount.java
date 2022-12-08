
package org.tron.walletcli;

import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.walletserver.GrpcClient;
import org.tron.walletserver.WalletApi;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryTaskaddGetAccount implements Runnable {

    private GrpcClient client;
    private Boolean flag;
    private static AtomicInteger invCount = new AtomicInteger(0);

    public QueryTaskaddGetAccount(GrpcClient client) {
     
        this.client = client;
    }

    @Override
    public void run() {

        flag = false;
        if (flag && (invCount.getAndIncrement() % 1 == 0)) {
            {
                System.out.println("1111:"+invCount.get());
                String curAccount = GetAllTransaction.accountQueue.poll();
                GetAllTransaction.accountQueue.offer(curAccount);
                Protocol.Account accMessage = client.queryAccount(WalletApi.decodeFromBase58Check(curAccount));
                System.out.println("balance:" + accMessage.getBalance());
            }
        }
    }
}
