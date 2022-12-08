sh /data/workspace/replay_workspace/server_workspace/check_deploy_result.sh
result=$?
report_text="`date +%Y-%m-%d` 压力测试/流量回放环境部署成功"
if [[ $result -eq 66 ]]
  then
    `slack $report_text`
    exit 0
fi

sleep 30

sh /data/workspace/replay_workspace/server_workspace/check_deploy_result.sh
result=$?
report_text="`date +%Y-%m-%d` 压力测试/流量回放环境部署成功"
if [[ $result -eq 66 ]]
  then
    `slack $report_text`
    exit 0
fi

sleep 30
sh /data/workspace/replay_workspace/server_workspace/check_deploy_result.sh
result=$?
report_text="`date +%Y-%m-%d` 压力测试/流量回放环境部署成功"
if [[ $result -eq 66 ]]
  then
    `slack $report_text`
    exit 0
fi


report_text="`date +%Y-%m-%d` 压力测试/流量回放环境部署失败，请定位"
`slack $report_text`
