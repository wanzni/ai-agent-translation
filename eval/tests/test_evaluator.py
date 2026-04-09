import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


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
            "eval/config/model-config.example.json",
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
        self.assertEqual(8, summary["datasetInfo"]["totalSamples"])
        self.assertAlmostEqual(1.0, summary["overallScore"], places=4)

        details_lines = details_path.read_text(encoding="utf-8").strip().splitlines()
        self.assertEqual(8, len(details_lines))
        report_text = report_path.read_text(encoding="utf-8")
        self.assertIn("Overall Metrics", report_text)


if __name__ == "__main__":
    unittest.main()
