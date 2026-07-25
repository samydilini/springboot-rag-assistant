plugins {
    id("java")
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.springboot.rag.assistant"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}


repositories {
    mavenCentral()
}

extra["springAiVersion"] = "1.0.1"

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // RAG plumbing (Spring AI): local in-process embeddings + pgvector store.
    // The embedding provider is swappable via Spring AI's EmbeddingModel interface —
    // replacing this starter with a Voyage/OpenAI one is a dependency + config change only.
    implementation("org.springframework.ai:spring-ai-starter-model-transformers")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")

    // PDF text extraction (PagePdfDocumentReader / PDFBox). The starters above do not
    // bundle a document reader, so this is required for ingestion.
    implementation("org.springframework.ai:spring-ai-pdf-document-reader")

    // Claude is used only for answer generation, via the official Anthropic SDK.
    implementation("com.anthropic:anthropic-java:2.34.0")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}
