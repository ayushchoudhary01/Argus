# yfinance Returns Multi-Level DataFrame Columns

## Symptom
float() argument must be a string or a real number, not 'Series'
Occurs in the seed script when computing price returns from yfinance data.

## Cause
Newer versions of yfinance return a DataFrame with multi-level column headers when `auto_adjust=True`. Accessing `df["Close"].iloc[0]` returns a Series instead of a scalar float.

## Fix
Extract the scalar value explicitly:
```python
price_start = float(df["Close"].iloc[0].values[0])
price_end = float(df["Close"].iloc[target_index].values[0])
```