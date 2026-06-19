# UTF-8 Encoding Corruption When Reading Seed Files on Windows

## Problem
Special characters (em-dashes, smart quotes) in `events.json` were corrupted when
read by `seed.py` — `—` became `â€"` in Qdrant payloads.

## Root Cause
`open(EVENTS_PATH, "r")` with no explicit encoding defaults to the OS locale encoding.
On Windows this is typically CP1252, not UTF-8, so multi-byte UTF-8 characters get
misinterpreted.

## Fix
Always specify `encoding="utf-8"` explicitly on every file read in Python, regardless
of platform:

```python
with open(EVENTS_PATH, "r", encoding="utf-8") as f:
```

## Rule
Any Python file I/O in this project must specify encoding explicitly. Never rely on
platform default encoding, since the dev environment is Windows but the Docker
container is Linux — behaviour differs silently between them.