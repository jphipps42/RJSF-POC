flowchart TD
    A["<b>FormSubmissionService</b><br/>syncToRelationalTable()"] --> B["<b>RelationalSyncStrategyManager</b><br/>(runtime switchable via REST API)"]

    B -->|"mode = MAPPER"| C["<b>MapperSyncStrategy</b>"]
    B -->|"mode = POJO"| D["<b>PojoSyncStrategy</b>"]

    C --> E["SubmissionWriteService<br/>(7-stage transformer pipeline)"]
    E --> F["PathResolver → TransformRegistry<br/>→ TypeCoercionService"]
    F --> G["<b>SqlExecutor</b><br/>(raw JDBC)"]

    D --> H["<b>FormDataExtractor</b><br/>(typed value extraction)"]
    H --> I["<b>JPA Repositories</b><br/>(Spring Data / Hibernate)"]

    G --> J[("<b>PostgreSQL</b><br/>form_pre_award_* tables<br/>(6 tables, 117 fields)")]
    I --> J

    style A fill:#2C3E50,color:#fff,stroke:#2C3E50
    style B fill:#428bca,color:#fff,stroke:#2158c6
    style C fill:#428bca,color:#fff,stroke:#2158c6
    style D fill:#FF9800,color:#fff,stroke:#E68900
    style E fill:#5DADE2,color:#fff,stroke:#2E86C1
    style F fill:#5DADE2,color:#fff,stroke:#2E86C1
    style G fill:#5DADE2,color:#fff,stroke:#2E86C1
    style H fill:#FFB74D,color:#2C3E50,stroke:#E68900
    style I fill:#FFB74D,color:#2C3E50,stroke:#E68900
    style J fill:#27AE60,color:#fff,stroke:#1E8449
