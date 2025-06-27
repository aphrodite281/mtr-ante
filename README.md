# MTR-ANTE

Aphrodite's Nemo's Transit Expansion (MTR-ANTE) 是一个基于Minecraft Transit Railway Mod的实验性功能扩展，目前仅支持MTR 3.1.2 ~ 3.2.2 版本，不支持4.0.0或其他版本。

## 支持

[文档](https://aphrodite281.github.io/mtr-ante/#/)、QQ群(836291719)

## 下载
[Releases](https://github.com/aphrodite281/mtr-ante/releases) 、 [Modrinth](https://modrinth.com/mod/mtr-ante/)

## 构建

在执行`build`任务前须先执行`setupLibrary`，可以用类似-PbuildVersion="1.18.2"参数指定版本(1.17.1 1.18.2 1.19.2 1.19.3 1.19.4 1.20.1)。
以1.18.2为例，  
第一次构建前执行：  

- `./gradlew setupLibrary -PbuildVersion="1.18.2"`   

每次构建时执行：  

- `./gradlew build -PbuildVersion="1.18.2"`  