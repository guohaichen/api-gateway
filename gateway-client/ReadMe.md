> 本模块主要是向下游服务提供服务，将下游服务同时注册到注册中心，让网关拉取服务，才能转发请求；

在 `AbstractClientRegisterManager`类中的 `RegisterCenter`使用了  `ServiceLoader ` 机制，动态加载注册中心，插件开发。

核心包
#### ApiInvoker 