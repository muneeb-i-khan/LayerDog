package com.layerdog.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.layerdog.utils.LayerDetector
import com.layerdog.utils.LayerDetector.Layer

/**
 * Inspection for DTO layer violations.
 * DTOs should only call API layer and do validations and conversions. No business logic.
 */
class DTOLayerInspection : BaseLayerInspection() {

    override fun checkClass(psiClass: PsiClass, holder: ProblemsHolder) {
        val currentLayer = LayerDetector.detectLayer(psiClass)
        
        if (currentLayer != Layer.DTO) return

        // Check if DTO has business logic
        if (LayerDetector.hasBusinessLogic(psiClass)) {
            createProblem(
                psiClass.nameIdentifier ?: psiClass,
                "DTO '${getClassName(psiClass)}' contains business logic. DTOs should only handle validation and conversion.",
                holder,
                MoveBusinessLogicToAPIQuickFix()
            )
        }
    }

    override fun checkMethodCall(expression: PsiMethodCallExpression, holder: ProblemsHolder) {
        val containingClass = getContainingClass(expression) ?: return
        val currentLayer = LayerDetector.detectLayer(containingClass)
        
        if (currentLayer != Layer.DTO) return
        
        // Skip self calls and Java library calls
        if (isSelfCall(expression) || isJavaLibraryCall(resolveTargetClass(expression))) {
            return
        }

        val targetClass = resolveTargetClass(expression) ?: return
        val targetLayer = LayerDetector.detectLayer(targetClass)

        if (!LayerDetector.isValidLayerCall(currentLayer, targetLayer)) {
            val message = when (targetLayer) {
                Layer.CONTROLLER -> "DTO '${getClassName(containingClass)}' is calling Controller '${getClassName(targetClass)}'. DTOs should not call Controllers."
                Layer.DAO -> "DTO '${getClassName(containingClass)}' is directly calling DAO layer '${getClassName(targetClass)}'. Use API layer instead."
                Layer.FLOW -> "DTO '${getClassName(containingClass)}' is calling FLOW layer '${getClassName(targetClass)}'. Use API layer instead."
                Layer.DTO -> "DTO '${getClassName(containingClass)}' is calling another DTO '${getClassName(targetClass)}'. Consider consolidating or using API layer."
                else -> "DTO '${getClassName(containingClass)}' is calling '${getClassName(targetClass)}' which is not in the API layer. DTOs should only call the API layer."
            }

            createProblem(
                expression.methodExpression,
                message,
                holder,
                MoveToAPILayerQuickFix(getClassName(targetClass))
            )
        }

        // Additional check for validation patterns
        checkValidationPatterns(expression, containingClass, holder)
    }

    /**
     * Check if DTO is following proper validation patterns
     */
    private fun checkValidationPatterns(expression: PsiMethodCallExpression, containingClass: PsiClass, holder: ProblemsHolder) {
        val methodName = expression.methodExpression.referenceName ?: return
        
        // Look for potential business logic patterns that should be in API layer
        if (isBusinessLogicPattern(methodName)) {
            createProblem(
                expression.methodExpression,
                "DTO '${getClassName(containingClass)}' method '$methodName' appears to contain business logic. Consider moving to API layer.",
                holder,
                MoveBusinessLogicToAPIQuickFix()
            )
        }
    }

    /**
     * Identifies method names that suggest business logic
     */
    private fun isBusinessLogicPattern(methodName: String): Boolean {
        val businessLogicPatterns = listOf(
            "calculate", "compute", "process", "execute", "perform", "apply",
            "analyze", "evaluate", "determine", "decide", "resolve"
        )
        
        return businessLogicPatterns.any { pattern ->
            methodName.contains(pattern, ignoreCase = true)
        }
    }

    /**
     * Quick fix to move business logic to API layer
     */
    private class MoveBusinessLogicToAPIQuickFix : LocalQuickFix {
        override fun getName(): String = "Move business logic to API layer"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val message = """
                To fix this violation:
                1. Create or identify the appropriate API/Service class
                2. Move the business logic methods from DTO to API class
                3. Keep only validation, conversion, and data transformation in DTO
                4. Have DTO call the API methods for business operations
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Move Business Logic", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }

    /**
     * Quick fix to move call to API layer
     */
    private class MoveToAPILayerQuickFix(private val targetClassName: String) : LocalQuickFix {
        override fun getName(): String = "Use API layer for this operation"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val message = """
                To fix this violation:
                1. Create an API/Service method that handles this operation
                2. Move the call to '$targetClassName' from DTO to the API layer
                3. Have the DTO call the API method instead
                4. Ensure proper data flow: DTO → API → DAO/FLOW
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Use API Layer", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }
}
