"""Property tests for the layering detector."""

from __future__ import annotations

import pytest
from hypothesis import given, settings, strategies as st

from surveillance.patterns.layering_spoofing import OrderEvent, detect_layering


pytestmark = pytest.mark.property


def test_obvious_layering_pattern_triggers_alert():
    base = 1_700_000_000_000_000
    events = [
        # Layered BUY orders (placed and quickly cancelled)
        OrderEvent("alice", "CH0038863350", "BUY", "PLACED", 100, 99.40, base + 0),
        OrderEvent("alice", "CH0038863350", "BUY", "PLACED", 100, 99.45, base + 100_000),
        OrderEvent("alice", "CH0038863350", "BUY", "PLACED", 100, 99.50, base + 200_000),
        OrderEvent("alice", "CH0038863350", "BUY", "CANCELED", 100, 99.40, base + 300_000),
        OrderEvent("alice", "CH0038863350", "BUY", "CANCELED", 100, 99.45, base + 400_000),
        OrderEvent("alice", "CH0038863350", "BUY", "CANCELED", 100, 99.50, base + 500_000),
        # Real SELL fill
        OrderEvent("alice", "CH0038863350", "SELL", "FILLED", 200, 99.51, base + 600_000),
    ]
    alerts = list(detect_layering(events))
    assert len(alerts) >= 1
    assert alerts[0].pattern == "LAYERING"
    assert alerts[0].trader_id == "alice"


def test_normal_trading_does_not_trigger_alert():
    base = 1_700_000_000_000_000
    events = [
        OrderEvent("bob", "CH0038863350", "BUY", "PLACED", 100, 99.50, base),
        OrderEvent("bob", "CH0038863350", "BUY", "FILLED", 100, 99.50, base + 100_000),
    ]
    assert list(detect_layering(events)) == []


@given(
    placed_count=st.integers(min_value=1, max_value=2),
    fills=st.integers(min_value=0, max_value=3),
)
@settings(max_examples=50)
def test_below_threshold_never_alerts(placed_count, fills):
    base = 1_700_000_000_000_000
    events = [
        OrderEvent("carol", "CH0038863350", "BUY", "PLACED", 50, 100.0, base + i * 100_000)
        for i in range(placed_count)
    ] + [
        OrderEvent("carol", "CH0038863350", "SELL", "FILLED", 50, 100.0, base + 1_000_000 + i * 100_000)
        for i in range(fills)
    ]
    # < min_layered_orders → no alert
    assert list(detect_layering(events)) == []
