# 项目帮助

铁路购票系统的完整说明见 [README.md](./README.md)。

常用命令：

```powershell
# 本地启动后端（dev）
.\mvnw.cmd spring-boot:run

# 运行后端测试（H2 内存库，无需 MySQL）
.\mvnw.cmd -B verify

# 前端
cd frontend
npm ci
npm run dev
```

```bash
# macOS / Linux
./mvnw spring-boot:run
./mvnw -B verify
```

容器部署：`Copy-Item .env.example .env` 后执行 `docker compose up -d --build`。

`pom.xml` 中空的 `<licenses>` / `<developers>` / `<scm>` 覆盖用于避免继承 Spring Boot parent 的元数据，待项目确定授权协议后再补全。
