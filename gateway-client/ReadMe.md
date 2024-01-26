### 工程介绍

#### `gateway-client`

> 1. 定义 `ApiSericce` 和 `ApiInvoker` 注解，实现服务定义，服务实例的构造；
> 2. 向应用提供服务注册功能，注册中心根据应用的配置自动选择；目前支持`Nacos`和`Zookeeper`;

### 代码导读

#### `AbstractClientRegisterManager`

> 根据应用的配置选择注册中心，并进行初始化，`RegisterCenter`.init()；*RegisterCenter 是我们定义的一个注册中心接口，如果扩展注册中心则需要实现该接口；例如在本工程中使用了支持`Nacos`和`Zookeeper`*
>
> Zookeeper:
>
> ```java
> @Override
> public void init(String registerAddress, String env) {
> 
>     this.registerAddress = registerAddress;
>     this.env = env;
> 
>     //Curator工厂类创建客户端对象
>     curatorClient = CuratorFrameworkFactory.builder()
>         .connectString(registerAddress)
>         .retryPolicy(new ExponentialBackoffRetry(1000, 3))
>         .namespace(env)
>         .build();
>     //启动客户端
>     curatorClient.start();
>     ｝
> ```
>
> Nacos:
>
> ```java
> @Override
> public void init(String registerAddress, String env) {
>     this.registerAddress = registerAddress;
>     this.env = env;
>     try {
>         //nacos sdk
>         this.namingService = NacosFactory.createNamingService(registerAddress);
>         this.namingMaintainService = NamingMaintainFactory.createMaintainService(registerAddress);
>     } catch (NacosException e) {
>         e.printStackTrace();
>     }
> ```
>
> register() 则为应用进行服务注册；*传入服务定义 ServiceDefinition* 和服务实例 *ServiceInstance*；

#### `ApiService`

> 核心注解，应用如果需要接入服务中心，加上该注解即可；**ElementType.TYPE**;
>
> 定义服务:`serviceId`、`version`、`protocol`、`patternPath`;后续会根据不同的 protocol（*目前支持 dubbo 和 http*） 向注册中心注册；

#### `ApiInvoker`

> 扫描接口方法的核心注解;**ElementType.METHOD**;
>
> 定义方法：`path`，后续会根据存放在`ServiceDefinition`中，path做一些功能增加，例如负载均衡，路由，限流等；

#### `ApiAnnotationScanner`

> ApiService 和 ApiInvoker 的功能实现类；主要扫描加了这两个注解的类、接口和方法；获得注解其中的参数，构建 `ServiceDefinition`并返回;
>
> 1. 如果`ApiInvoker.protocol`是 Http，则构建 `HttpServiceInvoker,`
> 2. 如果`ApiInvoker.protocol`是 Dubbo，则构建 `DubboServiceInvoker`, `DubboServiceInvoker`构建`ServiceDefinition`比 http 复杂一点，http 请求 访问 dubbo 用到了 **泛化调用**，保存了类全路径名，方法名，参数类型等；

#### `ApiClientAutoConfiguration`

> 根据应用的 IOC 容器是否含有 Http/Dubbo 相关的 bean 来创建对应的 `bean`；并且使用条件注解不存在才创建；
>
> 以 HTTP 为例，创建 bean：`SpringMvcClientRegisterManager`, 实现了 ApplicationListener<ApplicationEvent>, ApplicationContextAware 接口，以方便在服应用启动时扫描自己的 mapping 映射接口并构建 `ServiceDefinition` 与 `ServiceInstance` 后进行服务注册；而服务注册具体是由父类 `AbstractClientRegisterManager` 选择；