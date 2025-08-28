package com.layerdog.rules

import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.nio.file.*

/**
 * Manages external rule configuration files for LayerDog
 */
class RuleConfigurationManager {
    
    companion object {
        private val LOG = Logger.getInstance(RuleConfigurationManager::class.java)
        
        /**
         * Creates the external configuration directory and copies default rules
         */
        fun initializeExternalConfiguration(): Boolean {
            return try {
                val configDir = getExternalConfigDirectory()
                
                // Create directory if it doesn't exist
                if (!Files.exists(configDir)) {
                    Files.createDirectories(configDir)
                    LOG.info("Created LayerDog configuration directory: $configDir")
                }
                
                val configFile = configDir.resolve("layer-rules.json")
                
                // Copy default configuration if it doesn't exist
                if (!Files.exists(configFile)) {
                    copyDefaultConfiguration(configFile)
                    LOG.info("Created external configuration file: $configFile")
                }
                
                true
            } catch (e: Exception) {
                LOG.error("Failed to initialize external configuration", e)
                false
            }
        }
        
        /**
         * Gets the path to the external configuration directory
         */
        private fun getExternalConfigDirectory(): Path {
            val userHome = System.getProperty("user.home")
            return Paths.get(userHome, ".layerdog")
        }
        
        /**
         * Copies the default configuration from resources to external file
         */
        private fun copyDefaultConfiguration(targetFile: Path) {
            val resourceStream = RuleConfigurationManager::class.java.getResourceAsStream("/layer-rules.json")
            if (resourceStream != null) {
                Files.copy(resourceStream, targetFile)
            } else {
                // Create a basic configuration if resource is not found
                createBasicConfiguration(targetFile)
            }
        }
        
        /**
         * Creates a basic configuration file if resource is not available
         */
        private fun createBasicConfiguration(targetFile: Path) {
            val basicConfig = """
            {
              "version": "1.0",
              "description": "LayerDog Architecture Rules Configuration",
              "layers": {
                "CONTROLLER": {
                  "name": "Controller",
                  "description": "Should have no business logic and only call DTO layer",
                  "detection": {
                    "classNamePatterns": {
                      "suffixes": ["Controller", "Resource", "Endpoint"],
                      "prefixes": [],
                      "contains": []
                    },
                    "packagePatterns": {
                      "contains": ["controller", "web", "rest"],
                      "exact": []
                    },
                    "annotations": ["Controller", "RestController", "Resource"]
                  },
                  "allowedCalls": ["DTO"],
                  "rules": {
                    "businessLogicProhibited": true,
                    "businessLogicMessage": "Controller '{className}' contains business logic. Controllers should delegate to DTOs."
                  }
                },
                "DTO": {
                  "name": "DTO",
                  "description": "Should call only API layer and do validations and conversions",
                  "detection": {
                    "classNamePatterns": {
                      "suffixes": ["DTO", "Dto", "Request", "Response", "Model"],
                      "prefixes": [],
                      "contains": []
                    },
                    "packagePatterns": {
                      "contains": ["dto", "model"],
                      "exact": []
                    },
                    "annotations": []
                  },
                  "allowedCalls": ["API"],
                  "rules": {
                    "businessLogicProhibited": true,
                    "businessLogicMessage": "DTO '{className}' contains business logic. DTOs should only handle validation and conversion."
                  }
                },
                "API": {
                  "name": "API", 
                  "description": "Contains business logic, calls DAO layer",
                  "detection": {
                    "classNamePatterns": {
                      "suffixes": ["Service", "ServiceImpl", "Manager"],
                      "prefixes": [],
                      "contains": []
                    },
                    "packagePatterns": {
                      "contains": ["service", "business", "logic"],
                      "exact": []
                    },
                    "annotations": ["Service", "Component"]
                  },
                  "allowedCalls": ["DAO", "FLOW"],
                  "rules": {}
                }
              },
              "globalRules": {
                "businessLogicDetection": {
                  "complexLogicThreshold": {
                    "ifStatements": 2,
                    "switchStatements": 2,
                    "loopStatements": 2
                  },
                  "calculationLogicThreshold": {
                    "assignmentExpressions": 3,
                    "binaryExpressions": 5
                  }
                },
                "javaLibraryPackages": ["java.", "javax.", "com.sun."],
                "databaseRelatedPatterns": {
                  "classNames": ["Connection", "DataSource", "Driver"],
                  "packages": ["java.sql", "javax.sql", "hibernate"]
                },
                "soundConfiguration": {
                  "enabled": false,
                  "soundFile": "",
                  "volume": 0.7,
                  "playOnHover": true,
                  "playOnInspection": false,
                  "debounceMs": 1000
                }
              },
              "violationMessages": {
                "invalidLayerCall": {
                  "GENERIC": "{fromLayer} '{fromClass}' is calling '{toClass}' which violates layer architecture."
                }
              },
              "quickFixes": {
                "extractBusinessLogic": {
                  "name": "Extract business logic to API layer",
                  "description": "To fix this violation, move the business logic to the appropriate API layer."
                }
              }
            }
            """.trimIndent()
            
            Files.write(targetFile, basicConfig.toByteArray())
        }
        
        /**
         * Gets the path to the external configuration file
         */
        fun getExternalConfigurationPath(): Path {
            return getExternalConfigDirectory().resolve("layer-rules.json")
        }
        
        /**
         * Checks if external configuration exists
         */
        fun hasExternalConfiguration(): Boolean {
            return Files.exists(getExternalConfigurationPath())
        }
        
        /**
         * Resets external configuration to defaults
         */
        fun resetToDefaults(): Boolean {
            return try {
                val configFile = getExternalConfigurationPath()
                Files.deleteIfExists(configFile)
                copyDefaultConfiguration(configFile)
                true
            } catch (e: Exception) {
                LOG.error("Failed to reset configuration to defaults", e)
                false
            }
        }
    }
}
