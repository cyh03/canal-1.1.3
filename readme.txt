Canal是一个基于MySQL二进制日志的高性能数据同步系统。Canal广泛用于阿里巴巴集团（包括https://www.taobao.com），以提供可靠的低延迟增量数据管道。

Canal Server能够解析MySQL binlog并订阅数据更改，而Canal Client可以实现将更改广播到任何地方，例如数据库和Apache Kafka。

原理很简单：
Canal模拟MySQL的slave的交互协议，伪装成mysql slave，并将转发协议发送到MySQL Master服务器。
MySQL Master接收到转储请求并开始将二进制日志推送到slave（即canal）。
Canal将二进制日志对象解析为自己的数据类型（原始字节流）

mysql --help|grep 'my.cnf'
查看mysql启动的cnf文件目录

vi /usr/local/etc/my.cnf
log-bin=mysql-bin # 开启 binlog
binlog-format=ROW # 选择 ROW 模式
server_id=1 # 配置 MySQL replaction 需要定义，不要和 canal 的 slaveId 重复

针对阿里云 RDS for MySQL , 默认打开了 binlog , 并且账号默认具有 binlog dump 权限 , 不需要任何权限或者 binlog 设置,可以直接跳过这一步

授权 canal 链接 MySQL 账号具有作为 MySQL slave 的权限, 如果已有账户可直接 grant
CREATE USER canal IDENTIFIED BY 'canal';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
-- GRANT ALL PRIVILEGES ON *.* TO 'canal'@'%' ;
FLUSH PRIVILEGES;

数据库重启后测试设置参数
show variables like 'binlog_format';


优化内容

网络读取优化
socket receiveBuffer/sendBuffer调优
readBytes减少数据拷贝
引入BufferedInputStream提升读取能力

binlog parser优化
时间毫秒精度解析优化
并发解析模型 (引入ringbuffer，拆分了几个阶段：网络接收、Event基本解析、DML解析和protobuf构造、加入memory store，针对瓶颈点protobuf构造采用了多线程的模式提升吞吐)

序列化和传输优化
提前序列化，避免在SessionHandler里主线程里串行序列化
嵌套protobuf对象序列化会产生多次byte[]数据拷贝，硬编码拉平到一个byte[]里处理，避免拷贝
客户端接收数据时，做lazy解析，避免在主线程串行处理
客户端ack做异步处理，避免在主线程串行处理

产品设计
server 代表一个 canal 运行实例，对应于一个 jvm
instance 对应于一个数据队列 （1个 canal server 对应 1..n 个 instance )

instance 下的子模块
eventParser: 数据源接入，模拟 slave 协议和 master 进行交互，协议解析
eventSink: Parser 和 Store 链接器，进行数据过滤，加工，分发的工作
eventStore: 数据存储
metaManager: 增量订阅 & 消费信息管理器

整体类图设计
CanalLifeCycle: 所有 canal 模块的生命周期接口
CanalInstance: 组合 parser,sink,store 三个子模块，三个子模块的生命周期统一受 CanalInstance 管理
CanalServer: 聚合了多个 CanalInstance


EventParser 设计
每个EventParser都会关联两个内部组件
CanalLogPositionManager : 记录binlog 最后一次解析成功位置信息，主要是描述下一次canal启动的位点
CanalHAController: 控制 EventParser 的链接主机管理，判断当前该链接哪个mysql数据库
目前开源版本只支持 MySQL binlog , 默认通过 MySQL binlog dump 远程获取 binlog ,但也可以使用 LocalBinlog - 类 relay log 模式，直接消费本地文件中的 binlog

CanalLogPositionManager 设计
如果 CanalEventStore 选择的是内存模式，可不保留解析位置，下一次 canal 启动时直接依赖 CanalMetaManager 记录的最后一次消费成功的位点即可. (最后一次ack提交的数据位点)
如果 CanalEventStore 选择的是持久化模式，可通过 zookeeper 记录位点信息，canal instance 发生 failover 切换到另一台机器，可通过读取 zookeeper 获取位点信息
可通过实现自己的 CanalLogPositionManager，比如记录位点信息到本地文件/nas 文件实现简单可用的无 HA 模式

CanalHAController类图设计
失败检测常见方式可定时发送心跳语句到当前链接的数据库，超过一定次数检测失败时，尝试切换到备机
如果有一套数据库主备信息管理系统，当数据库主备切换或者机器下线，推送配置到各个应用节点，HAController 收到后，控制 EventParser 进行链接切换

在介绍instance配置之前，先了解一下canal如何维护一份增量订阅&消费的关系信息
解析位点 (parse模块会记录，上一次解析binlog到了什么位置，对应组件为：CanalLogPositionManager)
消费位点 (canal server在接收了客户端的ack后，就会记录客户端提交的最后位点，对应的组件为：CanalMetaManager)

对应的两个位点组件，目前都有几种实现
memory (memory-instance.xml中使用)
zookeeper
mixed
file (file-instance.xml中使用，集合了file+memory模式，先写内存，定时刷新数据到本地file上)
period (default-instance.xml中使用，集合了zookeeper+memory模式，先写内存，定时刷新数据到zookeeper上)

memory-instance.xml介绍
所有的组件(parser , sink , store)都选择了内存版模式，记录位点的都选择了memory模式，重启后又会回到初始位点进行解析
特点：速度最快，依赖最少(不需要zookeeper)
场景：一般应用在quickstart，或者是出现问题后，进行数据分析的场景，不应该将其应用于生产环境

file-instance.xml介绍
所有的组件(parser , sink , store)都选择了基于file持久化模式，注意，不支持HA机制.
特点：支持单机持久化
场景：生产环境，无HA需求，简单可用.

default-instance.xml介绍
所有的组件(parser , sink , store)都选择了持久化模式，目前持久化的方式主要是写入zookeeper，保证数据集群共享.
特点：支持HA
场景：生产环境，集群化部署.

group-instance.xml介绍
主要针对需要进行多库合并时，可以将多个物理instance合并为一个逻辑instance，提供客户端访问。
场景：分库业务。 比如产品数据拆分了4个库，每个库会有一个instance，如果不用group，业务上要消费数据时，需要启动4个客户端，分别链接4个instance实例。使用group后，可以在canal server上合并为一个逻辑instance，只需要启动1个客户端，链接这个逻辑instance即可.