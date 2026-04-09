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

Run with the mock client:

```bash
python eval/scripts/evaluator.py \
  --dataset-dir eval/samples/v1 \
  --model-config eval/config/model-config.example.json \
  --output-dir eval/results
```

Run unit tests:

```bash
python -m unittest discover -s eval/tests -p "test_*.py"
```
