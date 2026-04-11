import argparse
import csv
import json
import os
import sys
from pathlib import Path
from urllib import error as urllib_error
from urllib import request as urllib_request


DEFAULT_FILE = Path("eval/glossary/cross_border_ecommerce_seed_terms.csv")
DEFAULT_ENDPOINT = "/api/terminology"


def load_rows(csv_path):
    with csv_path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        rows = []
        for index, row in enumerate(reader, start=2):
            source_term = (row.get("sourceTerm") or "").strip()
            target_term = (row.get("targetTerm") or "").strip()
            if not source_term or not target_term:
                raise ValueError(f"Invalid glossary row at line {index}: sourceTerm/targetTerm is required")
            rows.append({
                "sourceTerm": source_term,
                "targetTerm": target_term,
                "sourceLanguage": (row.get("sourceLanguage") or "zh").strip() or "zh",
                "targetLanguage": (row.get("targetLanguage") or "en").strip() or "en",
                "category": (row.get("category") or "GENERAL").strip() or "GENERAL",
                "domain": (row.get("domain") or "").strip(),
                "definition": (row.get("definition") or "").strip(),
                "context": (row.get("context") or "").strip(),
            })
        return rows


def build_headers(token, user_id):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if user_id:
        headers["X-User-Id"] = str(user_id)
    return headers


def import_row(base_url, endpoint, headers, payload, timeout_seconds):
    req = urllib_request.Request(
        base_url.rstrip("/") + endpoint,
        data=json.dumps(payload).encode("utf-8"),
        headers=headers,
        method="POST",
    )
    with urllib_request.urlopen(req, timeout=timeout_seconds) as response:
        charset = response.headers.get_content_charset() or "utf-8"
        body = response.read().decode(charset)
        return response.status, json.loads(body) if body else {}


def main():
    parser = argparse.ArgumentParser(description="Import seed glossary rows into /api/terminology")
    parser.add_argument("--file", default=str(DEFAULT_FILE), help="CSV glossary file path")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080", help="Backend base URL")
    parser.add_argument("--endpoint", default=DEFAULT_ENDPOINT, help="Terminology create endpoint")
    parser.add_argument("--token", default=os.getenv("AUTH_TOKEN"), help="Auth token, defaults to AUTH_TOKEN env")
    parser.add_argument("--user-id", default=os.getenv("AUTH_USER_ID"), help="Optional X-User-Id header")
    parser.add_argument("--domain-override", help="Override domain for every row")
    parser.add_argument("--dry-run", action="store_true", help="Print payloads without sending requests")
    parser.add_argument("--timeout-seconds", type=int, default=30, help="HTTP timeout seconds")
    args = parser.parse_args()

    csv_path = Path(args.file)
    rows = load_rows(csv_path)

    if args.domain_override:
        for row in rows:
            row["domain"] = args.domain_override

    if args.dry_run:
        print(json.dumps({"count": len(rows), "rows": rows}, ensure_ascii=False, indent=2))
        return 0

    if not args.token:
        print("Missing token. Pass --token or set AUTH_TOKEN.", file=sys.stderr)
        return 1

    headers = build_headers(args.token, args.user_id)
    success = 0
    failures = []

    for row in rows:
        try:
            status, body = import_row(args.base_url, args.endpoint, headers, row, args.timeout_seconds)
            success += 1
            print(f"[OK] {row['sourceTerm']} -> {row['targetTerm']} (status={status})")
        except urllib_error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            failures.append({"sourceTerm": row["sourceTerm"], "status": exc.code, "body": body})
            print(f"[FAIL] {row['sourceTerm']} (status={exc.code}) {body}", file=sys.stderr)
        except Exception as exc:
            failures.append({"sourceTerm": row["sourceTerm"], "status": None, "body": str(exc)})
            print(f"[FAIL] {row['sourceTerm']} {exc}", file=sys.stderr)

    print(json.dumps({"total": len(rows), "success": success, "failed": len(failures), "failures": failures}, ensure_ascii=False, indent=2))
    return 0 if not failures else 2


if __name__ == "__main__":
    raise SystemExit(main())
