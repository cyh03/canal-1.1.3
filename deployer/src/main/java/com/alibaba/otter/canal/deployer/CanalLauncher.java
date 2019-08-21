package com.alibaba.otter.canal.deployer;

import com.alibaba.otter.canal.deployer.monitor.remote.RemoteCanalConfigMonitor;
import com.alibaba.otter.canal.deployer.monitor.remote.RemoteConfigLoader;
import com.alibaba.otter.canal.deployer.monitor.remote.RemoteConfigLoaderFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * canal独立版本启动的入口类
 *
 * @author jianghang 2012-11-6 下午05:20:49
 * @version 1.0.0
 */
//
public class CanalLauncher {

    private static final String    CLASSPATH_URL_PREFIX = "classpath:";
    private static final Logger    logger               = LoggerFactory.getLogger(CanalLauncher.class);
    public static volatile boolean running              = false;

    public static void main(String[] args) {
        try {
//            canal服务端启动
            running = true;
            logger.info("## set default uncaught exception handler");
            setGlobalUncaughtExceptionHandler();
//            加载canal.properties
            logger.info("## load canal configurations");
            String conf = System.getProperty("canal.conf", "classpath:canal.properties");
            Properties properties = new Properties();
            RemoteConfigLoader remoteConfigLoader = null;
            if (conf.startsWith(CLASSPATH_URL_PREFIX)) {
                conf = StringUtils.substringAfter(conf, CLASSPATH_URL_PREFIX);
//                加载canal.properties配置文件
                properties.load(CanalLauncher.class.getClassLoader().getResourceAsStream(conf));
            } else {
                properties.load(new FileInputStream(conf));
            }

//            远程mysql管理配置
            remoteConfigLoader = RemoteConfigLoaderFactory.getRemoteConfigLoader(properties);
            if (remoteConfigLoader != null) {
                // 从数据库中加载加载远程canal.properties
                Properties remoteConfig = remoteConfigLoader.loadRemoteConfig();
                // 从数据库中加载加载remote instance配置
                remoteConfigLoader.loadRemoteInstanceConfigs();
                if (remoteConfig != null) {
                    properties = remoteConfig;
                } else {
                    remoteConfigLoader = null;
                }
            }

            final CanalStater canalStater = new CanalStater();
            canalStater.start(properties);

            if (remoteConfigLoader != null) {
                remoteConfigLoader.startMonitor(new RemoteCanalConfigMonitor() {

                    @Override
                    public void onChange(Properties properties) {
                        try {
                            // 远程配置canal.properties修改重新加载整个应用
                            canalStater.destroy();
                            canalStater.start(properties);
                        } catch (Throwable throwable) {
                            logger.error(throwable.getMessage(), throwable);
                        }
                    }
                });
            }

            while (running) {
                Thread.sleep(1000);
            }

            if (remoteConfigLoader != null) {
                remoteConfigLoader.destroy();
            }
        } catch (Throwable e) {
            logger.error("## Something goes wrong when starting up the canal Server:", e);
        }
    }

    private static void setGlobalUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.error("UnCaughtException", e);
            }
        });
    }

}
