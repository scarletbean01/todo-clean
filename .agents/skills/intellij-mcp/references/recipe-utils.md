# MCP Steroid Recipes: Utils, Inspections, & Build

*For prerequisites, threading rules, and common imports, see the [Index](MCP_STEROID_RECIPES.md).*

## 1. File Inspections

### 1.1 Run all enabled inspections on a file

```kotlin
import com.intellij.psi.PsiDocumentManager

val filePath = "src/main/java/com/example/MyClass.java" // <-- change me
val vf = findProjectFile(filePath) ?: error("File not found: $filePath")

val problems = runInspectionsDirectly(vf)

val records = readAction {
    val docManager = PsiDocumentManager.getInstance(project)
    problems.flatMap { (inspectionId, descriptors) ->
        descriptors.map { problem ->
            val element = problem.psiElement
            val line = if (element != null) {
                val doc = docManager.getDocument(element.containingFile)
                doc?.getLineNumber(element.textOffset)?.plus(1) ?: -1
            } else -1
            listOf(inspectionId, line, problem.descriptionTemplate)
        }
    }
}

printCsv(
    headers = listOf("inspectionId", "line", "description"),
    rows = records
)
```

### 1.2 Get file problems (errors only)

Use the `get_file_problems` MCP tool directly, or via code:

```kotlin
import com.intellij.psi.PsiDocumentManager
import com.intellij.codeInspection.ProblemHighlightType

val filePath = "src/main/java/com/example/MyClass.java" // <-- change me
val vf = findProjectFile(filePath) ?: error("File not found: $filePath")

val problems = runInspectionsDirectly(vf)

val errors = readAction {
    val docManager = PsiDocumentManager.getInstance(project)
    problems.flatMap { (inspectionId, descriptors) ->
        descriptors
            .filter { 
                it.highlightType == ProblemHighlightType.GENERIC_ERROR || 
                it.highlightType == ProblemHighlightType.ERROR 
            }
            .map { problem ->
                val element = problem.psiElement
                val line = if (element != null) {
                    val doc = docManager.getDocument(element.containingFile)
                    doc?.getLineNumber(element.textOffset)?.plus(1) ?: -1
                } else -1
                listOf(inspectionId, line, problem.descriptionTemplate)
            }
    }
}

println("Errors: ${errors.size}")
printCsv(
    headers = listOf("inspectionId", "line", "description"),
    rows = errors
)
```

---

## 2. Compile & Build

### 2.1 Build the whole project

```kotlin
import com.intellij.task.ProjectTaskManager
import org.jetbrains.concurrency.await

val result = ProjectTaskManager.getInstance(project).buildAllModules().await()
println("Errors: ${result.hasErrors()}, Aborted: ${result.isAborted()}")
```

---

## 3. Token-Saving Utilities

### 3.1 Extract Class Outline (Stub Generator)

Extracts just the fields and method signatures from a massive Java file, skipping all method bodies. This is a crucial token-saver for AI agents analyzing large APIs.

```kotlin
import com.intellij.psi.*

val filePath = "src/main/java/com/example/MyService.java" // <-- change me
val psiFile = findProjectPsiFile(filePath) as? PsiJavaFile

val outline = readAction {
    if (psiFile != null) {
        buildString {
            for (cls in psiFile.classes) {
                appendLine("class ${cls.name} {")
                for (field in cls.fields) {
                    appendLine("    ${field.modifierList?.text ?: ""} ${field.type.presentableText} ${field.name};")
                }
                appendLine()
                for (method in cls.methods) {
                    val returnType = method.returnType?.presentableText ?: ""
                    val params = method.parameterList.parameters.joinToString(", ") { "${it.type.presentableText} ${it.name}" }
                    val modifiers = method.modifierList?.text ?: ""
                    appendLine("    $modifiers $returnType ${method.name}($params);")
                }
                appendLine("}")
            }
        }
    } else "File not found or not a Java file."
}
println(outline)
```

### 3.2 Auto-Resolve Missing Imports

Programmatically adds an `import` statement for a known class. This is perfect for fixing compilation errors after an AI injects new code containing unresolved references.

```kotlin
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.PsiJavaFile
import com.intellij.openapi.command.WriteCommandAction

val filePath = "src/main/java/com/example/MyService.java" // <-- change me
val classToImportFqn = "java.util.UUID" // <-- change me

val psiFile = findProjectPsiFile(filePath) as? PsiJavaFile
var success = false

WriteCommandAction.runWriteCommandAction(project) {
    val cls = JavaPsiFacade.getInstance(project).findClass(classToImportFqn, GlobalSearchScope.allScope(project))
    if (psiFile != null && cls != null) {
        JavaCodeStyleManager.getInstance(project).addImport(psiFile, cls)
        success = true
    }
}
writeAction { PsiDocumentManager.getInstance(project).commitAllDocuments() }
println(if (success) "Import added" else "Failed to find file or class")
```

### 3.3 Reformat / Auto-Indent File

Automatically fixes messy indentation and spacing left over from raw text edits.

```kotlin
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.openapi.command.WriteCommandAction

val filePath = "src/main/java/com/example/MyService.java" // <-- change me

val psiFile = findProjectPsiFile(filePath)
var success = false

WriteCommandAction.runWriteCommandAction(project) {
    if (psiFile != null) {
        CodeStyleManager.getInstance(project).reformat(psiFile)
        success = true
    }
}

writeAction { PsiDocumentManager.getInstance(project).commitAllDocuments() }
println(if (success) "Reformatted $filePath" else "File not found")
```
