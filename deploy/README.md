# 生产部署骨架

本目录只提供上线前可验证的容器化骨架，不包含真实证书、域名或密钥，也不会自动发布到生产环境。

## 准备

1. 在服务器复制 `production.env.example` 为 `.env`。
2. 替换全部 `CHANGE_ME`，生产密钥不得复用开发环境值。
3. 将 `CORS_ALLOWED_ORIGINS` 改为后台管理端正式 HTTPS 域名。
4. 在学校 HTTPS 反向代理或负载均衡器上配置证书，并转发至 `127.0.0.1:8088`。
5. 微信公众平台的 request 合法域名应指向同一 HTTPS API 域名，不允许 IP、HTTP 或自签名证书。

生成随机值示例：

```powershell
$tokenBytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($tokenBytes)
[Convert]::ToBase64String($tokenBytes)

$aesBytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($aesBytes)
[Convert]::ToBase64String($aesBytes)
```

## 构建与启动

```powershell
cd deploy
python verify_image_pins.py --verify-registry
docker compose --env-file .env -f docker-compose.prod.yml config
docker compose --env-file .env -f docker-compose.prod.yml build
docker compose --env-file .env -f docker-compose.prod.yml up -d
```

`image-lock.json` 是生产第三方镜像的审核清单。生产 Compose 必须使用清单中的不可变 `@sha256` 引用；`verify_image_pins.py` 会失败关闭地校验 Compose 与清单完全一致，并确认锁定 OCI 索引仍绑定已审核平台的 SLSA provenance attestation。升级 MySQL 或 Redis 时必须重新核对官方镜像版本、源代码修订和摘要，更新清单后由另一位维护者复核，禁止只修改 Compose 标签。

当前 Docker Official Image 的 OCI provenance 可验证其构建来源记录，但本项目尚未配置独立的发布者签名信任根。若学校上线规范要求发布者身份的密码学验证，应改用提供可验证签名的镜像来源，并在发布流水线或集群准入策略中强制验签。

验证：

```powershell
Invoke-RestMethod http://127.0.0.1:8088/actuator/health
```

## 数据库

Flyway 会在后端启动时执行迁移。首次生产部署前必须先在同版本 MySQL 的脱敏副本上完成迁移演练，并验证回滚方案。生产备份必须加密并存放在仓库之外。

管理员初始化在生产 Profile 中固定关闭。首个管理员应通过一次性受控流程创建，完成后立即撤销初始化凭据，不得把初始密码写入 Compose、镜像或 Git。
