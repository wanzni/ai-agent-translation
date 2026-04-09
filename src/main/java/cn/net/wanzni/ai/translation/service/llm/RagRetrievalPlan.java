package cn.net.wanzni.ai.translation.service.llm;

import java.util.List;

record RagRetrievalPlan(
        boolean retrievalTriggered,
        boolean retrieveTerminology,
        boolean retrieveHistory,
        List<String> reasons
) {
}
