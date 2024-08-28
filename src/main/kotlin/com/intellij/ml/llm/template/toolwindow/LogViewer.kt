package com.intellij.ml.llm.template.toolwindow

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.*


class LogViewer : JPanel() {
    // Set up the JTextArea
    private val logArea = JTextArea()

    init {
        logArea.isEditable = false // Make the text area read-only
        logArea.lineWrap = true // Wrap lines if they are too long
        logArea.wrapStyleWord = true // Wrap at word boundaries

        // Set up the JScrollPane
        val scrollPane = JScrollPane(logArea)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        scrollPane.preferredSize = Dimension(500, 400) // Adjust the size as needed

        // Add the JScrollPane to this JPanel
        layout = BorderLayout()
        add(scrollPane, BorderLayout.CENTER)
    }

    // Method to add logs
    fun appendLog(log: String) {
        logArea.append(log + "\n")

        // Auto-scroll to the bottom
        logArea.caretPosition = logArea.document.length
    }

//    companion object {
//        // Main method for testing
//        @JvmStatic
//        fun main(args: Array<String>) {
//            // Create the frame
//            val frame = JFrame("Log Viewer")
//            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
//
//            // Create an instance of LogViewer and add it to the frame
//            val logViewer = LogViewer()
//            frame.add(logViewer)
//
//            // Test: Append some logs
//            logViewer.appendLog("Application started.")
//            logViewer.appendLog("First log entry.")
//            logViewer.appendLog("Second log entry.")
//
//            // Display the frame
//            frame.pack()
//            frame.setLocationRelativeTo(null)
//            frame.isVisible = true
//
//            // Simulate appending logs over time
//            Timer(1000) { e: ActionEvent? ->
//                logViewer.appendLog(
//                    "Log entry at " + System.currentTimeMillis()
//                )
//            }.start()
//        }
//    }
}
