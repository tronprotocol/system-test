i=1
sleep 600
query_result=0
while [[ $i -le 3 && $query_result -eq 0 ]]
do
  export currentBlockNumber=`curl -s -X POST  http://10.40.10.244:50090/wallet/getnowblock | jq .block_header.raw_data.number`
  witness_address_1=`curl -s -X POST  http://10.40.10.244:50090/wallet/getblockbynum -d \{"num":$currentBlockNumber}\ | jq .block_header.raw_data.witness_address`
  witness_address_2=`curl -s -X POST  http://10.40.10.242:50090/wallet/getblockbynum -d \{"num":$[$currentBlockNumber-1]}\ | jq .block_header.raw_data.witness_address`
  witness_address_3=`curl -s -X POST  http://10.40.10.237:50090/wallet/getblockbynum -d \{"num":$[$currentBlockNumber-2]}\ | jq .block_header.raw_data.witness_address`
  witness_address_4=`curl -s -X POST  http://10.40.10.245:50090/wallet/getblockbynum -d \{"num":$[$currentBlockNumber-3]}\ | jq .block_header.raw_data.witness_address`
  witness_address_5=`curl -s -X POST  http://10.40.10.243:50090/wallet/getblockbynum -d \{"num":$[$currentBlockNumber-4]}\ | jq .block_header.raw_data.witness_address`
  witness_list=($witness_address_1 $witness_address_2 $witness_address_3 $witness_address_4 $witness_address_5)
  for((j=0;j<=4;j++));
  do
    for((m=$[$j+1];m<=4;m++));
      do
      if [ "${witness_list[$j]}" == "${witness_list[$m]}" ]; then
        query_result=0
        sleep 15
        break
      else   
        query_result=1
        continue
      fi
    done;
    if [ $query_result -eq 0 ]; then
    break
    fi
  done; 
  let i++
done
sleep 15
export nowBlockNumber=`curl -s -X POST  http://10.40.10.244:50090/wallet/getnowblock | jq .block_header.raw_data.number`
if [ $currentBlockNumber -eq $nowBlockNumber ]; then
#if [ $currentBlockNumber -eq $currentBlockNumber ]; then
 echo "The env can't produce block, please check env"
 exit 0
fi
if [ $query_result -eq 0 ]; then
  echo "There are less then one witness can't produce block after stress pressure test, please check env"
else
  echo "All 5 witness can produce block after stress pressure test"
fi
