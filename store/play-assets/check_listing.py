"""Checks LISTING.md's Play Console fields against their character limits.

Usage: python -I check_listing.py   (from any directory)
"""
import re
from pathlib import Path

LIMITS = {"App name": 30, "Short description": 80, "Full description": 4000, "What's new": 500}

text = (Path(__file__).parent / "LISTING.md").read_text(encoding="utf-8")
failed = False
for heading, body in re.findall(r"^## (.+?)\n+```\n(.*?)\n```", text, flags=re.S | re.M):
    field = next((name for name in LIMITS if heading.startswith(name)), None)
    if field is None:
        continue
    length, limit = len(body), LIMITS[field]
    failed |= length > limit
    print(f"{'OK  ' if length <= limit else 'OVER'} {field}: {length}/{limit}")
raise SystemExit(1 if failed else 0)
