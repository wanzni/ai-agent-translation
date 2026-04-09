from collections import Counter


def build_markdown_report(summary, details):
    lines = []
    lines.append("# Cross-Border E-commerce Eval Report")
    lines.append("")
    lines.append(f"- Run ID: `{summary['runId']}`")
    lines.append(f"- Run Name: `{summary['runName']}`")
    lines.append(f"- Timestamp: `{summary['timestamp']}`")
    lines.append("")
    lines.append("## Overall Metrics")
    lines.append("")
    lines.append(f"- Overall Score: `{summary['overallScore']:.4f}`")
    lines.append(f"- Term Accuracy: `{summary['termAccuracy']:.4f}`")
    lines.append(f"- Number Accuracy: `{summary['numberAccuracy']:.4f}`")
    lines.append(f"- Protected Token Retention: `{summary['protectedTokenRetention']:.4f}`")
    lines.append(f"- Critical Failure Rate: `{summary['criticalFailureRate']:.4f}`")
    lines.append("")
    lines.append("## Category Breakdown")
    lines.append("")
    for category, metrics in sorted(summary["categoryBreakdown"].items()):
        lines.append(
            f"- `{category}`: score={metrics['overallScore']:.4f}, "
            f"term={metrics['termAccuracy']:.4f}, number={metrics['numberAccuracy']:.4f}, "
            f"protected={metrics['protectedTokenRetention']:.4f}, critical={metrics['criticalFailureRate']:.4f}"
        )

    failure_counter = Counter()
    for detail in details:
        failure_counter.update(detail["criticalFailures"])

    lines.append("")
    lines.append("## Top Failure Rules")
    lines.append("")
    if failure_counter:
        for rule_id, count in failure_counter.most_common():
            lines.append(f"- `{rule_id}`: {count}")
    else:
        lines.append("- No critical failures.")

    lines.append("")
    lines.append("## Representative Failure Samples")
    lines.append("")
    failures = [detail for detail in details if detail["criticalFailures"]]
    failures.sort(key=lambda item: (len(item["criticalFailures"]), -item["sampleScore"]), reverse=True)
    if failures:
        for detail in failures[:5]:
            lines.append(f"### {detail['caseId']}")
            lines.append(f"- Category: `{detail['category']}`")
            lines.append(f"- Score: `{detail['sampleScore']:.4f}`")
            lines.append(f"- Critical Failures: `{', '.join(detail['criticalFailures'])}`")
            lines.append(f"- Model Output: {detail['modelOutput']}")
            lines.append("")
    else:
        lines.append("- No failure samples in this run.")

    lines.append("")
    return "\n".join(lines)
