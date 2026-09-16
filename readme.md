Project Overview
Standard AI models are limited to the data they were trained on. Retrieval-Augmented Generation (RAG) solves this by:

Retrieving relevant facts from your own database (PostgreSQL).
Augmenting the user's prompt with those facts.
Generating a response that is grounded in your private data.
This project specifically uses PGVector, an extension that turns a standard PostgreSQL database into a powerful vector engine.