package com.ai.translationmcpwithkotlin

import org.springframework.web.bind.annotation.*
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/api/translate")
class TranslationController(
    private val translationService: TranslationService
) {
    private val logger = LoggerFactory.getLogger(TranslationController::class.java)

    @PostMapping("/to-english")
    fun translateToEnglish(@RequestBody request: TranslationRequest): TranslationResponse {
        logger.debug("번역 요청 수신: {}", request.text)
        try {
            val translatedText = translationService.translateToEnglish(request.text)
            logger.debug("번역 응답 전송: {}", translatedText)
            return TranslationResponse(translatedText)
        } catch (e: Exception) {
            logger.error("번역 요청 처리 중 오류 발생", e)
            throw e
        }
    }
}

data class TranslationRequest(
    val text: String
)

data class TranslationResponse(
    val translatedText: String
)