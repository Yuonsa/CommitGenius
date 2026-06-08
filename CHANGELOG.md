<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# CommitGenius Changelog

## [Unreleased]

### Added

- Support for local Ollama AI models — no API key required for offline usage
  支持本地 Ollama 模型，无需 API Key 即可离线使用

### Changed

- Smart error handling: automatically guides to settings when API key is missing
  智能错误处理：API Key 未配置时自动引导至设置面板

## [0.0.1] - 2026-06-05

### Added

- AI-powered Git commit message generation integrated into IDE's native Commit window
  智能生成 Git 提交信息，深度集成于 IDE 原生 Commit 窗口
- Multi-model support (OpenAI, Anthropic) with customizable API endpoints and model IDs
  支持 OpenAI 和 Anthropic 多模型，可自定义 API 地址和模型 ID
- Row-level partial commit support — only analyzes actually selected code changes
  支持行级勾选提交（Partial Commit），只分析实际选中的代码改动
- Four output templates: Lite (title only), Std (title + summary), Ultra (title + changelog), Custom
  四种输出模板：Lite（仅标题）、Std（标题 + 摘要）、Ultra（标题 + 变更点列表）、Custom
- Dual-layer configuration: global settings with per-project override support
  全局配置和项目级配置分离，支持项目独立覆盖模型设置
- Smart dependency lockfile filtering to automatically optimize token usage
  智能过滤依赖锁文件，自动优化 Token 消耗
