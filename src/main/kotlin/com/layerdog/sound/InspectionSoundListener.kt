package com.layerdog.sound

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiDocumentManager
import com.layerdog.rules.SoundEvent
import java.awt.Point
import java.util.concurrent.ConcurrentHashMap

/**
 * Listens for mouse hover events over LayerDog inspection highlights and plays sounds
 */
class InspectionSoundListener(private val project: Project) : EditorMouseMotionListener, FileEditorManagerListener {
    
    companion object {
        private val LOG = Logger.getInstance(InspectionSoundListener::class.java)
        private val registeredProjects = ConcurrentHashMap<Project, InspectionSoundListener>()
        
        /**
         * Gets or creates a listener for the given project
         */
        fun getOrCreateListener(project: Project): InspectionSoundListener {
            return registeredProjects.computeIfAbsent(project) { InspectionSoundListener(it) }
        }
    }
    
    private val soundPlayer = SoundPlayer.getInstance()
    private var lastHoveredOffset: Int = -1
    private var lastHoverTime: Long = 0
    private val soundDebounceMs = 500L // Minimum time between sounds for same location
    
    init {
        // Register this listener with all open editors
        registerWithExistingEditors()
        
        // Listen for new editors being opened
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }
    
    /**
     * Registers this listener with all currently open editors
     */
    private fun registerWithExistingEditors() {
        ApplicationManager.getApplication().runReadAction {
            try {
                val fileEditorManager = FileEditorManager.getInstance(project)
                fileEditorManager.allEditors.forEach { fileEditor ->
                    val editor = fileEditor.getUserData(FileEditorManager.EDITOR_KEY)
                    if (editor != null) {
                        registerWithEditor(editor)
                    }
                }
            } catch (e: Exception) {
                LOG.warn("Failed to register with existing editors", e)
            }
        }
    }
    
    /**
     * Registers this listener with a specific editor
     */
    private fun registerWithEditor(editor: Editor) {
        try {
            // Remove if already added to avoid duplicates
            editor.removeEditorMouseMotionListener(this)
            // Add the listener
            editor.addEditorMouseMotionListener(this)
        } catch (e: Exception) {
            LOG.warn("Failed to register mouse listener with editor", e)
        }
    }
    
    /**
     * Called when mouse moves over editor
     */
    override fun mouseMoved(e: EditorMouseEvent) {
        try {
            val editor = e.editor
            val mousePoint = e.mouseEvent.point
            val offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(mousePoint))
            
            // Check if we're hovering over a LayerDog inspection
            if (isHoveringOverLayerDogInspection(editor, offset)) {
                val now = System.currentTimeMillis()
                
                // Debounce: only play sound if enough time has passed or we're over a different location
                if (offset != lastHoveredOffset || now - lastHoverTime > soundDebounceMs) {
                    soundPlayer.playSound(SoundEvent.TOOLTIP_HOVER)
                    lastHoveredOffset = offset
                    lastHoverTime = now
                }
            }
        } catch (e: Exception) {
            LOG.warn("Error in mouse hover handler", e)
        }
    }
    
    /**
     * Checks if the mouse is hovering over a LayerDog inspection highlight
     */
    private fun isHoveringOverLayerDogInspection(editor: Editor, offset: Int): Boolean {
        try {
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return false
            
            // Get highlights at the current offset
            val highlightInfos = DaemonCodeAnalyzer.getInstance(project)
                .getHighlights(editor.document, HighlightSeverity.WARNING, project)
            
            // Check if any highlight at this offset is from LayerDog
            for (highlightInfo in highlightInfos) {
                if (highlightInfo.startOffset <= offset && offset <= highlightInfo.endOffset) {
                    // Check if this is a LayerDog inspection by looking at the description or inspection tool
                    if (isLayerDogHighlight(highlightInfo)) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            LOG.warn("Error checking for LayerDog inspection", e)
        }
        
        return false
    }
    
    /**
     * Determines if a highlight info is from LayerDog inspections
     */
    private fun isLayerDogHighlight(highlightInfo: HighlightInfo): Boolean {
        val description = highlightInfo.description ?: return false
        val inspectionTool = highlightInfo.inspectionToolId ?: ""
        
        // Check if the description or tool ID indicates this is from LayerDog
        return description.contains("Controller") && description.contains("layer") ||
               description.contains("DTO") && description.contains("layer") ||
               description.contains("API") && description.contains("layer") ||
               description.contains("FLOW") && description.contains("layer") ||
               description.contains("DAO") && description.contains("layer") ||
               inspectionTool.contains("LayerViolation") ||
               inspectionTool.contains("Controller") ||
               inspectionTool.contains("DTO") ||
               inspectionTool.contains("API") ||
               inspectionTool.contains("Flow") ||
               inspectionTool.contains("DAO")
    }
    
    /**
     * Called when a new file editor is opened
     */
    override fun fileOpened(source: FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
        // Register with the new editor
        ApplicationManager.getApplication().invokeLater {
            val editor = source.selectedTextEditor
            if (editor != null) {
                registerWithEditor(editor)
            }
        }
    }
    
    /**
     * Called when file selection changes
     */
    override fun selectionChanged(event: FileEditorManagerEvent) {
        // Register with the newly selected editor
        val newEditor = event.newEditor?.getUserData(FileEditorManager.EDITOR_KEY)
        if (newEditor != null) {
            registerWithEditor(newEditor)
        }
    }
}

/**
 * Project activity to initialize the inspection sound listener when project opens
 */
class InspectionSoundListenerStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Initialize the sound listener for this project
        InspectionSoundListener.getOrCreateListener(project)
    }
}
