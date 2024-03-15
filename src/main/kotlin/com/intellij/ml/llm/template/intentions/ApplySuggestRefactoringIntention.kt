package com.intellij.ml.llm.template.intentions

import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.grazie.GrazieGPT4RequestProvider
import com.intellij.ml.llm.template.models.ollama.MistralChatRequestProvider
import com.intellij.ml.llm.template.prompts.SuggestRefactoringPrompt

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class ApplySuggestRefactoringIntention: ApplyExtractFunctionTransformationIntention(GrazieGPT4RequestProvider) {
    init {
        prompter = SuggestRefactoringPrompt();
    }
    override fun getInstruction(project: Project, editor: Editor): String? {
        return Messages.showInputDialog(project, "Enter prompt:", "Codex", null)
    }

    override fun getText(): String {
        return LLMBundle.message("intentions.apply.suggest.refactoring.family.name")
    }
    override fun getFamilyName(): String = text
}