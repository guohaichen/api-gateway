### 代码导读
### Bootstrap 启动类
> - 饿汉式单例`ConfigLoader`初始化核心配置 `config`，配置支持动态配置，可以从**配置文件**，**环境变量**，**jvm参数**，**运行参数**初始化；
> - `ConfigCenter`为==配置中心==接口，根据配置文件加载配置中心；注册中心使用到了 **java SPI** 机制，从 META-INF/services 加载其接口实现类，目前实现有`Naocs`和`Zookeeper` ；随后`init()`和`subscribeRulesChagne()`使用了各配置中心的SDK或第三方客户端来创建配置和订阅配置；当配置变更时会监听触发回调更新并写回配置；
> - `Container` 容器，定义了 netty 相关的服务；
> - `RegisterCenter`注册中心接口，和配置中心类似，提供了`init()`初始化操作,`register()`注册服务,`deregister()`注销服务，`subscribeAllServicesChang()`订阅所有服务变更；定义规范，后续添加注册中心只需实现该接口即可；
> - `ServiceDefinition` `ServiceInstance` 封装了服务定义和服务实例的元数据。
> - Runtime.getRuntime().addShutdownHook()当jvm退出时，创建一个线程去删除注册中心的实例；

#### Container

> container 的构造器中调用了初始化方法 `init()`；该方法中创建了 NettyHttpServer 和 NettyHttpClient ;

#### registerAndSubscribe()

> 主要功能：
>
> 1. 根据配置选择对应的注册中心并初始化；
> 2. 根据配置构建服务定义`ServiceDefinition`和服务实例`ServiceInstance`，并进行服务注册；
> 3. `subsrcibeAllServiceChange` 订阅所有服务变更；当服务有变更时，例如上线，下线；会同步到`DynamicConfigManager`中；
> 4. `DynamicConfigManager`中定义了两个 ConcurrentHashMap 分别用来存访服务定义和服务实例的关系；

### Netty相关

消息流转几个重要的流程：

1. <font color=red>入站处理器 `NettyServerHttpInboundHandler` 中 channelRead 将对消息进行封装为HttpRequestWrapper，交由 `NettyProcessor`.process 处理;</font>

2. `RequestHelper`.doContext 主要是构建网关核心上下文 gatewayContext，随后在`服务注册中心`根据消息的 ==uniqueId== 获取对应的服务实例；

3. `filterFactory`.buildFilterChain 构建过滤器链（这里根据配置中心的配置构建了过滤器链，比如负载均衡，路由过滤器等以及用户自定义的过滤器链，只要加了`FilterAspect`注解的都可以，利用了Java SPI 机制扫描了所有的Filter）； ==executeFilter==中遍历各个 `Filter`，执行 filter 的逻辑；以轮询过滤器举例，从 gatewayContext 中获取服务名，将 gatewayContext 中的请求中的 ip 和 port 替换为服务实例具体的 ip 和 port ；
4. 最后的过滤器会执行到`RouterFilter`;

### 核心类

#### DynamicConfigManager

> 

##### 属性介绍：

```java
//服务的定义集合：uniqueId代表服务的唯一标识
private final ConcurrentHashMap<String /* uniqueId */ , ServiceDefinition> serviceDefinitionMap = new ConcurrentHashMap<>();

//服务的实例集合：uniqueId与一堆服务实例对应
private final ConcurrentHashMap<String /* uniqueId */ , Set<ServiceInstance>> serviceInstanceMap = new ConcurrentHashMap<>();

//规则集合
private ConcurrentHashMap<String /* ruleId */ , Rule> ruleMap = new ConcurrentHashMap<>();

//路径以及规则集合
private ConcurrentHashMap<String/*路径 */, Rule> pathRuleMap = new ConcurrentHashMap<>();
private ConcurrentHashMap<String/*服务名*/, List<Rule>> serviceRuleMap = new ConcurrentHashMap<>();
```

