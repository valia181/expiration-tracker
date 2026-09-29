\# Календарний план проєкту



```mermaid

gantt

&#x20;   title Expiration Date Monitoring System

&#x20;   dateFormat  YYYY-MM-DD

&#x20;   axisFormat  %m-%d



&#x20;   section Phase 1: Core Setup

&#x20;   TSK-001 Setup MVVM \& DB (AI)       :a1, 2026-10-01, 1d

&#x20;   TSK-002 Test Validation (Joint)    :a2, after a1, 1d

&#x20;   TSK-003 Impl Validation (AI)       :a3, after a2, 1d

&#x20;   GATE-01 DB Review (Human)          :milestone, m1, after a3, 0d



&#x20;   section Phase 2: Core Features

&#x20;   TSK-004 Test List (Joint)          :b1, after m1, 1d

&#x20;   TSK-005 Impl List (AI)             :b2, after b1, 1d

&#x20;   TSK-006 Test Search (Joint)        :b3, after b2, 1d

&#x20;   TSK-007 Impl Search (AI)           :b4, after b3, 1d

&#x20;   TSK-008 Test Lead Time (Joint)     :b5, after b4, 1d

&#x20;   TSK-009 Impl Lead Time (AI)        :b6, after b5, 1d



&#x20;   section Phase 3: Background

&#x20;   TSK-010 Test Notifications (Joint) :c1, after m1, 1d

&#x20;   TSK-011 Impl Notifications (AI)    :c2, after c1, 1d

&#x20;   TSK-012 Test Backup (Joint)        :c3, after c2, 1d

&#x20;   TSK-013 Impl Backup (AI)           :c4, after c3, 1d



&#x20;   section Phase 4: Release

&#x20;   GATE-02 Pre-Release Gate (Human)   :milestone, m2, after b6 c4, 0d

&#x20;   TSK-014 Perf Verification (Joint)  :d1, after m2, 1d

