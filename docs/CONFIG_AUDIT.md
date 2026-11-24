# 无患影音配置体系审计（第一版）

> 审计分支：`wuhuan/development`  
> 审计基线：2026-11-24 工作分支 HEAD（本报告创建前已核验）  
> 范围：点播总配置、站点模型、直播配置、解析器、Spider 加载、配置解码、多仓与持久化  
> 状态：源码静态审计第一版；不是完整兼容性验收，也不代表所有字段均已真机验证。

## 1. 结论摘要

当前底座继承了 FongMi / CatVod 配置体系。源码已经具备读取 TVBox/FongMi 风格点播 JSON、直播配置、多仓入口、解析器、全局网络规则，以及 Java/JAR、JavaScript、Python Spider 的加载基础。

但“源码存在”不等于“手机端所有配置组合均已验证”。本项目仍需对配置字段的实际消费者、运行时依赖、构建变体和真机结果逐项确认。

当前最重要的工程判断：

1. 不另造一套影视源协议；优先保持上游配置模型和加载链。
2. 首次安装内置测试源应走现有 Config/VodConfig 初始化路径，不绕过数据库和事件机制。
3. 用户已保存的配置、首页站点、解析器选择及站点可变设置不得被测试源覆盖。
4. 外部 JAR/JS/Python 是可执行代码，不应与普通 JSON 数据配置同等信任。
5. 资源配置探活必须区分“JSON 可下载”“站点可列分类”“搜索/详情成功”“实际播放成功”。

## 2. 顶层 Vod 配置

源码位置：
- `app/src/main/java/com/fongmi/android/tv/api/config/VodConfig.java`
- `app/src/main/java/com/fongmi/android/tv/api/config/BaseConfig.java`

`VodConfig.parseConfig()` 当前处理以下字段：

| 字段 | 源码处理 | 后续核验重点 |
|---|---|---|
| `spider` | 交给 BaseLoader 预加载全局 JAR | URL、相对路径、MD5 缓存、下载失败处理 |
| `sites` | 转为 Site 列表，站点 jar 为空时回退全局 spider | type/API 组合、站点级 jar、字段默认值 |
| `parses` | 转为 Parse 列表，并自动插入内置超级解析项 | 各解析 type、JAR 扩展类依赖 |
| `lives` | 同步生成独立 Live Config 并交给 LiveConfig | 嵌套直播配置与独立直播配置差异 |
| `wallpaper` | 建立壁纸 Config | 图片/视频支持与手机端呈现 |
| `headers` | 解析后加入 OkHttp response interceptor | 具体匹配规则和作用方向 |
| `proxy` | 解析后加入 OkHttp selector | host 匹配、优先级、代理不可用行为 |
| `rules` | 转为网络拦截规则，触发 RuleConfig 缓存失效 | regex/script/exclude 实际调用点及误拦截 |
| `doh` | 解析 DoH 列表 | 系统默认 DoH 与源配置合并优先级 |
| `flags` | 保存平台标识列表 | 哪些播放器/解析路径读取 |
| `hosts` | 加入 OkHttp DNS 覆盖 | 格式校验、冲突和清理时机 |
| `ads` | 与 LiveConfig 广告域名合并 | 具体拦截器调用链、白名单及 fail-open |
| `logo` | 保存 Config logo | UI 消费位置 |
| `assrt` | 保存字幕搜索服务地址 | 是否可为空及网络请求边界 |
| `notice` | 保存加载公告 | 展示时机和内容安全 |
| `danmaku` | 保存弹幕 API 地址 | 弹幕服务的调用位置 |

注意：`BaseConfig.fetchArray()` 不仅能读取内嵌 JSON 数组，也能把字符串 URL 当作远程数组读取，并展开数组中以字符串形式引用的其他数组。这意味着配置可以间接引用外部规则/代理/请求头列表；导入时不能只检查顶层 JSON。

## 3. Site 站点字段

源码位置：`app/src/main/java/com/fongmi/android/tv/bean/Site.java`

已确认模型字段：

- 身份与请求：`key`、`name`、`api`、`type`
- 扩展与运行时：`ext`、`jar`
- 播放行为：`click`、`playUrl`
- 展示控制：`hide`、`indexs`、`categories`、`style`
- 功能开关：`searchable`、`changeable`、`quickSearch`、`danmaku`
- 网络参数：`timeout`、`header`

源码默认值（缺省时）：
- `type=0`、`hide=0`、`indexs=0`
- `searchable=1`、`changeable=1`、`quickSearch=1`、`danmaku=1`
- `timeout` 缺省时使用播放器常量；显式值按秒转毫秒，最小按 1 秒处理。
- `categories` 缺省为空列表；`header` 缺省为空 Map。

`Site.objectFrom()` 会反序列化后为缺省 `jar` 补全全局 spider，并对 `api`、`ext` 执行 URL 转换。随后 `sync()` 会将本地数据库中已保存的 searchable/changeable 设置同步到新配置，避免远端配置直接覆盖用户对这两个属性的本地调整。

关键风险：`Site.objectFrom()` 捕获异常后返回空 Site；坏站点可能被静默转成空对象。需要在资源导入诊断中统计被跳过/解析失败的条目，而不是只展示“导入成功”。

`SiteApi` 进一步确认：`type=3` 走 Spider；`type=4` 走带 Base64 ext 参数的 HTTP API；其他类型走直接 HTTP 请求。分类首页会按 `Site.categories` 白名单过滤分类；但 `searchContent()` 当前没有应用该白名单，因此不能把 categories 误认为搜索结果的内容过滤器。

## 4. Spider 运行时分派

源码位置：
- `api/loader/BaseLoader.java`
- `api/loader/JarLoader.java`
- `api/loader/JsLoader.java`
- `api/loader/PyLoader.java`

当前代码依据 `api` 字符串形态分派：
- 含 `.py`：Python / Chaquopy。
- 含 `.js`：JavaScript / QuickJS。
- 以 `csp_` 开头：Java Spider / JAR / DexClassLoader。
- 其他形式：返回 `SpiderNull`，由普通 HTTP API 站点链路处理。

JAR 可由全局 `spider` 或站点级 `jar` 提供；站点级值在 Site 反序列化时优先，全局值用于补缺。JAR 支持 URL、file、本地 assets 路径，并支持 `;md5;` 校验形式。加载器会下载并通过 DexClassLoader 执行外部代码。

JS 加载器会把站点 api 和对应 dex loader 交给 QuickJS Loader；Python 加载器通过 Chaquopy Loader 建立 Spider。构建文件仍包含 `:catvod`、`:chaquo`、`:quickjs` 模块依赖。

安全与兼容性要求：
- 外部 JAR/JS/Python 必须视为代码执行，不是普通配置。
- 需要记录来源 URL、校验信息、缓存位置、加载错误和运行时版本。
- 不可仅凭 `type=3` 就推断实际运行时；本分支 BaseLoader 的具体分派条件是 `api` 字符串。
- 必须用真实样本分别验证三种运行时；当前仅确认源码路径存在。

## 5. 解析器

源码位置：`bean/Parse.java`、`VodConfig.initParse()`。

字段包括 `name`、`type`、`url`、`ext.flag`、`ext.header`。配置中的解析器按名称与用户保存的当前解析器选择关联；若没有匹配项则回退到列表第一项。非空解析列表会自动在首位插入内置“超级解析”对象。

解析器 type 的详细语义需与当前分支解析执行代码逐项对照后再写入兼容承诺。不能只按其他分支文档推断本分支行为。

## 6. Live 直播配置

源码位置：`api/config/LiveConfig.java`。

Vod 内嵌的 `lives` 会创建/更新独立 Live Config；LiveConfig 支持 JSON 对象，也支持非 JSON 文本列表并交给 LiveParser 解析。独立 Live 顶层目前处理：
- `spider`、`lives`
- `headers`、`proxy`、`rules`、`hosts`、`ads`

DoH 不在 LiveConfig.initList() 中处理；不要把 Vod 顶层字段清单机械套用到独立 Live 配置。

Live 来源模型及频道字段还需继续审计，包括 TXT/M3U/JSON、分组、EPG、请求头、频道线路、回看和播放协议。

## 7. 多仓与配置持久化

源码位置：`bean/Config.java`、`bean/Depot.java`、`bean/BuiltinDepot.java`、`BaseConfig.parseDepot()`。

- Config 使用 Room 数据库持久化，唯一索引为 `url + type`。
- `Config.vod()` 从数据库读取点播配置；数据库无记录时会用 `BuiltinDepot.URL/NAME` 创建默认配置。
- `VodConfig.checkJson()` 发现顶层 `urls` 时进入 depot 分支，解析 Depot 列表并加载其中第一条配置。
- 审计起点时 `BuiltinDepot.URL/NAME` 为空，且代码尝试读取的 `assets/clys/chenlong.jpg` 不存在于当前分支文件树；这条旧多仓初始化路径因此没有可用的内置数据。
- 本轮已将首次安装入口改为 `assets://wuhuan/test-source.json`，并新增一个公开 CMS 测试源配置；旧 `getConfigs()` 多仓逻辑尚未重构，仍需单独处理。
- `Config.update()` 会更新时间并保存当前 URL 到偏好设置；Config 还保存用户选择的 home site 和 parse 名称。

重要待核验点：
1. `parseDepot()` 当前实际加载列表第一条并删除 depot 自身 Config；需要核查 UI 的多仓选择/切换机制如何使用 `BuiltinDepot.getConfigs()`，不可把它描述成自动合并全部仓库。
2. BuiltinDepot 对 assets 文件读取异常采用静默捕获；需要确认文件真实内容及首次安装行为。
3. 首次内置测试源必须避免覆盖用户已有 Config，且仅在首次初始化时安装；用户删除后不应每次启动又自动恢复。
4. 更新配置时必须保留用户本地的 home、parse、searchable、changeable 等自定义状态。
5. 当前首次安装测试源使用应用内 assets JSON 作为配置文件，再由该文件引用远程 CMS API；配置文件本身不捆绑影视内容或 Spider 代码。

## 8. 配置解码与 URL 转换

源码位置：`api/Decoder.java`、`utils/UrlUtil.java`。

Decoder.getJson() 通过 OkHttp 下载配置后执行 verify/fix：
- 普通 JSON 对象直接进入路径修正。
- 含 `**` 的包装文本会尝试提取 Base64 内容。
- 以 `2423` 开头的数据会走十六进制解码及 AES-CBC 解密路径，密钥与 IV 片段从解码数据中提取。
- 会修正配置中相对 `./`、`../` 资源路径，以及特殊占位标记。

这证明当前底座包含特定配置包装/加密格式的读取逻辑；不代表支持所有社区私有加密格式。需要为每种格式建立独立样例和测试，未验证的算法不得宣称兼容。

## 9. 参考开发文档

以下资料用于理解生态约定；实际行为以本仓库当前源码为准：

- FongMi-TV 配置说明：<https://github.com/HerbertHe/FongMi-TV/blob/fongmi/docs/CONFIG.md>
- FongMi Spider 接入指南：<https://fongmi.github.io/TV/spider/>
- CatVodTVSpider 示例工程：<https://github.com/drizzle888/CatVodTVSpider>
- TVBoxorg 聚合配置项目：<https://github.com/TVboxorg/TVbox>

FongMi 配置文档列出了 Site type 0/1/3/4、解析器类型、Live 字段以及共享网络配置对象。本文只把它作为生态规范参考，不把其他分支文档直接当成本分支的运行事实。

## 10. 首次安装测试源（本轮新增，待真机验证）

配置文件：`app/src/main/assets/wuhuan/test-source.json`  
入口常量：`BuiltinDepot.URL = assets://wuhuan/test-source.json`  
默认名称：`无患影音测试源`  
CMS API：`https://suoniapi.com/api.php/provide/vod/`

验证记录：2026-11-24 直接访问 `?ac=list` 返回 JSON，`code=1`，包含分页列表和分类数组；因此确认 API 当前能响应标准 CMS 列表请求。尚未通过本应用验证分类、搜索、详情、播放链路。

测试配置通过 `categories` 白名单只展示常见电影、电视剧、综艺、动漫、纪录片、演唱会、体育、科普和儿童内容类别。该第三方 API 的原始分类表还包含成人向类别；由于当前搜索调用没有按 categories 白名单过滤，测试阶段应避免使用成人类别关键词。该接口只是临时测试依赖，不代表内容授权、服务稳定性或正式发布推荐；若无法确认适用性，正式发布前应替换或移除。

`Config.vod()` 现已处理两种情况：新安装时创建该内置配置；早期版本已留下空 URL 点播 Config 时，原位补齐测试配置 URL。已有非空用户配置保持不变。用户主动删除后，当前逻辑不会在每次启动时重新创建默认源。

## 11. 下一步审计与验证任务

- [ ] 核查 assets/clys/chenlong.jpg 的实际内容、调用入口和多仓 UI。
- [ ] 逐项核查 Parse type 0–4 在本分支的执行实现。
- [ ] 逐项核查 Live、Group、Channel、EPG、catchup 和播放 URL 字段。
- [ ] 核查 headers/proxy/rules/doh/hosts/ads 的真实调用位置和作用范围。
- [ ] 核查 UrlUtil 支持的资源协议、相对路径、assets/file/data URI 转换。
- [ ] 核查站点请求头与全局请求头的合并/覆盖关系。
- [ ] 建立普通 CMS、XML、Java/JAR、JS、Python、Depot、多仓配置的最小可复现样例。
- [ ] 通过云端构建及 OPPO Android 14 真机验证配置导入、分类、搜索、详情、播放。
- [ ] 对内置测试源做内容合规、来源稳定性、隐私和可访问性检查；不捆绑 Spider 可执行代码。
