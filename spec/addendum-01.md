# Addendum 01: Item Editing Functionality
**Date:** 2026-11-06
**Goal:** Allow users to edit the details of an existing tracked item without having to delete and recreate it.

## 1. Changes to SRS (srs.md)
**New Functional Requirement:**
*   **REQ-F-012:** The system shall allow the user to edit an existing item's attributes (title, expiration date, category, storage location) and save the changes to the local database.

**New Acceptance Criteria:**
*   **AC-F-012 (Item Editing):** Given an existing item in the list, when the user initiates the edit action, modifies the fields with valid data, and saves, then the item is successfully updated in the database and the UI reflects the changes instantly (including color status recalculation).

## 2. Changes to OpenAPI (openapi.yaml)
Add the following `PUT` method under the existing `/items/{id}` path:

```yaml
    put:
      summary: Update an existing item
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ItemInput'
      responses:
        '200':
          description: Item successfully updated
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Item'
        '400':
          description: Validation error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '404':
          description: Item not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'