# Architecture

## Overview


                    User
                     |
                     |
              Spring Boot API
                     |
        ----------------------------
        |                          |
Document Service          Query Service
|                          |
|                          |
PostgreSQL              Vector Search
|                          |
pgvector <-------------- Retriever
|
|
Claude


## Components

### Spring Boot API

Responsibilities:

- File upload
- Query handling
- Authentication


### Document Processing

Responsibilities:

- Extract text
- Chunk documents
- Generate embeddings


### Vector Store

Technology:

PostgreSQL + pgvector

Stores:

- Document chunks
- Embeddings
- Metadata


### LLM

Claude:

- Receives retrieved context
- Generates final response


## Data Flow


Upload:

User
|
Upload PDF
|
Extract text
|
Chunk text
|
Create embeddings
|
Store vectors


Query:

User question
|
Create query embedding
|
Similarity search
|
Retrieve chunks
|
Send context to Claude
|
Return answer