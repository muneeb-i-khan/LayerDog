package com.layerdog.rules

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.layerdog.utils.LayerDetector.Layer
import java.io.IOException
import java.io.InputStream
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Engine responsible for loading, parsing, and evaluating LayerDog rules
 */
class RuleEngine {
    
    companion object {
        private val LOG = Logger.getInstance(RuleEngine::class.java)
        
        @Volatile
        private var instance: RuleEngine? = null
        
        fun getInstance(): RuleEngine {
            return instance ?: synchronized(this) {
                instance ?: RuleEngine().also { instance = it }
            }
        }
    }
    
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private var configuration: LayerRuleConfiguration? = null
    private val layerCache = ConcurrentHashMap<String, Layer>()
    private var configFilePath: Path? = null
    private var fileWatcher: WatchService? = null
    
    init {
        loadConfiguration()
        setupFileWatcher()
    }
    
    /**
     * Loads rule configuration from the default resource file
     */
    private fun loadConfiguration() {
        try {
            // First try to load from external file (for runtime updates)
            val externalConfigPath = getExternalConfigPath()
            if (Files.exists(externalConfigPath)) {
                configuration = objectMapper.readValue(Files.newInputStream(externalConfigPath), LayerRuleConfiguration::class.java)
                configFilePath = externalConfigPath
                LOG.info("Loaded LayerDog rules from external config: $externalConfigPath")
            } else {
                // Fall back to resource file
                loadFromResource()
            }
        } catch (e: Exception) {
            LOG.error("Failed to load rule configuration", e)
            // Try to load from resource as fallback
            loadFromResource()
        }
    }
    
    private fun loadFromResource() {
        try {
            val resourceStream = javaClass.getResourceAsStream("/layer-rules.json")
            if (resourceStream != null) {
                configuration = objectMapper.readValue(resourceStream, LayerRuleConfiguration::class.java)
                LOG.info("Loaded LayerDog rules from resource file")
            } else {
                LOG.error("Could not find layer-rules.json resource file")
            }
        } catch (e: Exception) {
            LOG.error("Failed to load rule configuration from resource", e)
        }
    }
    
    /**
     * Gets the external config file path (in user's home directory)
     */
    private fun getExternalConfigPath(): Path {
        val userHome = System.getProperty("user.home")
        return Paths.get(userHome, ".layerdog", "layer-rules.json")
    }
    
    /**
     * Sets up file watcher for runtime configuration updates
     */
    private fun setupFileWatcher() {
        try {
            fileWatcher = FileSystems.getDefault().newWatchService()
            val configPath = configFilePath ?: getExternalConfigPath()
            
            if (Files.exists(configPath)) {
                val parentDir = configPath.parent
                parentDir.register(fileWatcher, StandardWatchEventKinds.ENTRY_MODIFY)
                
                // Start watching in a separate thread
                Thread {
                    watchForChanges()
                }.start()
            }
        } catch (e: Exception) {
            LOG.warn("Failed to setup file watcher for rule configuration", e)
        }
    }
    
    /**
     * Watches for configuration file changes and reloads
     */
    private fun watchForChanges() {
        try {
            while (true) {
                val key = fileWatcher?.take() ?: break
                
                for (event in key.pollEvents()) {
                    val fileName = event.context().toString()
                    if (fileName == "layer-rules.json") {
                        LOG.info("Rule configuration file changed, reloading...")
                        Thread.sleep(100) // Brief delay to ensure file write is complete
                        loadConfiguration()
                        clearCaches()
                    }
                }
                
                key.reset()
            }
        } catch (e: InterruptedException) {
            LOG.info("File watcher interrupted")
        } catch (e: Exception) {
            LOG.error("Error in file watcher", e)
        }
    }
    
    /**
     * Clears all caches when configuration is reloaded
     */
    private fun clearCaches() {
        layerCache.clear()
    }
    
    /**
     * Detects which layer a class belongs to using configured rules
     */
    fun detectLayer(psiClass: PsiClass): Layer {
        val className = psiClass.name ?: return Layer.UNKNOWN
        val qualifiedName = psiClass.qualifiedName ?: className
        
        // Check cache first
        layerCache[qualifiedName]?.let { return it }
        
        val config = configuration ?: return Layer.UNKNOWN
        val packageName = getPackageName(psiClass)
        
        // Check each layer definition
        for ((layerKey, layerDef) in config.layers) {
            if (matchesLayerDetection(className, packageName, psiClass.annotations, layerDef.detection)) {
                val layer = Layer.valueOf(layerKey)
                layerCache[qualifiedName] = layer
                return layer
            }
        }
        
        val unknownLayer = Layer.UNKNOWN
        layerCache[qualifiedName] = unknownLayer
        return unknownLayer
    }
    
    /**
     * Checks if a class matches the detection criteria for a layer
     */
    private fun matchesLayerDetection(
        className: String,
        packageName: String,
        annotations: Array<PsiAnnotation>,
        detection: LayerDetection
    ): Boolean {
        // Check class name patterns
        val classPatterns = detection.classNamePatterns
        if (classPatterns.suffixes.any { className.endsWith(it, ignoreCase = true) } ||
            classPatterns.prefixes.any { className.startsWith(it, ignoreCase = true) } ||
            classPatterns.contains.any { className.contains(it, ignoreCase = true) }) {
            return true
        }
        
        // Check package patterns
        val packagePatterns = detection.packagePatterns
        if (packagePatterns.contains.any { packageName.contains(it, ignoreCase = true) } ||
            packagePatterns.exact.any { packageName.equals(it, ignoreCase = true) }) {
            return true
        }
        
        // Check annotations
        if (detection.annotations.isNotEmpty()) {
            for (annotation in annotations) {
                val annotationName = annotation.qualifiedName ?: continue
                if (detection.annotations.any { 
                    annotationName.contains(it, ignoreCase = true) || 
                    annotationName.equals(it, ignoreCase = true) 
                }) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Validates if a layer call is allowed according to the rules
     */
    fun isValidLayerCall(fromLayer: Layer, toLayer: Layer): Boolean {
        val config = configuration ?: return true
        
        val layerDef = config.layers[fromLayer.name] ?: return true
        return layerDef.allowedCalls.contains(toLayer.name) || toLayer == Layer.UNKNOWN
    }
    
    /**
     * Checks if a class contains business logic according to the rules
     */
    fun hasBusinessLogic(psiClass: PsiClass): Boolean {
        val config = configuration ?: return false
        val layer = detectLayer(psiClass)
        val layerDef = config.layers[layer.name] ?: return false
        
        if (!layerDef.rules.businessLogicProhibited) return false
        
        val methods = psiClass.methods
        val thresholds = config.globalRules.businessLogicDetection
        
        for (method in methods) {
            if (hasComplexLogic(method, thresholds.complexLogicThreshold) ||
                hasCalculationLogic(method, thresholds.calculationLogicThreshold)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Checks if a method name contains business logic patterns for a specific layer
     */
    fun hasBusinessLogicPattern(methodName: String, layer: Layer): Boolean {
        val config = configuration ?: return false
        val layerDef = config.layers[layer.name] ?: return false
        
        return layerDef.rules.businessLogicPatterns.any { pattern ->
            methodName.contains(pattern, ignoreCase = true)
        }
    }
    
    /**
     * Checks if a method name indicates direct data access patterns
     */
    fun hasDirectDataAccessPattern(methodName: String, layer: Layer): Boolean {
        val config = configuration ?: return false
        val layerDef = config.layers[layer.name] ?: return false
        
        return layerDef.rules.directDataAccessPatterns.any { pattern ->
            methodName.contains(pattern, ignoreCase = true) &&
            layerDef.rules.directDataAccessIndicators.any { indicator ->
                methodName.contains(indicator, ignoreCase = true)
            }
        }
    }
    
    /**
     * Gets violation message for invalid layer calls
     */
    fun getViolationMessage(fromLayer: Layer, toLayer: Layer, fromClass: String, toClass: String): String {
        val config = configuration ?: return "Layer violation detected"
        
        val messageKey = "${fromLayer.name}_TO_${toLayer.name}"
        val message = config.violationMessages.invalidLayerCall[messageKey]
            ?: config.violationMessages.invalidLayerCall["GENERIC"]
            ?: "Layer violation detected"
            
        return message
            .replace("{fromLayer}", fromLayer.name)
            .replace("{toLayer}", toLayer.name)
            .replace("{fromClass}", fromClass)
            .replace("{toClass}", toClass)
            .replace("{allowedLayers}", getAllowedLayersForLayer(fromLayer).joinToString(", "))
    }
    
    /**
     * Gets business logic violation message for a layer
     */
    fun getBusinessLogicMessage(layer: Layer, className: String): String {
        val config = configuration ?: return "Business logic violation detected"
        val layerDef = config.layers[layer.name] ?: return "Business logic violation detected"
        
        return layerDef.rules.businessLogicMessage.replace("{className}", className)
    }
    
    /**
     * Gets quick fix definition by key
     */
    fun getQuickFix(key: String): QuickFixDefinition? {
        return configuration?.quickFixes?.get(key)
    }
    
    /**
     * Gets the sound configuration
     */
    fun getSoundConfiguration(): SoundConfiguration {
        val config = configuration ?: return SoundConfiguration()
        return config.globalRules.soundConfiguration
    }
    
    /**
     * Checks if a class is Java library related
     */
    fun isJavaLibraryCall(targetClass: PsiClass?): Boolean {
        if (targetClass == null) return false
        val qualifiedName = targetClass.qualifiedName ?: return false
        val config = configuration ?: return false
        
        return config.globalRules.javaLibraryPackages.any { 
            qualifiedName.startsWith(it) 
        }
    }
    
    /**
     * Checks if a class is database related
     */
    fun isDatabaseRelated(psiClass: PsiClass): Boolean {
        val config = configuration ?: return false
        val className = psiClass.name ?: return false
        val packageName = getPackageName(psiClass)
        val patterns = config.globalRules.databaseRelatedPatterns
        
        return patterns.classNames.any { className.contains(it, ignoreCase = true) } ||
                patterns.packages.any { packageName.contains(it, ignoreCase = true) }
    }
    
    private fun getAllowedLayersForLayer(layer: Layer): List<String> {
        val config = configuration ?: return emptyList()
        return config.layers[layer.name]?.allowedCalls ?: emptyList()
    }
    
    private fun hasComplexLogic(method: PsiMethod, threshold: ComplexLogicThreshold): Boolean {
        val body = method.body ?: return false
        
        val ifStatements = countStatements(body, PsiIfStatement::class.java)
        val switchStatements = countStatements(body, PsiSwitchStatement::class.java)
        val loops = countStatements(body, PsiLoopStatement::class.java)
        
        return ifStatements + switchStatements + loops > threshold.ifStatements + threshold.switchStatements + threshold.loopStatements
    }
    
    private fun hasCalculationLogic(method: PsiMethod, threshold: CalculationLogicThreshold): Boolean {
        val body = method.body ?: return false
        
        val assignments = countStatements(body, PsiAssignmentExpression::class.java)
        val binaryExpressions = countStatements(body, PsiBinaryExpression::class.java)
        
        return assignments > threshold.assignmentExpressions || binaryExpressions > threshold.binaryExpressions
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
    
    private fun getPackageName(psiClass: PsiClass): String {
        val containingFile = psiClass.containingFile as? PsiJavaFile
        return containingFile?.packageName ?: ""
    }
}
