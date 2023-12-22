### 代码导读
### Bootstrap 启动类
> - 饿汉式单例`ConfigLoader`初始化核心配置 `config`，配置支持动态配置，可以从**配置文件**，**环境变量**，**jvm参数**，**运行参数**初始化；
> - `ConfigCenter`为注册中心接口，加载注册中心（目前是使用了 Nacos 作为注册中心）使用到了 **java SPI** 机制，从 META-INF/services 加载其接口实现类`NacosConfigCenter` 类，随后`init()`和`subscribeRulesChagne()`使用了 nacos 的 SDK 来创建配置和订阅配置；当配置变更时会监听到；
> - `Container` 容器，定义了 netty 相关的服务；
> - `RegisterConfig` 注册中心接口，依然使用了 SPI 机制加载其实现类 `NacosRegisterConfig`；`ServiceDefinition` `ServiceInstance` 封装了服务定义和服务实例的元数据。

#### Container

> container 的构造器中调用了初始化方法 `init()`；该方法中创建了 NettyHttpServer 和 NettyHttpClient ;

#### Netty

消息流转几个重要的流程：

1. 入站处理器 `NettyServerHttpInboundHandler` 中 channelRead 将对消息进行封装，交由 `NettyProcessor`.process 处理;

2. `RequestHelper`.doContext 主要是构建网关核心上下文 gatewayContext，随后在`服务中心`根据消息的 ==uniqueId== 获取对应的服务实例；

3. `filterFactory`.buildFilterChain 构建过滤器链（这里根据配置中心的配置构建了过滤器链，比如负载均衡，路由过滤器等以及用户自定义的过滤器链，只要加了`FilterAspect`注解的都可以，利用了Java SPI 机制扫描了所有的Filter）； ==executeFilter==中遍历各个 `Filter`，执行 filter 的逻辑；以轮询过滤器举例，从 gatewayContext 中获取服务名，将 gatewayContext 中的请求中的 ip 和 port 替换为服务实例具体的 ip 和 port ；
4. 最后的过滤器会执行到`RouterFilter`;

