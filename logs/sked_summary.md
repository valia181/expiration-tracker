# SKED Dialogue & Requirement Elicitation Summary

## 1. Identified Uncertainties
During the iterative SKED requirement elicitation process, the following core system boundaries and architectural uncertainties were raised and addressed:
*   **Platform & Connectivity:** Whether the app should be cross-platform with cloud syncing or native and 100% offline.
*   **Item Entry & Data Sources:** Whether to support barcode scanning, receipt parsing, or external grocery integrations, and how to handle items without explicit expiration dates.
*   **Notifications:** Required notification channels (local push vs. email/SMS) and lead-time configuration scope (global vs. per-item).
*   **Categorization:** Whether users can create custom categories or be restricted to a predefined set.
*   **Data Persistence & Maintenance:** Handling of expired items (archive vs. persistent highlight) and data migration/backup mechanisms.
*   **UI & Search:** Layout preferences for sorting, filtering, and searching items.

---

## 2. Human Decisions (Accepted Scope)
The user explicitly accepted and locked in the following architectural and functional choices for the MVP:
*   **Platform:** Native Android application using a local SQLite database, ensuring 100% offline functionality.
*   **User Scope & Architecture:** Single-user application with no cloud synchronization.
*   **Item Entry:** Manual item entry only (Title, Expiration Date, Category, and optional Storage Location).
*   **Categories:** Strictly limited to four predefined categories for the MVP: **Food**, **Medicine**, **Cosmetics**, and **Chemicals**.
*   **Notifications:** Local push notifications triggered via persistent background scheduling (Android WorkManager) using a **global notification lead time** configuration (1, 3, 7, or 14 days).
*   **Expired Item Handling:** Expired items remain in the main chronological list, visually highlighted in red until manually deleted by the user.
*   **Backup & Restore:** Database export and import via JSON files utilizing the native Android Storage Access Framework (SAF).
*   **UI/UX:** Unified chronological list sorted by expiration date, supplemented by category filter chips, a title-based search bar, and a 3-level color status indicator (Red, Orange, Green).

---

## 3. Rejected & Corrected Assumptions
To maintain a strict and deliverable MVP scope, the user rejected or corrected the following initial model assumptions:
*   **Cross-Platform Development:** Rejected cross-platform support in favor of a strictly native Android implementation.
*   **Cloud Syncing & Multi-User Support:** Rejected multi-user accounts and cloud backends in favor of local-only storage (SQLite).
*   **Automated Item Entry:** Rejected barcode scanning and third-party receipt parsing for the initial MVP.
*   **Custom Categories:** Rejected user-defined custom categories to keep data structure and filtering simple.
*   **Per-Item Notification Lead Times:** Rejected flexible per-item notification intervals in favor of a simpler global notification lead-time setting.
