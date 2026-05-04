"""FastAPI entrypoint for the reference-data service.

Phase 8 (T182, T183) wires three repositories — Instrument, LegalEntity, Calendar — and a
nightly Bloomberg Data License ingest endpoint that reads the SHA-256 of the file delivered by
``apps/venue-adapter-bloomberg`` and merges new rows into the in-memory store.

Phase 14 swaps the in-memory dicts for psycopg + Postgres ``instrument_master`` /
``legal_entity`` / ``business_calendar`` tables.
"""

from __future__ import annotations

import csv
import datetime as dt
import hashlib
import io
import logging
from typing import Annotated

from fastapi import FastAPI, HTTPException, UploadFile, File
from pydantic import BaseModel, Field

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s reference-data %(message)s")
log = logging.getLogger("reference-data")

app = FastAPI(title="Swiss-TMS Reference Data Service", version="0.2.0")


class Instrument(BaseModel):
    isin: str = Field(pattern=r"^[A-Z]{2}[A-Z0-9]{9}[0-9]$")
    primary_mic: str = Field(pattern=r"^[A-Z0-9]{4}$")
    symbol: str
    asset_class: str
    currency: str = Field(pattern=r"^[A-Z]{3}$")
    tick_size: float
    lot_size: float


class LegalEntity(BaseModel):
    lei: str = Field(pattern=r"^[A-Z0-9]{18}[0-9]{2}$")
    legal_name: str
    country: str = Field(pattern=r"^[A-Z]{2}$")
    entity_status: str = "ACTIVE"


class Calendar(BaseModel):
    name: str
    holidays: list[dt.date]


# In-memory stores — replaced by Postgres in Phase 14.
_INSTRUMENTS: dict[tuple[str, str], Instrument] = {
    ("CH0038863350", "XSWX"): Instrument(
        isin="CH0038863350", primary_mic="XSWX", symbol="NESN",
        asset_class="EQUITY", currency="CHF", tick_size=0.01, lot_size=1.0,
    ),
    ("CH0012005267", "XSWX"): Instrument(
        isin="CH0012005267", primary_mic="XSWX", symbol="NOVN",
        asset_class="EQUITY", currency="CHF", tick_size=0.01, lot_size=1.0,
    ),
}

_LEGAL_ENTITIES: dict[str, LegalEntity] = {
    "5493001KJTIIGC8Y1R12": LegalEntity(
        lei="5493001KJTIIGC8Y1R12", legal_name="Acme Capital AG", country="CH",
    ),
}

_CALENDARS: dict[str, Calendar] = {
    "ZURICH": Calendar(
        name="ZURICH",
        holidays=[
            dt.date(2026, 1, 1),
            dt.date(2026, 4, 3),
            dt.date(2026, 4, 6),
            dt.date(2026, 5, 1),
            dt.date(2026, 8, 1),
            dt.date(2026, 12, 25),
            dt.date(2026, 12, 26),
        ],
    ),
}


@app.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "UP",
        "instruments": str(len(_INSTRUMENTS)),
        "legalEntities": str(len(_LEGAL_ENTITIES)),
        "calendars": str(len(_CALENDARS)),
    }


@app.get("/instruments/{isin}/{mic}", response_model=Instrument)
def get_instrument(isin: str, mic: str) -> Instrument:
    instrument = _INSTRUMENTS.get((isin.upper(), mic.upper()))
    if instrument is None:
        raise HTTPException(status_code=404, detail=f"Instrument {isin}@{mic} not found")
    return instrument


@app.put("/instruments/{isin}/{mic}", response_model=Instrument, status_code=201)
def upsert_instrument(isin: str, mic: str, instrument: Instrument) -> Instrument:
    _INSTRUMENTS[(isin.upper(), mic.upper())] = instrument
    return instrument


@app.get("/instruments")
def list_instruments() -> list[Instrument]:
    return list(_INSTRUMENTS.values())


@app.get("/legal-entities/{lei}", response_model=LegalEntity)
def get_legal_entity(lei: str) -> LegalEntity:
    le = _LEGAL_ENTITIES.get(lei.upper())
    if le is None:
        raise HTTPException(status_code=404, detail=f"LEI {lei} not found")
    return le


@app.put("/legal-entities/{lei}", response_model=LegalEntity, status_code=201)
def upsert_legal_entity(lei: str, le: LegalEntity) -> LegalEntity:
    _LEGAL_ENTITIES[lei.upper()] = le
    return le


@app.get("/calendars/{name}", response_model=Calendar)
def get_calendar(name: str) -> Calendar:
    cal = _CALENDARS.get(name.upper())
    if cal is None:
        raise HTTPException(status_code=404, detail=f"Calendar {name} not found")
    return cal


@app.post("/ingest/bloomberg-dl", status_code=202)
async def ingest_bloomberg_dl(file: Annotated[UploadFile, File()]) -> dict[str, str]:
    """T183 — Bloomberg DL nightly ingest.

    Body is the CSV the puller pulled. We compute SHA-256 (so the audit chain in the puller
    matches), then upsert each row into ``_INSTRUMENTS``. Phase 14 streams to Postgres.
    """
    payload = await file.read()
    sha = hashlib.sha256(payload).hexdigest()
    text = payload.decode("utf-8", errors="strict")
    reader = csv.DictReader(io.StringIO(text))
    upserted = 0
    for row in reader:
        try:
            inst = Instrument(
                isin=row["isin"].upper(),
                primary_mic=row["mic"].upper(),
                symbol=row["symbol"],
                asset_class=row["asset_class"],
                currency=row["currency"].upper(),
                tick_size=float(row["tick_size"]),
                lot_size=float(row["lot_size"]),
            )
        except (KeyError, ValueError) as exc:
            log.warning("Skipping malformed DL row: %s", exc)
            continue
        _INSTRUMENTS[(inst.isin, inst.primary_mic)] = inst
        upserted += 1
    log.info("Bloomberg DL ingest: %d rows upserted (sha256=%s)", upserted, sha)
    return {"status": "ACCEPTED", "rowsUpserted": str(upserted), "sha256": sha}
