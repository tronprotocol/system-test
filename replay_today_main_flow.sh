nodeIp=127.0.0.1:50090
testgroup017=10.40.10.245
testgroup018=10.40.10.243
testgroup014=10.40.10.244
cd /data/workspace/replay_workspace/
sh /data/workspace/replay_workspace/collect_mainNet_flow.sh
cd /data/workspace/replay_workspace/server_workspace/
cd /data/java-tron
sh stop.sh
#cd /data/databackup/java-tron
#sh stop.sh
cd /data/workspace/replay_workspace/
ssh -p 22008 java-tron@$testgroup017 '/data/databackup/java-tron/replayFork.sh > /dev/null 2>&1 &'
ssh -p 22008 java-tron@$testgroup018 '/data/databackup/java-tron/replayFork.sh > /dev/null 2>&1 &'
ssh -p 22008 java-tron@$testgroup014 '/data/databackup/java-tron/replayDelay.sh > /dev/null 2>&1 &'
sh /data/workspace/replay_workspace/send_mainNet_flow.sh
sh /data/workspace/replay_workspace/send_mainNet_flow.sh
sh /data/workspace/replay_workspace/send_mainNet_flow.sh
cd /data/java-tron
sh start.sh
sleep 120
queryClient=10.40.10.224
ssh -p 22008 java-tron@$queryClient 'source ~/.bash_profile && cd /data/databackup/mainnetApiQueryTask && sh kill_query_task.sh '
sleep 1
ssh -p 22008 java-tron@$queryClient "/data/databackup/mainnetApiQueryTask/do_mainnet_query_task.sh > /dev/null 2>&1 &"
#cd /data/databackup/java-tron
#sh start.sh
