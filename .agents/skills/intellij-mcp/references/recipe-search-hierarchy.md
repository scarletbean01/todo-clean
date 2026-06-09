# MCP Steroid Recipes: Search & Hierarchy

*For prerequisites, threading rules, and common imports, see the [Index](MCP_STEROID_RECIPES.md).*

## 1. Inspect Project Structure

### 1.1 List all modules

```kotlin
import com.intellij.openapi.module.ModuleManager

val modules = readAction {
    ModuleManager.getInstance(project).modules
        .map { "${it.name} (${it.moduleTypeName})" }
        .sorted()
}
println(modules.joinToString("\n"))
```

### 1.2 List content roots

```kotlin
import com.intellij.openapi.roots.ProjectRootManager

val roots = readAction {
    ProjectRootManager.getInstance(project).contentRoots
        .map { it.path }
        .sorted()
}
println(roots.joinToString("\n"))
```

---

## 2. Search Symbols

### 2.1 Find a class by simple name

```kotlin
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache

val className = "MyService" // <-- change me

val classes = readAction {
    PsiShortNamesCache.getInstance(project)
        .getClassesByName(className, GlobalSearchScope.projectScope(project))
        .map { "${it.qualifiedName} -> ${it.containingFile?.virtualFile?.path}" }
}

if (classes.isEmpty()) println("No classes named '$className' found.")
else println(classes.joinToString("\n"))
```

### 2.2 Fuzzy class search by name fragment

Avoids loading all class names at once and prevents index nesting deadlocks.

```kotlin
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache

val fragment = "Controller" // <-- change me

val matches = readAction {
    val matchingNames = mutableListOf<String>()
    // Stream names to avoid allocating the massive allClassNames array
    PsiShortNamesCache.getInstance(project).processAllClassNames { name ->
        if (name.contains(fragment, ignoreCase = true)) {
            matchingNames.add(name)
        }
        true
    }
    
    // Perform lookups outside the callback to avoid index nesting deadlocks
    matchingNames.flatMap { name ->
        PsiShortNamesCache.getInstance(project)
            .getClassesByName(name, GlobalSearchScope.projectScope(project))
            .map { it.qualifiedName ?: it.name ?: "" }
            .filter { it.isNotEmpty() }
    }
    .distinct()
    .sorted()
}
println(matches.joinToString("\n"))
```

### 2.3 Find a method by name

Outputs a token-efficient CSV table with path deduplication.

```kotlin
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiUtilCore

val methodName = "handle" // <-- change me

val methods = readAction {
    PsiShortNamesCache.getInstance(project)
        .getMethodsByName(methodName, GlobalSearchScope.projectScope(project))
        .map {
            val cls = it.containingClass?.qualifiedName ?: "<top-level>"
            val file = PsiUtilCore.getVirtualFile(it)?.path ?: "<unknown>"
            listOf(cls, it.name, it.parameterList.parametersCount, file)
        }
}

printCsv(
    headers = listOf("class", "method", "params", "path"),
    rows = methods,
    dictColumns = setOf("path")
)
```

---

## 3. Class Hierarchy

### 3.1 Show superclass chain + direct inheritors

```kotlin
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.PsiClass
import com.intellij.psi.search.searches.ClassInheritorsSearch

val targetClassName = "RuntimeException" // <-- change me

val result = readAction {
    val classes = PsiShortNamesCache.getInstance(project)
        .getClassesByName(targetClassName, GlobalSearchScope.projectScope(project))

    if (classes.isEmpty()) {
        "No class named '$targetClassName' found."
    } else {
        buildString {
            for (cls in classes) {
                appendLine("CLASS: ${cls.qualifiedName}")
                appendLine("FILE: ${cls.containingFile?.virtualFile?.path}")

                // Superclasses
                val hierarchy = mutableListOf<String>()
                var current: PsiClass? = cls
                while (current != null) {
                    hierarchy.add(current.qualifiedName ?: current.name ?: "<anonymous>")
                    current = current.superClass
                }
                appendLine("HIERARCHY:")
                hierarchy.forEachIndexed { index, name ->
                    appendLine("${"  ".repeat(index)}$name")
                }

                // Subclasses inside the project
                val inheritors = mutableListOf<String>()
                ClassInheritorsSearch.search(cls, GlobalSearchScope.projectScope(project), true)
                    .forEach { inheritor ->
                        inheritors.add(inheritor.qualifiedName ?: inheritor.name ?: "<anonymous>")
                    }
                if (inheritors.isNotEmpty()) {
                    appendLine("INHERITORS:")
                    inheritors.sorted().forEach { appendLine("  $it") }
                }
            }
        }
    }
}
println(result)
```

### 3.2 Find all implementations of an interface

```kotlin
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.searches.ClassInheritorsSearch

val interfaceName = "Runnable" // <-- change me

val implementations = readAction {
    PsiShortNamesCache.getInstance(project)
        .getClassesByName(interfaceName, GlobalSearchScope.projectScope(project))
        .flatMap { iface ->
            ClassInheritorsSearch.search(iface, GlobalSearchScope.projectScope(project), true)
                .map { it.qualifiedName ?: it.name }
        }
        .distinct()
        .sorted()
}
println(implementations.joinToString("\n"))
```

### 3.3 Find overriding methods

```kotlin
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.util.PsiUtilCore

val methodName = "process" // <-- change me
val interfaceName = "Processor" // <-- change me

val overrides = readAction {
    val baseClass = PsiShortNamesCache.getInstance(project)
        .getClassesByName(interfaceName, GlobalSearchScope.projectScope(project))
        .firstOrNull()
        
    val baseMethod = baseClass?.findMethodsByName(methodName, false)?.firstOrNull()

    if (baseMethod != null) {
        OverridingMethodsSearch.search(baseMethod).findAll().map { method ->
            val cls = method.containingClass?.qualifiedName ?: "<anonymous>"
            val file = PsiUtilCore.getVirtualFile(method)?.path ?: "<unknown>"
            listOf(cls, file)
        }
    } else emptyList()
}

if (overrides.isEmpty()) println("No overriding methods found.") else {
    printCsv(listOf("class", "path"), overrides, setOf("path"))
}
```

---

## 4. Find Usages

### 4.1 Find references to a class

Computes correct 1-based line numbers and deduplicates file paths via `printCsv`.

```kotlin
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiUtilCore

val className = "JmcMcpDomainException" // <-- change me

val usages = readAction {
    val docManager = PsiDocumentManager.getInstance(project)
    PsiShortNamesCache.getInstance(project)
        .getClassesByName(className, GlobalSearchScope.projectScope(project))
        .flatMap { cls ->
            ReferencesSearch.search(cls, GlobalSearchScope.projectScope(project))
                .findAll()
                .map { ref ->
                    val element = ref.element
                    val file = PsiUtilCore.getVirtualFile(element)?.path ?: "<unknown>"
                    val doc = docManager.getDocument(element.containingFile)
                    val line = doc?.getLineNumber(element.textOffset)?.plus(1) ?: -1
                    listOf(file, line, element.textOffset)
                }
        }
        .distinct()
}

printCsv(
    headers = listOf("path", "line", "offset"),
    rows = usages,
    dictColumns = setOf("path")
)
```

### 4.2 Find references to a method

```kotlin
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiUtilCore

val methodName = "calculateTotal" // <-- change me
val className = "OrderService" // <-- optional filter

val usages = readAction {
    val docManager = PsiDocumentManager.getInstance(project)
    val methods = PsiShortNamesCache.getInstance(project)
        .getMethodsByName(methodName, GlobalSearchScope.projectScope(project))
        .filter { className == null || it.containingClass?.name == className }

    methods.flatMap { method ->
        ReferencesSearch.search(method, GlobalSearchScope.projectScope(project))
            .findAll()
            .map { ref ->
                val element = ref.element
                val file = PsiUtilCore.getVirtualFile(element)?.path ?: "<unknown>"
                val doc = docManager.getDocument(element.containingFile)
                val line = doc?.getLineNumber(element.textOffset)?.plus(1) ?: -1
                listOf(file, line, element.textOffset)
            }
    }.distinct()
}

printCsv(
    headers = listOf("path", "line", "offset"),
    rows = usages,
    dictColumns = setOf("path")
)
```

---

## 5. Find Annotated Elements

```kotlin
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiUtilCore

val annotationFqn = "jakarta.enterprise.context.ApplicationScoped" // <-- change me

val results = readAction {
    val annotationClass = PsiShortNamesCache.getInstance(project)
        .getClassesByName(annotationFqn.substringAfterLast('.'), GlobalSearchScope.allScope(project))
        .firstOrNull { it.qualifiedName == annotationFqn }

    if (annotationClass != null) {
        // Search for classes
        val classes = AnnotatedElementsSearch.searchPsiClasses(annotationClass, GlobalSearchScope.projectScope(project))
            .findAll()
            .map { 
                val file = PsiUtilCore.getVirtualFile(it)?.path ?: "<unknown>"
                listOf("Class", it.qualifiedName, file) 
            }
            
        // Search for methods
        val methods = AnnotatedElementsSearch.searchPsiMethods(annotationClass, GlobalSearchScope.projectScope(project))
            .findAll()
            .map { 
                val file = PsiUtilCore.getVirtualFile(it)?.path ?: "<unknown>"
                val parentClass = it.containingClass?.qualifiedName ?: ""
                listOf("Method", "$parentClass#${it.name}", file) 
            }
            
        classes + methods
    } else emptyList()
}

if (results.isEmpty()) println("No elements found annotated with $annotationFqn.")
else printCsv(listOf("Type", "Symbol", "Path"), results, setOf("Path"))
```
