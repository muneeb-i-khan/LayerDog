package com.layerdog.rules

import com.fasterxml.jackson.annotation.JsonProperty
import com.layerdog.utils.LayerDetector.Layer

/**
 * Root configuration model for LayerDog rules
 */
data class LayerRuleConfiguration(
    val version: String,
    val description: String,
    val layers: Map<String, LayerDefinition>,
    val globalRules: GlobalRules,
    val violationMessages: ViolationMessages,
    val quickFixes: Map<String, QuickFixDefinition>
)

/**
 * Definition of a specific architectural layer
 */
data class LayerDefinition(
    val name: String,
    val description: String,
    val detection: LayerDetection,
    val allowedCalls: List<String>,
    val rules: LayerSpecificRules = LayerSpecificRules()
)

/**
 * Rules for detecting which layer a class belongs to
 */
data class LayerDetection(
    val classNamePatterns: ClassNamePatterns,
    val packagePatterns: PackagePatterns,
    val annotations: List<String>
)

/**
 * Patterns for matching class names
 */
data class ClassNamePatterns(
    val suffixes: List<String>,
    val prefixes: List<String>,
    val contains: List<String>
)

/**
 * Patterns for matching package names
 */
data class PackagePatterns(
    val contains: List<String>,
    val exact: List<String>
)

/**
 * Layer-specific validation rules
 */
data class LayerSpecificRules(
    val businessLogicProhibited: Boolean = false,
    val businessLogicMessage: String = "",
    val businessLogicPatterns: List<String> = emptyList(),
    val directApiCallsProhibited: Boolean = false,
    val directApiCallMessage: String = "",
    val directDataAccessPatterns: List<String> = emptyList(),
    val directDataAccessIndicators: List<String> = emptyList()
)

/**
 * Global rules that apply across all layers
 */
data class GlobalRules(
    val businessLogicDetection: BusinessLogicDetection,
    val javaLibraryPackages: List<String>,
    val databaseRelatedPatterns: DatabaseRelatedPatterns,
    val soundConfiguration: SoundConfiguration = SoundConfiguration()
)

/**
 * Configuration for detecting business logic patterns
 */
data class BusinessLogicDetection(
    val complexLogicThreshold: ComplexLogicThreshold,
    val calculationLogicThreshold: CalculationLogicThreshold
)

/**
 * Thresholds for complex logic detection
 */
data class ComplexLogicThreshold(
    val ifStatements: Int,
    val switchStatements: Int,
    val loopStatements: Int
)

/**
 * Thresholds for calculation logic detection
 */
data class CalculationLogicThreshold(
    val assignmentExpressions: Int,
    val binaryExpressions: Int
)

/**
 * Patterns for identifying database-related classes
 */
data class DatabaseRelatedPatterns(
    val classNames: List<String>,
    val packages: List<String>
)

/**
 * Pre-defined violation messages for different scenarios
 */
data class ViolationMessages(
    val invalidLayerCall: Map<String, String>
)

/**
 * Definition of a quick fix suggestion
 */
data class QuickFixDefinition(
    val name: String,
    val description: String
)

/**
 * Context information for rule evaluation
 */
data class RuleContext(
    val fromLayer: Layer,
    val toLayer: Layer,
    val fromClass: String,
    val toClass: String,
    val methodName: String? = null
)

/**
 * Result of a rule evaluation
 */
data class RuleViolation(
    val message: String,
    val quickFixKey: String? = null,
    val severity: ViolationSeverity = ViolationSeverity.WARNING
)

/**
 * Severity levels for violations
 */
enum class ViolationSeverity {
    ERROR,
    WARNING,
    INFO
}

/**
 * Configuration for sound effects on violations
 */
data class SoundConfiguration(
    val enabled: Boolean = false,
    val soundFile: String = "",
    val volume: Float = 0.7f,
    val playOnHover: Boolean = true,
    val playOnInspection: Boolean = false,
    val debounceMs: Long = 1000L // Minimum time between sound plays to avoid spam
)

/**
 * Sound event types
 */
enum class SoundEvent {
    TOOLTIP_HOVER,
    INSPECTION_FOUND,
    VIOLATION_CREATED
}
