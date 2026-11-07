package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "AiMathAssistantService"

data class MathSolutionStep(
    val stepNumber: Int,
    val title: String,
    val formulaLatex: String,
    val explanation: String
)

data class MathAssistantResponse(
    val query: String,
    val mode: String, // "SOLVE", "EXPLAIN", "PRACTICE", "CORRECT"
    val summary: String,
    val steps: List<MathSolutionStep>,
    val finalAnswer: String,
    val rawText: String
)

class AiMathAssistantService {

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun solveMathProblem(problem: String): Result<MathAssistantResponse> {
        val prompt = """
            You are an expert AI Mathematics Professor and tutor for teachers and students.
            Solve the following math problem thoroughly with pedagogical step-by-step clarity:
            Problem: "$problem"

            Please format your response clearly:
            SUMMARY: [1-2 sentences summarizing what we are solving]
            STEP 1: [Step Title] | FORMULA: [Mathematical notation or equation] | EXPLANATION: [Clear explanation]
            STEP 2: [Step Title] | FORMULA: [Equation] | EXPLANATION: [Clear explanation]
            (continue with steps as needed)
            FINAL ANSWER: [Exact final solution]
        """.trimIndent()
        return executeQuery(problem, "SOLVE", prompt)
    }

    suspend fun explainConcept(concept: String): Result<MathAssistantResponse> {
        val prompt = """
            You are an expert mathematics educator.
            Explain the following mathematical concept, theorem, or formula for students:
            Concept: "$concept"

            Format:
            SUMMARY: [Intuitive definition and real-world significance]
            STEP 1: [Core Principles] | FORMULA: [Key formula] | EXPLANATION: [Breakdown of variables]
            STEP 2: [Example Problem] | FORMULA: [Application] | EXPLANATION: [How it works]
            STEP 3: [Common Mistakes to Avoid] | FORMULA: [Warning] | EXPLANATION: [Tip]
            FINAL ANSWER: [Key takeaway rule]
        """.trimIndent()
        return executeQuery(concept, "EXPLAIN", prompt)
    }

    suspend fun generatePracticeProblems(topic: String): Result<MathAssistantResponse> {
        val prompt = """
            You are a math curriculum designer.
            Generate 3 graded practice exercises with solutions for the topic:
            Topic: "$topic"

            Format:
            SUMMARY: [Target grade and learning objective]
            STEP 1: [Exercise 1 (Foundational)] | FORMULA: [Question 1] | EXPLANATION: [Solution and answer key]
            STEP 2: [Exercise 2 (Intermediate)] | FORMULA: [Question 2] | EXPLANATION: [Solution and answer key]
            STEP 3: [Exercise 3 (Advanced Challenge)] | FORMULA: [Question 3] | EXPLANATION: [Solution and answer key]
            FINAL ANSWER: [Teacher grading tip or rubric]
        """.trimIndent()
        return executeQuery(topic, "PRACTICE", prompt)
    }

    suspend fun findAndCorrectMistakes(work: String): Result<MathAssistantResponse> {
        val prompt = """
            You are an experienced math teacher reviewing student work.
            Review the following math calculation and identify any arithmetic, algebraic, or conceptual errors:
            Student Work: "$work"

            Format:
            SUMMARY: [Overall diagnosis: whether work is correct or contains errors]
            STEP 1: [Review of Steps] | FORMULA: [Error location if any] | EXPLANATION: [Where the reasoning went wrong or succeeded]
            STEP 2: [Corrected Derivation] | FORMULA: [Corrected math line] | EXPLANATION: [Why this is right]
            FINAL ANSWER: [Corrected Final Answer]
        """.trimIndent()
        return executeQuery(work, "CORRECT", prompt)
    }

    private suspend fun executeQuery(
        query: String,
        mode: String,
        prompt: String
    ): Result<MathAssistantResponse> = withContext(Dispatchers.Default) {
        try {
            val response = withTimeout(55_000L) {
                model.generateContent(prompt)
            }
            val text = response.text.orEmpty().trim()
            if (text.isBlank()) {
                return@withContext Result.failure(IllegalStateException("AI model returned an empty response."))
            }

            var summary = "Mathematical solution breakdown"
            var finalAnswer = ""
            val steps = mutableListOf<MathSolutionStep>()
            var stepCounter = 1

            for (line in text.lines()) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("SUMMARY:", ignoreCase = true) -> {
                        summary = trimmed.substringAfter(":").trim()
                    }
                    trimmed.startsWith("FINAL ANSWER:", ignoreCase = true) -> {
                        finalAnswer = trimmed.substringAfter(":").trim()
                    }
                    trimmed.contains("STEP", ignoreCase = true) && trimmed.contains("|") -> {
                        val parts = trimmed.split("|")
                        var title = "Step $stepCounter"
                        var formula = ""
                        var explanation = ""

                        for (part in parts) {
                            val p = part.trim()
                            when {
                                p.startsWith("STEP", ignoreCase = true) -> {
                                    title = p.substringAfter(":").ifBlank { p }
                                }
                                p.startsWith("FORMULA:", ignoreCase = true) -> {
                                    formula = p.substringAfter(":").trim()
                                }
                                p.startsWith("EXPLANATION:", ignoreCase = true) -> {
                                    explanation = p.substringAfter(":").trim()
                                }
                            }
                        }
                        steps.add(
                            MathSolutionStep(
                                stepNumber = stepCounter++,
                                title = title,
                                formulaLatex = formula,
                                explanation = explanation.ifBlank { trimmed }
                            )
                        )
                    }
                }
            }

            if (steps.isEmpty()) {
                steps.add(
                    MathSolutionStep(
                        stepNumber = 1,
                        title = "Solution",
                        formulaLatex = query,
                        explanation = text
                    )
                )
            }

            Result.success(
                MathAssistantResponse(
                    query = query,
                    mode = mode,
                    summary = summary,
                    steps = steps,
                    finalAnswer = finalAnswer.ifBlank { "Solution completed" },
                    rawText = text
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Math Assistant timed out", e)
            Result.failure(Exception("AI calculation timed out after 55 seconds. Please verify network connectivity and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Math Assistant error", e)
            Result.failure(e)
        }
    }
}
