# Software Requirements Specification (SRS)
## Expiration Date Monitoring System

---

### 1. Introduction & System Purpose

#### 1.1 Purpose
This document specifies the software requirements for the **Expiration Date Monitoring System**, a native Android application designed to help users track household supplies (food, medicine, cosmetics, and chemicals), monitor item conditions, prevent spoilage, and ensure timely disposal of expired goods.

#### 1.2 Document Conventions
- **REQ-F-XXX**: Functional Requirement
- **REQ-NF-XXX**: Non-Functional Requirement
- **AC-XXX**: Acceptance Criteria

#### 1.3 Intended Audience
This specification is intended for developers, testers, and stakeholders involved in the design, implementation, and verification of the Expiration Date Monitoring System.

---

### 2. System Boundaries & Scope

#### 2.1 Scope
The system is a self-contained, single-user mobile application running locally on Android devices. 

#### 2.2 System Boundaries
- **In-Scope**:
  - Manual item entry (Title, Expiration Date, Category, Storage Location).
  - Predefined categories: Food, Medicine, Cosmetics, Chemicals.
  - Local SQLite database storage ensuring 100% offline functionality.
  - Chronological main view with dynamic 3-level color status (Red, Orange, Green).
  - Search functionality by item title.
  - Category filtering using filter chips.
  - Global notification lead time configuration (1, 3, 7, or 14 days).
  - Local push notifications triggered via persistent background scheduling (WorkManager).
  - Persistent expired item management (remains visible until manually deleted).
  - Database backup and restore via JSON export/import utilizing the Android Storage Access Framework (SAF).

- **Out-of-Scope**:
  - Cloud syncing and multi-user accounts.
  - Automated item entry via barcode scanning or receipt OCR for the MVP.
  - External API integrations (e.g., grocery store loyalty accounts, smart fridge APIs).
  - Built-in reference databases for items without explicit expiration dates.
  - Non-local notification channels (Email, SMS).
  - Per-item custom notification lead times.
  - Custom user-created categories.

---

### 3. Glossary of Domain Terms

*   **Item**: A discrete household supply manually entered into the application for tracking, containing attributes such as title, expiration date, category, and optional storage location.
*   **Expiration Date**: The specific calendar date manually provided by the user, after which the item is considered expired and targeted for disposal.
*   **Category**: A classification attribute used to group items. For the MVP, this is strictly limited to four predefined values: **Food**, **Medicine**, **Cosmetics**, and **Chemicals**.
*   **Notification Lead Time**: A global configuration setting (measured in days) that determines how far in advance of the Expiration Date the system triggers a local warning notification.
*   **Item Status (Color Code)**: A visual indicator assigned to an item based on its expiration date relative to the current date:
    *   **Red (Expired)**: The expiration date has passed (Date < Today). The item remains in the list until manually deleted.
    *   **Orange (Warning)**: The expiration date is approaching within the configured Notification Lead Time.
    *   **Green (Safe)**: The expiration date is strictly beyond the configured Notification Lead Time window (Date > Today + Lead Time).
*   **Storage Location**: An optional text attribute indicating the physical placement of the item within the household (e.g., pantry, refrigerator, bathroom cabinet).

---

### 4. General Description

#### 4.1 Product Perspective
The system is a standalone native Android application. It interacts directly with the local device storage (SQLite database) and local notification manager. It requires no internet connectivity or external server infrastructure.

#### 4.2 Product Functions
- Create, view, search, filter, and delete tracked items.
- Provide real-time visual status highlighting based on expiration dates.
- Schedule and deliver local push notifications for approaching expirations.
- Export and import local database backups as JSON files via Android SAF.

#### 4.3 Constraints
- **Platform Constraint**: Strictly restricted to native Android OS.
- **Connectivity Constraint**: Must operate fully offline without reliance on network services.
- **Category Constraint**: Limited exclusively to Food, Medicine, Cosmetics, and Chemicals.

---

### 5. Functional Requirements

#### **Item Creation & Validation**
*   **REQ-F-001**: The system shall allow the user to manually create a new item by providing a title, expiration date, category, and an optional storage location.
*   **REQ-F-002**: The system shall validate that the item title is neither empty nor consists solely of whitespace characters before saving the item.
*   **REQ-F-003**: The system shall validate that a valid calendar date is provided for the expiration date before saving the item (dates in the past must be permitted to allow entry of already expired items).

#### **Display & Status**
*   **REQ-F-004**: The system shall display all saved items in a unified chronological list, sorted by expiration date with the soonest expiring items appearing first.
*   **REQ-F-005**: The system shall dynamically assign and display a color status code for each item based on its expiration date relative to the current date:
    *   **Red**: Expiration date has passed.
    *   **Orange**: Expiration date falls within the configured global notification lead time.
    *   **Green**: Expiration date is strictly beyond the configured Notification Lead Time window.

#### **Search & Filtering**
*   **REQ-F-006**: The system shall provide a search interface allowing users to filter and view items whose titles match the search query.
*   **REQ-F-007**: The system shall provide category filter controls allowing users to instantly filter the item list by any of the predefined categories (Food, Medicine, Cosmetics, Chemicals) or view all categories simultaneously.

#### **Notification Configuration**
*   **REQ-F-008**: The system shall provide a settings interface allowing the user to configure the global notification lead time by selecting from predefined options (1, 3, 7, or 14 days).

#### **Backup & Restore**
*   **REQ-F-009**: The system shall provide an export function that serializes the local database into a JSON file, utilizing the native Android Storage Access Framework (SAF) to let the user select the destination.
*   **REQ-F-010**: The system shall provide an import function that reads a JSON file via the native Android Storage Access Framework (SAF) and populates the local database.

---

### 6. Non-Functional Requirements

#### **Performance**
*   **REQ-NF-001**: The application UI shall maintain smooth scrolling and a minimum of 60 frames per second (fps) when displaying and filtering the item list.
*   **REQ-NF-002**: The application shall complete local database queries (such as search and category filtering) and render the results within 100 milliseconds.

#### **Reliability & Data Integrity**
*   **REQ-NF-003**: The local SQLite database shall ensure transactional integrity to prevent data corruption during read, write, export, and import operations.
*   **REQ-NF-004**: Background notification checks shall utilize persistent OS scheduling (such as Android WorkManager) to ensure scheduled tasks reliably survive application restarts and device reboots.

#### **Usability**
*   **REQ-NF-005**: The application's color-coding status indicators (Red, Orange, Green) shall adhere to WCAG 2.1 Level AA contrast ratio standards (minimum 4.5:1 ratio against the background) to ensure readability for users with color vision deficiencies.
*   **REQ-NF-006**: The user interface shall strictly adhere to Android Material Design 3 guidelines, specifically ensuring that all interactive elements (buttons, filter chips) maintain a minimum touch target size of 48x48 dp.

#### **Maintainability**
*   **REQ-NF-007**: The codebase shall adhere to a clean architecture pattern (such as MVVM - Model-View-ViewModel) to clearly separate UI logic, business logic, and local data access.

---

### 7. Acceptance Criteria

#### **Functional Acceptance Criteria**
*   **AC-F-001 (Manual Item Creation):** Given the user is on the item creation screen, when they provide a valid title, expiration date, category, and optional storage location and tap "Save", then the item is successfully stored in the local database and appears in the main item list.
*   **AC-F-002 (Title Validation):** Given the user attempts to save an item with an empty or whitespace-only title, when the save action is triggered, then the system prevents saving and displays a validation error message prompting for a title.
*   **AC-F-003 (Date Validation):** Given the user attempts to save an item without selecting a valid calendar date, when the save action is triggered, then the system prevents saving and displays a validation error message.
*   **AC-F-004 (Chronological Display):** Given multiple items with varying expiration dates exist in the database, when the main screen loads, then the items are rendered in a single unified list sorted strictly in ascending order by expiration date (soonest first).
*   **AC-F-005 (Dynamic Color Status):** Given the items are displayed in the list:
    *   An item whose expiration date is before the current date displays a **Red** indicator.
    *   An item whose expiration date falls within the configured global notification lead time displays an **Orange** indicator.
    *   An item whose expiration date is strictly beyond the configured Notification Lead Time window displays a **Green** indicator.
*   **AC-F-006 (Search Functionality):** Given the user types a query into the search bar, when the text changes, then the main list dynamically updates to show only items whose titles contain the search query (case-insensitive).
*   **AC-F-007 (Category Filtering):** Given the user taps a category filter chip (Food, Medicine, Cosmetics, or Chemicals), when the filter is active, then only items belonging to that specific category are displayed. Tapping "All" resets the filter to show all items.
*   **AC-F-008 (Global Notification Lead Time Configuration):** Given the user navigates to the settings screen and selects a new lead time option (e.g., changing from 3 days to 7 days), when saved, then items falling within the new 7-day window correctly update their status to Orange.
*   **AC-F-009 (Database Export via SAF):** Given the user triggers the backup export action, when the native Android Storage Access Framework (SAF) document picker opens and a destination is chosen, then a JSON file containing all current item records is successfully generated and saved to the target location.
*   **AC-F-010 (Database Import via SAF):** Given the user triggers the backup import action and selects a valid JSON backup file via the SAF picker, when the file is processed, then the local database is successfully populated with the imported data, and the main UI refreshes to display the newly imported items.

#### **Non-Functional Acceptance Criteria**
*   **AC-NF-001 (Background Reliability):** Given a notification is scheduled for an item, when the Android device is rebooted, then the scheduled WorkManager task is automatically restored and triggers the notification at the correct time.
*   **AC-NF-002 (Query Performance):** Given a database populated with simulated item records, when a search or category filter is executed, then the database query execution and UI rendering complete in under 100 milliseconds.
*   **AC-NF-003 (Usability & Accessibility):** Given the application UI is rendered, when analyzed, then all interactive elements (buttons, filter chips) measure a minimum touch target area of 48x48 dp, and color indicators maintain a minimum 4.5:1 contrast ratio.
*   **AC-NF-004 (Architecture Maintainability):** Given the source code is submitted for review, when statically analyzed, then the codebase demonstrates a strict MVVM separation: UI classes contain no direct SQLite, and data access is handled through Repository layers.
