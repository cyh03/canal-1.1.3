package com.alibaba.otter.canal.client.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.apache.commons.lang.StringUtils;

import com.alibaba.otter.canal.client.CanalNodeAccessStrategy;
import com.alibaba.otter.canal.common.utils.JsonUtils;
import com.alibaba.otter.canal.common.zookeeper.ZkClientx;
import com.alibaba.otter.canal.common.zookeeper.ZookeeperPathUtils;
import com.alibaba.otter.canal.common.zookeeper.running.ServerRunningData;

/**
 * 集群模式的调度策略
 * 
 * @author jianghang 2012-12-3 下午10:01:04
 * @version 1.0.0
 */
//
public class ClusterNodeAccessStrategy implements CanalNodeAccessStrategy {

    private String                           destination;
    private IZkChildListener                 childListener;                                      // 监听所有的服务器列表
    private IZkDataListener                  dataListener;                                       // 监听当前的工作节点
    private ZkClientx                        zkClient;
    private volatile List<InetSocketAddress> currentAddress = new ArrayList<InetSocketAddress>();
    private volatile InetSocketAddress       runningAddress = null;

    public ClusterNodeAccessStrategy(String destination, ZkClientx zkClient){
        this.destination = destination;
        this.zkClient = zkClient;
        childListener = new IZkChildListener() {

            public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                initClusters(currentChilds);
            }

        };

        dataListener = new IZkDataListener() {

            public void handleDataDeleted(String dataPath) throws Exception {
                runningAddress = null;
            }

            public void handleDataChange(String dataPath, Object data) throws Exception {
                initRunning(data);
            }

        };

//        获取zk canal节点路径
        String clusterPath = ZookeeperPathUtils.getDestinationClusterRoot(destination);
//        订阅canal zk节点变化
        this.zkClient.subscribeChildChanges(clusterPath, childListener);
//        初始化canal集群节点
        initClusters(this.zkClient.getChildren(clusterPath));

//        获取运行的canal zk节点路径
        String runningPath = ZookeeperPathUtils.getDestinationServerRunning(destination);
//        订阅运行的canal zk节点变化
        this.zkClient.subscribeDataChanges(runningPath, dataListener);
//        初始化运行的canal节点地址
        initRunning(this.zkClient.readData(runningPath, true));
    }

    @Override
    public SocketAddress currentNode() {
        return nextNode();
    }

//
    public SocketAddress nextNode() {
        if (runningAddress != null) {// 如果服务已经启动，直接选择当前正在工作的节点
            return runningAddress;
        } else if (!currentAddress.isEmpty()) { // 如果不存在已经启动的服务，可能服务是一种lazy启动，随机选择一台触发服务器进行启动
            return currentAddress.get(0);// 默认返回第一个节点，之前已经做过shuffle
        } else {
            throw new ServerNotFoundException("no alive canal server for " + destination);
        }
    }

//
    private void initClusters(List<String> currentChilds) {
        if (currentChilds == null || currentChilds.isEmpty()) {
            currentAddress = new ArrayList<InetSocketAddress>();
        } else {
            List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
            for (String address : currentChilds) {
                String[] strs = StringUtils.split(address, ":");
                if (strs != null && strs.length == 2) {
                    addresses.add(new InetSocketAddress(strs[0], Integer.valueOf(strs[1])));
                }
            }

//            打乱集合中元素顺序，get(0)获取的元素就是随机的
            Collections.shuffle(addresses);
            currentAddress = addresses;// 直接切换引用
        }
    }

//
    private void initRunning(Object data) {
        if (data == null) {
            return;
        }

        ServerRunningData runningData = JsonUtils.unmarshalFromByte((byte[]) data, ServerRunningData.class);
        String[] strs = StringUtils.split(runningData.getAddress(), ':');
        if (strs.length == 2) {
            runningAddress = new InetSocketAddress(strs[0], Integer.valueOf(strs[1]));
        }
    }

    public void setZkClient(ZkClientx zkClient) {
        this.zkClient = zkClient;
    }

    public ZkClientx getZkClient() {
        return zkClient;
    }

}
