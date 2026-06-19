# Cache Never Hit Because Timestamp Was Part of the Cache Key

## Problem
Redis caching by event hash was implemented and appeared to work — no errors, no warnings
— but identical events submitted multiple times never returned a cached result. Every
request re-ran the full pipeline, including all three agent calls and NLI classification.

## Root Cause
The cache key was built from three fields concatenated together:
```java
String raw = request.getEvent() + String.join(",", request.getAssetContext()) + request.getTimestamp().toString();
```

The frontend generates `timestamp` fresh on every submission:
```typescript
timestamp: new Date().toISOString()
```

This means the timestamp is unique down to the millisecond on every single request, even
for the exact same event text and asset list submitted seconds apart. Since the timestamp
is part of the hash input, the resulting cache key is effectively always different — the
cache was functioning correctly as code, but functionally inert as a feature.

## Why It Went Unnoticed
No error, no exception, no log warning. The cache simply never hit. It would only be
caught by deliberately checking Redis hit rates or submitting the identical request twice
and noticing the second one took just as long as the first.

## Fix
Removed `timestamp` from the cache key entirely. Caching should be based on what makes
two requests "the same" from a business logic perspective — the event text and the asset
context — not when the request happened to arrive.

```java
private String buildCacheKey(EventRequest request) {
    String raw = request.getEvent() + String.join(",", request.getAssetContext());
    return "amrs:analysis:" + DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
}
```

## Rule
When building a cache key, only include fields that determine semantic equivalence of the
request. Never include fields that are guaranteed to differ on every call (timestamps,
request IDs, correlation IDs) unless the intent is genuinely to never cache — in which case,
the cache shouldn't exist at all.