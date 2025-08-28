package com.layerdog.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.layerdog.rules.RuleConfigurationManager
import java.awt.Desktop
import java.io.IOException

/**
 * Action to initialize external rule configuration for LayerDog
 */
class InitializeRulesAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        if (RuleConfigurationManager.hasExternalConfiguration()) {
            val result = Messages.showYesNoDialog(
                project,
                "External configuration already exists at:\n${RuleConfigurationManager.getExternalConfigurationPath()}\n\nWould you like to reset it to defaults?",
                "LayerDog Configuration",
                "Reset to Defaults",
                "Open Existing",
                Messages.getQuestionIcon()
            )
            
            if (result == Messages.YES) {
                // Reset to defaults
                if (RuleConfigurationManager.resetToDefaults()) {
                    Messages.showInfoMessage(
                        project,
                        "Configuration has been reset to defaults at:\n${RuleConfigurationManager.getExternalConfigurationPath()}",
                        "Configuration Reset"
                    )
                    openConfigurationFile()
                } else {
                    Messages.showErrorDialog(
                        project,
                        "Failed to reset configuration to defaults.",
                        "Error"
                    )
                }
            } else {
                // Open existing configuration
                openConfigurationFile()
            }
        } else {
            // Initialize new configuration
            if (RuleConfigurationManager.initializeExternalConfiguration()) {
                Messages.showInfoMessage(
                    project,
                    """
                    LayerDog external configuration has been created at:
                    ${RuleConfigurationManager.getExternalConfigurationPath()}
                    
                    You can now customize the rules and they will be automatically reloaded when the file is saved.
                    
                    The file will be opened in your default editor.
                    """.trimIndent(),
                    "Configuration Initialized"
                )
                openConfigurationFile()
            } else {
                Messages.showErrorDialog(
                    project,
                    "Failed to initialize external configuration.",
                    "Error"
                )
            }
        }
    }
    
    private fun openConfigurationFile() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(RuleConfigurationManager.getExternalConfigurationPath().toFile())
            }
        } catch (e: IOException) {
            // Silently ignore if we can't open the file
        }
    }
}
