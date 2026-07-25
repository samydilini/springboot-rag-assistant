package com.springboot.rag.assistant.query;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ThinkingConfigDisabled;
import com.springboot.rag.assistant.config.RagProperties;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Generates a grounded answer with Claude via the official Anthropic SDK.
 *
 * <p>The model id is read from {@link RagProperties#getGenerationModel()} (configurable,
 * default {@code claude-sonnet-5}) — never hardcoded. Extended thinking is disabled to
 * keep answer latency within the query-response target; a grounded synthesis over
 * retrieved context does not require it.
 */
@Service
public class AnswerGenerator {

    private static final Logger log = LoggerFactory.getLogger(AnswerGenerator.class);

    private final AnthropicClient client;
    private final RagProperties properties;

    public AnswerGenerator(AnthropicClient client, RagProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public String generate(String systemPrompt, String userMessage) {
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(properties.getGenerationModel())
                    .maxTokens(properties.getAnswerMaxTokens())
                    .thinking(ThinkingConfigDisabled.builder().build())
                    .system(systemPrompt)
                    .addUserMessage(userMessage)
                    .build();

            Message response = client.messages().create(params);
            response.stopReason().ifPresent(reason -> log.debug("Claude stop_reason={}", reason));

            String answer = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(TextBlock::text)
                    .collect(Collectors.joining())
                    .strip();

            // Empty content covers a pre-output refusal; fall back to the "don't know" reply.
            return answer.isEmpty() ? RagPromptBuilder.NO_ANSWER : answer;
        } catch (RuntimeException e) {
            log.error("Answer generation failed", e);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Answer generation failed (is ANTHROPIC_API_KEY set?): " + e.getMessage(), e);
        }
    }
}
