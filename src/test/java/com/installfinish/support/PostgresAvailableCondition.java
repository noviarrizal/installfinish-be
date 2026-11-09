package com.installfinish.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Disables PostgreSQL integration tests, visibly, when neither TEST_DATABASE_URL nor Docker is available. */
class PostgresAvailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return PostgresIntegrationTest.databaseAvailable()
                ? ConditionEvaluationResult.enabled("PostgreSQL available")
                : ConditionEvaluationResult.disabled("Skipped: Docker is not available and TEST_DATABASE_URL is not set");
    }
}
