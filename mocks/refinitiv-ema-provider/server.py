"""T175 — Refinitiv EMA OmmProvider mock (localhost:14002).

Tiny socket server that speaks a JSON-line dialect mimicking the OMM update messages an
``OmmConsumer`` would receive. Real EMA is binary (RWF); we use JSON in dev so that integration
tests don't need the proprietary ``com.refinitiv.ema:ema:3.7.x`` JAR. The consumer-side bridge in
``apps/venue-adapter-refinitiv`` (Phase 14) decodes whichever transport is wired.

Run::

    uv run --project mocks/refinitiv-ema-provider python server.py
"""

from __future__ import annotations

import asyncio
import json
import logging
import random
import sys
import time

logging.basicConfig(stream=sys.stdout, level=logging.INFO,
                    format="%(asctime)s refinitiv-ema-provider %(message)s")
log = logging.getLogger("refinitiv-ema-provider")


async def handle_consumer(reader: asyncio.StreamReader, writer: asyncio.StreamWriter) -> None:
    peer = writer.get_extra_info("peername")
    log.info("OmmConsumer connected from %s", peer)
    rics = ["NESN.S", "NOVN.S", "ROG.S"]
    try:
        seq = 0
        while not writer.is_closing():
            ric = random.choice(rics)
            seq += 1
            update = {
                "type": "OMM_UPDATE",
                "ric": ric,
                "domain": "MARKET_PRICE",
                "fields": {
                    "BID": round(80 + random.random() * 40, 4),
                    "ASK": round(80 + random.random() * 40, 4),
                    "BIDSIZE": random.randint(100, 5_000),
                    "ASKSIZE": random.randint(100, 5_000),
                },
                "seq": seq,
                "ts": int(time.time() * 1000),
            }
            writer.write((json.dumps(update) + "\n").encode("utf-8"))
            await writer.drain()
            await asyncio.sleep(0.01)
    except (ConnectionResetError, BrokenPipeError):
        log.info("OmmConsumer %s disconnected", peer)
    finally:
        writer.close()


async def main() -> None:
    server = await asyncio.start_server(handle_consumer, host="0.0.0.0", port=14002)
    log.info("Refinitiv EMA OmmProvider mock listening on tcp://0.0.0.0:14002")
    async with server:
        await server.serve_forever()


if __name__ == "__main__":  # pragma: no cover
    asyncio.run(main())
