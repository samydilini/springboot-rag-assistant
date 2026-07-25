package com.springboot.rag.assistant.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Provides the official Anthropic client used for answer generation (Increment 3).
 *
 * <p>The bean is {@link Lazy} so the application boots without an
 * {@code ANTHROPIC_API_KEY} present — the key is only required the first time a
 * query actually calls Claude. Credentials are resolved from the environment
 * ({@code ANTHROPIC_API_KEY}, or an {@code ant auth login} profile).
 */
@Configuration
public class AnthropicConfig {

    @Bean
    @Lazy
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.fromEnv();
    }
}
