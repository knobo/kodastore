i# KodaStore: Spesifikasjon

KodaStore er en moderne, åpen kildekode Event Sourcing-database bygget på toppen av PostgreSQL (v17+). Den er designet for å gi utviklere i Kotlin/JVM-økosystemet den beste utvikleropplevelsen (DX) kombinert med den operasjonelle tryggheten til en moderne relasjonsdatabase.

## 1\. Kjernefilosofi

- **Open Source Forever:** Ingen funksjonalitet bak betalingsmurer. Full gjennomsiktighet og community-drevet utvikling.
    
- **Postgres som motor:** Utnytt de nyeste funksjonene i Postgres (17, 18+) for å løse klassiske Event Sourcing-utfordringer som wraparound og failover-synkronisering.
    
- **DDD & Hexagonal:** Arkitekturen er bygget rundt Aggregates og klare skiller mellom kjerne og infrastruktur.
    
- **Excellent DX:** "Zero-config" lokal kjøring med Docker, ferdige `docker-compose.yaml`\-filer, og en intuitiv Kotlin-klient som fjerner "mental fatigue".
    

## 2\. Arkitektur (Hexagonal / Clean)

KodaStore følger hexagonal arkitektur for å sikre at kjerne-logikken for event-håndtering er isolert fra lagring og transport.

### Kjerne (Domain)

- **Stream Engine:** Håndterer append-only logikk, sekvensnummer og versjonskontroll. Bruker partisjonering for å håndtere millioner av strømmer.
    
- **Consistency Manager:** Støtter både *Optimistic Concurrency Control (OCC)* og sterke transaksjonelle garantier (Strong Consistency).
    
- **Subscription Manager:** Bruker **Logical Replication Slots** for å strømme events til klienter via gRPC med ekstremt lav latency.
    

### Porter (Ports)

- **Inbound:** gRPC API (hovedgrensesnitt for høy ytelse), REST API (for admin/helse).
    
- **Outbound:** Storage Adapter (PostgreSQL 17+), Observability (OpenTelemetry/Prometheus).
    

## 3\. Teknisk Stack

- **Språk:** Kotlin 2.x (Coroutines, Flow, `kotlinx.serialization`).
    
- **Runtime:** JVM 21+ (optimalisert for Virtual Threads/Project Loom for å håndtere tusenvis av samtidige gRPC-strømmer).
    
- **Database:** **PostgreSQL 17+** (bruker XID64 for å eliminere wraparound-problematikk og innebygd inkrementell backup).
    
- **Kommunikasjon:** gRPC med Protobuf for binær effektivitet og typesikkerhet.
    

## 4\. Design av Streams og Aggregates

### Stream-struktur

- **Navnestandard:** `Category-ID` (f.eks. `Order-123`).
    
- **Virtuelle Streams (Kategorier):** Bruker Postgres-indeksering og **JSON_TABLE** (PG 17) for å generere virtuelle visninger av alle events i en kategori (f.eks. `$ce-Order`) uten lagrings-overhead eller kompliserte "link-to"-events.
    
- **Metadata:** Innebygd støtte for `correlationId` og `causalityId` for fullstendig tracing.
    

### Aggregate Design (Kotlin-først)

KodaStore fremmer en funksjonell tilnærming:

- **State:** En immutable data class.
    
- **Evolve:** En ren funksjon: `(State, Event) -> State`.
    
- **Command Handling:** Rene funksjoner som returnerer `Result<List<Event>, Error>`.
    
- **Snapshotting:** Automatisk lagring av tilstand (snapshots) i Postgres for å akselerere innlasting av lange strømmer.
    

## 5\. Datamodell & Lagring (PostgreSQL 17+ Spesifikk)

Vi utnytter de nyeste Postgres-funksjonene for å bli teknisk overlegne:

```
-- Bruker XID64 (når tilgjengelig i PG 17/18) for å eliminere wraparound-vedlikehold
CREATE TABLE events (
    global_offset BIGSERIAL PRIMARY KEY, -- Total orden (Checkpoint)
    stream_id VARCHAR(255) NOT NULL,    -- Aggregate-ID
    stream_version INT NOT NULL,        -- Versjon for OCC
    event_type VARCHAR(100) NOT NULL,   -- Navn på event
    payload JSONB NOT NULL,             -- Selve dataene (binært JSON)
    metadata JSONB,                     -- Trace-info, bruker-ID, etc.
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (stream_id, stream_version)  -- Garanterer konsistens
) PARTITION BY HASH (stream_id); -- Skalerer til millioner av strømmer

-- Projeksjonseksempel ved bruk av JSON_TABLE (PG 17+)
-- Dette lar utviklere bruke SQL direkte på event-strømmen uten kode
CREATE VIEW order_summary AS
SELECT * FROM events,
JSON_TABLE(payload, '$' COLUMNS (
    order_id TEXT PATH '$.orderId',
    total_amount NUMERIC PATH '$.amount'
)) AS jt
WHERE event_type = 'OrderCreated';
```

### Konsistensstrategier

- **Strong Consistency:** Mulighet for å oppdatere lesemodeller i samme transaksjon som eventet (hvis de ligger i samme DB).
    
- **High Availability Subscriptions:** Bruker **Failover Slots** (PG 17) for å sikre at subscriptions overlever database-failover uten tap av events eller manuelle re-connections.
    

## 6\. Skalerbarhet og Drift

- **Inkrementell Backup (PG 17):** Gjør det mulig å drifte terabytes med event-data med lave kostnader i skyen (GCP/AWS).
    
- **Elastisitet:** gRPC-klienten håndterer automatisk failover og "backpressure" ved høy belastning.
    

## 7\. Developer Experience (DX)

- **Docker & Kubernetes:** Ferdige `docker-compose.yaml`\-filer med Postgres 17 optimalisert for Event Sourcing.
    
- **KodaUI:** Et moderne web-grensesnitt for:
    
    - Inspeksjon av strømmer.
        
    - **GDPR-Scrubbing:** Mulighet for å overskrive sensitive felt i JSONB-payload uten å bryte den immutabel kjeden (Postgres `UPDATE` på spesifikke rader).
        
- **Testcontainers:** Offisiell støtte for `KodaStoreContainer`.
    

## 8\. Sikkerhet

- **mTLS:** Standard for all intern kommunikasjon.
    
- **RBAC:** Tilgangskontroll via Postgres-roller eller JWT-integrasjon i gRPC-laget.
    

## 9\. Veien mot verdensherredømme

1.  **Fokus på stabilitet:** Bruk Postgres som det solide fundamentet alle stoler på.
    
2.  **Fjern "Eventual Consistency" smerte:** Tillat transaksjonelle projections som standardvalg for enklere onboarding.
    
3.  **Open Source Stolthet:** Bygget av utviklere som hater mental fatigue, for utviklere som vil ha ting som funker.
