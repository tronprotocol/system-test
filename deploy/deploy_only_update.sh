testnet=(
10.40.10.244
10.40.10.242
10.40.10.237
10.40.10.245
10.40.10.243
10.40.10.239
10.40.10.240
)


echo "Start build java-tron"
rm -rf /data/workspace/replay_workspace/server_workspace/java-tron
cp -r /data/workspace/replay_workspace/server_workspace/daily-build-java-tron/ /data/workspace/replay_workspace/server_workspace/java-tron
cd /data/workspace/replay_workspace/server_workspace/java-tron/
branch_name=test_contract_timeout
#branch_name=release_4.1.2
#git fetch

#git reset --hard origin/$branch_name
#git reset --hard origin/$branch_name
#git checkout $branch_name
#git reset --hard origin/$branch_name
#git checkout $branch_name
#git pull
#git pull
sed -i "s/for (int i = 1\; i < slot/\/\*for (int i = 1\; i < slot/g" /data/workspace/replay_workspace/server_workspace/java-tron/consensus/src/main/java/org/tron/consensus/dpos/StatisticManager.java
sed -i "s/consensusDelegate.applyBlock(true)/consensusDelegate.applyBlock(true)\*\//g" /data/workspace/replay_workspace/server_workspace/java-tron/consensus/src/main/java/org/tron/consensus/dpos/StatisticManager.java
sed -i "s/long headBlockTime = chainBaseManager.getHeadBlockTimeStamp()/\/\*long headBlockTime = chainBaseManager.getHeadBlockTimeStamp()/g" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/db/Manager.java
sed -i "s/void validateDup(TransactionCapsule/\*\/\}void validateDup(TransactionCapsule/g" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/db/Manager.java
sed -i "s/validateTapos(trxCap)/\/\/validateTapos(trxCap)/g" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/db/Manager.java
sed -i "s/validateCommon(trxCap)/\/\/validateCommon(trxCap)/g" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/db/Manager.java
#sed -i '$s/.$//' /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/db/Manager.java

sed -i 's/ApplicationFactory.create(context);/ApplicationFactory.create(context);saveNextMaintenanceTime(context);/g' /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/program/FullNode.java
sed -i 's/shutdown(appT);/shutdown(appT);mockWitness(context);/g' /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/program/FullNode.java
sed -i '$d' /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/program/FullNode.java
sed -i "2a `cat /data/workspace/replay_workspace/server_workspace/build_insert/FullNode_import | xargs`" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/program/FullNode.java
cat /data/workspace/replay_workspace/server_workspace/build_insert/FullNode_insert >> /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/program/FullNode.java

sed -i "s/private volatile boolean needSyncFromPeer = true/private volatile boolean needSyncFromPeer = false/g" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/net/peer/PeerConnection.java
sed -i "s/private volatile boolean needSyncFromUs = true/private volatile boolean needSyncFromUs = false/g" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/net/peer/PeerConnection.java
#sed -i "s/public static final long FROZEN_PERIOD = 86_400_000L/public static final long FROZEN_PERIOD = 0L/g" /data/workspace/replay_workspace/server_workspace/java-tron/common/src/main/java/org/tron/core/config/Parameter.java
./gradlew clean build -x test -x check
if [ $? = 1 ];then
./gradlew clean build -x test -x check
if [ $? = 1 ];then
slack "deploy jar failed"
exit 1
fi
fi


rm -rf /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/

unzip -o -d /data/workspace/replay_workspace/server_workspace/ /data/workspace/replay_workspace/server_workspace/java-tron/build/distributions/java-tron-1.0.0.zip
sed -i '$a-XX:+HeapDumpOnOutOfMemoryError' /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
sed -i '$a-XX:HeapDumpPath=/data/databackup/java-tron/heapdump/' /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
sed -i '$a-Dcom.sun.management.jmxremote' /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
sed -i '$a-Dcom.sun.management.jmxremote.port=9996' /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
sed -i '$a-Dcom.sun.management.jmxremote.authenticate=false' /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
sed -i '$a-Dcom.sun.management.jmxremote.ssl=false' /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
cp /data/workspace/replay_workspace/server_workspace/java-tron.vmoptions_cms /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
cd /data/workspace/replay_workspace/server_workspace/

for i in ${testnet[@]}; do
  ssh -p 22008 java-tron@$i 'source ~/.bash_profile && cd /data/databackup/java-tron && sh /data/databackup/java-tron/stop.sh'
  echo "Stop java-tron on ${i} completed"
  sleep 3

  ssh -p 22008 java-tron@$i 'cd /data/databackup/java-tron && rm -rf java-tron-1.0.0'

  scp -P 22008  /data/workspace/replay_workspace/server_workspace/conf/config.conf_$i java-tron@$i:/data/databackup/java-tron/config.conf
  tar -c java-tron-1.0.0/ |pigz |ssh -p 22008 java-tron@$i "gzip -d|tar -xC /data/databackup/java-tron/"
  echo "Send java-tron.jar and config.conf and start.sh to ${i} completed"
  ssh -p 22008 java-tron@$i 'source ~/.bash_profile && cd /data/databackup/java-tron && sh /data/databackup/java-tron/start.sh'
  sleep 50
done
echo "Finish send java-tron-1.0.0 and start all node"
