# 校园失物招领（非官方）· Java 后端

Spring Boot 3 + 内存存储的 walking skeleton 实现，与 `backend-python` 功能等价。

## 运行测试

```bash
# 需先指向 JDK 21（否则 mvn 报 JAVA_HOME environment variable is not defined correctly）
export JAVA_HOME="/d/develop/Java/jdk-21"   # Git Bash 示例，按本机实际路径调整
mvn test
```

> 注意：全局 `JAVA_HOME` 若被设为未展开的字面量（如 `%JAVA11_HOME%`）会导致 `mvn` 启动即失败，运行前务必显式导出 JDK 21 路径。

## 启动服务

```bash
mvn spring-boot:run     # 默认监听 8080
```

## 跨域来源

通过环境变量 `CORS_ORIGINS` 配置运营后台来源白名单（逗号分隔），默认允许 `http://localhost:5173,http://127.0.0.1:5173`。生产环境请指向真实后台域名，禁用通配来源。
