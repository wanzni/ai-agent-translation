import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path("eval/scripts").resolve()))
from evaluator import HttpTranslationClient  # noqa: E402


class HttpTranslationClientTests(unittest.TestCase):
    def test_build_headers_uses_auth_token_from_env(self):
        os.environ["AUTH_TOKEN"] = "jwt-token"
        self.addCleanup(lambda: os.environ.pop("AUTH_TOKEN", None))

        client = HttpTranslationClient({"baseUrl": "http://127.0.0.1:7002"})

        headers = client._build_headers()
        self.assertEqual("Bearer jwt-token", headers["Authorization"])
        self.assertEqual("application/json", headers["Content-Type"])

    def test_extract_translated_text_supports_wrapped_api_response(self):
        body = {"success": True, "data": {"translatedText": "hello"}}
        self.assertEqual("hello", HttpTranslationClient._extract_translated_text(body, "case-1"))


class EvaluatorTests(unittest.TestCase):
    def test_smoke_run_generates_outputs(self):
        tmpdir = Path(tempfile.mkdtemp(prefix="eval-smoke-"))
        self.addCleanup(lambda: shutil.rmtree(tmpdir, ignore_errors=True))

        command = [
            sys.executable,
            "eval/scripts/evaluator.py",
            "--dataset-dir",
            "eval/samples/v1",
            "--model-config",
            "eval/config/model-config.mock.json",
            "--output-dir",
            str(tmpdir),
        ]
        subprocess.run(command, check=True)

        runs = list(tmpdir.glob("run-*"))
        self.assertEqual(1, len(runs))
        run_dir = runs[0]
        summary_path = run_dir / "summary.json"
        details_path = run_dir / "details.jsonl"
        report_path = run_dir / "report.md"
        self.assertTrue(summary_path.exists())
        self.assertTrue(details_path.exists())
        self.assertTrue(report_path.exists())

        summary = json.loads(summary_path.read_text(encoding="utf-8"))
        self.assertEqual("cross-border-ecom-eval", summary["datasetInfo"]["datasetName"])
        self.assertEqual(16, summary["datasetInfo"]["totalSamples"])
        self.assertAlmostEqual(1.0, summary["overallScore"], places=4)

        details_lines = details_path.read_text(encoding="utf-8").strip().splitlines()
        self.assertEqual(16, len(details_lines))
        report_text = report_path.read_text(encoding="utf-8")
        self.assertIn("Overall Metrics", report_text)


if __name__ == "__main__":
    unittest.main()