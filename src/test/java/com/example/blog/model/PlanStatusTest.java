package com.example.blog.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanStatusTest {
    @Test
    void convertsLegacyNumericValues() {
        assertEquals(PlanStatus.IN_PROGRESS, PlanStatus.fromValue("0"));
        assertEquals(PlanStatus.COMPLETED, PlanStatus.fromValue("1"));
        assertEquals(PlanStatus.SHELVED, PlanStatus.fromValue("2"));
    }

    @Test
    void fallsBackToInProgressForUnknownValue() {
        assertEquals(PlanStatus.IN_PROGRESS, PlanStatus.fromValue("unknown"));
        assertEquals(PlanStatus.IN_PROGRESS, PlanStatus.fromValue(null));
    }
}
