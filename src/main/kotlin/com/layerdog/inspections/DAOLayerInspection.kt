package com.layerdog.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.layerdog.utils.LayerDetector
import com.layerdog.utils.LayerDetector.Layer

/**
 * Inspection for DAO layer violations.
 * DAO layer should only talk to database and related persistence technologies.
 */
class DAOLayerInspection : BaseLayerInspection() {

    override fun checkClass(psiClass: PsiClass, holder: ProblemsHolder) {
        val currentLayer = LayerDetector.detectLayer(psiClass)
        
        if (currentLayer != Layer.DAO) return

        // Check if DAO has business logic (it shouldn't)
        if (LayerDetector.hasBusinessLogic(psiClass)) {
            createProblem(
                psiClass.nameIdentifier ?: psiClass,
                "DAO '${getClassName(psiClass)}' contains business logic. DAOs should only handle data persistence operations.",
                holder,
                MoveBusinessLogicToAPIQuickFix()
            )
        }

        // Validate DAO patterns
        validateDAOPatterns(psiClass, holder)
    }

    override fun checkMethodCall(expression: PsiMethodCallExpression, holder: ProblemsHolder) {
        val containingClass = getContainingClass(expression) ?: return
        val currentLayer = LayerDetector.detectLayer(containingClass)
        
        if (currentLayer != Layer.DAO) return
        
        // Skip self calls and Java library calls
        if (isSelfCall(expression) || isJavaLibraryCall(resolveTargetClass(expression))) {
            return
        }

        val targetClass = resolveTargetClass(expression) ?: return
        val targetLayer = LayerDetector.detectLayer(targetClass)

        // Check if DAO is calling inappropriate layers
        if (!LayerDetector.isValidLayerCall(currentLayer, targetLayer)) {
            // Special handling for database-related calls
            if (LayerDetector.isDatabaseRelated(targetClass)) {
                return // This is fine - DAO can call database-related classes
            }

            val message = when (targetLayer) {
                Layer.CONTROLLER -> "DAO '${getClassName(containingClass)}' should not call Controller '${getClassName(targetClass)}'. Controllers should call DAOs, not the reverse."
                Layer.DTO -> "DAO '${getClassName(containingClass)}' should not call DTO '${getClassName(targetClass)}'. DTOs should be handled by upper layers."
                Layer.API -> "DAO '${getClassName(containingClass)}' should not call API '${getClassName(targetClass)}'. APIs should call DAOs, not the reverse."
                Layer.FLOW -> "DAO '${getClassName(containingClass)}' should not call FLOW '${getClassName(targetClass)}'. FLOW should orchestrate APIs, which call DAOs."
                Layer.DAO -> "DAO '${getClassName(containingClass)}' is calling another DAO '${getClassName(targetClass)}'. Consider consolidating or using composition patterns."
                else -> "DAO '${getClassName(containingClass)}' is calling '${getClassName(targetClass)}' which may not be database-related. DAOs should primarily interact with database technologies."
            }

            createProblem(
                expression.methodExpression,
                message,
                holder,
                RefactorDAOCallQuickFix(targetLayer, getClassName(targetClass))
            )
        }

        // Check for potential business logic in method calls
        checkForBusinessLogicInCalls(expression, containingClass, holder)
    }

    /**
     * Validates DAO patterns and structure
     */
    private fun validateDAOPatterns(psiClass: PsiClass, holder: ProblemsHolder) {
        val methods = psiClass.methods
        var hasDatabaseInteraction = false

        for (method in methods) {
            if (method.isConstructor) continue

            // Check if method has database interaction patterns
            if (hasDataAccessPatterns(method)) {
                hasDatabaseInteraction = true
                break
            }
        }

        if (!hasDatabaseInteraction) {
            createProblem(
                psiClass.nameIdentifier ?: psiClass,
                "DAO '${getClassName(psiClass)}' doesn't seem to have database interaction patterns. Consider if this belongs in the DAO layer.",
                holder,
                AddDatabaseInteractionQuickFix()
            )
        }
    }

    /**
     * Checks if method has database access patterns
     */
    private fun hasDataAccessPatterns(method: PsiMethod): Boolean {
        val methodName = method.name.lowercase()
        val body = method.body ?: return false

        // Check method name patterns
        val dataAccessPatterns = listOf(
            "find", "save", "delete", "update", "insert", "select",
            "create", "remove", "get", "set", "query", "execute"
        )

        val hasDataPattern = dataAccessPatterns.any { pattern -> 
            methodName.contains(pattern) 
        }

        // Check for database-related code in method body
        var hasDatabaseCode = false
        body.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val targetClass = resolveTargetClass(expression)
                if (targetClass != null && LayerDetector.isDatabaseRelated(targetClass)) {
                    hasDatabaseCode = true
                }
            }
        })

        return hasDataPattern || hasDatabaseCode
    }

    /**
     * Checks for business logic patterns in DAO method calls
     */
    private fun checkForBusinessLogicInCalls(
        expression: PsiMethodCallExpression,
        containingClass: PsiClass,
        holder: ProblemsHolder
    ) {
        val methodName = expression.methodExpression.referenceName ?: return

        // Look for business logic patterns
        val businessLogicPatterns = listOf(
            "calculate", "compute", "process", "validate", "transform",
            "analyze", "evaluate", "determine", "decide", "apply"
        )

        if (businessLogicPatterns.any { pattern -> 
                methodName.contains(pattern, ignoreCase = true) 
            }) {
            createProblem(
                expression.methodExpression,
                "DAO '${getClassName(containingClass)}' method call '$methodName' appears to involve business logic. DAOs should focus on data persistence.",
                holder,
                MoveBusinessLogicToAPIQuickFix()
            )
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
                To fix this business logic violation in DAO:
                1. Create or identify the appropriate API/Service class
                2. Move the business logic from DAO to API class
                3. Keep only data access operations in DAO (CRUD, queries)
                4. Have API call DAO for data operations after business processing
                
                DAO should focus on:
                - Database queries (SELECT, INSERT, UPDATE, DELETE)
                - Data mapping between objects and database
                - Transaction management
                - Connection handling
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Move Business Logic from DAO", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }

    /**
     * Quick fix to refactor DAO calls
     */
    private class RefactorDAOCallQuickFix(
        private val targetLayer: Layer,
        private val targetClassName: String
    ) : LocalQuickFix {
        override fun getName(): String = "Refactor DAO layer call"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val suggestion = when (targetLayer) {
                Layer.CONTROLLER -> "Remove the call from DAO to Controller. Controllers should call DAOs through APIs."
                Layer.DTO -> "Remove DTO handling from DAO. Let upper layers (API/Controller) handle DTOs."
                Layer.API -> "Remove the call from DAO to API. APIs should call DAOs, not the reverse."
                Layer.FLOW -> "Remove the call from DAO to FLOW. FLOW should orchestrate APIs, which call DAOs."
                Layer.DAO -> "Consider consolidating DAO operations or using proper DAO composition patterns."
                else -> "Ensure the call is to a database-related component or utility."
            }

            val message = """
                To fix this DAO layer violation:
                $suggestion
                
                Current call to '$targetClassName' (${targetLayer.name} layer) violates DAO principles.
                
                DAOs should:
                - Only interact with database and persistence technologies
                - Be called by API layer, not call other layers
                - Focus on data access operations
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Refactor DAO Call", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }

    /**
     * Quick fix to add database interaction
     */
    private class AddDatabaseInteractionQuickFix : LocalQuickFix {
        override fun getName(): String = "Add database interaction patterns"
        override fun getFamilyName(): String = "Layer Dog"

        override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: ProblemDescriptor) {
            val message = """
                To make this a proper DAO class:
                1. Add methods for database operations (find, save, delete, update)
                2. Use appropriate database technologies (JPA, Hibernate, MyBatis, JDBC)
                3. Handle data mapping between objects and database tables
                4. Focus on persistence operations only
                
                Common DAO patterns:
                - findById(id)
                - save(entity)
                - delete(entity)
                - findByXxx(criteria)
                - updateXxx(entity)
                
                If this class doesn't need database interaction, consider moving it to the appropriate layer.
            """.trimIndent()
            
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Layer Dog")
                .createNotification("Add Database Interaction", message, com.intellij.notification.NotificationType.INFORMATION)
                .notify(project)
        }
    }
}
