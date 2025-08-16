package com.layerdog.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.layerdog.utils.LayerDetector
import com.layerdog.utils.LayerDetector.Layer

/**
 * Inspection for API layer violations.
 * API should have all the logic and call DAO layer. It should not call other API layers directly.
 */
class APILayerInspection : BaseLayerInspection() {

    override fun checkMethodCall(expression: PsiMethodCallExpression, holder: ProblemsHolder) {
        val containingClass = getContainingClass(expression) ?: return
        val currentLayer = LayerDetector.detectLayer(containingClass)
        
        if (currentLayer != Layer.API) return
        
        // Skip self calls and Java library calls
        if (isSelfCall(expression) || isJavaLibraryCall(resolveTargetClass(expression))) {
            return
        }

        val targetClass = resolveTargetClass(expression) ?: return
        val targetLayer = LayerDetector.detectLayer(targetClass)

        // Check for direct API-to-API calls
        if (targetLayer == Layer.API && !isSelfCall(expression)) {
            createProblem(
                expression.methodExpression,
                "API '${getClassName(containingClass)}' is directly calling another API '${getClassName(targetClass)}'. Use FLOW layer for API-to-API communication.",
                holder,
                UseFlowLayerQuickFix(getClassName(targetClass))
            )
            return
        }

        // Check for invalid layer calls
        if (!LayerDetector.isValidLayerCall(currentLayer, targetLayer)) {
            val message = when (targetLayer) {
                Layer.CONTROLLER -> "API '${getClassName(containingClass)}' should not call Controller '${getClassName(targetClass)}'. Controllers should call APIs, not the reverse."
                Layer.DTO -> "API '${getClassName(containingClass)}' should not directly call DTO '${getClassName(targetClass)}'. DTOs should call APIs, not the reverse."
                else -> "API '${getClassName(containingClass)}' is calling '${getClassName(targetClass)}' which violates layer architecture. APIs should call DAO or FLOW layers."
            }

            createProblem(
                expression.methodExpression,
                message,
                holder,
                RefactorLayerCallQuickFix(targetLayer, getClassName(targetClass))
            )
        }

        // Additional checks for business logic validation
        checkBusinessLogicPatterns(expression, containingClass, holder)
    }

    /**
     * Validates that API layer contains appropriate business logic patterns
     */
    private fun checkBusinessLogicPatterns(expression: PsiMethodCallExpression, containingClass: PsiClass, holder: ProblemsHolder) {
        val methodName = expression.methodExpression.referenceName ?: return
        
        // Check for data access patterns that should go through DAO
        if (isDirectDataAccessPattern(methodName)) {
            createProblem(
                expression.methodExpression,
                "API '${getClassName(containingClass)}' appears to have direct data access in method '$methodName'. Consider using DAO layer.",
                holder,
                CreateDAOMethodQuickFix(methodName)
            )
        }
    }

    /**
     * Identifies method patterns that suggest direct data access
     */
    private fun isDirectDataAccessPattern(methodName: String): Boolean {
        val dataAccessPatterns = listOf(
            "select", "insert", "update", "delete", "query", "execute",
            "findBy", "saveAs", "removeBy", "countBy"
        )
        
        return dataAccessPatterns.any { pattern ->
            methodName.contains(pattern, ignoreCase = true) && 
            (methodName.contains("sql", ignoreCase = true) || 
             methodName.contains("db", ignoreCase = true) ||
             methodName.contains("database", ignoreCase = true))
        }
    }

    /**
     * Quick fix to use FLOW layer for API-to-API communication
     */
    private class UseFlowLayerQuickFix(private val targetAPIName: String) : LocalQuickFix {
        override fun getName(): String = "Use FLOW layer for API communication"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val message = """
                To fix this API-to-API communication violation:
                1. Create or identify a FLOW class for this operation
                2. Move the call to '$targetAPIName' from this API to the FLOW layer
                3. Have this API call the FLOW method instead
                4. Let the FLOW orchestrate the API calls
                
                Example:
                // Before: api1.method() calls api2.method()
                // After: api1.method() calls flow.orchestrateOperation() 
                //        flow.orchestrateOperation() calls api2.method()
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Use FLOW Layer", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }

    /**
     * Quick fix for invalid layer calls
     */
    private class RefactorLayerCallQuickFix(
        private val targetLayer: Layer,
        private val targetClassName: String
    ) : LocalQuickFix {
        override fun getName(): String = "Refactor layer call"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val suggestion = when (targetLayer) {
                Layer.CONTROLLER -> "Controllers should call APIs, not the reverse. Consider restructuring your call flow."
                Layer.DTO -> "DTOs should call APIs, not the reverse. Consider moving data transformation to DTO layer."
                else -> "Consider using appropriate layer: DAO for data access, FLOW for API orchestration."
            }

            val message = """
                To fix this layer violation:
                $suggestion
                
                Current call to '$targetClassName' (${targetLayer.name} layer) needs refactoring.
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Refactor Layer Call", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }

    /**
     * Quick fix to create DAO method
     */
    private class CreateDAOMethodQuickFix(private val methodName: String) : LocalQuickFix {
        override fun getName(): String = "Create DAO method for data access"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val message = """
                To fix this direct data access violation:
                1. Create a DAO class if it doesn't exist
                2. Move the data access logic for '$methodName' to the DAO
                3. Have the API call the DAO method instead
                4. Keep business logic in API, data access in DAO
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Create DAO Method", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }
}
