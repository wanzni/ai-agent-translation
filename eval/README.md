# Cross-Border E-commerce Eval

Offline evaluation toolkit for cross-border e-commerce translation quality.

## Layout

- `schema/`: sample schema definition
- `config/`: rule registry, scoring config, model config example
- `samples/v1/`: curated evaluation samples
- `glossary/`: seed terminology files extracted from evaluation samples
- `scripts/`: Python evaluator and helpers
- `tests/`: Python unit tests
- `results/`: generated outputs, ignored by git

## Quick Start

Run against your local translation service:

```bash
set AUTH_TOKEN=your_login_token
python eval/scripts/evaluator.py \
  --dataset-dir eval/samples/v1 \
  --model-config eval/config/model-config.example.json \
  --output-dir eval/results
```

The default HTTP config points to `http://127.0.0.1:7002/api/translation/translate` and sends:

- `Authorization: Bearer %AUTH_TOKEN%`
- `translationEngine: "QWEN"`
- `translationType: "TEXT"`
- `useTerminology: true`
- `useRag: true`
- `needQualityAssessment: false`
- `domain: "cross_border_ecommerce"`
- `requestSource: "offline_eval"`

The evaluator accepts either of these response shapes:

- `{ "translatedText": "..." }`
- `{ "success": true, "data": { "translatedText": "..." } }`

If your local service runs on another port or you want a different engine such as `ALIBABA_CLOUD`, update [model-config.example.json](G:/programme/Projects/translation-ai-agent/eval/config/model-config.example.json).

Run with the mock client instead:

```bash
python eval/scripts/evaluator.py \
  --dataset-dir eval/samples/v1 \
  --model-config eval/config/model-config.mock.json \
  --output-dir eval/results
```

## Seed Glossary

The first seed glossary extracted from the 8 evaluation samples is in [cross_border_ecommerce_seed_terms.csv](G:/programme/Projects/translation-ai-agent/eval/glossary/cross_border_ecommerce_seed_terms.csv).

Preview the glossary payloads without sending requests:

```bash
python eval/scripts/import_glossary.py --dry-run
```

Import the seed glossary through the existing create API:

```bash
set AUTH_TOKEN=your_login_token
set AUTH_USER_ID=your_user_id
python eval/scripts/import_glossary.py --base-url http://127.0.0.1:7002
```

Notes:

- The backend `importTerminology` file-upload service is still a stub, so the practical import path is `POST /api/terminology` per row.
- The script is idempotent enough for reseeding because the backend updates rows with the same `(sourceTerm, sourceLanguage, targetLanguage)` key.
- `AUTH_TOKEN` must be a real login token, not a plain user id, because `/api/terminology` and `/api/translation/translate` are behind the auth interceptor.

Run unit tests:

```bash
python -m unittest discover -s eval/tests -p "test_*.py"
```