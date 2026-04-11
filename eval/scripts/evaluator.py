import argparse
import json
import time
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path
from urllib import request as urllib_request

from loaders import discover_samples, load_rule_registry, load_scoring_config, read_json, write_json, write_jsonl
from matchers import evaluate_critical_failures, evaluate_numbers, evaluate_protected_tokens, evaluate_terms
from reporters import build_markdown_report


class MockTranslationClient:
    def __init__(self, config):
        self.strategy = config.get("strategy", "reference")
        self.fixtures = config.get("fixtures", {})
        self.name = config.get("name", "mock")

    def translate(self, sample):
        if self.strategy == "reference":
            return sample["referenceText"]
        if self.strategy == "source":
            return sample["sourceText"]
        if self.strategy == "fixtures":
            return self.fixtures.get(sample["caseId"], sample["referenceText"])
        raise ValueError(f"Unsupported mock strategy: {self.strategy}")


class HttpTranslationClient:
    def __init__(self, config):
        self.base_url = config["baseUrl"].rstrip("/")
        self.endpoint = config.get("endpoint", "/api/translation/translate")
        self.translation_engine = config.get("translationEngine", "QWEN")
        self.translation_type = config.get("translationType", "TEXT")
        self.use_terminology = config.get("useTerminology", True)
        self.use_rag = config.get("useRag", True)
        self.need_quality_assessment = config.get("needQualityAssessment", False)
        self.priority = config.get("priority", 1)
        self.domain = config.get("domain")
        self.request_source = config.get("requestSource", "offline_eval")
        self.client_info = config.get("clientInfo")
        self.timeout_seconds = config.get("timeoutSeconds", 60)
        self.name = config.get("name", "http")

    def translate(self, sample):
        payload = {
            "sourceText": sample["sourceText"],
            "sourceLanguage": sample["sourceLanguage"],
            "targetLanguage": sample["targetLanguage"],
            "translationType": self.translation_type,
            "translationEngine": self.translation_engine,
            "useTerminology": self.use_terminology,
            "useRag": self.use_rag,
            "needQualityAssessment": self.need_quality_assessment,
            "priority": self.priority,
            "domain": self.domain,
            "requestSource": self.request_source,
        }
        if self.client_info is not None:
            payload["clientInfo"] = self.client_info
        req = urllib_request.Request(
            self.base_url + self.endpoint,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib_request.urlopen(req, timeout=self.timeout_seconds) as response:
            body = json.loads(response.read().decode("utf-8"))
        translated_text = body.get("translatedText")
        if not translated_text:
            raise ValueError(f"No translatedText found in response for case {sample['caseId']}")
        return translated_text


def build_client(model_config_path):
    config = read_json(model_config_path)
    client_type = config.get("type")
    if client_type == "mock":
        return MockTranslationClient(config), config
    if client_type == "http":
        return HttpTranslationClient(config), config
    raise ValueError(f"Unsupported model config type: {client_type}")


def ratio(results, key):
    if not results:
        return 1.0
    return sum(1 for item in results if item[key]) / len(results)


def compute_sample_score(category_weights, term_results, number_results, protected_results, critical_failures):
    term_accuracy = ratio(term_results, "matched")
    number_accuracy = ratio(number_results, "matched")
    protected_retention = ratio(protected_results, "retained")
    critical_penalty = 0.0 if critical_failures else 1.0
    score = (
        term_accuracy * category_weights["termAccuracy"]
        + number_accuracy * category_weights["numberAccuracy"]
        + protected_retention * category_weights["protectedTokenRetention"]
        + critical_penalty * category_weights["criticalFailurePenalty"]
    )
    return round(score, 4)


def evaluate_dataset(samples, client, scoring_config):
    details = []
    for sample in samples:
        started = time.perf_counter()
        output = client.translate(sample)
        elapsed_ms = round((time.perf_counter() - started) * 1000)

        term_results = evaluate_terms(output, sample)
        number_results = evaluate_numbers(output, sample)
        protected_results = evaluate_protected_tokens(output, sample)
        critical_failures = evaluate_critical_failures(sample, term_results, number_results, protected_results, output)
        weights = scoring_config[sample["category"]]
        sample_score = compute_sample_score(weights, term_results, number_results, protected_results, critical_failures)

        details.append(
            {
                "caseId": sample["caseId"],
                "category": sample["category"],
                "modelOutput": output,
                "termResults": term_results,
                "numberResults": number_results,
                "protectedTokenResults": protected_results,
                "criticalFailures": critical_failures,
                "sampleScore": sample_score,
                "processingTimeMs": elapsed_ms,
                "timestamp": datetime.now(timezone.utc).isoformat(),
            }
        )
    return details


def build_summary(run_id, run_name, model_config, samples, details):
    category_groups = defaultdict(list)
    for detail in details:
        category_groups[detail["category"]].append(detail)

    def category_metrics(items):
        return {
            "overallScore": round(sum(item["sampleScore"] for item in items) / len(items), 4),
            "termAccuracy": round(sum(ratio(item["termResults"], "matched") for item in items) / len(items), 4),
            "numberAccuracy": round(sum(ratio(item["numberResults"], "matched") for item in items) / len(items), 4),
            "protectedTokenRetention": round(sum(ratio(item["protectedTokenResults"], "retained") for item in items) / len(items), 4),
            "criticalFailureRate": round(sum(1 for item in items if item["criticalFailures"]) / len(items), 4),
        }

    overall = category_metrics(details)
    dataset_counter = Counter(sample["category"] for sample in samples)
    return {
        "runId": run_id,
        "runName": run_name,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "modelConfig": model_config,
        "datasetInfo": {
            "datasetName": "cross-border-ecom-eval",
            "schemaVersion": "cross-border-ecom-eval/v3",
            "totalSamples": len(samples),
            "categories": dict(dataset_counter),
        },
        "overallScore": overall["overallScore"],
        "termAccuracy": overall["termAccuracy"],
        "numberAccuracy": overall["numberAccuracy"],
        "protectedTokenRetention": overall["protectedTokenRetention"],
        "criticalFailureRate": overall["criticalFailureRate"],
        "categoryBreakdown": {category: category_metrics(items) for category, items in sorted(category_groups.items())},
    }


def parse_categories(raw_value):
    if not raw_value:
        return None
    return [item.strip() for item in raw_value.split(",") if item.strip()]


def main():
    parser = argparse.ArgumentParser(description="Cross-border e-commerce translation evaluator")
    parser.add_argument("--dataset-dir", required=True, help="Path to dataset directory containing JSONL files")
    parser.add_argument("--model-config", required=True, help="Path to model config JSON")
    parser.add_argument("--output-dir", required=True, help="Path to write evaluation results")
    parser.add_argument("--categories", help="Comma-separated category filter")
    parser.add_argument("--rule-registry", default="eval/config/rule-registry.json", help="Path to critical rule registry JSON")
    parser.add_argument("--scoring-config", default="eval/config/scoring-config.json", help="Path to scoring config JSON")
    parser.add_argument("--run-name", default="offline-eval", help="Optional run name")
    args = parser.parse_args()

    categories = parse_categories(args.categories)
    samples = discover_samples(args.dataset_dir, categories)
    load_rule_registry(args.rule_registry)
    scoring_config = load_scoring_config(args.scoring_config)
    client, model_config = build_client(args.model_config)
    run_id = f"run-{datetime.now().strftime('%Y%m%d-%H%M%S')}"
    output_dir = Path(args.output_dir) / run_id

    details = evaluate_dataset(samples, client, scoring_config)
    summary = build_summary(run_id, args.run_name, model_config, samples, details)
    write_json(output_dir / "summary.json", summary)
    write_jsonl(output_dir / "details.jsonl", details)
    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / "report.md").write_text(build_markdown_report(summary, details), encoding="utf-8")
    print(f"Eval completed: {output_dir}")


if __name__ == "__main__":
    main()

