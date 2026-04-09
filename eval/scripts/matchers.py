import re
import string


SENTIMENT_POSITIVE = {
    "good", "great", "excellent", "comfortable", "thoughtful", "love", "pretty good"
}
SENTIMENT_NEGATIVE = {
    "slow", "bad", "terrible", "short", "refund", "delay", "not convenient", "inconvenient"
}


def normalize_text(text):
    lowered = text.lower()
    translation = str.maketrans({char: " " for char in string.punctuation})
    lowered = lowered.translate(translation)
    lowered = re.sub(r"\s+", " ", lowered).strip()
    return lowered


def contains_normalized(output, target):
    return normalize_text(target) in normalize_text(output)


def exact_normalized(output, target):
    return normalize_text(target) in normalize_text(output)


def match_term(output, term, default_mode):
    mode = term.get("matchingMode", default_mode)
    for target in term.get("targets", []):
        if mode == "exact":
            if exact_normalized(output, target):
                return True, target, "exact"
        elif mode == "contains":
            if target.lower() in output.lower():
                return True, target, "contains"
        elif mode == "normalized":
            if contains_normalized(output, target):
                return True, target, "normalized"
    return False, None, None


def evaluate_terms(output, sample):
    default_mode = sample["evaluationRules"]["termMatchingMode"]
    results = []
    for term in sample["expectedTerms"]:
        matched, matched_target, match_type = match_term(output, term, default_mode)
        results.append({
            "source": term["source"],
            "expected": term["targets"],
            "matched": matched,
            "matchedTarget": matched_target,
            "matchType": match_type,
            "severity": term["severity"],
            "required": term["required"],
            "category": term["category"],
        })
    return results


def match_number(output, number_item):
    if number_item.get("rule") != "preserve":
        return False
    expected = str(number_item.get("expected", "")).strip()
    source = str(number_item.get("source", "")).strip()
    if expected and expected in output:
        return True
    if source and source in output:
        return True
    return False


def evaluate_numbers(output, sample):
    results = []
    for number_item in sample["expectedNumbersOrUnits"]:
        matched = match_number(output, number_item)
        results.append({
            "source": number_item["source"],
            "expected": number_item["expected"],
            "matched": matched,
            "severity": number_item["severity"],
            "rule": number_item["rule"],
        })
    return results


def evaluate_protected_tokens(output, sample):
    results = []
    lowered_output = output.lower()
    for item in sample["expectedProtectedTokens"]:
        token = item["token"]
        retained = token.lower() in lowered_output
        results.append({
            "token": token,
            "type": item["type"],
            "retained": retained,
            "severity": item["severity"],
        })
    return results


def _sentiment_score(text):
    normalized = normalize_text(text)
    positive = sum(1 for token in SENTIMENT_POSITIVE if token in normalized)
    negative = sum(1 for token in SENTIMENT_NEGATIVE if token in normalized)
    if positive > negative:
        return "positive"
    if negative > positive:
        return "negative"
    return "neutral"


def evaluate_critical_failures(sample, term_results, number_results, protected_results, output):
    failures = []
    context = sample.get("context", {})

    if "brand_mistranslation" in sample["criticalFailureRuleIds"]:
        brand_name = context.get("brandName")
        if brand_name and brand_name.lower() not in output.lower():
            failures.append("brand_mistranslation")

    if "high_severity_term_missing" in sample["criticalFailureRuleIds"]:
        if any(item["severity"] == "high" and not item["matched"] for item in term_results):
            failures.append("high_severity_term_missing")

    if "number_unit_error" in sample["criticalFailureRuleIds"]:
        if any(item["severity"] == "high" and not item["matched"] for item in number_results):
            failures.append("number_unit_error")

    if "placeholder_corruption" in sample["criticalFailureRuleIds"]:
        if any(item["type"] == "placeholder" and not item["retained"] for item in protected_results):
            failures.append("placeholder_corruption")

    if "size_confusion" in sample["criticalFailureRuleIds"]:
        if any(item["type"] == "size" and not item["retained"] for item in protected_results):
            failures.append("size_confusion")

    if "policy_misrepresentation" in sample["criticalFailureRuleIds"]:
        if any(
            item["category"] == "policy" and item["severity"] == "high" and not item["matched"]
            for item in term_results
        ):
            failures.append("policy_misrepresentation")

    if "sentiment_reversal" in sample["criticalFailureRuleIds"]:
        reference_sentiment = _sentiment_score(sample["referenceText"])
        output_sentiment = _sentiment_score(output)
        if (
            reference_sentiment in {"positive", "negative"}
            and output_sentiment in {"positive", "negative"}
            and reference_sentiment != output_sentiment
        ):
            failures.append("sentiment_reversal")

    return failures

