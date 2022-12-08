setQps=$1
echo "2222222222:wq"
echo $setQps
testWitness=(
10.40.10.244
10.40.10.242
10.40.10.237
10.40.10.245
10.40.10.243
)

testnet=(
10.40.10.244
10.40.10.242
10.40.10.237
10.40.10.245
10.40.10.243

10.40.10.239
10.40.10.240
10.40.10.224
#10.40.10.16
#10.40.10.31
)

queryClient=10.40.10.224

cp /data/workspace/replay_workspace/config_connect_to_stress.conf /data/workspace/replay_workspace/wallet-cli/src/main/resources/config.conf
#cp /data/workspace/replay_workspace/config_connect_to_stress.conf /data/workspace/replay_workspace/second-wallet-cli/src/main/resources/config.conf
cd /data/workspace/replay_workspace/
cp /data/workspace/replay_workspace/sendTransaction.java /data/workspace/replay_workspace/wallet-cli/src/main/java/org/tron/walletcli/GetAllTransaction.java
cp /data/workspace/replay_workspace/MyTask.java /data/workspace/replay_workspace/wallet-cli/src/main/java/org/tron/walletcli/MyTask.java
cp /data/workspace/replay_workspace/QueryTaskaddGetAccount.java /data/workspace/replay_workspace/wallet-cli/src/main/java/org/tron/walletcli/QueryTaskaddGetAccount.java
cd /data/workspace/replay_workspace/wallet-cli/src/main/java/org/tron/walletcli

if [ -n "$1" ]
 then
 echo $1
 echo "qps not null"
 setQps=$1
#sed -i 's/sendTransaction(clients\, \"\/data\/workspace\/replay_workspace\/getTransactions.txt\"\,1000);/sendTransaction(clients\, \"\/data\/workspace\/replay_workspace\/getTransactions.txt\"\,'"$setQps"');/g' /data/workspace/replay_workspace/wallet-cli/src/main/java/org/tron/walletcli/GetAllTransaction.java
sed -i 's/sendTransaction(clients\, \"\/data\/workspace\/replay_workspace\/getTransactions.txt\"\,1000);/sendTransaction(clients\, \"\/data\/workspace\/replay_workspace\/getTransactions.txt\"\,'"$setQps"');/g' /data/workspace/replay_workspace/wallet-cli/src/main/java/org/tron/walletcli/GetAllTransaction.java

fi


if [ -z "$2" ]
then
 echo "error:param2_boolean is null!"
else
setBoolean=$2
 if [[ $setBoolean == "true" ||  $setBoolean == "false" ]]
  then
sed -i 's/flag = false;/flag = '"$setBoolean"';/g' QueryTaskaddGetAccount.java
  fi
fi



cp /data/workspace/replay_workspace/WalletApi.java /data/workspace/replay_workspace/wallet-cli/src/main/java/org/tron/walletserver/WalletApi.java
#cp /data/workspace/replay_workspace/WalletApi.java /data/workspace/replay_workspace/second-wallet-cli/src/main/java/org/tron/walletserver/WalletApi.java
cd /data/workspace/replay_workspace/wallet-cli/
cp /data/workspace/replay_workspace/build.gradle /data/workspace/replay_workspace/wallet-cli/build.gradle
#cp /data/workspace/replay_workspace/build.gradle /data/workspace/replay_workspace/second-wallet-cli/build.gradle
./gradlew build
#cd /data/workspace/replay_workspace/second-wallet-cli/
#./gradlew build
export replayStartNum=`curl -s -X POST  http://10.40.10.244:50090/wallet/getnowblock | jq .block_header.raw_data.number`
export replayStartNum=$[$replayStartNum + 10]
ssh -p 22008 java-tron@$queryClient 'source ~/.bash_profile && cd /data/databackup/mainnetApiQueryTask && sh kill_query_task.sh '
sleep 1
ssh -p 22008 java-tron@$queryClient "/data/databackup/mainnetApiQueryTask/do_query_task.sh > /dev/null 2>&1 &"
echo "hi-----"
echo $replayStartNum
java -jar /data/workspace/replay_workspace/wallet-cli/build/libs/wallet-cli.jar

ssh -p 22008 java-tron@$queryClient 'source ~/.bash_profile && cd /data/databackup/mainnetApiQueryTask && sh kill_query_task.sh '


export replayEndNum=`curl -s -X POST  http://10.40.10.244:50090/wallet/getnowblock | jq .block_header.raw_data.number`
export replayEndNum=$[$replayEndNum - 10]
echo "end the program"
replayServer=10.40.10.244:50090
replayStartTime=`curl -s -X POST http://$replayServer/wallet/getblockbynum -d \{"num":$replayStartNum}\ | jq .block_header.raw_data.timestamp`
replayEndTime=`curl -s -X POST http://$replayServer/wallet/getblockbynum -d \{"num":$replayEndNum}\ | jq .block_header.raw_data.timestamp`
echo "replayStartNum: $replayStartNum"
echo "replayEndNum: $replayEndNum"
TransactionCount=1
maxTransactionSizeInOneBlock=0
minTransactionSizeInOneBlock=10000
for((i=replayStartNum;i<=replayEndNum;i++));
do
transactionNumInThisBlock=`curl -s -X POST http://$replayServer/wallet/getblockbynum -d \{"num":$i}\ | jq . | grep "txID" | wc -l`
TransactionCount=$[$TransactionCount+$transactionNumInThisBlock]
  if [ $transactionNumInThisBlock -gt $maxTransactionSizeInOneBlock ]
    then
    maxTransactionSizeInOneBlock=$transactionNumInThisBlock
  fi
  if [ $transactionNumInThisBlock -lt $minTransactionSizeInOneBlock ]
    then
    minTransactionSizeInOneBlock=$transactionNumInThisBlock
  fi
done
targetTime=$((replayEndNum - replayStartNum))
targetTime=`expr $targetTime \* 3`
echo "targetTime is "$targetTime

costTime=$((replayEndTime - replayStartTime))
costTime=$(($costTime/1000))
tps=$(($TransactionCount/$costTime))
costHours=$(printf "%.2f" `echo "scale=2;$costTime/3600"|bc`)
backwardTime=$(($costTime-$targetTime))
MissBlockRate=`awk 'BEGIN{printf "%.1f%\n",('$backwardTime'/'$costTime')*100}'`
echo "Total transactions: $TransactionCount, cost time: $costHours"h", average tps: $tps/s, MissBlockRate: $MissBlockRate"
replay_massage="1.Replay report: Total transactions: $TransactionCount, cost time: $costHours"h", max block size : $maxTransactionSizeInOneBlock, min block size : $minTransactionSizeInOneBlock, average tps: $tps/s, MissBlockRate: $MissBlockRate"

export currentBlockNumber=`curl -s -X POST  http://10.40.10.244:50090/wallet/getnowblock | jq .block_header.raw_data.number`

export witness_produce_block_status=`sh /data/workspace/replay_workspace/query_witness_status.sh`
echo "--------------hello-------------"
export witness_produce_block_status_liqi=`sh /data/workspace/replay_workspace/query_witness_status_liqi1.sh`
cur_data=`date +%Y-%m-%d`
report_text="`date +%Y-%m-%d` 现网流量回放报告："
#########echo "Replay Main Net flow report:" >> /data/workspace/replay_workspace/Replay_Daily_Report
echo $replay_massage >> /data/workspace/replay_workspace/Replay_Daily_Report
echo $witness_produce_block_status >> /data/workspace/replay_workspace/Replay_Daily_Report
echo "--------------hello-------------"
echo $witness_produce_block_status_liqi >> /data/workspace/replay_workspace/Replay_Daily_Report

report_text=$report_text"\n"$replay_massage
report_text=$report_text"\n"$witness_produce_block_status
report_text=$report_text"\n"$witness_produce_block_status_liqi
for i in ${testWitness[@]}; do
  export node_ip=$i
  get_CPU_MEM_result=""
  replay_log=`ssh -p 22008 java-tron@$i "grep 'at org.tron' /data/databackup/java-tron/logs/tron.log"`
  gc_log=`ssh -p 22008 java-tron@$i "grep 'Full GC' /data/databackup/java-tron/gc.log"`
  if [ -z "$replay_log" ]; then
    report_text="$report_text\n$i has no Exception. $get_CPU_MEM_result"
    echo "$i has no Exception. $get_CPU_MEM_result" >> /data/workspace/compression_workspace/Stress_Daily_Report
  else
    echo "$replay_log. $get_CPU_MEM_result" >> /data/workspace/compression_workspace/Stress_Daily_Report
    report_text="$i $report_text\n$replay_log. $get_CPU_MEM_result"
  fi

  if [ -z "$gc_log" ]; then
    report_text="$report_text\n$i has no Full GC. $get_CPU_MEM_result"
    echo "$i has no Full GC. $get_CPU_MEM_result" >> /data/workspace/compression_workspace/Stress_Daily_Report
  else
    echo "$replay_log. $get_CPU_MEM_result" >> /data/workspace/compression_workspace/Stress_Daily_Report
    report_text="$i $report_text\n$i has Full GC,please check. $get_CPU_MEM_result"
  fi

done
echo $report_text
`slack $report_text`
curl "https://oapi.dingtalk.com/robot/send?access_token=78304ebbbd027113ac62080541818c0fe12fd8d66b29e1b598b5f0594eda0f92" \
    -H 'Content-Type: application/json' \
    -d "
{
    \"msgtype\": \"text\",
    \"text\": {
        \"content\": \"$report_text\"
    }
}
"
