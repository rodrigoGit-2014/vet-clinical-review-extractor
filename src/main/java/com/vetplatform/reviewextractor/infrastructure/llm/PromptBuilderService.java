package com.vetplatform.reviewextractor.infrastructure.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PromptBuilderService {

    private final String promptVersion;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public PromptBuilderService(@Value("${llm.prompt-version}") String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String buildSystemPrompt() {
        return loadTemplate("system.txt");
    }

    public String buildSystemPrompt(String versionOverride) {
        return loadTemplate(versionOverride, "system.txt");
    }

    public String buildUserPrompt(String reviewText, String locale) {
        String template = loadTemplate("user.txt");
        return template
                .replace("{{locale}}", locale)
                .replace("{{reviewText}}", reviewText);
    }

    public String buildUserPrompt(String reviewText, String locale, String versionOverride) {
        String template = loadTemplate(versionOverride, "user.txt");
        return template
                .replace("{{locale}}", locale)
                .replace("{{reviewText}}", reviewText);
    }

    public String getEffectiveVersion(String versionOverride) {
        return (versionOverride != null && !versionOverride.isBlank()) ? versionOverride : promptVersion;
    }

    private String loadTemplate(String fileName) {
        return loadTemplate(promptVersion, fileName);
    }

    private String loadTemplate(String version, String fileName) {
        String cacheKey = version + "/" + fileName;
        return templateCache.computeIfAbsent(cacheKey, key -> {
            String path = "prompts/" + version + "/" + fileName;
            try {
                ClassPathResource resource = new ClassPathResource(path);
                return resource.getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo cargar el template: " + path, e);
            }
        });
    }
}
