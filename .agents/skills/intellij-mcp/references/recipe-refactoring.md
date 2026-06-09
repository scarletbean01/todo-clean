# MCP Steroid Recipes: Advanced Refactoring

*For prerequisites, threading rules, and common imports, see the [Index](MCP_STEROID_RECIPES.md).*

**Critical Threading Rule:** Refactoring processors manage their own internal read/write locks. You must wrap their execution in `writeIntentReadAction { }` (never `writeAction`, which will deadlock). Finally, commit the documents to the PSI.

### 1. Safe Rename (Class, Method, or Field)

```kotlin
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.PsiDocumentManager

val oldClassName = "TargetClass" // <-- target to rename
val newClassName = "RenamedClass" // <-- new name

val success = writeIntentReadAction {
    val targetClass = PsiShortNamesCache.getInstance(project)
        .getClassesByName(oldClassName, GlobalSearchScope.projectScope(project))
        .firstOrNull()
        
    if (targetClass != null) {
        RenameProcessor(project, targetClass, newClassName, true, true).run()
        true
    } else false
}

writeAction { PsiDocumentManager.getInstance(project).commitAllDocuments() }
println(if (success) "Successfully renamed '$oldClassName' to '$newClassName'" else "Target class not found.")
```

### 2. Safe Delete 

```kotlin
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDocumentManager

val targetClass = "UnusedClass" // <-- target to delete

val result = writeIntentReadAction {
    val cls = PsiShortNamesCache.getInstance(project)
        .getClassesByName(targetClass, GlobalSearchScope.projectScope(project))
        .firstOrNull()
        
    if (cls != null) {
        SafeDeleteProcessor.createInstance(project, null, arrayOf<PsiElement>(cls), true, true).run()
        true
    } else false
}

writeAction { PsiDocumentManager.getInstance(project).commitAllDocuments() }
println(if (result) "Successfully safe-deleted '$targetClass'" else "Target not found.")
```

### 3. Optimize Imports

```kotlin
import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.psi.PsiDocumentManager

val targetFile = "src/main/java/com/example/MyService.java" // <-- File to clean up

val result = writeIntentReadAction {
    val psiFile = findProjectPsiFile(targetFile)
    if (psiFile != null) {
        OptimizeImportsProcessor(project, psiFile).run()
        true
    } else false
}

writeAction { PsiDocumentManager.getInstance(project).commitAllDocuments() }
println(if (result) "Optimized imports in '$targetFile'." else "File not found.")
```

### 4. Move Class/Package

```kotlin
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesProcessor
import com.intellij.refactoring.move.moveClassesOrPackages.SingleSourceRootMoveDestination
import com.intellij.refactoring.PackageWrapper
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager

val className = "MyService" // <-- class to move
val targetPackageName = "com.example.newpackage" // <-- target package

val success = writeIntentReadAction {
    val cls = PsiShortNamesCache.getInstance(project)
        .getClassesByName(className, GlobalSearchScope.projectScope(project))
        .firstOrNull()
        
    val psiFacade = JavaPsiFacade.getInstance(project)
    val targetPackage = psiFacade.findPackage(targetPackageName)
    val targetDir = targetPackage?.directories?.firstOrNull()
    
    if (cls != null && targetPackage != null && targetDir != null) {
        val destination = SingleSourceRootMoveDestination(PackageWrapper(targetPackage), targetDir)
        MoveClassesOrPackagesProcessor(project, arrayOf(cls), destination, true, true, null).run()
        true
    } else false
}

writeAction { PsiDocumentManager.getInstance(project).commitAllDocuments() }
println(if (success) "Moved $className to $targetPackageName" else "Failed to resolve class or target package/directory.")
```

### 5. Change Method Signature

```kotlin
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor
import com.intellij.refactoring.changeSignature.ParameterInfoImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.PsiDocumentManager

val methodName = "processOrder"
val className = "OrderService"

val success = writeIntentReadAction {
    val method = PsiShortNamesCache.getInstance(project)
        .getMethodsByName(methodName, GlobalSearchScope.projectScope(project))
        .firstOrNull { it.containingClass?.name == className }
        
    if (method != null) {
        val typeFactory = com.intellij.psi.PsiElementFactory.getInstance(project)
        val newParamType = typeFactory.createTypeFromText("java.lang.String", method)
        
        // Add a new parameter to the end of the method
        val newParam = ParameterInfoImpl(-1, "newParam", newParamType, "\"DEFAULT\"")
        val newParams = method.parameterList.parameters.mapIndexed { i, p ->
            ParameterInfoImpl(i, p.name, p.type)
        }.toMutableList()
        newParams.add(newParam)
        
        ChangeSignatureProcessor(
            project, method, false, null, method.name, method.returnType, newParams.toTypedArray()
        ).run()
        true
    } else false
}

writeAction { PsiDocumentManager.getInstance(project).commitAllDocuments() }
println(if (success) "Signature changed" else "Method not found")
```

### 6. Inline Method

```kotlin
import com.intellij.refactoring.inline.InlineMethodProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.PsiDocumentManager

val methodName = "helperMethod"
val className = "MyUtils"

val success = writeIntentReadAction {
    val method = PsiShortNamesCache.getInstance(project)
        .getMethodsByName(methodName, GlobalSearchScope.projectScope(project))
        .firstOrNull { it.containingClass?.name == className }
        
    if (method != null) {
        // parameters: project, method, reference, isInlineThisOnly
        InlineMethodProcessor(project, method, null, null, false).run()
        true
    } else false
}

writeAction { PsiDocumentManager.getInstance(project).commitAllDocuments() }
println(if (success) "Inlined method $methodName" else "Method not found")
```

### 7. Rename Package Refactoring

```kotlin
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager

val oldPackageName = "com.example.oldpkg"
val newPackageName = "com.example.newpkg"

val success = writeIntentReadAction {
    val pkg = JavaPsiFacade.getInstance(project).findPackage(oldPackageName)
    if (pkg != null) {
        RenameProcessor(project, pkg, newPackageName, true, true).run()
        true
    } else false
}

writeAction { PsiDocumentManager.getInstance(project).commitAllDocuments() }
println(if (success) "Renamed package '$oldPackageName' to '$newPackageName'" else "Package not found.")
```

### 8. Type Migration Refactoring

```kotlin
import com.intellij.refactoring.typeMigration.TypeMigrationProcessor
import com.intellij.refactoring.typeMigration.TypeMigrationRules
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.JavaPsiFacade

val className = "MyEntity"
val fieldName = "id"
val newTypeFqn = "java.lang.Long" // e.g. "java.lang.String"

val success = writeIntentReadAction {
    val cls = PsiShortNamesCache.getInstance(project)
        .getClassesByName(className, GlobalSearchScope.projectScope(project))
        .firstOrNull()
        
    val field = cls?.findFieldByName(fieldName, false)
    val typeFactory = JavaPsiFacade.getElementFactory(project)
    
    if (field != null) {
        val newType = typeFactory.createTypeByFQClassName(newTypeFqn, GlobalSearchScope.allScope(project))
        val rules = TypeMigrationRules(project)
        rules.setBoundScope(GlobalSearchScope.projectScope(project))
        
        TypeMigrationProcessor(
            project, 
            arrayOf(field), 
            com.intellij.util.Function { newType }, 
            rules, 
            true
        ).run()
        true
    } else false
}

writeAction { PsiDocumentManager.getInstance(project).commitAllDocuments() }
println(if (success) "Migrated type of $fieldName to $newTypeFqn" else "Field not found")
```
