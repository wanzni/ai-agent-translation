import unittest

from eval.scripts.loaders import discover_samples, validate_sample


class LoaderTests(unittest.TestCase):
    def test_discover_samples_loads_eight_seed_samples(self):
        samples = discover_samples("eval/samples/v1")
        self.assertEqual(8, len(samples))

    def test_missing_reference_text_is_invalid(self):
        sample = {
            "schemaVersion": "cross-border-ecom-eval/v3",
            "caseId": "x",
            "sourceDataset": "self-curated",
            "split": "eval",
            "domain": "cross_border_ecommerce",
            "category": "product_title",
            "difficulty": "easy",
            "sourceLanguage": "zh",
            "targetLanguage": "en",
            "sourceText": "a",
            "referenceText": "",
            "evaluationRules": {
                "termMatchingMode": "contains",
                "numberMatchingMode": "exact",
                "allowUnitConversion": False,
            },
            "expectedTerms": [],
            "expectedNumbersOrUnits": [],
            "expectedProtectedTokens": [],
            "criticalFailureRuleIds": [],
            "tags": [],
            "annotationMetadata": {
                "annotator": "manual",
                "annotatedAt": "2026-01-01T00:00:00Z",
                "reviewed": True,
                "confidence": "high",
            },
        }
        with self.assertRaises(ValueError):
            validate_sample(sample)

    def test_empty_term_targets_is_invalid(self):
        sample = {
            "schemaVersion": "cross-border-ecom-eval/v3",
            "caseId": "x",
            "sourceDataset": "self-curated",
            "split": "eval",
            "domain": "cross_border_ecommerce",
            "category": "product_title",
            "difficulty": "easy",
            "sourceLanguage": "zh",
            "targetLanguage": "en",
            "sourceText": "a",
            "referenceText": "b",
            "evaluationRules": {
                "termMatchingMode": "contains",
                "numberMatchingMode": "exact",
                "allowUnitConversion": False,
            },
            "expectedTerms": [
                {"source": "词", "targets": [], "required": True, "severity": "high", "category": "feature"}
            ],
            "expectedNumbersOrUnits": [],
            "expectedProtectedTokens": [],
            "criticalFailureRuleIds": [],
            "tags": [],
            "annotationMetadata": {
                "annotator": "manual",
                "annotatedAt": "2026-01-01T00:00:00Z",
                "reviewed": True,
                "confidence": "high",
            },
        }
        with self.assertRaises(ValueError):
            validate_sample(sample)

    def test_invalid_rule_id_is_rejected(self):
        sample = {
            "schemaVersion": "cross-border-ecom-eval/v3",
            "caseId": "x",
            "sourceDataset": "self-curated",
            "split": "eval",
            "domain": "cross_border_ecommerce",
            "category": "product_title",
            "difficulty": "easy",
            "sourceLanguage": "zh",
            "targetLanguage": "en",
            "sourceText": "a",
            "referenceText": "b",
            "evaluationRules": {
                "termMatchingMode": "contains",
                "numberMatchingMode": "exact",
                "allowUnitConversion": False,
            },
            "expectedTerms": [],
            "expectedNumbersOrUnits": [],
            "expectedProtectedTokens": [],
            "criticalFailureRuleIds": ["unknown_rule"],
            "tags": [],
            "annotationMetadata": {
                "annotator": "manual",
                "annotatedAt": "2026-01-01T00:00:00Z",
                "reviewed": True,
                "confidence": "high",
            },
        }
        with self.assertRaises(ValueError):
            validate_sample(sample)


if __name__ == "__main__":
    unittest.main()
