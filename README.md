# Comso1 Android V0.4.6

Comso1 是一个 Android 原生 Kotlin + Jetpack Compose 客户端，用手机聊天界面通过 SSH 连接 Ubuntu VPS，并在 VPS 上以 `codex` 用户运行 Codex CLI。

客户端不会开放新的公网服务，也不会直接暴露 Codex RPC/MCP 端口。所有交互都走 SSH。

## 当前功能

- 中文界面，默认聊天模式。
- 深色 Gemini 风格聊天界面，以蓝色作为统一强调色；状态栏与导航栏安全区已适配，系统栏图标固定为适合深色背景的浅色。
- Comso1 使用黑底蓝白同心光环自适应启动图标；Android 12 及以上的系统启动页和应用窗口背景统一为纯黑色。
- 覆盖式会话抽屉可完全收起，收起后只保留左上角菜单按钮。
- 抽屉底部提供“设置”和“终端/聊天”切换。
- 底部输入框单独响应软键盘，顶栏与整页不会随键盘重复位移。
- 聊天顶栏使用由黑色到透明的过渡遮罩，只保留侧边栏、新对话和当前会话操作；消息滚动时可从顶栏下方经过。输入区仅圆角输入框本身遮挡消息。
- 欢迎词保持静态，只随软键盘出现调整位置，不会因图片或位置附件改变垂直位置。输入框前的状态点以暗色、蓝色和红色分别表示未连接/连接中、连接成功和连接失败。
- 模型与推理档位入口位于消息输入栏。App 进入主界面、新对话或历史对话时会检查上次读取时间；超过 20 分钟才通过 SSH 调用 Codex `app-server` 的 `model/list`，读取 VPS 实际可用模型及档位。20 分钟内复用上次的模型目录和蓝色/红色状态；配置面板右上角的刷新按钮可随时强制覆盖缓存。
- SSH 配置：Host、Port、Username、私钥、远端工作目录。
- 使用 Android Keystore 加密保存 SSH 私钥。
- 聊天模式执行：
  - 新会话：`codex exec --json --skip-git-repo-check [模型/档位参数] "<message>"`
  - 续聊：`codex exec resume --json --skip-git-repo-check [模型/档位参数] <thread_id> "<message>"`
- 流式读取 stdout JSONL，显示 Codex 回复、工具调用、命令事件、错误和完成状态。
- 按 Codex 的 `commentary` / `final_answer` 阶段区分工作过程和最终回复；运行时展开过程，完成后自动折叠。命令、工具和未中断回合的运行期错误都收进过程卡，只有真正导致回合失败的错误常驻聊天区。
- 不再把未知 JSONL 直接放进聊天记录；旧版保存的“原始 JSONL 事件”也会在读取时自动隐藏。
- Markdown 支持标题、段落、粗体、斜体、删除线、链接、列表、引用、代码块、分隔线和表格。
- 支持从系统照片选择器添加最多 4 张图片，经 SFTP 上传后通过 `codex exec --image` 发送。
- 支持用户主动附加一次手机当前位置，并把坐标、精度和采集时间随本次提示提供给 VPS 上的 Codex。定位会依次尝试网络定位、GPS 和系统最近位置；使用缓存坐标时会明确标注“最近”。
- 首轮完成后可调用兼容 OpenAI Chat Completions 的模型 API 自动生成标题；支持侧边栏手动改名。
- 本地保存会话列表、thread id、标题、工作目录和消息历史。
- 会话行不显示常驻编辑按钮；长按会话可查看会话 ID、工作目录和更新时间，也可置顶、取消置顶、重命名、归档或移出归档。
- 选择历史会话后，会加载本地消息；下一次发送时使用保存的 thread id 和工作目录 resume。
- 任务运行期间锁定新建、历史选择、设置和模式切换，防止旧任务事件写入错误会话。
- Stop/Cancel 会断开当前 SSH exec channel；远端 shell 使用 `exec` 启动 Codex，使断开信号直接到达 Codex 进程。
- 新会话会显式请求 Codex 的 workspace-write 权限配置（自动审批复核、网络访问、当前工作目录与 `/srv/agent` 可写），不再沿用远端默认的 read-only 启动配置；恢复历史会话时沿用该 thread 已保存的配置。
- 终端模式使用真正的 SSH PTY 和持续交互式 shell，支持 ANSI/xterm-256color、光标、滚动缓冲、终端全屏程序、软键盘输入、Esc/Tab/Ctrl+C/Ctrl+D/方向键、双指及按钮字体缩放、尺寸锁定和重新连接。终端区域会随软键盘收缩，键盘弹出时 PTY 行数和 xterm 网格同步更新；旋转屏幕只调整 PTY 行列，不重建 SSH 会话。
- 终端尺寸以 xterm 的实际网格为准：连接建立前记录的网格会在 SSH 通道就绪后补齐，重连使用最新网格，并按固定间隔校正一次，避免远端 PTY 与本地列数长期不一致。
- 终端渲染使用离线打包的 xterm.js；不会访问 CDN，也不会新增公网服务。

## Android 端配置

打开 App 后，点击侧边栏底部“设置”，填写：

| 字段 | 示例 | 说明 |
| --- | --- | --- |
| Host / 主机 | `1.2.3.4` 或 `vps.example.com` | VPS 的 SSH 地址 |
| Port / 端口 | `22` | SSH 端口 |
| Username / 用户名 | `codex` | 推荐固定使用 VPS 上的 `codex` 用户 |
| 工作目录 | `/home/codex/project` | Codex CLI 执行时进入的目录 |
| Codex 命令 | `codex` | 默认自动从 PATH 查找；也可以填绝对路径 |
| 私钥 | `-----BEGIN OPENSSH PRIVATE KEY-----...` | 与 VPS `codex` 用户公钥匹配的私钥 |
| 私钥口令 | 留空或填写 passphrase | 如果生成私钥时设置过 passphrase，这里必须填写 |

设置页下半部分是可选的标题生成配置：

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| 自动生成会话标题 | 关闭 | 开启后，首轮消息和最终回复会发送给标题模型 |
| 标题 API Base URL | `https://api.deepseek.com` | OpenAI Chat Completions 兼容服务地址 |
| 标题模型 | `deepseek-flash` | DeepSeek 当前 Flash 模型 ID，也可以换成兼容模型 |
| 标题 API Key | 空 | 使用 Android Keystore 加密保存，不写入日志 |

为了避免未告知的数据外发，标题生成默认关闭。填写 API Key、打开开关并点击“保存”后才会生效；未保存时使用首条用户消息作为本地回退标题。设置页会明确显示“未启用 / 缺少 API Key / 已配置”，也可以先点击“测试标题 API”验证地址、模型与 Key。标题服务失败时会保留本地回退标题，不覆盖已有标题。

标题是短文本任务，调用 DeepSeek 时会显式使用非思考模式，避免推理 token 挤占标题正文。

保存后返回聊天页，输入消息并点击“发送”。

## 模型与推理档位

点击消息输入栏内的“模型 档位”会打开底部配置面板。模型和思考强度分别使用下拉选择器，支持重置为 VPS 默认设置：

- App 连接 VPS 上的 `codex app-server --listen stdio://`，通过 SSH stdin 发送初始化和 `model/list` JSONL 请求。
- 模型名称、默认模型和 `supportedReasoningEfforts` 均按 VPS 返回值渲染，不再要求手机端版本表与服务端完全一致。
- 面板右上角的刷新按钮可重新读取；远端读取失败时保留内置目录和上一次选择，聊天仍可使用。
- 读取命令有 20 秒上限，不会因为模型服务暂时超时无限挂起。

- `VPS 默认模型`：不传 `--model`，使用 VPS 上 Codex 的默认模型。
- GPT-5.6 系列：`GPT-5.6 Sol`、`GPT-5.6 Terra`、`GPT-5.6 Luna`。
- GPT-6 系列：`GPT-6 Astra`、`GPT-6 Sol`、`GPT-6 Luna`。
- `默认`档位不会传 `model_reasoning_effort`，由所选模型和 VPS 配置决定。
- App 根据模型能力自动渲染档位。Luna 提供`低`、`中`、`高`、`超高`四个显式档位；其他当前模型还可选择`最高`。

例如选择 `GPT-5.6 Sol · 高` 后，新会话会附加：

```text
--model gpt-5.6-sol --config model_reasoning_effort=high
```

历史会话的 `resume` 也会使用当前选择。若远端提示模型不可用或不支持该档位，请刷新目录或切回 `VPS 默认模型 · 默认`。

Codex 偶尔可能在 stderr 输出 `failed to refresh available models: request timed out`。这表示在线模型目录刷新超时；如果本地缓存仍可用，任务会继续执行。当前版本会把它视为可恢复警告，不再显示红色错误卡；真正的命令失败仍会正常报告。

如果你使用的是：

```powershell
ssh-keygen -t ed25519 -f codex_remote_ed25519 -C "codex-remote-android"
```

App 的“私钥”字段应粘贴 `codex_remote_ed25519` 的完整内容，而不是 `.pub` 文件内容。`.pub` 文件内容应放到 VPS 的 `/home/codex/.ssh/authorized_keys`。

## VPS 端准备

以下命令仅作参考，请按你的 VPS 环境调整。

1. 创建 `codex` 用户：

```bash
sudo adduser codex
sudo usermod -aG sudo codex
```

2. 配置 SSH 公钥：

```bash
sudo mkdir -p /home/codex/.ssh
sudo nano /home/codex/.ssh/authorized_keys
sudo chown -R codex:codex /home/codex/.ssh
sudo chmod 700 /home/codex/.ssh
sudo chmod 600 /home/codex/.ssh/authorized_keys
```

把手机端私钥对应的公钥写入 `authorized_keys`。

3. 安装并登录 Codex CLI：

```bash
su - codex
codex --version
codex exec --json "hello"
codex app-server --help
```

确保这些命令能以 `codex` 用户运行。如果 CLI 需要登录或 token，请先在 VPS 上完成认证。自动模型发现需要当前 Codex CLI 提供 `app-server` 和 `model/list`；旧版 CLI 不支持时，App 会自动使用内置模型目录，不影响基本聊天。

Ubuntu 默认提供 `timeout` 命令。可以这样确认：

```bash
command -v timeout
```

4. 准备工作目录：

```bash
mkdir -p /home/codex/project
cd /home/codex/project
```

App 设置里的“工作目录”应填写这个路径。

## 使用方式

### 新建会话

点击侧边栏“新建”，在底部输入框发送消息。首次成功返回后，App 会记录 Codex 输出里的 `thread_id`。

### 恢复会话

在侧边栏选择历史会话。App 会恢复本地保存的消息历史，并在下一次发送时执行：

```bash
codex exec resume --json --skip-git-repo-check <thread_id> "<message>"
```

同时会使用该会话保存的工作目录。

### 停止运行

Codex 正在运行时，点击聊天页右上角“更多”，选择“停止当前任务”。这会取消当前任务并断开 SSH exec channel。

### 图片与位置

- 点击输入框左侧的回形针，从 Android 系统照片选择器添加图片。
- 图片先复制到 App 私有目录，再通过 SSH/SFTP 上传到 VPS 的 `~/.codex-remote/uploads/`。
- 远端命令通过一个或多个 `--image <path>` 参数把图片交给 Codex。
- 点击位置图标并授权后，只获取并附加当前一次位置；App 不会后台持续追踪。若室内暂时无法取得新鲜定位，会回退到系统最近一次位置，并保留其原始采集时间。
- 位置以结构化 `<phone_location>` 文本随提示发送，包括经纬度、精度和采集时间。

### 会话标题

启用并保存标题 API 后，第一次交互成功完成时，App 使用首条用户消息和最终回复生成标题。标题仅在 Codex 回合正常完成后触发，取消或失败的回合不会调用标题服务。侧边栏中长按会话后可选择重命名；手动标题会被锁定，后续不会被自动覆盖。

### 终端模式

点击侧边栏底部“终端”进入终端模式。App 会建立独立 SSH shell channel，申请 `xterm-256color` PTY，并启动远端用户的 login shell，然后自动进入当前会话的工作目录。它是持续的交互式终端，可以运行：

```bash
pwd
ls -la
git status
top
vim
tmux
```

终端底部的 `-` / `+` 调整字号；顶部锁形按钮用于锁定字号和 PTY 网格，避免旋转或手势造成尺寸变化；刷新按钮会断开并重新建立终端。底部还提供移动端常用的 Esc、Tab、Ctrl+C、Ctrl+D 和方向键。执行目录为当前会话保存的工作目录；如果没有选择会话，则使用设置里的默认工作目录。

终端内容按可用空间自动伸缩：软键盘弹出时终端收缩到键盘上方，PTY 行列随之更新；横屏空间不足时会隐藏顶部栏并临时隐藏系统状态栏，让提示符保持可见。旋转屏幕只调整网格，不会重建 SSH 会话。

终端会话与 Codex JSONL 会话是两条独立 SSH 连接。关闭终端页面会立即断开 PTY，不会在后台保留无人管理的 shell。

## 本地保存内容

App 使用 SharedPreferences 保存：

- VPS Host、Port、Username、工作目录。
- 当前模型与推理强度。
- 自动读取的模型目录、检查时间和最近一次成功/失败状态会持久化；默认模型与档位选择也会持久化。
- 加密后的 SSH 私钥。
- 加密后的标题 API Key，以及标题服务地址、模型和启用状态。
- 会话列表：thread id、标题、标题来源、更新时间、工作目录。
- 每个 thread 的本地消息历史、折叠后的工作过程、图片本地路径和显式共享的位置。

私钥会通过 Android Keystore 派生的 AES-GCM key 加密后落盘。不要把 debug 日志、截图或备份文件发给不可信对象。

## 安全模型

- 只通过 SSH 连接 VPS。
- 不把 Codex RPC/MCP 端口暴露到公网。
- 远端 Codex 运行身份推荐为 `codex` 用户，不建议 root。
- App 不记录 SSH 私钥、token、代理订阅等秘密到日志。
- 标题 API Key 与 SSH 私钥均使用 Android Keystore 派生的 AES-GCM key 加密。
- 标题生成默认关闭；开启后首轮对话内容会发送给用户配置的第三方 API。
- 定位只在用户点击位置按钮后单次获取；不在后台采集。
- 图片仅通过 SSH/SFTP 传输，不创建 HTTP 上传服务。远端副本位于 `codex` 用户家目录。
- 交互式终端只连接同一 SSH Host；xterm.js 资源随 APK 离线提供，WebView 禁止跳转到外部 URL。
- SSH 主机密钥采用 TOFU（首次使用信任）策略：第一次连接时在本机记录主机公钥，后续公钥变化时拒绝连接，降低中间人攻击风险。

## 构建 APK

在项目根目录执行：

```powershell
$env:ANDROID_USER_HOME="$PWD\.android"
gradle testDebugUnitTest assembleDebug
```

调试 APK 输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

本任务已额外复制一份到：

```text
outputs/comso1-v0.4.2-debug.apk
```

## 常见问题

### 发送按钮不可点击

请确认已经保存 Host 和私钥，并且输入框不为空。

### SSH 连接失败

检查 Host、Port、Username 是否正确；确认 VPS 防火墙允许 SSH；确认私钥和 `authorized_keys` 匹配。

如果 App 显示：

```text
Auth fail for methods 'publickey,password'
```

说明已经连到 SSH 服务，但服务器没有接受当前私钥。优先检查：

- App 里的 Username 是否是 `codex`。
- App 里的私钥是否是 `codex_remote_ed25519`，不是 `codex_remote_ed25519.pub`。
- VPS 的 `/home/codex/.ssh/authorized_keys` 是否包含对应 `.pub` 公钥。
- VPS 文件权限是否正确：

```bash
sudo chown -R codex:codex /home/codex/.ssh
sudo chmod 700 /home/codex/.ssh
sudo chmod 600 /home/codex/.ssh/authorized_keys
```

- 如果生成私钥时设置过 passphrase，App 的“私钥口令”必须填写同一个 passphrase。

当前版本已内置 BouncyCastle 并强制 JSch 使用 BC 的 Ed25519 实现，以支持 Android 上的 `ssh-ed25519` 私钥认证。

### Codex 没有回复

先在 VPS 上以 `codex` 用户手动执行：

```bash
cd /home/codex/project
codex exec --json "hello"
```

如果这里需要交互登录、报错或卡住，App 端也会失败。

如果 App 显示：

```text
sh: 1: codex: not found
Remote command exited with status 127
```

说明 SSH 已经登录成功，但非交互 SSH 环境找不到 `codex` 命令。当前版本会自动加载常见 profile、nvm 和这些路径：

```text
~/.local/bin
~/bin
~/.npm-global/bin
~/.npm-packages/bin
~/.bun/bin
~/.cargo/bin
~/.nvm/versions/node/*/bin
/usr/local/bin
/usr/bin
/bin
```

如果仍然找不到，可以在 App 设置里的“Codex 命令”填绝对路径。例如当前这台 VPS 检测到：

```text
/home/codex/.nvm/versions/node/v24.21.0/bin/codex
```

如果 App 显示：

```text
Reading additional input from stdin...
```

说明 Codex CLI 已经启动，但正在等待 stdin。当前版本已在 SSH channel 里发送空输入流，并在远端命令后追加 `</dev/null`，让 `codex exec` 立即收到 EOF。

如果 App 显示：

```text
Not inside a trusted directory and --skip-git-repo-check was not specified.
```

说明工作目录不是 Codex 已信任的 Git 仓库。客户端现在会为新会话和 resume 都自动添加 `--skip-git-repo-check`。这只跳过仓库检查，不会绕过 Codex 的审批或沙箱策略。

### 模型或推理档位不受支持

模型和档位由远端 Codex 账号及 CLI 决定，不是 SSH 权限。先在输入栏选择 `VPS 默认`；如果仍有问题，在 VPS 上以 `codex` 用户运行：

```bash
codex --version
codex exec --help
codex exec resume --help
```

### Resume 没有进入原会话

确认历史会话里保存了正确的 thread id。当前版本会从 Codex JSONL 输出中尽量识别 `thread_id`，如果 Codex CLI 输出格式变化，可能需要同步更新解析器。

### SSH 主机密钥发生变化

如果 VPS 重装或 SSH host key 确实更换，App 会拒绝连接。这是预期的安全行为。确认新主机密钥可信后，可清除 App 数据并重新配置；后续版本会增加单独的“忘记主机密钥”按钮。

## 后续建议

- 增加主机密钥指纹查看和单独重置入口。
- 增加语言切换，而不是只有默认中文。
- 支持导出/导入本地会话历史。
- 将 transport 从 `codex exec` 扩展到未来的 `codex app-server`。
