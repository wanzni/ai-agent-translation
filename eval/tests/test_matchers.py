import unittest

from eval.scripts.matchers import evaluate_critical_failures, evaluate_numbers, evaluate_protected_tokens, match_term


class MatcherTests(unittest.TestCase):
    def test_exact_match(self):
        matched, target, mode = match_term(
            "Breathable Mesh",
            {"targets": ["Breathable Mesh"], "matchingMode": "exact"},
            "contains",
        )
        self.assertTrue(matched)
        self.assertEqual("Breathable Mesh", target)
        self.assertEqual("exact", mode)

    def test_contains_match(self):
        matched, _, mode = match_term(
            "This is a waterproof running shoe.",
            {"targets": ["waterproof"], "matchingMode": "contains"},
            "contains",
        )
        self.assertTrue(matched)
        self.assertEqual("contains", mode)

    def test_normalized_match(self):
        matched, _, mode = match_term(
            "Quick   Dry training shirt",
            {"targets": ["quick-dry"], "matchingMode": "normalized"},
            "contains",
        )
        self.assertTrue(matched)
        self.assertEqual("normalized", mode)

    def test_number_preserve_success(self):
        sample = {"expectedNumbersOrUnits": [{"source": "14", "expected": "14", "rule": "preserve", "severity": "high"}]}
        result = evaluate_numbers("Battery lasts 14 days.", sample)
        self.assertTrue(result[0]["matched"])

    def test_number_preserve_failure(self):
        sample = {"expectedNumbersOrUnits": [{"source": "14", "expected": "14", "rule": "preserve", "severity": "high"}]}
        result = evaluate_numbers("Battery lasts ten days.", sample)
        self.assertFalse(result[0]["matched"])

    def test_protected_token_brand_retained(self):
        sample = {"expectedProtectedTokens": [{"token": "TechFit", "type": "brand", "severity": "critical"}]}
        result = evaluate_protected_tokens("TechFit Quick-Dry Shirt", sample)
        self.assertTrue(result[0]["retained"])

    def test_protected_token_placeholder_missing(self):
        sample = {"expectedProtectedTokens": [{"token": "{{orderId}}", "type": "placeholder", "severity": "critical"}]}
        result = evaluate_protected_tokens("Your order is ready", sample)
        self.assertFalse(result[0]["retained"])

    def test_brand_mistranslation_failure(self):
        sample = {"context": {"brandName": "TechFit"}, "criticalFailureRuleIds": ["brand_mistranslation"]}
        failures = evaluate_critical_failures(sample, [], [], [], "Quick-Dry Shirt")
        self.assertIn("brand_mistranslation", failures)

    def test_brand_mistranslation_non_failure(self):
        sample = {"context": {"brandName": "TechFit"}, "criticalFailureRuleIds": ["brand_mistranslation"]}
        failures = evaluate_critical_failures(sample, [], [], [], "TechFit Quick-Dry Shirt")
        self.assertNotIn("brand_mistranslation", failures)

    def test_high_severity_term_missing_failure(self):
        sample = {"criticalFailureRuleIds": ["high_severity_term_missing"], "context": {}}
        term_results = [{"severity": "high", "matched": False, "category": "feature"}]
        failures = evaluate_critical_failures(sample, term_results, [], [], "output")
        self.assertIn("high_severity_term_missing", failures)

    def test_high_severity_term_missing_non_failure(self):
        sample = {"criticalFailureRuleIds": ["high_severity_term_missing"], "context": {}}
        term_results = [{"severity": "high", "matched": True, "category": "feature"}]
        failures = evaluate_critical_failures(sample, term_results, [], [], "output")
        self.assertNotIn("high_severity_term_missing", failures)

    def test_number_unit_error_failure(self):
        sample = {"criticalFailureRuleIds": ["number_unit_error"], "context": {}}
        number_results = [{"severity": "high", "matched": False}]
        failures = evaluate_critical_failures(sample, [], number_results, [], "output")
        self.assertIn("number_unit_error", failures)

    def test_number_unit_error_non_failure(self):
        sample = {"criticalFailureRuleIds": ["number_unit_error"], "context": {}}
        number_results = [{"severity": "high", "matched": True}]
        failures = evaluate_critical_failures(sample, [], number_results, [], "output")
        self.assertNotIn("number_unit_error", failures)

    def test_placeholder_corruption_failure(self):
        sample = {"criticalFailureRuleIds": ["placeholder_corruption"], "context": {}}
        protected_results = [{"type": "placeholder", "retained": False}]
        failures = evaluate_critical_failures(sample, [], [], protected_results, "output")
        self.assertIn("placeholder_corruption", failures)

    def test_placeholder_corruption_non_failure(self):
        sample = {"criticalFailureRuleIds": ["placeholder_corruption"], "context": {}}
        protected_results = [{"type": "placeholder", "retained": True}]
        failures = evaluate_critical_failures(sample, [], [], protected_results, "output")
        self.assertNotIn("placeholder_corruption", failures)

    def test_size_confusion_failure(self):
        sample = {"criticalFailureRuleIds": ["size_confusion"], "context": {}}
        protected_results = [{"type": "size", "retained": False}]
        failures = evaluate_critical_failures(sample, [], [], protected_results, "output")
        self.assertIn("size_confusion", failures)

    def test_size_confusion_non_failure(self):
        sample = {"criticalFailureRuleIds": ["size_confusion"], "context": {}}
        protected_results = [{"type": "size", "retained": True}]
        failures = evaluate_critical_failures(sample, [], [], protected_results, "output")
        self.assertNotIn("size_confusion", failures)

    def test_policy_misrepresentation_failure(self):
        sample = {"criticalFailureRuleIds": ["policy_misrepresentation"], "context": {}}
        term_results = [{"category": "policy", "severity": "high", "matched": False}]
        failures = evaluate_critical_failures(sample, term_results, [], [], "output")
        self.assertIn("policy_misrepresentation", failures)

    def test_policy_misrepresentation_non_failure(self):
        sample = {"criticalFailureRuleIds": ["policy_misrepresentation"], "context": {}}
        term_results = [{"category": "policy", "severity": "high", "matched": True}]
        failures = evaluate_critical_failures(sample, term_results, [], [], "output")
        self.assertNotIn("policy_misrepresentation", failures)

    def test_sentiment_reversal_failure(self):
        sample = {
            "criticalFailureRuleIds": ["sentiment_reversal"],
            "context": {},
            "referenceText": "This product is great and very comfortable.",
        }
        failures = evaluate_critical_failures(sample, [], [], [], "This product is terrible and bad.")
        self.assertIn("sentiment_reversal", failures)

    def test_sentiment_reversal_non_failure(self):
        sample = {
            "criticalFailureRuleIds": ["sentiment_reversal"],
            "context": {},
            "referenceText": "This product is great and very comfortable.",
        }
        failures = evaluate_critical_failures(sample, [], [], [], "This product is good and comfortable.")
        self.assertNotIn("sentiment_reversal", failures)


if __name__ == "__main__":
    unittest.main()
