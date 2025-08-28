package com.layerdog.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.layerdog.utils.LayerDetector
import com.layerdog.utils.LayerDetector.Layer
import com.layerdog.rules.RuleEngine

/**
 * Inspection for Controller layer violations.
 * Controllers should have no business logic and should only call DTO layer.
 */
class ControllerLayerInspection : BaseLayerInspection() {
    
    private val ruleEngine = RuleEngine.getInstance()

    override fun checkClass(psiClass: PsiClass, holder: ProblemsHolder) {
        val currentLayer = LayerDetector.detectLayer(psiClass)
        
        if (currentLayer != Layer.CONTROLLER) return

        // Check if controller has business logic
        if (LayerDetector.hasBusinessLogic(psiClass)) {
            val message = ruleEngine.getBusinessLogicMessage(currentLayer, getClassName(psiClass))
            createProblem(
                psiClass.nameIdentifier ?: psiClass,
                message,
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
            val message = ruleEngine.getViolationMessage(
                currentLayer, 
                targetLayer, 
                getClassName(containingClass), 
                getClassName(targetClass)
            )

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
        private val ruleEngine = RuleEngine.getInstance()
        
        override fun getName(): String {
            return ruleEngine.getQuickFix("extractBusinessLogic")?.name 
                ?: "Extract business logic to API layer"
        }
        
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val quickFix = ruleEngine.getQuickFix("extractBusinessLogic")
            val message = quickFix?.description ?: """
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
        private val ruleEngine = RuleEngine.getInstance()
        
        override fun getName(): String {
            return ruleEngine.getQuickFix("moveToDTO")?.name ?: "Move call to DTO layer"
        }
        
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val quickFix = ruleEngine.getQuickFix("moveToDTO")
            val message = quickFix?.description?.replace("{targetClass}", targetClassName) ?: """
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
