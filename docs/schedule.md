```mermaid
gantt
   title Expiration Date Monitoring System (Updated Dependencies)
   dateFormat  YYYY-MM-DD
   axisFormat  %m-%d

   section Phase 1: Core Setup
   TSK-001 Setup MVVM, DB, Log (AI)  :a1, 2026-10-01, 1d
   TSK-002 Test Validation (Joint)   :a2, after a1, 1d
   TSK-003 Impl Validation (AI)      :a3, after a2, 1d
   GATE-01 DB & Arch Review (Human)  :milestone, m1, after a3, 0d

   section Phase 2: Core UI (List)
   TSK-004 Test List (Joint)         :b1, after m1, 1d
   TSK-005 Impl List (AI)            :b2, after b1, 1d

   section Phase 3: Parallel UI Features
   TSK-006 Test Deletion (Joint)     :c1, after b2, 1d
   TSK-007 Impl Deletion (AI)        :c2, after c1, 1d
   TSK-008 Test Search (Joint)       :c3, after b2, 1d
   TSK-009 Impl Search (AI)          :c4, after c3, 1d
   TSK-010 Test Lead Time (Joint)    :c5, after b2, 1d
   TSK-011 Impl Lead Time (AI)       :c6, after c5, 1d

   section Phase 4: Background & Backup
   TSK-012 Test Notifications (Joint):d1, after m1, 1d
   TSK-013 Impl Notifications (AI)   :d2, after d1 c6, 1d
   TSK-014 Test Backup (Joint)       :d3, after m1, 1d
   TSK-015 Impl Backup (AI)          :d4, after d3, 1d

   section Phase 5: Release
   GATE-02 Pre-Release Gate (Human)  :milestone, m2, after c2 c4 d2 d4, 0d
   TSK-016 Perf Verification (Joint) :e1, after m2, 1d