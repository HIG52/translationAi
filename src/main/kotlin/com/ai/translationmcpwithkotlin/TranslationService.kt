package com.ai.translationmcpwithkotlin

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.DisposableBean
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.slf4j.LoggerFactory

@Service
class TranslationService : InitializingBean, DisposableBean {
    private val logger = LoggerFactory.getLogger(TranslationService::class.java)

    @Value("\${anthropic.api.key}")
    private lateinit var apiKey: String

    // 타입을 AnthropicClient로 변경
    private lateinit var anthropic: AnthropicClient

    private val messageParamsBuilder: MessageCreateParams.Builder by lazy {
        MessageCreateParams.builder()
            .model(Model.CLAUDE_3_5_SONNET_20241022)
            .maxTokens(1024)
    }

    override fun afterPropertiesSet() {
        logger.info("TranslationService 초기화 중...")
        // API 키를 명시적으로 설정
        anthropic = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .build()
        logger.info("TranslationService 초기화 완료")
    }

    fun translateToEnglish(content: String): String {
        logger.debug("번역 요청 시작: {}", content)
        try {
            if (content.isBlank()) {
                logger.warn("빈 텍스트 번역 시도")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "번역할 텍스트가 비어있습니다.")
            }

            val messages = mutableListOf(
                MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .content("다음 한국어 텍스트를 영어로 번역해주세요. 번역된 텍스트만 반환해주세요:\n\n$content")
                    .build()
            )

            logger.debug("Claude API 호출 시작")
            val response = anthropic.messages().create(
                messageParamsBuilder
                    .messages(messages)
                    .build()
            )
            logger.debug("Claude API 호출 완료")

            val translatedText = response.content()
                .filter { it.isText() }
                .mapNotNull { it.text().orElse(null)?.text() }
                .joinToString("\n")
                .takeIf { it.isNotBlank() }
                ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "번역 결과가 비어있습니다.")

            logger.debug("번역 완료: {}", translatedText)
            return translatedText
        } catch (e: Exception) {
            logger.error("번역 중 오류 발생", e)
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "번역 중 오류가 발생했습니다: ${e.message}"
            )
        }
    }

    override fun destroy() {
        logger.info("TranslationService 종료 중...")
        anthropic.close()
        logger.info("TranslationService 종료 완료")
    }
}