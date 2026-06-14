# Qdrant search() Method No Longer Available

## Symptom
AttributeError: 'QdrantClient' object has no attribute 'search'

## Cause
qdrant-client v1.13+ removed the .search() method and replaced it with .query_points().

## Fix
Replace:
```python
results = qdrant.search(
    collection_name=QDRANT_COLLECTION,
    query_vector=embedding,
    limit=5,
    score_threshold=SIMILARITY_THRESHOLD,
)
```
With:
```python
results = qdrant.query_points(
    collection_name=QDRANT_COLLECTION,
    query=embedding,
    limit=5,
).points
```

Note: score_threshold filtering must now be done manually in Python after fetching results.