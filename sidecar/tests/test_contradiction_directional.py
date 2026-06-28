from models.schemas import CausalChain, CausalDirection
from services.contradiction import _detect_directional_conflicts


def _chain(agent: str, direction: CausalDirection, target_variable: str = "Federal Reserve Terminal Rate") -> CausalChain:
    return CausalChain(
        driver=f"{agent} driver",
        direction=direction,
        target_variable=target_variable,
        agent=agent,
    )


def test_opposing_directions_on_same_target_variable_is_flagged():
    chains = [
        _chain("structural", CausalDirection.UPWARD_PRESSURE),
        _chain("contrarian", CausalDirection.DOWNWARD_PRESSURE),
    ]

    conflicts, skipped_pairs = _detect_directional_conflicts(chains)

    assert len(conflicts) == 1
    assert conflicts[0].agents == ["structural", "contrarian"]
    assert conflicts[0].target_variable == "Federal Reserve Terminal Rate"


def test_same_direction_on_same_target_variable_is_not_flagged():
    chains = [
        _chain("structural", CausalDirection.UPWARD_PRESSURE),
        _chain("risk", CausalDirection.UPWARD_PRESSURE),
    ]

    conflicts, skipped_pairs = _detect_directional_conflicts(chains)

    assert conflicts == []
    assert skipped_pairs == set()


def test_opposing_directions_on_different_target_variables_is_not_flagged():
    chains = [
        _chain("structural", CausalDirection.UPWARD_PRESSURE, target_variable="Inflation"),
        _chain("risk", CausalDirection.DOWNWARD_PRESSURE, target_variable="Corporate Profit Margins"),
    ]

    conflicts, skipped_pairs = _detect_directional_conflicts(chains)

    assert conflicts == []


def test_target_variable_comparison_is_case_insensitive():
    chains = [
        _chain("structural", CausalDirection.UPWARD_PRESSURE, target_variable="Inflation Expectations"),
        _chain("contrarian", CausalDirection.DOWNWARD_PRESSURE, target_variable="INFLATION EXPECTATIONS"),
    ]

    conflicts, skipped_pairs = _detect_directional_conflicts(chains)

    assert len(conflicts) == 1


def test_flagged_pair_is_recorded_in_skipped_pairs_both_directions():
    chains = [
        _chain("structural", CausalDirection.UPWARD_PRESSURE),
        _chain("contrarian", CausalDirection.DOWNWARD_PRESSURE),
    ]

    _, skipped_pairs = _detect_directional_conflicts(chains)

    assert ("structural", "contrarian") in skipped_pairs
    assert ("contrarian", "structural") in skipped_pairs


def test_three_agents_with_one_conflicting_pair_only_flags_that_pair():
    chains = [
        _chain("structural", CausalDirection.UPWARD_PRESSURE, target_variable="Terminal Rate"),
        _chain("risk", CausalDirection.UPWARD_PRESSURE, target_variable="Market Volatility"),
        _chain("contrarian", CausalDirection.DOWNWARD_PRESSURE, target_variable="Terminal Rate"),
    ]

    conflicts, skipped_pairs = _detect_directional_conflicts(chains)

    assert len(conflicts) == 1
    assert conflicts[0].agents == ["structural", "contrarian"]


def test_no_chains_produces_no_conflicts():
    conflicts, skipped_pairs = _detect_directional_conflicts([])

    assert conflicts == []
    assert skipped_pairs == set()


def test_single_chain_produces_no_conflicts():
    conflicts, skipped_pairs = _detect_directional_conflicts([
        _chain("structural", CausalDirection.UPWARD_PRESSURE),
    ])

    assert conflicts == []
