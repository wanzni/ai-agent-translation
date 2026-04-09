import json
from pathlib import Path


ALLOWED_CATEGORIES = {
    "product_title",
    "product_description",
    "customer_review",
    "customer_service_chat",
}
ALLOWED_DIFFICULTIES = {"easy", "medium", "hard"}
ALLOWED_TERM_MODES = {"exact", "contains", "normalized"}
ALLOWED_NUMBER_MODES = {"exact"}
ALLOWED_RULE_IDS = {
    "brand_mistranslation",
    "high_severity_term_missing",
    "number_unit_error",
    "placeholder_corruption",
    "size_confusion",
    "policy_misrepresentation",
    "sentiment_reversal",
}


def read_json(path):
    with open(path, "r", encoding="utf-8-sig") as handle:
        return json.load(handle)


def read_jsonl(path):
    rows = []
    with open(path, "r", encoding="utf-8-sig") as handle:
        for lineno, line in enumerate(handle, 1):
            line = line.strip()
            if not line:
                continue
            try:
                rows.append(json.loads(line))
            except json.JSONDecodeError as exc:
                raise ValueError(f"Invalid JSONL in {path}:{lineno}: {exc}") from exc
    return rows


def write_json(path, payload):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)


def write_jsonl(path, rows):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False) + "\n")


def load_rule_registry(path):
    payload = read_json(path)
    critical_rules = payload.get("criticalRules", [])
    return {item["ruleId"]: item for item in critical_rules}


def load_scoring_config(path):
    payload = read_json(path)
    return payload.get("categories", {})


def discover_samples(dataset_dir, categories=None):
    dataset_path = Path(dataset_dir)
    if not dataset_path.exists():
        raise ValueError(f"Dataset directory does not exist: {dataset_dir}")

    wanted = None
    if categories:
        wanted = {item.strip() for item in categories if item.strip()}

    samples = []
    for file_path in sorted(dataset_path.glob("*.jsonl")):
        for sample in read_jsonl(file_path):
            validate_sample(sample)
            if wanted and sample["category"] not in wanted:
                continue
            samples.append(sample)

    if not samples:
        raise ValueError("No samples loaded from dataset directory")
    return samples


def validate_sample(sample):
    required = [
        "schemaVersion",
        "caseId",
        "sourceDataset",
        "split",
        "domain",
        "category",
        "difficulty",
        "sourceLanguage",
        "targetLanguage",
        "sourceText",
        "referenceText",
        "evaluationRules",
        "expectedTerms",
        "expectedNumbersOrUnits",
        "expectedProtectedTokens",
        "criticalFailureRuleIds",
        "tags",
        "annotationMetadata",
    ]
    for key in required:
        if key not in sample:
            raise ValueError(f"Missing required field: {key}")

    if sample["schemaVersion"] != "cross-border-ecom-eval/v3":
        raise ValueError("Unsupported schemaVersion")
    if sample["category"] not in ALLOWED_CATEGORIES:
        raise ValueError("Invalid category")
    if sample["difficulty"] not in ALLOWED_DIFFICULTIES:
        raise ValueError("Invalid difficulty")
    if sample["split"] != "eval":
        raise ValueError("Only eval split is supported")
    if sample["domain"] != "cross_border_ecommerce":
        raise ValueError("Unsupported domain")
    if not sample["referenceText"]:
        raise ValueError("referenceText must not be empty")

    rules = sample["evaluationRules"]
    term_mode = rules.get("termMatchingMode")
    number_mode = rules.get("numberMatchingMode")
    allow_unit_conversion = rules.get("allowUnitConversion")
    if term_mode not in ALLOWED_TERM_MODES:
        raise ValueError("Invalid termMatchingMode")
    if number_mode not in ALLOWED_NUMBER_MODES:
        raise ValueError("Invalid numberMatchingMode")
    if allow_unit_conversion not in (False, 0):
        raise ValueError("allowUnitConversion must be false in v1")

    for term in sample["expectedTerms"]:
        if not term.get("targets"):
            raise ValueError("expectedTerms.targets must not be empty")
        matching_mode = term.get("matchingMode", term_mode)
        if matching_mode not in ALLOWED_TERM_MODES:
            raise ValueError("Invalid term matching mode")

    for item in sample["criticalFailureRuleIds"]:
        if item not in ALLOWED_RULE_IDS:
            raise ValueError(f"Invalid criticalFailureRuleId: {item}")

    metadata = sample["annotationMetadata"]
    for key in ["annotator", "annotatedAt", "reviewed", "confidence"]:
        if key not in metadata:
            raise ValueError(f"annotationMetadata.{key} is required")
