package com.layerdog.utils

import com.intellij.psi.*

/**
 * Utility class to detect which architectural layer a class belongs to
 * based on naming conventions and package structure.
 */
object LayerDetector {
    
    enum class Layer {
        CONTROLLER,
        DTO,
        API,
        FLOW,
        DAO,
        UNKNOWN
    }

    /**
     * Determines the architectural layer of a PsiClass
     */
    fun detectLayer(psiClass: PsiClass): Layer {
        val className = psiClass.name ?: return Layer.UNKNOWN
        val packageName = getPackageName(psiClass)
        
        // Check by class name suffixes/prefixes first
        when {
            className.endsWith("Controller", ignoreCase = true) ||
            className.endsWith("Resource", ignoreCase = true) ||
            className.endsWith("Endpoint", ignoreCase = true) ||
            packageName.contains("controller", ignoreCase = true) ||
            packageName.contains("web", ignoreCase = true) ||
            packageName.contains("rest", ignoreCase = true) -> return Layer.CONTROLLER

            className.endsWith("DTO", ignoreCase = true) ||
            className.endsWith("Dto", ignoreCase = true) ||
            className.endsWith("Request", ignoreCase = true) ||
            className.endsWith("Response", ignoreCase = true) ||
            className.endsWith("Model", ignoreCase = true) ||
            packageName.contains("dto", ignoreCase = true) ||
            packageName.contains("model", ignoreCase = true) -> return Layer.DTO

            className.endsWith("Service", ignoreCase = true) ||
            className.endsWith("ServiceImpl", ignoreCase = true) ||
            className.endsWith("Manager", ignoreCase = true) ||
            className.endsWith("Handler", ignoreCase = true) ||
            className.endsWith("Processor", ignoreCase = true) ||
            packageName.contains("service", ignoreCase = true) ||
            packageName.contains("business", ignoreCase = true) ||
            packageName.contains("logic", ignoreCase = true) -> return Layer.API

            className.endsWith("Flow", ignoreCase = true) ||
            className.endsWith("Workflow", ignoreCase = true) ||
            className.endsWith("Orchestrator", ignoreCase = true) ||
            className.endsWith("Coordinator", ignoreCase = true) ||
            packageName.contains("flow", ignoreCase = true) ||
            packageName.contains("workflow", ignoreCase = true) ||
            packageName.contains("orchestrat", ignoreCase = true) -> return Layer.FLOW

            className.endsWith("DAO", ignoreCase = true) ||
            className.endsWith("Dao", ignoreCase = true) ||
            className.endsWith("Repository", ignoreCase = true) ||
            className.endsWith("Mapper", ignoreCase = true) ||
            className.endsWith("Entity", ignoreCase = true) ||
            packageName.contains("dao", ignoreCase = true) ||
            packageName.contains("repository", ignoreCase = true) ||
            packageName.contains("data", ignoreCase = true) ||
            packageName.contains("persistence", ignoreCase = true) -> return Layer.DAO
        }

        // Check by annotations
        val annotations = psiClass.annotations
        for (annotation in annotations) {
            val annotationName = annotation.qualifiedName ?: continue
            when {
                annotationName.contains("Controller") ||
                annotationName.contains("RestController") ||
                annotationName.contains("Resource") -> return Layer.CONTROLLER

                annotationName.contains("Service") -> return Layer.API
                
                annotationName.contains("Repository") ||
                annotationName.contains("Entity") ||
                annotationName.contains("Table") -> return Layer.DAO
            }
        }

        return Layer.UNKNOWN
    }

    /**
     * Checks if a method call violates layer rules
     */
    fun isValidLayerCall(fromLayer: Layer, toLayer: Layer): Boolean {
        return when (fromLayer) {
            Layer.CONTROLLER -> toLayer == Layer.DTO
            Layer.DTO -> toLayer == Layer.API
            Layer.API -> toLayer == Layer.DAO || toLayer == Layer.FLOW
            Layer.FLOW -> toLayer == Layer.API
            Layer.DAO -> isValidDAODependency(toLayer)
            Layer.UNKNOWN -> true // Skip validation for unknown layers
        }
    }

    /**
     * Checks if the target class represents a database-related dependency
     */
    private fun isValidDAODependency(targetLayer: Layer): Boolean {
        // DAO can call other DAOs, database drivers, etc.
        return targetLayer == Layer.DAO || targetLayer == Layer.UNKNOWN
    }

    /**
     * Checks if a class is likely to be a database-related class
     */
    fun isDatabaseRelated(psiClass: PsiClass): Boolean {
        val className = psiClass.name ?: return false
        val packageName = getPackageName(psiClass)
        
        return className.contains("Connection", ignoreCase = true) ||
                className.contains("DataSource", ignoreCase = true) ||
                className.contains("Driver", ignoreCase = true) ||
                className.contains("Statement", ignoreCase = true) ||
                className.contains("ResultSet", ignoreCase = true) ||
                packageName.contains("java.sql") ||
                packageName.contains("javax.sql") ||
                packageName.contains("hibernate") ||
                packageName.contains("mybatis") ||
                packageName.contains("jpa") ||
                packageName.contains("jdbc")
    }

    /**
     * Gets the package name of a PsiClass
     */
    private fun getPackageName(psiClass: PsiClass): String {
        val containingFile = psiClass.containingFile as? PsiJavaFile
        return containingFile?.packageName ?: ""
    }

    /**
     * Checks if a class contains business logic patterns
     */
    fun hasBusinessLogic(psiClass: PsiClass): Boolean {
        val methods = psiClass.methods
        
        for (method in methods) {
            // Check for complex conditional logic
            if (hasComplexLogic(method)) return true
            
            // Check for calculation/transformation logic
            if (hasCalculationLogic(method)) return true
        }
        
        return false
    }

    private fun hasComplexLogic(method: PsiMethod): Boolean {
        val body = method.body ?: return false
        
        // Count if/else, switch, loops as indicators of business logic
        val ifStatements = countStatements(body, PsiIfStatement::class.java)
        val switchStatements = countStatements(body, PsiSwitchStatement::class.java)
        val loops = countStatements(body, PsiLoopStatement::class.java)
        
        // Simple heuristic: more than 2 conditional branches suggests business logic
        return ifStatements + switchStatements + loops > 2
    }

    private fun hasCalculationLogic(method: PsiMethod): Boolean {
        val body = method.body ?: return false
        
        // Look for mathematical operations, string manipulations, etc.
        val assignments = countStatements(body, PsiAssignmentExpression::class.java)
        val binaryExpressions = countStatements(body, PsiBinaryExpression::class.java)
        
        return assignments > 3 || binaryExpressions > 5
    }

    private fun <T : PsiElement> countStatements(element: PsiElement, clazz: Class<T>): Int {
        var count = 0
        element.accept(object : JavaRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (clazz.isInstance(element)) {
                    count++
                }
                super.visitElement(element)
            }
        })
        return count
    }
}
