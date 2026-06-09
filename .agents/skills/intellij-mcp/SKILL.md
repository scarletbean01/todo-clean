---
name: intellij-mcp
description: Rules and decision matrices for optimal usage of IntelliJ MCP servers (intellij-mcpserver and mcp-steroid). Use this skill whenever you need to search, edit, compile, or debug code in an IntelliJ project.
license: MIT
compatibility: opencode
metadata:
  audience: ai-agents
  framework: intellij
---

# IntelliJ MCP Usage Guide

I provide strict guidelines on how AI agents should interact with an IntelliJ-backed project using MCP servers. I explain which tools to use for specific tasks to avoid common pitfalls like context-window bloat, stale PSI indexes, and token exhaustion.

## What I Do

1. Define tool priority rules between `intellij-mcpserver` and `mcp-steroid`.
2. Provide a decision matrix mapping intent to the correct MCP tool.
3. Detail rules for memory-efficient and state-safe file editing.

## When to Use Me

Load this skill whenever:
- You need to perform code operations (editing, refactoring).
- You need to execute project-wide semantic searches.
- You need to compile the project, run tests, or attach a debugger.
- You are unsure whether to use a native shell command, a native MCP tool, or an injected Kotlin script.

## Core Rules & Priorities

1. **CRITICAL PRIORITY**: Always prefer `mcp-steroid` (`steroid_execute_code`) over `intellij-mcpserver` for code-aware tasks. You MUST be intimately acquainted with the optimized code recipes provided in [`MCP_STEROID_RECIPES.md`](references/MCP_STEROID_RECIPES.md) and [`MCP_STEROID_RECOMMENDATIONS.md`](references/MCP_STEROID_RECOMMENDATIONS.md).
2. **Avoid `replace_text_in_file`**: It suffers from the "Read-before-Edit tax" and bypasses the IntelliJ Virtual File System, which causes the IDE's internal indexes to become stale.
3. **Avoid Native Search Tools**: Native tools like `search_symbol` and `search_text` return massive, bloated JSON payloads that consume your context window. Use `steroid_execute_code` with `printCsv` deduplication instead to save ~60% of tokens.
4. **Use IntelliJ Refactoring Processors**: For renaming or safely deleting elements, use `steroid_execute_code` with the `writeIntentReadAction { }` wrapper.
5. **Atomic Batch Edits**: Use the `applyPatch { }` DSL inside `steroid_execute_code` to edit multiple files in a single atomic WriteCommandAction.

## Quick Decision Matrix

### File & Semantic Search
- **Find class/method/field**: `steroid_execute_code` (with `printCsv`)
- **Get symbol docs**: `steroid_execute_code` (PSI analysis)
- **Find file by name**: `find_files_by_name_keyword`
- **Find file by glob**: `find_files_by_glob`
- **List project modules**: `get_project_modules`

### Code Editing & Refactoring
- **Targeted edit / batch edit**: `steroid_execute_code` (with `applyPatch { }` DSL)
- **Rename everywhere**: `steroid_execute_code` (with `RenameProcessor`)
- **Create new file**: `create_new_file`
- **Reformat file**: `reformat_file`

### Build, Test, & Execution
- **Compile/build project**: `build_project` or `get_file_problems`
- **Run tests or run configurations**: `execute_run_configuration`
- **Run shell/git command**: `execute_terminal_command`

### Advanced IDE Control (mcp-steroid)
- **Discover quick-fixes at caret**: `steroid_action_discovery`
- **Check if IDE is indexing**: `steroid_list_windows`
- **Take screenshot of IDE UI**: `steroid_take_screenshot`
- **Click/type in IDE UI**: `steroid_input`

### Debugging (xdebug)
- **Check session state**: `xdebug_get_debugger_status`
- **Set a breakpoint**: `xdebug_set_breakpoint`
- **View call stack**: `xdebug_get_stack`
- **Inspect frame variables**: `xdebug_get_frame_values`
- **Step or resume**: `xdebug_control_session`

## References & Required Reading

Before engaging in any IntelliJ MCP operation, you must review:
1. [`MCP_STEROID_RECOMMENDATIONS.md`](references/MCP_STEROID_RECOMMENDATIONS.md) — The philosophical rationale and token optimization strategies for using `mcp-steroid`.
2. [`MCP_STEROID_RECIPES.md`](references/MCP_STEROID_RECIPES.md) — Copy-pasteable, thread-safe, and highly optimized Kotlin scripts for executing PSI search, advanced refactoring, and atomic multi-file edits.
