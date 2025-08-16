package com.layerdog.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.layerdog.utils.LayerDetector
import com.layerdog.utils.LayerDetector.Layer

/**
 * Inspection for Controller layer violations.
 * Controllers should have no business logic and should only call DTO layer.
 */
class ControllerLayerInspection : BaseLayerInspection() {

    override fun checkClass(psiClass: PsiClass, holder: ProblemsHolder) {
        val currentLayer = LayerDetector.detectLayer(psiClass)
        
        if (currentLayer != Layer.CONTROLLER) return

        // Check if controller has business logic
        if (LayerDetector.hasBusinessLogic(psiClass)) {
            createProblem(
                psiClass.nameIdentifier ?: psiClass,
                "Controller '${getClassName(psiClass)}' contains business logic. Controllers should delegate to DTOs.",
                holder,
                ExtractBusinessLogicQuickFix()
            )
        }
    }

    override fun checkMethodCall(expression: PsiMethodCallExpression, holder: ProblemsHolder) {
        val containingClass = getContainingClass(expression) ?: return
        val currentLayer = LayerDetector.detectLayer(containingClass)
        
        if (currentLayer != Layer.CONTROLLER) return
        
        // Skip self calls and Java library calls
        if (isSelfCall(expression) || isJavaLibraryCall(resolveTargetClass(expression))) {
            return
        }

        val targetClass = resolveTargetClass(expression) ?: return
        val targetLayer = LayerDetector.detectLayer(targetClass)

        if (!LayerDetector.isValidLayerCall(currentLayer, targetLayer)) {
            val message = when (targetLayer) {
                Layer.API -> "Controller '${getClassName(containingClass)}' is directly calling API layer '${getClassName(targetClass)}'. Use DTO layer instead."
                Layer.DAO -> "Controller '${getClassName(containingClass)}' is directly calling DAO layer '${getClassName(targetClass)}'. Use DTO layer instead."
                Layer.FLOW -> "Controller '${getClassName(containingClass)}' is calling FLOW layer '${getClassName(targetClass)}'. Use DTO layer instead."
                else -> "Controller '${getClassName(containingClass)}' is calling '${getClassName(targetClass)}' which is not in the DTO layer. Controllers should only call DTOs."
            }

            createProblem(
                expression.methodExpression,
                message,
                holder,
                MoveToDTOQuickFix(getClassName(targetClass))
            )
        }
    }

    /**
     * Quick fix to extract business logic to API layer
     */
    private class ExtractBusinessLogicQuickFix : LocalQuickFix {
        override fun getName(): String = "Extract business logic to API layer"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            // Implementation would involve refactoring the code
            // For now, we'll just show a popup with guidance
            val message = """
                To fix this violation:
                1. Create a new service/API class
                2. Move the business logic methods to the API class
                3. Inject the API class into the controller
                4. Update the controller to call the API methods
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Business Logic Extraction", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }

    /**
     * Quick fix to move call to DTO layer
     */
    private class MoveToDTOQuickFix(private val targetClassName: String) : LocalQuickFix {
        override fun getName(): String = "Move call to DTO layer"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val message = """
                To fix this violation:
                1. Create a DTO class if it doesn't exist
                2. Move the call to '$targetClassName' from controller to DTO
                3. Have the controller call the DTO method instead
                4. Ensure proper data transformation in DTO
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Move to DTO Layer", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }
}
