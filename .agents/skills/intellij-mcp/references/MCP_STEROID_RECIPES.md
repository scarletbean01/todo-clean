# MCP Steroid Cookbook (Index)

A collection of reusable Kotlin scripts for the [MCP Steroid](https://github.com/jonnyzzz/mcp-steroid) IntelliJ plugin.

To drastically reduce token load, the cookbook has been split into task-specific reference guides. **Instead of reading this entire folder, ONLY read the specific markdown file that matches your current intent.**

---

## 📚 Recipe Categories

### 1. Code Navigation & Semantic Search
See: [`recipe-search-hierarchy.md`](recipe-search-hierarchy.md)
**Use this for:**
- Listing project modules and content roots
- Finding classes, interfaces, and methods by name
- Discovering all usages and references to a method or class
- Inspecting superclass chains and inheritors
- Finding overriding methods for an interface

### 2. Advanced IDE Refactoring
See: [`recipe-refactoring.md`](recipe-refactoring.md)
**Use this for:**
- Safe Rename (Class, Method, Field)
- Safe Delete
- Moving a Class to a new Package
- Changing a Method Signature (adding/removing parameters)
- Inlining a Method
- Optimizing Imports

### 3. File Inspections, Build & Token Utilities
See: [`recipe-utils.md`](recipe-utils.md)
**Use this for:**
- Running IntelliJ inspections and collecting file problems
- Triggering a Project Build programmatically
- Generating a "Class Outline / Stub" to save context tokens

---

## 🛠️ Global Prerequisites & Rules

These rules apply to **all** scripts in the cookbook.

- A project must be loaded in IntelliJ (headless mode works).
- Indexing must be complete (`waitForSmartMode()` is executed automatically in `smart_non_modal` mode).
- **Every script must wrap PSI/VFS access in `readAction { }` or `writeIntentReadAction { }`.** The coroutine/read-action context does not persist across script invocations.

### Read / Write Actions Cheat Sheet

| What you are doing | Required wrapper |
|:---|:---|
| Read PSI, VFS, indexes, documents | `readAction { }` |
| Traverse `VirtualFile` trees | `readAction { }` |
| Use `FilenameIndex`, `ReferencesSearch`, `ClassInheritorsSearch` | `readAction { }` |
| Write to a file via VFS | `writeAction { }` |
| Create/delete files or directories | `writeAction { }` |
| Invoke refactoring processors (Rename, Move, etc.) | `writeIntentReadAction { }` |
| Commit documents to PSI after edits | `writeAction { PsiDocumentManager.getInstance(project).commitAllDocuments() }` |

### Common Imports

Copy and paste these into your scripts when needed:
```kotlin
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.task.ProjectTaskManager
import org.jetbrains.concurrency.await
```

### Tips

- Always end a script with `println(...)` or `printJson(...)`. The last expression is **not** auto-printed.
- Use `printCsv(headers, rows)` for tabular results; it is more compact than JSON for large lists.
- For duplicate-code detection, use the IDE's PSI-based duplicate finder rather than text grep. See `mcp-steroid://ide/find-duplicates`.
- When making the same edit in multiple files, batch them in a single `applyPatch { }` DSL block inside `steroid_execute_code`.
