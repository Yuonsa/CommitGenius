# CommitGenius

![Build](https://github.com/Yuonsa/CommitGenius/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)][plugin]
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)][plugin]

## Overview

**CommitGenius** is an intelligent, context-aware Git commit message generator for IntelliJ IDEA. It streamlines your
version control workflow by automatically turning code changes into meaningful, standardized commit messages using AI.
**CommitGenius** 是一款专为 IntelliJ 平台打造的智能 Git 提交日志生成助手。它通过深入理解代码改动上下文，自动将错综复杂的
Diff 转化为清晰、规范的 Commit Message。

## Features

- **AI-Powered Generation** — Generates structured commit messages
  following [Conventional Commits][conventionalcommits] standards
  AI 驱动，自动生成符合结构化提交规范的 Commit Message
- **Multi-Model Support** — Supports OpenAI, Anthropic, and other models with customizable API endpoints
  支持 OpenAI、Anthropic 等多模型，可自定义 API 地址
- **Row-Level Partial Commit** — Only analyzes actually selected code changes for precise commit messages
  支持行级勾选提交，只分析实际选中的代码改动
- **Output Templates** — Lite / Std / Ultra / Custom, four templates to suit different needs
  四种输出模板（Lite/Std/Ultra/Custom），满足不同场景需求
- **Dual-Layer Configuration** — Global settings with per-project override support
  全局配置与项目级配置分离，灵活适配多项目场景
- **Smart Token Optimization** — Intelligent dependency lockfile filtering to minimize token consumption
  智能过滤依赖锁文件，自动优化 Token 消耗

## Installation

- **From JetBrains Marketplace:**
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "CommitGenius"</kbd> >
  <kbd>Install</kbd>
- **From Disk (Manual):**
  Download the [latest release][last-release] and install it using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Quick Start

1. Open the **Commit Window** (<kbd>Ctrl+K</kbd> / <kbd>Cmd+K</kbd>)
2. Click the **CommitGenius** gear icon ⚙️ to configure your API Key and model settings
3. Stage your changes, click the lightning icon ⚡, and watch the commit message generate

## Feedback & Contribution

- Report issues: [GitHub Issues](https://github.com/Yuonsa/CommitGenius/issues)
- Source code: [GitHub Repository](https://github.com/Yuonsa/CommitGenius)

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

[plugin]: https://plugins.jetbrains.com/plugin/32165-commitgenius

[conventionalcommits]: https://www.conventionalcommits.org/

[last-release]: https://github.com/Yuonsa/CommitGenius/releases/latest
