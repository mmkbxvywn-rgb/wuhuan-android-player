# 无患影音：现有手机 UI 结构盘点

> 日期：2026-11-23  
> 审查基线：`dev-5.6.1`  
> 性质：源码结构盘点，不是截图视觉验收。

## 已检查文件

- `app/src/mobile/res/layout/activity_home.xml`
- `app/src/mobile/res/layout/fragment_vod.xml`
- `app/src/mobile/res/layout/fragment_home.xml`
- `app/src/mobile/res/layout/adapter_continue.xml`
- `app/src/mobile/res/layout/item_recommend_row.xml`
- `app/src/mobile/res/layout/activity_video.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/mobile/java/com/fongmi/android/tv/ui/activity/HomeActivity.java`

## 现有结构事实

### 首页框架

- `activity_home.xml` 使用单一 Fragment 容器，底部为悬浮式自定义导航栏，包含点播、直播（按配置显示）、历史、设置。
- `HomeActivity` 通过 `GlassDrawable` 为底部导航设置玻璃背景；导航可在列表滚动时收起。
- 点播主页面 `fragment_vod.xml` 使用顶部 Toolbar、横向分类列表和页面 Pager。
- 首页内容 `fragment_home.xml` 采用纵向滚动结构；继续观看区按需显示，推荐内容以多个横向 RecyclerView 行呈现。
- 继续观看卡片目前为约 140dp 宽的 16:9 横图，下面显示标题与备注。

### 播放与详情

- `activity_video.xml` 同时承载播放器、延迟播放封面、剧集详情、线路/集数等内容。
- 详情态包含全屏剧照背景、上下渐变遮罩、居中标题与元信息、播放按钮及圆形快捷操作。
- 播放控制栏通过独立 include 布局加载，默认隐藏，交互时再显示。

### 颜色与主题

- 当前主色为薄荷绿 `#1FD6A6`。
- 当前主要背景色包括 `#121212` 与 `#0E1116`，表面色为深灰系。
- 当前颜色令牌与注释明确围绕暗色主题、玻璃材质和薄荷绿强调色构建。
- 这说明现有界面有较强的暗色 / 玻璃视觉方向；仅凭 XML 无法判断实际设备上的视觉效果、层级和质感，必须补充真实截图后再评估。

## 对无患影音的设计影响

1. 不直接沿用现有“玻璃效果 + 高亮薄荷绿”作为最终视觉方案；应先建立无患影音自己的颜色、字体、间距、图标与组件令牌。
2. 首页已有“继续观看 + 分组推荐”的内容骨架，可以评估保留其信息结构，但重新安排内容优先级、卡片比例、留白和导航样式。
3. 播放页承担播放器和详情两种状态，后续设计需明确“播放优先”与“详情浏览”的切换关系，避免两种任务争夺视觉焦点。
4. 当前源码结构可供复用评估，不代表现有布局就是最终方案；重构前需检查 Java / ViewBinding 对控件 ID 的依赖。
5. 需获取真实运行截图，尤其是首页首屏、滚动状态、详情态、播放控制态和设置页；没有截图前不做最终视觉结论。

## 下一步

- 盘点首页、媒体详情、播放控制、设置页的控件 ID 与交互依赖。
- 建立无患影音视觉令牌初稿，并先做首页与播放页两张原型。
- 原型经确认后再改 XML / drawable / theme，逐页回归，避免只换颜色却保留原有拥挤结构。
