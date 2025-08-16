package com.layerdog.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.layerdog.utils.LayerDetector

/**
 * Base class for all layer inspection implementations
 */
abstract class BaseLayerInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitClass(aClass: PsiClass) {
                super.visitClass(aClass)
                checkClass(aClass, holder)
            }

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                checkMethodCall(expression, holder)
            }
        }
    }

    /**
     * Check the entire class for layer violations
     */
    protected open fun checkClass(psiClass: PsiClass, holder: ProblemsHolder) {
        // Override in subclasses to check class-level violations
    }

    /**
     * Check individual method calls for layer violations
     */
    protected open fun checkMethodCall(expression: PsiMethodCallExpression, holder: ProblemsHolder) {
        // Override in subclasses to check method call violations
    }

    /**
     * Helper method to get the containing class of a method call
     */
    protected fun getContainingClass(expression: PsiMethodCallExpression): PsiClass? {
        var element: PsiElement? = expression
        while (element != null) {
            if (element is PsiClass) {
                return element
            }
            element = element.parent
        }
        return null
    }

    /**
     * Helper method to resolve the target class of a method call
     */
    protected fun resolveTargetClass(expression: PsiMethodCallExpression): PsiClass? {
        val method = expression.resolveMethod() ?: return null
        return method.containingClass
    }

    /**
     * Helper method to create problem descriptors with quick fixes
     */
    protected fun createProblem(
        element: PsiElement,
        message: String,
        holder: ProblemsHolder,
        vararg quickFixes: LocalQuickFix
    ) {
        holder.registerProblem(
            element,
            message,
            ProblemHighlightType.WARNING,
            *quickFixes
        )
    }

    /**
     * Helper method to get class name safely
     */
    protected fun getClassName(psiClass: PsiClass?): String {
        return psiClass?.name ?: "Unknown"
    }

    /**
     * Helper method to check if a method call is within the same class
     */
    protected fun isSelfCall(expression: PsiMethodCallExpression): Boolean {
        val containingClass = getContainingClass(expression)
        val targetClass = resolveTargetClass(expression)
        return containingClass == targetClass
    }

    /**
     * Helper method to check if method call is to a standard Java library
     */
    protected fun isJavaLibraryCall(targetClass: PsiClass?): Boolean {
        if (targetClass == null) return false
        val qualifiedName = targetClass.qualifiedName ?: return false
        
        return qualifiedName.startsWith("java.") ||
                qualifiedName.startsWith("javax.") ||
                qualifiedName.startsWith("com.sun.") ||
                qualifiedName.startsWith("org.w3c.") ||
                qualifiedName.startsWith("org.xml.")
    }
}
