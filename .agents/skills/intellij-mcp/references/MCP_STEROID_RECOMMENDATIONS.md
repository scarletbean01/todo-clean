# MCP Steroid vs intellij-idea-mcp: Recommendations & Best Practices

**CRITICAL NOTE FOR AI AGENTS:** Before performing code operations, you should always be intimately acquainted with the optimized code recipes provided in [`MCP_STEROID_RECIPES.md`](./MCP_STEROID_RECIPES.md). These recipes dictate the safest and most token-efficient ways to navigate and manipulate the project via the IntelliJ Context API.

## 1. Core Philosophy

The design of MCP Steroid is governed by three strict tenets (as outlined in [PHILOSOPHY.md](https://github.com/jonnyzzz/mcp-steroid/blob/main/docs/PHILOSOPHY.md)):

1. **Minimal MCP tool surface**: Don't propose new `steroid_*` tools. The tool surface is intentionally kept small. Power comes from richer prompt resources and teaching the agent to call IntelliJ APIs directly.
2. **Power lives in prompts and direct API usage**: Teach the AI to call IntelliJ's APIs exactly as IntelliJ exposes them. Do not wrap them in "agent-friendly" abstractions or add custom helper methods. The AI gets the full power of the IDE via `steroid_execute_code`.
3. **Stateless execution (`devrig` is stateless)**: The backend binary holds no persistent state across calls. Every invocation is a fresh process, ensuring reproducible and clean execution environments.

The fundamental difference between MCP Steroid and native MCP servers is architectural:
* **`intellij-idea-mcp` (Native Tools)**: Provides fixed-function, coarse-grained tools (e.g., `Read`, `Edit`, `Find Usages`, `Grep`).
* **`MCP Steroid`**: Provides an execution engine (`steroid_execute_code`) that runs arbitrary Kotlin scripts directly inside the IntelliJ JVM, allowing you to orchestrate the IDE's Virtual File System (VFS) and Program Structure Interface (PSI) directly.

## 2. Token Consumption Comparison

### File Editing (The "Read-before-Edit" Tax)
* **intellij-idea-mcp**: The native `Edit` tool enforces a strict contract—you must read the file first to know the exact `old_string`. For a 200-line file, the `Read` tool consumes thousands of input tokens just to stage a 1-line edit. If you edit 5 files, you pay the tool-call overhead 5 times.
* **MCP Steroid**: The file is read natively inside the IntelliJ JVM (`vf.contentsToByteArray()`), meaning the file content **never crosses the MCP boundary**. The tool payload is just a ~300-byte Kotlin script containing your `replace` or `applyPatch { }` DSL. 
* **Net Result**: Steroid uses a fraction of the tokens because it eliminates the pre-read tax.

### Structural Searches (Usages, Hierarchy, Inspections)
* **intellij-idea-mcp**: Native search tools typically return verbose, prose-heavy JSON arrays. They often duplicate long absolute file paths for every result in the same file and include large snippets of surrounding source code for "context".
* **MCP Steroid**: You format the output exactly as needed. By utilizing `printCsv(headers, rows, dictColumns = setOf("path"))`, Steroid emits a preamble that dictionary-encodes absolute paths (e.g., `p1=/home/user/Project/File.java`), replacing them with short IDs (`p1`) in the data rows. 
* **Net Result**: Steroid's CSV representation is typically **~60% cheaper** in token consumption than native prose/JSON equivalents.

### Batching Operations
* **intellij-idea-mcp**: One task = One tool call. If you need to rename a method, find all its usages, and update 10 call sites, that equates to 1 `Search` call + 10 `Edit` calls (+ 10 `Read` calls).
* **MCP Steroid**: You can batch the search and the edits into a **single** `steroid_execute_code` call using the `applyPatch { }` DSL. 
* **Net Result**: Massively reduced token overhead from multi-turn conversational tool usage.

## 3. Consistency & IDE State

* **intellij-idea-mcp (Native Edit/Bash)**: Native `Edit` or `Bash` tools write directly to the disk. They bypass IntelliJ's Virtual File System (VFS) and Program Structure Interface (PSI). If you edit a file natively and then immediately ask IntelliJ for "Find Usages," caches are stale, and it will return outdated results based on the pre-edit AST until you force a refresh.
* **MCP Steroid**: Operates directly on the VFS and PSI. Scripts automatically run with `smart_non_modal` locks, commit documents, and trigger VFS refreshes before returning. The next semantic query is guaranteed to see the changes you just made.

## 4. Decision Tree: When to Use What

When you have access to both, **MCP Steroid should be your default for any code-aware task**, while native tools are reserved for specific edge cases.

### Use MCP Steroid (`steroid_execute_code`) for:
1. **In-place Edits**: Always use Steroid for editing code. Wrap multiple file edits in a single `applyPatch { hunk(...) }` call. You get atomic IDE undo, pre-flight string validation, and zero stale PSI issues.
2. **Semantic Queries**: Use it for `Find Usages`, `Call Hierarchy`, and class/method lookups. ALWAYS use `printCsv` or `printToon` to keep the context window highly optimized.
3. **Complex Refactoring**: Script IntelliJ's native refactoring processors (e.g., Rename, Move, Change Signature) under a `writeIntentReadAction { }` lock instead of manually editing text.
4. **Project Builds & Test Runners**: Instead of falling back to raw `./mvnw` or `./gradlew` Bash commands (which cold-start and take 30+ seconds), use Steroid to trigger IntelliJ's warm internal build/test runner.

### Use intellij-idea-mcp (Native Tools) or Bash for:
1. **Simple File Reads**: If you simply need to dump the entire contents of a config file into your context window to read it, a native `Read File` tool requires slightly less thought than writing a Steroid script.
2. **True Out-of-IDE Tasks**: Docker CLI manipulation, Git operations, or raw shell scripting should be routed to native `Bash` or system tools, as `steroid_execute_code` explicitly bans `ProcessBuilder` for classpath safety.
3. **IDE Runner Fallback**: If an IDE internal runner fails entirely (e.g., a broken run configuration), use native Bash tools as a final fallback.
