# Section 8: Big O Analysis for Critical Paths

---

## 8.1 Auto-Routing Model Selection

### Algorithm

```
Input: N models in the registry, C required capabilities per request
Output: 1 best model

Steps:
1. Filter active models: O(N)
2. Filter by capabilities: O(N * C) for each model, check all required capabilities
3. Filter by context window: O(N)
4. Filter by provider health: O(N) with O(1) hash map lookup per model
5. Score remaining models: O(N) -- constant-time score computation per model
6. Sort by score: O(N log N)
7. Tiebreak top tier: O(T) where T = models within 5% of top score
```

### Complexity

| Operation | Time | Space |
|-----------|------|-------|
| Filter phase | O(N * C) | O(N) for filtered list |
| Score phase | O(N) | O(N) for scored list |
| Sort phase | O(N log N) | O(log N) for sort stack |
| Tiebreak phase | O(T) where T << N | O(T) |
| **Total** | **O(N * C + N log N)** | **O(N)** |

### Practical Analysis

In production, N (number of available models) is typically 20-50, and C (required capabilities) is 1-3. The dominant term is O(N log N) from sorting, but with N < 100 this is effectively constant time. The entire routing operation completes in microseconds.

**Optimization**: The model registry is cached in memory (singleton). The filter + score step runs against an in-memory list with no I/O. No database queries are needed for routing.

```
N = 50 models, C = 3 capabilities
Filter:    50 * 3 = 150 comparisons
Score:     50 multiplications
Sort:      50 * log2(50) ~ 50 * 6 = 300 comparisons
Total:     ~500 operations -> <1 microsecond
```

---

## 8.2 Credit Consumption Calculation

### Algorithm

```
Input: CompletionRequest, AiModel, User (with plan, monthly usage, active coupon)
Output: Credits (final cost)

Steps:
1. Look up model cost table: O(1) hash map lookup
2. Calculate base cost: O(1) -- arithmetic: (inputTokens * rate / 1000) + (outputTokens * rate / 1000)
3. Apply plan discount: O(1) -- multiplication
4. Apply volume discount: O(1) -- conditional check on monthly usage
5. Apply coupon discount: O(1) -- conditional check + arithmetic
6. Apply minimum floor: O(1) -- max(1, result)
```

### Complexity

| Operation | Time | Space |
|-----------|------|-------|
| Cost table lookup | O(1) | O(1) |
| Base calculation | O(1) | O(1) |
| Discount chain (3 decorators) | O(D) where D = number of decorators | O(1) |
| **Total** | **O(1)** | **O(1)** |

### Practical Analysis

The decorator chain has a fixed depth (currently 3: plan, volume, coupon). Even if more discount types are added, D remains small and bounded, making this effectively O(1).

The critical concern is **atomicity**, not complexity. Credit consumption uses a database-level pessimistic lock (`SELECT ... FOR UPDATE`) to prevent race conditions:

```
Lock acquisition: depends on database contention
  - Low contention: O(1) -- immediate lock
  - High contention: O(W) where W = number of waiting transactions
  - Mitigated by: per-user locking (each user has their own row)
```

Since locks are per-user (not global), contention is naturally distributed. A single user sending concurrent requests is the only bottleneck scenario, and rate limiting prevents this.

---

## 8.3 Partner Commission Calculation

### Algorithm

```
Input: CreditsPurchasedEvent (with userId, amount, couponCode)
Output: Commission record saved

Steps:
1. Look up coupon by code: O(1) -- indexed database lookup
2. Look up partner by coupon.partnerId: O(1) -- indexed primary key lookup
3. Determine effective commission rate:
   a. Get base commission rate: O(1)
   b. Get tier bonus: O(1) -- tier -> bonus mapping
   c. Add rates: O(1)
4. Calculate commission amount: O(1) -- multiplication
5. Save commission record: O(1) -- database insert
6. Update partner earnings: O(1) -- database update
```

### Complexity

| Operation | Time | Space |
|-----------|------|-------|
| Coupon lookup | O(1) amortized (B-tree index) | O(1) |
| Partner lookup | O(1) (primary key) | O(1) |
| Rate calculation | O(1) | O(1) |
| DB writes (2 operations) | O(1) amortized | O(1) |
| **Total** | **O(1)** | **O(1)** |

### Batch Commission Report Generation

When generating commission reports for all partners over a time range:

```
Input: N partners, M total commissions in the time range
Output: Aggregated report

Steps:
1. Query commissions in date range: O(M) where M = matching records
   (B-tree index on partner_id + created_at makes range scan efficient)
2. Group by partner: O(M) with hash map
3. Calculate totals per partner: O(M) -- single pass
4. Sort by total (for ranking): O(N log N) where N = distinct partners

Total: O(M + N log N)
Space: O(N) for aggregation hash map
```

In practice, the database does the grouping with `GROUP BY`, making this a single optimized query:

```sql
SELECT partner_id, SUM(amount), COUNT(*)
FROM commissions
WHERE created_at BETWEEN :from AND :to
GROUP BY partner_id
ORDER BY SUM(amount) DESC;
```

Database complexity: O(M) scan + O(N log N) sort, but index-optimized.

---

## 8.4 Chat History Search

### Algorithm: Full-Text Search with PostgreSQL

```
Input: query string Q, user's conversations
Output: matching messages, ordered by relevance

Approach: PostgreSQL full-text search with tsvector/tsquery
```

### Database Schema

```sql
-- Add full-text search index
ALTER TABLE messages ADD COLUMN search_vector tsvector;

CREATE INDEX idx_messages_search ON messages USING GIN(search_vector);

-- Trigger to update search vector on insert/update
CREATE TRIGGER messages_search_update
    BEFORE INSERT OR UPDATE ON messages
    FOR EACH ROW EXECUTE FUNCTION
    tsvector_update_trigger(search_vector, 'pg_catalog.english', content);

-- Composite index for user-scoped search
CREATE INDEX idx_messages_conv_search
    ON messages(conversation_id) INCLUDE (search_vector);
```

### Complexity

```
Input: Q = query terms, N = total messages, U = user's messages, R = result count

Steps:
1. Parse query string to tsquery: O(|Q|) where |Q| = query length
2. Filter by user's conversations: O(log N) -- B-tree index on user_id/conversation_id
3. GIN index lookup for matching terms: O(|Q| * log V) where V = vocabulary size
4. Rank results by relevance: O(R log R)
5. Pagination (LIMIT/OFFSET): O(1) for first page, O(page * size) for deep pagination

Total query complexity: O(log N + |Q| * log V + R log R)
Space: O(R) for result set
```

### Practical Analysis

- GIN (Generalized Inverted Index) stores pre-computed word-to-document mappings, making term lookups O(log V)
- For a user with 1,000 conversations and 50,000 messages, the user-scoped filter narrows the search space dramatically
- `ts_rank` scoring is computed only for matching documents (R << U)
- For deeper search needs (semantic search), a future optimization is to add vector embeddings with pgvector

```
Typical case:
  N = 10M total messages (all users)
  U = 50K messages (one user)
  V = 500K vocabulary
  R = 50 matching messages

  Time: O(log 10M + 3 * log 500K + 50 * log 50)
      = O(23 + 3 * 19 + 50 * 6)
      = O(23 + 57 + 300)
      = O(380 comparisons)
      -> sub-millisecond with indexes
```

### Cursor-Based Pagination (for large result sets)

```java
// Instead of OFFSET-based pagination (O(offset + limit)):
// Use cursor-based pagination (O(log N + limit)):

public interface MessageRepository {
    /**
     * Cursor-based pagination for search results.
     * O(log N + limit) regardless of how deep into the result set.
     */
    List<Message> searchWithCursor(
        UserId userId,
        String query,
        UUID cursorMessageId,  // last message ID from previous page
        int limit
    );
}

// SQL:
// SELECT * FROM messages
// WHERE conversation_id IN (SELECT id FROM conversations WHERE user_id = :userId)
//   AND search_vector @@ to_tsquery(:query)
//   AND id < :cursorId
// ORDER BY ts_rank(search_vector, to_tsquery(:query)) DESC, id DESC
// LIMIT :limit
```

---

## 8.5 Semantic Cache Lookup

### Algorithm

```
Input: prompt content (string), model ID, parameters (temperature, maxTokens)
Output: Optional<CachedCompletion>

Steps:
1. Check if request is cacheable: O(1) -- temperature must be 0
2. Normalize prompt: O(P) where P = prompt length
   - lowercase: O(P)
   - trim whitespace: O(P)
   - collapse multiple spaces: O(P)
3. Build cache key string: O(P) -- concatenation
4. SHA-256 hash of key: O(P) -- single pass over key bytes
5. Redis GET lookup: O(1) -- hash map operation (Redis is O(1) for GET)
6. Deserialize if hit: O(R) where R = response size
```

### Complexity

| Operation | Time | Space |
|-----------|------|-------|
| Cacheability check | O(1) | O(1) |
| Prompt normalization | O(P) | O(P) for normalized string |
| SHA-256 hashing | O(P) | O(1) -- 32-byte output |
| Redis GET | O(1) | O(1) for key |
| Deserialization (on hit) | O(R) | O(R) for response object |
| **Total** | **O(P + R) on hit, O(P) on miss** | **O(P + R)** |

### Practical Analysis

```
Typical prompt P = 2,000 characters (~500 tokens)
Typical response R = 8,000 characters (~2,000 tokens)

Normalization: ~2,000 character operations
SHA-256:       ~2,000 byte operations
Redis GET:     <1ms network round trip

Total on cache hit:  ~10,000 operations + 1ms network -> ~1-2ms
Total on cache miss: ~4,000 operations + 1ms network  -> ~1ms

Compare to AI API call: 500ms - 30,000ms (0.5 - 30 seconds)
Cache speedup: 250x - 15,000x
```

### Cache Hit Rate Optimization

```
Cache effectiveness depends on:
1. Temperature = 0 (deterministic) -- REQUIRED for caching
2. Exact prompt match after normalization
3. Same model and parameters

Expected hit rates:
- Template-based prompts: 40-60% (users using same templates)
- Repeated queries: 20-30% (same questions asked by different users)
- Custom prompts: <5% (unique conversations)

Memory usage estimation:
- Average cached response: 10KB
- 100,000 cached entries: ~1GB Redis memory
- TTL: 24 hours for most entries, 1 hour for large responses
```

### Future Optimization: Vector Similarity Cache

For non-exact matches, a vector similarity approach could increase hit rates:

```
1. Embed prompt using a small embedding model: O(P) + API latency
2. Search Redis vector index for similar prompts: O(log N) with HNSW index
3. If similarity > threshold (e.g., 0.95), return cached result

This is a future optimization; the current exact-match approach is
simpler and has zero false positives.
```

---

## 8.6 Summary Table

| Critical Path | Time Complexity | Space Complexity | Typical Latency |
|---|---|---|---|
| Model auto-routing | O(N*C + N log N) | O(N) | <0.1ms |
| Credit calculation | O(1) | O(1) | <0.01ms |
| Credit consumption (with lock) | O(1) + lock wait | O(1) | 1-5ms |
| Commission calculation | O(1) | O(1) | 1-3ms |
| Commission report (batch) | O(M + N log N) | O(N) | 50-500ms |
| Chat history search (FTS) | O(log N + Q*log V + R log R) | O(R) | 1-10ms |
| Semantic cache lookup | O(P + R) | O(P + R) | 1-2ms |
| Semantic cache miss + AI call | O(P) + API latency | O(P) | 500-30,000ms |

Where:
- N = number of models / total records
- C = number of required capabilities
- M = matching records in range
- P = prompt length
- R = response/result size
- Q = number of query terms
- V = vocabulary size
