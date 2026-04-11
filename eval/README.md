# Cross-Border E-commerce Eval

Offline evaluation toolkit for cross-border e-commerce translation quality.

## Layout

- `schema/`: sample schema definition
- `config/`: rule registry, scoring config, model config example
- `samples/v1/`: curated evaluation samples
- `scripts/`: Python evaluator and helpers
- `tests/`: Python unit tests
- `results/`: generated outputs, ignored by git

## Quick Start

Run against your local translation service:

```bash
python eval/scripts/evaluator.py \
  --dataset-dir eval/samples/v1 \
  --model-config eval/config/model-config.example.json \
  --output-dir eval/results
```

The default HTTP config points to `http://127.0.0.1:8080/api/translation/translate` and sends:

- `translationEngine: "QWEN"`
- `translationType: "TEXT"`
- `useTerminology: true`
- `useRag: true`
- `needQualityAssessment: false`
- `domain: "cross_border_ecommerce"`
- `requestSource: "offline_eval"`

If your local service runs on another port or you want a different engine such as `ALIBABA_CLOUD`, update [model-config.example.json](G:/programme/Projects/translation-ai-agent/eval/config/model-config.example.json).

Run with the mock client instead:

```bash
python eval/scripts/evaluator.py \
  --dataset-dir eval/samples/v1 \
  --model-config eval/config/model-config.mock.json \
  --output-dir eval/results
```

Run unit tests:

```bash
python -m unittest discover -s eval/tests -p "test_*.py"
```
