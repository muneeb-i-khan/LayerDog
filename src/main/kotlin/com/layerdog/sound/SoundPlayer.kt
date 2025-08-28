package com.layerdog.sound

import com.intellij.openapi.diagnostic.Logger
import com.layerdog.rules.RuleEngine
import com.layerdog.rules.SoundEvent
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.*

/**
 * Utility class for playing sounds when LayerDog violations are detected or hovered
 */
class SoundPlayer {
    
    companion object {
        private val LOG = Logger.getInstance(SoundPlayer::class.java)
        
        @Volatile
        private var instance: SoundPlayer? = null
        
        fun getInstance(): SoundPlayer {
            return instance ?: synchronized(this) {
                instance ?: SoundPlayer().also { instance = it }
            }
        }
    }
    
    private val ruleEngine = RuleEngine.getInstance()
    private val lastPlayTime = ConcurrentHashMap<SoundEvent, Long>()
    
    /**
     * Plays sound for a specific event type if configured
     */
    fun playSound(event: SoundEvent) {
        try {
            val config = ruleEngine.getSoundConfiguration()
            
            // Check if sounds are enabled
            if (!config.enabled) return
            
            // Check event-specific configuration
            when (event) {
                SoundEvent.TOOLTIP_HOVER -> if (!config.playOnHover) return
                SoundEvent.INSPECTION_FOUND, SoundEvent.VIOLATION_CREATED -> if (!config.playOnInspection) return
            }
            
            // Check debounce time
            val now = System.currentTimeMillis()
            val lastPlay = lastPlayTime[event] ?: 0
            if (now - lastPlay < config.debounceMs) {
                return // Too soon to play again
            }
            
            // Play the sound
            if (config.soundFile.isNotBlank()) {
                playAudioFile(config.soundFile, config.volume)
                lastPlayTime[event] = now
            } else {
                // Play default system sound if no custom sound is configured
                playSystemSound()
                lastPlayTime[event] = now
            }
            
        } catch (e: Exception) {
            LOG.warn("Failed to play LayerDog sound", e)
        }
    }
    
    /**
     * Plays a custom audio file
     */
    private fun playAudioFile(soundFilePath: String, volume: Float) {
        try {
            val soundFile = File(soundFilePath)
            
            if (!soundFile.exists()) {
                LOG.warn("Sound file not found: $soundFilePath")
                return
            }
            
            val audioInputStream = AudioSystem.getAudioInputStream(soundFile)
            val clip = AudioSystem.getClip()
            clip.open(audioInputStream)
            
            // Set volume
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val range = gainControl.maximum - gainControl.minimum
                val gain = (range * volume) + gainControl.minimum
                gainControl.value = gain
            }
            
            // Play the sound
            clip.start()
            
            // Clean up when sound finishes (in a separate thread to avoid blocking)
            Thread {
                try {
                    Thread.sleep(clip.microsecondLength / 1000)
                    clip.close()
                    audioInputStream.close()
                } catch (e: Exception) {
                    LOG.warn("Error cleaning up audio resources", e)
                }
            }.start()
            
        } catch (e: Exception) {
            LOG.warn("Failed to play custom sound file: $soundFilePath", e)
            // Fallback to system sound
            playSystemSound()
        }
    }
    
    /**
     * Plays the system default beep sound as fallback
     */
    private fun playSystemSound() {
        try {
            java.awt.Toolkit.getDefaultToolkit().beep()
        } catch (e: Exception) {
            LOG.warn("Failed to play system beep", e)
        }
    }
    
    /**
     * Validates if a sound file is supported
     */
    fun isSoundFileSupported(filePath: String): Boolean {
        try {
            val file = File(filePath)
            if (!file.exists()) return false
            
            val audioInputStream = AudioSystem.getAudioInputStream(file)
            audioInputStream.close()
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Gets supported audio file extensions
     */
    fun getSupportedExtensions(): List<String> {
        return listOf("wav", "au", "aiff", "aif")
    }
    
    /**
     * Tests playing a sound file
     */
    fun testSound(soundFilePath: String, volume: Float = 0.7f) {
        try {
            playAudioFile(soundFilePath, volume)
        } catch (e: Exception) {
            LOG.error("Failed to test sound file: $soundFilePath", e)
            throw e
        }
    }
}
