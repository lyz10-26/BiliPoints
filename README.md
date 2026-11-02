# BiliPoints

面向 B 站大会员的 Android 积分管理与任务辅助工具。

## 功能

- 同步账号头像、等级、会员状态、有效期、硬币与大积分。
- 查看月度积分进度，辅助执行签到、浏览等任务并核验积分记录。
- 展示官方装扮商城商品、积分价格、库存及兑换状态。
- 支持手动执行任务和按北京时间设置每日自动任务。
- 兑换后查询官方订单状态，避免对待确认订单重复操作。

## 安装

当前保留版本：**1.1.6**

[下载 BiliPoints 1.1.6 APK](https://github.com/lyz10-26/BiliPoints/raw/refs/heads/main/BiliPoints-1.1.6-release.apk)

Android 8.0（API 26）及以上版本可安装。覆盖安装需使用相同签名的安装包。

## 构建

需要 JDK 17、Android SDK 35 和 Gradle 8.9。

```sh
gradle :app:assembleDebug
```

也可通过 GitHub Actions 构建 Debug APK。Debug 包与 Release 包签名不同，通常不能直接覆盖安装 Release 版本。

## 免责声明

本项目为第三方工具，非哔哩哔哩官方应用。任务执行、积分到账及商品兑换结果以官方服务端实际返回为准。请勿将账号凭据或签名私钥提交到仓库。
