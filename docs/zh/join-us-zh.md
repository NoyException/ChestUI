# 如何快速参与本项目的开发

## 0. 阅读Minecraft EULA

在参与开发之前，请确保你已经阅读并同意了[Minecraft的最终用户许可协议（EULA）](https://aka.ms/MinecraftEULA)。

## 1. 克隆项目到本地

```bash
git clone git@github.com:NoyException/ChestUI.git
```

## 2. 安装依赖

你需要安装：

- JDK 21
- Gradle

## 3. 构建、运行项目

构建项目并将jar复制到服务器：

```bash
./gradlew buildAndCopy
# 或者使用缩写
./gradlew bac
```

启动服务器：
```bash
./gradlew runServer
```

当你使用bac重新构建jar后，需要在服务器控制台输入下列指令来加载更新：
```minecraft
/reload confirm
```

如果你想让离线用户加入服务器进行测试，需要修改`server/server.properties`文件中的`online-mode`为`false`。

服务器默认地址：`localhost:25565`

## 4. Debug

使用远程JVM调试即可，端口为`5005`。

如果你使用的是IntelliJ IDEA，点击右上角的`编辑配置`，再点击左上角的加号，选择`远程 JVM 调试`即可。