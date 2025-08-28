package com.layerdog.utils

import com.intellij.psi.*
import com.layerdog.rules.RuleEngine

/**
 * Utility class to detect which architectural layer a class belongs to
 * based on configurable rules from the rule engine.
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

    private val ruleEngine = RuleEngine.getInstance()

    /**
     * Determines the architectural layer of a PsiClass using the rule engine
     */
    fun detectLayer(psiClass: PsiClass): Layer {
        return ruleEngine.detectLayer(psiClass)
    }

    /**
     * Checks if a method call violates layer rules using the rule engine
     */
    fun isValidLayerCall(fromLayer: Layer, toLayer: Layer): Boolean {
        return ruleEngine.isValidLayerCall(fromLayer, toLayer)
    }

    /**
     * Checks if a class is likely to be a database-related class using the rule engine
     */
    fun isDatabaseRelated(psiClass: PsiClass): Boolean {
        return ruleEngine.isDatabaseRelated(psiClass)
    }

    /**
     * Checks if a class contains business logic patterns using the rule engine
     */
    fun hasBusinessLogic(psiClass: PsiClass): Boolean {
        return ruleEngine.hasBusinessLogic(psiClass)
    }
}
