package com.sealand.gateway.etcd;

import com.alibaba.fastjson.JSON;
import com.sealand.common.config.Rule;
import com.sealand.common.constants.BasicConst;
import com.sealand.gateway.config.center.api.ConfigCenter;
import com.sealand.gateway.config.center.api.RulesChangeListener;
import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author cgh
 * @create 2024/7/6
 * etcd 配置中心实现类
 */


@Slf4j
public class EtcdConfigCenter implements ConfigCenter {


    private Client client;

    private static final String CONFIG = "/api-gateway/config/api-gateway";

    private KV kvClient;

    private String env;

    private Watch watchClient;

    @Override
    public void init(String serverAddress, String env) {
        this.client = Client.builder().endpoints(serverAddress).build();
        this.kvClient = client.getKVClient();
        this.watchClient = client.getWatchClient();
        this.env = env;
        ByteSequence configKey = ByteSequence.from((BasicConst.PATH_SEPARATOR + env + CONFIG).getBytes());

        String value = "{\n" +
                "  \"rules\": [\n" +
                "    {\n" +
                "      \"id\": \"001\",\n" +
                "      \"name\": \"1号规则\",\n" +
                "      \"protocol\": \"http\",\n" +
                "      \"serviceId\": \"backend-http-server\",\n" +
                "      \"prefix\": \"/http-server\",\n" +
                "      \"paths\": [\n" +
                "        \"/http-server/ping\",\n" +
                "        \"/http-server/test\",\n" +
                "        \"/http-server/post\"\n" +
                "      ],\n" +
                "      \"filterConfigs\": [\n" +
                "        {\n" +
                "          \"id\": \"load_balance_filter\",\n" +
                "          \"config\": {\n" +
                "            \"load_balance\": \"RoundRobin\"\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"flow_ctl_filter\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"flowCtlConfigs\": [\n" +
                "        {\n" +
                "          \"type\": \"path\",\n" +
                "          \"model\": \"distributed_flowCtl\",\n" +
                "          \"value\": \"/http-server/ping\",\n" +
                "          \"config\": {\n" +
                "            \"duration\": 20,\n" +
                "            \"permits\": 2\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"dubbo.100\",\n" +
                "      \"name\": \"dubbo规则\",\n" +
                "      \"protocol\": \"dubbo\",\n" +
                "      \"serviceId\": \"backend-dubbo-server\",\n" +
                "      \"prefix\": \"/dubbo\",\n" +
                "      \"paths\": [\n" +
                "        \"/dubbo-server/ping\",\n" +
                "        \"/dubbo-server/list\",\n" +
                "        \"/dubbo-server/user\"\n" +
                "      ],\n" +
                "      \"filterConfigs\": [\n" +
                "        {\n" +
                "          \"id\": \"dubbo_filter\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"load_balance_filter\",\n" +
                "          \"config\": {\n" +
                "            \"load_balance\": \"RoundRobin\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        kvClient.put(configKey, ByteSequence.from(value.getBytes()));
        log.info("config load success...");
    }



    @Override
    public void subscribeRulesChange(RulesChangeListener rulesChangeListener) {
        log.info("etcd config init...");
        ByteSequence configKey = ByteSequence.from((BasicConst.PATH_SEPARATOR + env + CONFIG).getBytes());

        try {
            GetResponse getResponse = kvClient.get(configKey).get();
            getResponse.getKvs().forEach(
                    keyValue -> {
                        List<Rule> rules = JSON.parseObject(keyValue.getValue().toString()).getJSONArray("rules").toJavaList(Rule.class);
                        log.info("获取 etcd 配置，config: {}", keyValue.getValue().toString());
                        rulesChangeListener.onRulesChange(rules);
                        //监听器
                        watchClient.watch(configKey, Watch.listener(watchResponse -> {
                            watchResponse.getEvents().forEach(
                                    watchEvent -> {
                                        if (watchEvent.getEventType().equals(WatchEvent.EventType.PUT)) {
                                            KeyValue kv = watchEvent.getKeyValue();
                                            List<Rule> changeRules = JSON.parseObject(kv.getValue().toString()).getJSONArray("rules").toJavaList(Rule.class);
                                            log.info("配置变化，监听器生效；config:{}",kv.getValue().toString());
                                            rulesChangeListener.onRulesChange(changeRules);
                                        }
                                    }
                            );
                        }));
                    }
            );
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
