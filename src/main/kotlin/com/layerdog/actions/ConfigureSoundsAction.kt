package com.layerdog.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.layerdog.rules.RuleConfigurationManager
import com.layerdog.sound.SoundPlayer
import java.awt.Desktop
import java.io.IOException

/**
 * Action to configure sound settings for LayerDog
 */
class ConfigureSoundsAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val soundPlayer = SoundPlayer.getInstance()
        
        // Check if external configuration exists
        if (!RuleConfigurationManager.hasExternalConfiguration()) {
            val result = Messages.showYesNoDialog(
                project,
                "No external configuration found. Would you like to initialize it first?",
                "LayerDog Sound Configuration",
                "Initialize Configuration",
                "Cancel",
                Messages.getQuestionIcon()
            )
            
            if (result == Messages.YES) {
                if (RuleConfigurationManager.initializeExternalConfiguration()) {
                    Messages.showInfoMessage(
                        project,
                        "Configuration initialized. Please run this action again to configure sounds.",
                        "Configuration Created"
                    )
                }
            }
            return
        }
        
        // Show sound configuration dialog
        showSoundConfigurationDialog(project, soundPlayer)
    }
    
    private fun showSoundConfigurationDialog(project: com.intellij.openapi.project.Project, soundPlayer: SoundPlayer) {
        val options = arrayOf(
            "Select Sound File",
            "Test Current Sound", 
            "Enable/Disable Sounds",
            "View Sound Configuration",
            "Cancel"
        )
        
        val choice = Messages.showDialog(
            project,
            "Configure LayerDog sound settings:\n\n" +
            "• Select Sound File: Choose a custom sound file (.wav, .au, .aiff)\n" +
            "• Test Current Sound: Play the currently configured sound\n" +
            "• Enable/Disable Sounds: Toggle sound effects on/off\n" +
            "• View Sound Configuration: Open the configuration file",
            "LayerDog Sound Configuration",
            options,
            0,
            Messages.getQuestionIcon()
        )
        
        when (choice) {
            0 -> selectSoundFile(project, soundPlayer)
            1 -> testCurrentSound(project, soundPlayer)
            2 -> showEnableDisableInfo(project)
            3 -> openConfigurationFile(project)
            // 4 is cancel, do nothing
        }
    }
    
    private fun selectSoundFile(project: com.intellij.openapi.project.Project, soundPlayer: SoundPlayer) {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
        descriptor.title = "Select Sound File"
        descriptor.description = "Choose an audio file for LayerDog violation sounds"
        
        // Set file type filters
        val extensions = soundPlayer.getSupportedExtensions()
        descriptor.withFileFilter { file: VirtualFile ->
            extensions.any { ext -> file.name.endsWith(".$ext", ignoreCase = true) }
        }
        
        val selectedFile = FileChooser.chooseFile(descriptor, project, null)
        
        if (selectedFile != null) {
            val filePath = selectedFile.path
            
            // Validate the sound file
            if (soundPlayer.isSoundFileSupported(filePath)) {
                // Test the sound
                try {
                    soundPlayer.testSound(filePath)
                    
                    Messages.showInfoMessage(
                        project,
                        """
                        Sound file selected: ${selectedFile.name}
                        Path: $filePath
                        
                        To use this sound:
                        1. Open the configuration file (next dialog)
                        2. Set "enabled": true
                        3. Set "soundFile": "$filePath"
                        4. Save the file
                        
                        The sound will be automatically reloaded.
                        """.trimIndent(),
                        "Sound File Selected"
                    )
                    
                    // Offer to open the configuration file
                    val openConfig = Messages.showYesNoDialog(
                        project,
                        "Would you like to open the configuration file to enable this sound?",
                        "Open Configuration",
                        "Open Configuration",
                        "Later",
                        Messages.getQuestionIcon()
                    )
                    
                    if (openConfig == Messages.YES) {
                        openConfigurationFile(project)
                    }
                    
                } catch (e: Exception) {
                    Messages.showErrorDialog(
                        project,
                        "Failed to play the selected sound file: ${e.message}\n\nPlease choose a different file.",
                        "Invalid Sound File"
                    )
                }
            } else {
                Messages.showErrorDialog(
                    project,
                    "The selected file is not a supported audio format.\n\n" +
                    "Supported formats: ${extensions.joinToString(", ")}",
                    "Unsupported File Format"
                )
            }
        }
    }
    
    private fun testCurrentSound(project: com.intellij.openapi.project.Project, soundPlayer: SoundPlayer) {
        try {
            val config = com.layerdog.rules.RuleEngine.getInstance().getSoundConfiguration()
            
            if (!config.enabled) {
                Messages.showInfoMessage(
                    project,
                    "Sounds are currently disabled in the configuration.\n\nTo enable sounds, set \"enabled\": true in the configuration file.",
                    "Sounds Disabled"
                )
                return
            }
            
            if (config.soundFile.isBlank()) {
                Messages.showInfoMessage(
                    project,
                    "No custom sound file configured. Playing system beep...",
                    "Testing System Sound"
                )
                soundPlayer.playSound(com.layerdog.rules.SoundEvent.TOOLTIP_HOVER)
            } else {
                Messages.showInfoMessage(
                    project,
                    "Testing configured sound file: ${config.soundFile}\n\nVolume: ${(config.volume * 100).toInt()}%",
                    "Testing Custom Sound"
                )
                soundPlayer.testSound(config.soundFile, config.volume)
            }
            
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Failed to test sound: ${e.message}",
                "Sound Test Failed"
            )
        }
    }
    
    private fun showEnableDisableInfo(project: com.intellij.openapi.project.Project) {
        val config = com.layerdog.rules.RuleEngine.getInstance().getSoundConfiguration()
        val status = if (config.enabled) "ENABLED" else "DISABLED"
        
        Messages.showInfoMessage(
            project,
            """
            Current sound status: $status
            
            To change this setting:
            1. Open the configuration file (layer-rules.json)
            2. Find the "soundConfiguration" section
            3. Change "enabled": ${!config.enabled}
            4. Save the file
            
            Settings will be automatically reloaded.
            """.trimIndent(),
            "Sound Status: $status"
        )
    }
    
    private fun openConfigurationFile(project: com.intellij.openapi.project.Project) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(RuleConfigurationManager.getExternalConfigurationPath().toFile())
            } else {
                Messages.showInfoMessage(
                    project,
                    "Configuration file location:\n${RuleConfigurationManager.getExternalConfigurationPath()}",
                    "Configuration File Location"
                )
            }
        } catch (e: IOException) {
            Messages.showErrorDialog(
                project,
                "Failed to open configuration file: ${e.message}\n\nLocation: ${RuleConfigurationManager.getExternalConfigurationPath()}",
                "Cannot Open File"
            )
        }
    }
}
