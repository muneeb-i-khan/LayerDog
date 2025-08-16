package com.layerdog.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.layerdog.utils.LayerDetector
import com.layerdog.utils.LayerDetector.Layer

/**
 * Inspection for FLOW layer violations.
 * FLOW should be used when API needs to call other APIs. FLOW orchestrates API calls.
 */
class FlowLayerInspection : BaseLayerInspection() {

    override fun checkClass(psiClass: PsiClass, holder: ProblemsHolder) {
        val currentLayer = LayerDetector.detectLayer(psiClass)
        
        if (currentLayer != Layer.FLOW) return

        // Check if FLOW class has proper orchestration patterns
        validateFlowOrchestrationPatterns(psiClass, holder)
    }

    override fun checkMethodCall(expression: PsiMethodCallExpression, holder: ProblemsHolder) {
        val containingClass = getContainingClass(expression) ?: return
        val currentLayer = LayerDetector.detectLayer(containingClass)
        
        if (currentLayer != Layer.FLOW) return
        
        // Skip self calls and Java library calls
        if (isSelfCall(expression) || isJavaLibraryCall(resolveTargetClass(expression))) {
            return
        }

        val targetClass = resolveTargetClass(expression) ?: return
        val targetLayer = LayerDetector.detectLayer(targetClass)

        // Validate FLOW layer calls
        if (!LayerDetector.isValidLayerCall(currentLayer, targetLayer)) {
            val message = when (targetLayer) {
                Layer.CONTROLLER -> "FLOW '${getClassName(containingClass)}' should not call Controller '${getClassName(targetClass)}'. FLOW orchestrates APIs, not Controllers."
                Layer.DTO -> "FLOW '${getClassName(containingClass)}' should not directly call DTO '${getClassName(targetClass)}'. FLOW should orchestrate APIs."
                Layer.DAO -> "FLOW '${getClassName(containingClass)}' should not directly call DAO '${getClassName(targetClass)}'. FLOW should call APIs, which then call DAOs."
                Layer.FLOW -> "FLOW '${getClassName(containingClass)}' is calling another FLOW '${getClassName(targetClass)}'. Consider consolidating or restructuring flows."
                else -> "FLOW '${getClassName(containingClass)}' should primarily call API layers to orchestrate business operations."
            }

            createProblem(
                expression.methodExpression,
                message,
                holder,
                RefactorFlowCallQuickFix(targetLayer, getClassName(targetClass))
            )
        }

        // Additional validation for proper API orchestration
        validateAPIOrchestration(expression, containingClass, targetClass, targetLayer, holder)
    }

    /**
     * Validates that FLOW class follows proper orchestration patterns
     */
    private fun validateFlowOrchestrationPatterns(psiClass: PsiClass, holder: ProblemsHolder) {
        val methods = psiClass.methods
        var hasAPICallPattern = false
        var hasOrchestrationPattern = false

        for (method in methods) {
            if (method.isConstructor) continue
            
            val body = method.body ?: continue
            
            // Check if method has multiple API calls (orchestration pattern)
            val apiCallCount = countAPICallsInMethod(body)
            if (apiCallCount >= 2) {
                hasOrchestrationPattern = true
            }
            if (apiCallCount >= 1) {
                hasAPICallPattern = true
            }
        }

        if (!hasAPICallPattern) {
            createProblem(
                psiClass.nameIdentifier ?: psiClass,
                "FLOW '${getClassName(psiClass)}' doesn't seem to orchestrate any API calls. FLOW should coordinate multiple API operations.",
                holder,
                AddAPIOrchestrationQuickFix()
            )
        } else if (!hasOrchestrationPattern) {
            createProblem(
                psiClass.nameIdentifier ?: psiClass,
                "FLOW '${getClassName(psiClass)}' only makes single API calls. Consider if this operation belongs in the API layer instead.",
                holder,
                ConsiderAPILayerQuickFix()
            )
        }
    }

    /**
     * Validates API orchestration patterns
     */
    private fun validateAPIOrchestration(
        expression: PsiMethodCallExpression,
        containingClass: PsiClass,
        targetClass: PsiClass,
        targetLayer: Layer,
        holder: ProblemsHolder
    ) {
        if (targetLayer == Layer.API) {
            // Check if this is part of a proper orchestration (multiple API calls in method)
            val containingMethod = getContainingMethod(expression)
            if (containingMethod != null) {
                val apiCallCount = countAPICallsInMethod(containingMethod.body)
                if (apiCallCount == 1) {
                    createProblem(
                        expression.methodExpression,
                        "FLOW '${getClassName(containingClass)}' makes only one API call to '${getClassName(targetClass)}'. Consider moving this logic to API layer.",
                        holder,
                        MoveToAPILayerQuickFix(getClassName(targetClass))
                    )
                }
            }
        }
    }

    /**
     * Counts the number of API calls in a method body
     */
    private fun countAPICallsInMethod(body: PsiCodeBlock?): Int {
        if (body == null) return 0
        
        var count = 0
        body.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                
                val targetClass = resolveTargetClass(expression)
                if (targetClass != null && LayerDetector.detectLayer(targetClass) == Layer.API) {
                    count++
                }
            }
        })
        return count
    }

    /**
     * Gets the containing method of an expression
     */
    private fun getContainingMethod(expression: PsiExpression): PsiMethod? {
        var element: PsiElement? = expression
        while (element != null) {
            if (element is PsiMethod) {
                return element
            }
            element = element.parent
        }
        return null
    }

    /**
     * Quick fix to refactor FLOW calls
     */
    private class RefactorFlowCallQuickFix(
        private val targetLayer: Layer,
        private val targetClassName: String
    ) : LocalQuickFix {
        override fun getName(): String = "Refactor FLOW layer call"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val suggestion = when (targetLayer) {
                Layer.CONTROLLER, Layer.DTO -> "FLOW should orchestrate APIs, not call Controllers or DTOs directly."
                Layer.DAO -> "FLOW should call APIs, which then handle DAO interactions."
                Layer.FLOW -> "Consider consolidating FLOW operations or restructuring the flow hierarchy."
                else -> "FLOW should primarily orchestrate API calls."
            }

            val message = """
                To fix this FLOW layer violation:
                $suggestion
                
                Current call to '$targetClassName' (${targetLayer.name} layer) should be refactored.
                FLOW classes should orchestrate multiple API operations.
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Refactor FLOW Call", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }

    /**
     * Quick fix to add API orchestration
     */
    private class AddAPIOrchestrationQuickFix : LocalQuickFix {
        override fun getName(): String = "Add API orchestration pattern"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val message = """
                To make this a proper FLOW class:
                1. Identify the APIs that need to be coordinated
                2. Add method calls to multiple API services
                3. Handle the orchestration logic (sequencing, error handling, data flow)
                4. Ensure this class coordinates complex operations across APIs
                
                Example pattern:
                - Call API1 to get data
                - Transform/validate data
                - Call API2 with processed data
                - Aggregate results and return
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Add API Orchestration", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }

    /**
     * Quick fix to consider moving to API layer
     */
    private class ConsiderAPILayerQuickFix : LocalQuickFix {
        override fun getName(): String = "Consider moving to API layer"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val message = """
                This FLOW class makes only single API calls. Consider:
                1. If this is simple business logic, move it to the API layer
                2. If this will grow to coordinate multiple APIs, enhance the orchestration
                3. FLOW should be used when you need to coordinate multiple API operations
                
                Move to API layer if:
                - Only calling one API service
                - Simple business logic
                - No complex orchestration needed
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Consider API Layer", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }

    /**
     * Quick fix to move simple operations to API layer
     */
    private class MoveToAPILayerQuickFix(private val apiClassName: String) : LocalQuickFix {
        override fun getName(): String = "Move simple operation to API layer"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val message = """
                To move this operation to API layer:
                1. Move the single API call logic to the '$apiClassName' class
                2. Remove this FLOW method or class if it becomes empty
                3. Update calling code to call API directly
                4. Reserve FLOW for complex multi-API orchestration
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Move to API Layer", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }
}
