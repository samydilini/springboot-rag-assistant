# AI Document Assistant Specification
## Project Scope

This project is being developed iteratively.

Version 1 (MVP) includes:
- Document upload
- Document ingestion
- Vector search
- Claude-powered question answering

The following capabilities are planned for future versions:
- User authentication
- Authorization
- Multi-user document isolation
- OCR support

## 1. Purpose

Build a Retrieval Augmented Generation (RAG) application
that allows users to upload documents and ask questions
about those documents using natural language.

The system should provide answers grounded in uploaded
documents and minimise hallucination.

---

## 2. Functional Requirements

### Document Upload

The system shall:

- Allow users to upload PDF files
- Store document metadata
- Extract text from documents
- Split documents into chunks
- Generate embeddings
- Store embeddings for retrieval


### Question Answering

The system shall:

- Accept user questions
- Retrieve relevant document chunks
- Provide retrieved context to the LLM
- Generate an answer using Claude
- Return source references

---

## 3. Non Functional Requirements

### Performance

- Upload API response < 5 seconds
- Query response < 5 seconds


### Reliability

- Failed document processing should retry
- Processing failures should be recorded


### Security
Future Version (V2)
- Users should only access their own documents
- Authentication and authorization will be added in Version 2.

---

## 4. AI Requirements

### LLM

Model:
Claude Sonnet

Purpose:
Answer questions using retrieved context


### Retrieval

Chunk size:
800 tokens

Chunk overlap:
100 tokens

Top K:
5


### Prompt Rules

The assistant must:

- Answer only from provided context
- Say "I don't know" when information is unavailable
- Include document references

---

## 5. Future Improvements

- OCR support
- Multi-document conversations
- Feedback-based evaluation