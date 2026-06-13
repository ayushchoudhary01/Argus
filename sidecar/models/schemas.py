from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field


class DataQuality(str, Enum):
    VERIFIED = "VERIFIED"
    UNAVAILABLE = "UNAVAILABLE"


class HistoricalCoverage(str, Enum):
    SUFFICIENT = "SUFFICIENT"
    INSUFFICIENT = "INSUFFICIENT"


class CausalDirection(str, Enum):
    UPWARD_PRESSURE = "UPWARD_PRESSURE"
    DOWNWARD_PRESSURE = "DOWNWARD_PRESSURE"


class DivergenceAssessment(str, Enum):
    SUBSTANTIVE = "SUBSTANTIVE"
    LOW_VARIANCE = "LOW_VARIANCE"
    DATA_GAP = "DATA_GAP"


class ConflictType(str, Enum):
    DIRECTIONAL_CONFLICT = "DIRECTIONAL_CONFLICT"
    FLAGGED_TENSION = "FLAGGED_TENSION"
    ENTAILMENT = "ENTAILMENT"
    NEUTRAL = "NEUTRAL"


class DetectionMethod(str, Enum):
    CAUSAL_CHAIN_COMPARISON = "causal_chain_comparison"
    DEBERTA_NLI = "deberta_nli"

class AnalyticsRequest(BaseModel):
    event: str = Field(..., min_length=10, max_length=2000)
    asset_context: list[str] = Field(..., min_length=1)
    timestamp: str

class VolatilityContext(BaseModel):
    vix_level: Optional[float]
    percentile_30d: Optional[float]


class TechnicalContext(BaseModel):
    spy_rsi_14: Optional[float]
    spy_trend: Optional[str]


class MacroContext(BaseModel):
    yield_curve_slope: Optional[float]
    dxy_momentum: Optional[str]


class EventOutcome(BaseModel):
    ticker: str
    window_days: int
    return_pct: Optional[float]
    quality: DataQuality


class HistoricalAnalogue(BaseModel):
    event_id: str
    title: str
    date: str
    similarity_score: float
    what_happened: str
    outcomes: list[EventOutcome]


class MarketContext(BaseModel):
    volatility: VolatilityContext
    technical: TechnicalContext
    macro: MacroContext
    anomaly_flags: list[str]
    historical_coverage: HistoricalCoverage
    historical_similarity: list[HistoricalAnalogue]

class Assumption(BaseModel):
    id: str
    text: str
    agent: str


class CausalChain(BaseModel):
    driver: str
    direction: CausalDirection
    target_variable: str
    agent: str


class NLIRequest(BaseModel):
    assumptions: list[Assumption]
    causal_chains: list[CausalChain]

class DirectionalConflict(BaseModel):
    premise: str
    hypothesis: str
    type: ConflictType = ConflictType.DIRECTIONAL_CONFLICT
    method: DetectionMethod = DetectionMethod.CAUSAL_CHAIN_COMPARISON
    agents: list[str]
    target_variable: str


class FlaggedTension(BaseModel):
    premise: Assumption
    hypothesis: Assumption
    label: ConflictType = ConflictType.FLAGGED_TENSION
    probability: float
    method: DetectionMethod = DetectionMethod.DEBERTA_NLI
    note: str = "Linguistic tension detected. Economic interpretation required."


class ContradictionMap(BaseModel):
    directional_conflicts: list[DirectionalConflict]
    flagged_tensions: list[FlaggedTension]
    entailments: list[dict]
    neutral_pairs: list[dict]