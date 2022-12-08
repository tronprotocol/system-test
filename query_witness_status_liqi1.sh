i=1
#sleep 600
length=0
while [[ $i -le 1 ]]
do
  export currentBlockNumber=`curl -s -X POST  http://10.40.10.244:50090/wallet/getnowblock | jq .block_header.raw_data.number`
  declare -A resultMap

  for ((time=0; time<86; time++))
  do
witness=witness_address_
expect=$witness$time
expect=`curl -s -X POST  http://10.40.10.244:50090/wallet/getblockbynum -d \{"num":$[$currentBlockNumber-$time]}\ | jq .block_header.raw_data.witness_address`
resultMap[$expect]=$expect
#echo $expect
 done
length=${#resultMap[@]}
#echo "hello------"
#echo $length
  if [ $length -ne 27 ]; then
     let i++
  else
    break;
  fi
done

sleep 15
export nowBlockNumber=`curl -s -X POST  http://10.40.10.244:50090/wallet/getnowblock | jq .block_header.raw_data.number`
if [ $currentBlockNumber -eq $nowBlockNumber ]; then
 echo "The env can't produce block, please check env"
 exit 0
fi
if [ $length -eq 27 ]; then
  echo "All 27 witnesses can produce block after stress pressure test"
else
  export failed_witness=`expr 27 - $length`
  echo "There are ${failed_witness} witnesses can't produce block after stress pressure test, please check env"
fi
