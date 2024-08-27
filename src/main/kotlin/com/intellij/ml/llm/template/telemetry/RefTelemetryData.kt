package com.intellij.ml.llm.template.telemetry

import com.google.gson.annotations.SerializedName
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.UncreatableRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EfCandidateType
import com.intellij.ml.llm.template.utils.EFCandidateApplicationPayload
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import java.util.*

data class RefTelemetryData(
    @SerializedName("id")
    var id: String,
) {
    @SerializedName("hostFunctionTelemetryData")
    lateinit var hostFunctionTelemetryData: HostFunctionTelemetryData

    @SerializedName("candidatesTelemetryData")
    lateinit var candidatesTelemetryData: RefCandidatesTelemetryData

    @SerializedName("userSelectionTelemetryData")
    lateinit var userSelectionTelemetryData: EFUserSelectionTelemetryData

    @SerializedName("elapsedTime")
    lateinit var elapsedTime: List<CandidateElapsedTimeTelemetryData>

    @SerializedName("processingTime")
    lateinit var processingTime: EFTelemetryDataProcessingTime

    @Transient
    lateinit var refactoringObjects: List<AbstractRefactoring>
}

data class HostFunctionTelemetryData(
    @SerializedName("hostFunctionSize")
    var hostFunctionSize: Int,

    @SerializedName("lineStart")
    var lineStart: Int,

    @SerializedName("lineEnd")
    var lineEnd: Int,

    @SerializedName("bodyLineStart")
    var bodyLineStart: Int,

    @SerializedName("language")
    var language: String,

    @SerializedName("sourceCode")
    var sourceCode: String
)

data class RefCandidatesTelemetryData(
    @SerializedName("numberOfSuggestions")
    var numberOfSuggestions: Int,

    @SerializedName("candidates")
    var candidates: List<RefCandidateTelemetryData>,
)

data class RefCandidateTelemetryData(
    @SerializedName("lineStart")
    var lineStart: Int,

    @SerializedName("lineEnd")
    var lineEnd: Int,

    @SerializedName("refactoringType")
    var refactoringType: String,

    @SerializedName("description")
    var description: String,

    @SerializedName("couldCreateRefObject")
    var couldCreateRefObject: Boolean?=null,

    @SerializedName("valid")
    var valid: Boolean?=null,

    @SerializedName("applied")
    var applied: Boolean?=null,

    @SerializedName("undone")
    var undone: Boolean?=null,

    @SerializedName("userRating")
    var rating: String? = null

)

data class EFUserSelectionTelemetryData(
    @SerializedName("lineStart")
    var lineStart: Int,

    @SerializedName("lineEnd")
    var lineEnd: Int,

    @SerializedName("functionSize")
    var functionSize: Int,

    @SerializedName("positionInHostFunction")
    var positionInHostFunction: Int,

    @SerializedName("selectedCandidateIndex")
    var selectedCandidateIndex: Int,

    @SerializedName("candidateType")
    var candidateType: EfCandidateType,

    @SerializedName("elementsType")
    var elementsType: List<EFPsiElementsTypesTelemetryData>,
)

data class CandidateElapsedTimeTelemetryData(
    @SerializedName("candidateIndex")
    var candidateIndex: Int,

    @SerializedName("elapsedTime")
    var elapsedTime: Long,
)

data class EFPsiElementsTypesTelemetryData(
    @SerializedName("elementType")
    var type: String,

    @SerializedName("quantity")
    var quantity: Int,
)


enum class TelemetryDataAction() {
    START,
    STOP
}

data class EFTelemetryDataElapsedTimeNotificationPayload(
    @SerializedName("action")
    var action: TelemetryDataAction,

    @SerializedName("currentSelectionIndex")
    var selectionIndex: Int
)

data class EFTelemetryDataProcessingTime(
    @SerializedName("llmResponseTime")
    var llmResponseTime: Long,

    @SerializedName("pluginProcessingTime")
    var pluginProcessingTime: Long,

    @SerializedName("totalTime")
    var totalTime: Long
)

data class AgenticTelemetry(
    @SerializedName("agentData")
    val agentIterationData: List<AgentIterationTelemetry>
){
    companion object{
        fun createFromSessionIds(sessionIds: List<String>,
                                 telemetryDataManager: EFTelemetryDataManager): AgenticTelemetry{
            return AgenticTelemetry(
                sessionIds
                    .map { telemetryDataManager.getData(it) }
                    .filterNotNull()
                    .mapIndexed { index: Int, refTelemetryData: RefTelemetryData
                        ->  AgentIterationTelemetry(iteration = index, refTelemetryData = refTelemetryData)}
            )
        }
    }
}

data class AgentIterationTelemetry(
    @SerializedName("iteration")
    val iteration: Int,

    @SerializedName("iterationData")
    val refTelemetryData: RefTelemetryData
)

class EFTelemetryDataManager {
    private var currentSessionId: String = ""
    private val data: MutableMap<String, RefTelemetryData> = mutableMapOf()
    private lateinit var currentTelemetryData: RefTelemetryData

    fun newSession(): String {
        currentSessionId = UUID.randomUUID().toString()
        currentTelemetryData = RefTelemetryData(currentSessionId)
        data[currentSessionId] = currentTelemetryData
        return currentSessionId
    }

    fun currentSession(): String {
        if (currentSessionId.isNotEmpty()) return currentSessionId
        return newSession()
    }

    fun addHostFunctionTelemetryData(hostFunctionTelemetryData: HostFunctionTelemetryData): EFTelemetryDataManager {
        currentTelemetryData.hostFunctionTelemetryData = hostFunctionTelemetryData
        return this
    }

    fun addCandidatesTelemetryData(candidatesTelemetryData: RefCandidatesTelemetryData): EFTelemetryDataManager {
        currentTelemetryData.candidatesTelemetryData = candidatesTelemetryData
        return this
    }

    fun addUserSelectionTelemetryData(userSelectionTelemetryData: EFUserSelectionTelemetryData): EFTelemetryDataManager {
        currentTelemetryData.userSelectionTelemetryData = userSelectionTelemetryData
        return this
    }

    fun getData(sessionId: String? = null): RefTelemetryData? {

        val sId = sessionId ?: currentSession()
        val refTelemetryData = data.getOrDefault(sId, null)
        if (refTelemetryData != null) {
            processAbstractRefactorings(refTelemetryData)
        }
        return refTelemetryData
    }

    private fun processAbstractRefactorings(refTelemetryData: RefTelemetryData) {
        val objs = refTelemetryData.refactoringObjects.map { it ->
            RefCandidateTelemetryData(
                it.startLoc, it.endLoc,
                it::class.simpleName.toString(),
                it.description,
                couldCreateRefObject = it !is UncreatableRefactoring,
                valid = it.isValid,
                applied = it.applied,
                undone = it.undone,
                rating = it.userRating
            )
        }

        refTelemetryData.candidatesTelemetryData = RefCandidatesTelemetryData(
            refTelemetryData.refactoringObjects?.size ?: 0, objs
        )

    }

    fun setRefactoringObjects(refactoringCandidates: List<AbstractRefactoring>) {
        currentTelemetryData.refactoringObjects = refactoringCandidates
    }
}

class EFTelemetryDataUtils {
    companion object {
        fun buildHostFunctionTelemetryData(
            codeSnippet: String,
            lineStart: Int,
            bodyLineStart: Int,
            language: String
        ): HostFunctionTelemetryData {
            val functionSize = codeSnippet.lines().size
            return HostFunctionTelemetryData(
                lineStart = lineStart,
                lineEnd = lineStart + functionSize - 1,
                hostFunctionSize = functionSize,
                bodyLineStart = bodyLineStart,
                language = language,
                sourceCode = codeSnippet
            )
        }

        private fun buildCandidateTelemetryData(candidateApplicationPayload: EFCandidateApplicationPayload): RefCandidateTelemetryData {
            val candidate = candidateApplicationPayload.candidate
            return RefCandidateTelemetryData(
                lineStart = candidate.startLoc,
                lineEnd = candidate.endLoc,
//                candidateType = candidate.getRefactoringName(),
//                candidateType = EfCandidateType.AS_IS,
//                applicationResult = candidateApplicationPayload.result,
//                reason = candidateApplicationPayload.reason,
                description = candidate.description,
                refactoringType = candidate::class.java.toString(),
//
            )
        }

        fun buildCandidateTelemetryData(candidateApplicationPayloadList: List<EFCandidateApplicationPayload>): List<RefCandidateTelemetryData> {
            val candidateTelemetryDataList: MutableList<RefCandidateTelemetryData> = mutableListOf()
            candidateApplicationPayloadList.forEach {
                candidateTelemetryDataList.add(buildCandidateTelemetryData(it))
            }
            return candidateTelemetryDataList.toList()
        }

        fun buildUserSelectionTelemetryData(
            efCandidate: AbstractRefactoring,
            candidateIndex: Int,
            hostFunctionTelemetryData: HostFunctionTelemetryData?,
            file: PsiFile
        ): EFUserSelectionTelemetryData {
            var positionInHostFunction = -1
            if (hostFunctionTelemetryData != null) {
                positionInHostFunction = efCandidate.startLoc - hostFunctionTelemetryData.bodyLineStart
            }
            return EFUserSelectionTelemetryData(
                lineStart = efCandidate.startLoc,
                lineEnd = efCandidate.endLoc,
                functionSize = efCandidate.endLoc - efCandidate.startLoc + 1,
                positionInHostFunction = positionInHostFunction,
                selectedCandidateIndex = candidateIndex,
                candidateType = EfCandidateType.AS_IS,
                elementsType = buildElementsTypeTelemetryData(efCandidate, file),
            )
        }

        fun buildElementsTypeTelemetryData(
            efCandidate: AbstractRefactoring,
            file: PsiFile
        ): List<EFPsiElementsTypesTelemetryData> {
            val psiElements = file.elementsInRange(TextRange(efCandidate.getStartOffset(), efCandidate.getEndOffset()))
            val namesList = psiElements.filter { it !is PsiWhiteSpace }.map { it.elementType.toString() }
            val namesQuantityMap = namesList.groupingBy { it }.eachCount()
            val result = namesQuantityMap.entries.map {
                EFPsiElementsTypesTelemetryData(
                    type = it.key,
                    quantity = it.value
                )
            }
            return result
        }
    }
}