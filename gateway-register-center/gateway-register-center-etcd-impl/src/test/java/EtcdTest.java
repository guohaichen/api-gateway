import com.alibaba.fastjson.JSON;
import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.common.constants.BasicConst;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @author cgh
 * @create 2024/7/7
 */
public class EtcdTest {

    @Test
    public void selectAllKV() throws ExecutionException, InterruptedException {
        Client client = Client.builder().endpoints("http://localhost:2379").build();
        KV kvClient = client.getKVClient();

        List<String> serviceDefinitions = Arrays.asList("/dev/api-gateway/service/api-gateway", "/dev/api-gateway/service/backend-http-server");

        for (String serviceDefinition : serviceDefinitions) {
            ByteSequence prefixKey = ByteSequence.from(serviceDefinition.getBytes());
            GetOption getOption = GetOption.newBuilder().isPrefix(true).build();
//            GetResponse getResponse = kvClient.get(prefixKey, getOption).get();
            GetResponse getResponse = kvClient.get(prefixKey).get();
            getResponse.getKvs().forEach(kv -> {
                System.out.print("key: " + kv.getKey().toString());
                System.out.println("\tvalue: " + kv.getValue().toString());
            });
        }
        // 打印查询结果
        client.close();
    }


    @Test
    public void watchKey() throws ExecutionException, InterruptedException {
        Client client = Client.builder().endpoints("http://localhost:2379").build();
        KV kvClient = client.getKVClient();
        Watch watchClient = client.getWatchClient();

//        while (true) {
        List<String> serviceDefinitions = Arrays.asList("/dev/api-gateway/service/api-gateway", "/dev/api-gateway/service/backend-http-server");
        Map<ServiceDefinition, Set<ServiceInstance>> serviceMap = new HashMap<>();


        GetOption getOption = GetOption.newBuilder().isPrefix(true).build();

        for (String serviceDefinition : serviceDefinitions) {
            GetResponse getResponse = kvClient.get(ByteSequence.from(serviceDefinition.getBytes())).get();
            getResponse.getKvs().forEach(keyValue -> {
                String serviceValue = keyValue.getValue().toString();
                ServiceDefinition sd = JSON.parseObject(serviceValue, ServiceDefinition.class);

                Set<ServiceInstance> set = new HashSet<>();

                GetResponse instanceResp = null;
                try {
                    instanceResp = kvClient.get(ByteSequence.from((serviceDefinition + BasicConst.PATH_SEPARATOR).getBytes()),getOption).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                instanceResp.getKvs().forEach(kv -> {
                        System.out.println("add instance: " + kv.getValue().toString());
                        set.add(JSON.parseObject(kv.getValue().toString(), ServiceInstance.class));
                    });
                    serviceMap.put(sd, set);

            });
        }
        for (Map.Entry<ServiceDefinition, Set<ServiceInstance>> entry : serviceMap.entrySet()) {
            System.out.println("key: " + JSON.toJSONString(entry.getKey().getServiceId()) + "\t value: " + entry.getValue().size());
        }

//            for (String serviceDefinition : serviceDefinitions) {
//                ByteSequence prefixKey = ByteSequence.from(serviceDefinition.getBytes());
//                GetOption getOption = GetOption.newBuilder().isPrefix(true).build();
////            GetResponse getResponse = kvClient.get(prefixKey, getOption).get();
//                GetResponse getResponse = kvClient.get(prefixKey, getOption).get();
//
//                System.out.println("kvs 大小：\t" + getResponse.getKvs().size());
//                getResponse.getKvs().forEach(kv -> {
////                    System.out.print("key: " + kv.getKey().toString());
////                System.out.println("\t value: " + kv.getValue().toString());
//                    watchClient.watch(kv.getKey(), Watch.listener(watchResponse -> {
//                        watchResponse.getEvents().forEach(
//                                watchEvent -> {
//                                    //put 操作
//                                    if (watchEvent.getEventType().equals(WatchEvent.EventType.PUT)) {
//                                        KeyValue keyValue = watchEvent.getKeyValue();
//                                        System.out.print("key: \t" + keyValue.getKey().toString());
//                                        System.out.println("\tvalue:\t" + keyValue.getValue().toString());
//                                    }
//
//                                }
//                        );
//                    }));
//                });
//            }
//            // 打印查询结果
//            Thread.sleep(2000);

    }

    @Test
    public void getValue() throws ExecutionException, InterruptedException {
        Client client = Client.builder().endpoints("http://localhost:2379").build();
        KV kvClient = client.getKVClient();

        List<String> serviceDefinitions = Arrays.asList("/dev/api-gateway/service/api-gateway", "/dev/api-gateway/service/backend-http-server");

        GetOption getOption = GetOption.newBuilder().isPrefix(true).build();

        for (String serviceDefinition : serviceDefinitions) {
            ByteSequence sequence = ByteSequence.from((serviceDefinition+BasicConst.PATH_SEPARATOR).getBytes());
            kvClient.get(sequence,getOption).get().getKvs().forEach(
                    keyValue -> {
                        System.out.println("key:\t"+ keyValue.getKey());
                        System.out.println("value:\t"+ keyValue.getValue());
                    }
            );
        }
    }

}
