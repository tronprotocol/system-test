sleep 300
testnet=(
10.40.10.244
10.40.10.242
10.40.10.237
10.40.10.245
10.40.10.243
)




for i in ${testnet[@]}; do
  ssh -p 22008 java-tron@$i 'source ~/.bash_profile && cd /data/databackup/java-tron && sh /data/databackup/java-tron/stop.sh'
  sleep 30
  ssh -p 22008 java-tron@$i 'source ~/.bash_profile && cd /data/databackup/java-tron && sh /data/databackup/java-tron/start.sh'
  sleep 200
done
