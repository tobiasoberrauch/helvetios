"""Minimal FIX 4.4 helpers built on simplefix."""

from __future__ import annotations

import simplefix


def build_new_order_single(
    *,
    sender_comp_id: str,
    target_comp_id: str,
    msg_seq_num: int,
    cl_ord_id: str,
    symbol: str,
    side: str,
    order_qty: int,
    ord_type: str,
    price: float | None = None,
) -> bytes:
    """Build a FIX 4.4 NewOrderSingle (35=D) and return the raw bytes."""
    msg = simplefix.FixMessage()
    msg.append_pair(8, "FIX.4.4")
    msg.append_pair(35, "D")
    msg.append_pair(49, sender_comp_id)
    msg.append_pair(56, target_comp_id)
    msg.append_pair(34, msg_seq_num)
    msg.append_utc_timestamp(52, precision=6, header=True)
    msg.append_pair(11, cl_ord_id)
    msg.append_pair(21, "1")  # HandlInst — Automated, no intervention
    msg.append_pair(55, symbol)
    msg.append_pair(54, side)
    msg.append_pair(38, str(order_qty))
    msg.append_pair(40, ord_type)
    if price is not None:
        msg.append_pair(44, str(price))
    msg.append_utc_timestamp(60, precision=6)
    return msg.encode()


def parse_message(raw: bytes) -> dict[int, str]:
    """Parse a raw FIX bytestream into a tag → value dictionary."""
    parser = simplefix.FixParser()
    parser.append_buffer(raw)
    msg = parser.get_message()
    if msg is None:
        raise ValueError("Incomplete FIX message")
    return {int(tag): value.decode() for tag, value in msg.pairs}
