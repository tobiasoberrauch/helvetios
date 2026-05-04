"""Property-based tests for the simplefix wrappers."""

from __future__ import annotations

import pytest
from hypothesis import given, settings, strategies as st

from swisstms_fix_codec import build_new_order_single, parse_message


pytestmark = pytest.mark.property


@given(
    cl_ord_id=st.text(alphabet=st.characters(min_codepoint=0x21, max_codepoint=0x7E,
                                              blacklist_characters="="), min_size=1, max_size=32),
    symbol=st.from_regex(r"[A-Z]{2,5}\.SW", fullmatch=True),
    side=st.sampled_from(["1", "2", "5"]),
    order_qty=st.integers(min_value=1, max_value=10_000_000),
    ord_type=st.sampled_from(["1", "2"]),
    price=st.decimals(min_value=1, max_value=999, places=2, allow_nan=False, allow_infinity=False),
    msg_seq_num=st.integers(min_value=1, max_value=10_000),
)
@settings(max_examples=200, deadline=None)
def test_new_order_single_roundtrip(cl_ord_id, symbol, side, order_qty, ord_type, price, msg_seq_num):
    raw = build_new_order_single(
        sender_comp_id="SWISSTMS",
        target_comp_id="SIX-STI",
        msg_seq_num=msg_seq_num,
        cl_ord_id=cl_ord_id,
        symbol=symbol,
        side=side,
        order_qty=order_qty,
        ord_type=ord_type,
        price=float(price) if ord_type == "2" else None,
    )
    fields = parse_message(raw)
    assert fields[8] == "FIX.4.4"
    assert fields[35] == "D"
    assert fields[11] == cl_ord_id
    assert fields[55] == symbol
    assert fields[54] == side
    assert int(fields[38]) == order_qty
    assert fields[40] == ord_type
