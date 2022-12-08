export currentBlockNum=`curl -s -X POST  http://127.0.0.1:50090/wallet/getnowblock | jq .block_header.raw_data.number`
export startNum=46606807
count=0
while [ -z "$currentBlockNum" ]
do
  sleep 10
  export currentBlockNum=`curl -s -X POST  http://127.0.0.1:50090/wallet/getnowblock | jq .block_header.raw_data.number`  
  count=$[$count+1]
  if [ $count -eq 10 ];then
    echo "Try to start java-tron"
    cd /data/java-tron/
    sh start.sh
  fi
  if [ $count -eq 20 ];then
    export endNum=`expr $startNum - 14400`
    break
  fi
done
export endNum=`expr $currentBlockNum - 1`
rm -f /data/workspace/replay_workspace/getTransactions.txt
cp /data/workspace/replay_workspace/templet_of_getTransaction.java /data/workspace/replay_workspace/getTransaction.java
cp /data/workspace/replay_workspace/config_connect_to_mainNet.conf /data/workspace/replay_workspace/wallet-cli/src/main/resources/config.conf
cp /data/workspace/replay_workspace/WalletApi.java /data/workspace/replay_workspace/wallet-cli/src/main/java/org/tron/walletserver/WalletApi.java
cd /data/workspace/replay_workspace/
sed -i "s/startNum/$startNum/g" getTransaction.java
sed -i "s/endNum/$endNum/g" getTransaction.java
cp /data/workspace/replay_workspace/getTransaction.java /data/workspace/replay_workspace/wallet-cli/src/main/java/org/tron/walletcli/GetAllTransaction.java
cd /data/workspace/replay_workspace/wallet-cli/
cp /data/workspace/replay_workspace/build.gradle /data/workspace/replay_workspace/wallet-cli/build.gradle
./gradlew build
java -jar /data/workspace/replay_workspace/wallet-cli/build/libs/wallet-cli.jar
