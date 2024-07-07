import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

/**
 * @author cgh
 * @create 2024/7/7
 */
public class EtcdTest {

    @Test
    public void selectAll() throws ExecutionException, InterruptedException {
        Client client = Client.builder().endpoints("http://localhost:2379").build();
        KV kvClient = client.getKVClient();
        ByteSequence prefixKey = ByteSequence.from(("/api-gateway/service/").getBytes());
        GetOption getOption = GetOption.newBuilder().isPrefix(true).build();
        GetResponse getResponse = kvClient.get(prefixKey, getOption).get();
        // 打印查询结果
        getResponse.getKvs().forEach(kv -> {
            System.out.print("key: " + kv.getKey().toString());
            System.out.println("\tvalue: " + kv.getValue().toString());
        });

        client.close();
    }

}
