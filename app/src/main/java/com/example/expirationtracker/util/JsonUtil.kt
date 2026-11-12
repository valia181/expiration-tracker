package com.example.expirationtracker.util

import com.example.expirationtracker.domain.model.BackupData
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.Settings
import com.example.expirationtracker.domain.model.StatusColor
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serialization utility providing robust JSON parsing and generation for database backup and restore operations
 * utilizing Android's native `org.json` parser.
 *
 * Traceability: REQ-F-009, REQ-F-010, AC-F-009, AC-F-010.
 */
object JsonUtil {

    /**
     * Serializes a [BackupData] model (comprising global settings and all item records) into a formatted JSON string.
     *
     * @param backupData The [BackupData] domain model to serialize.
     * @return A pretty-printed JSON string representation of the backup payload.
     */
    fun toJson(backupData: BackupData): String {
        val jsonObject = JSONObject()

        // Settings
        val settingsObj = JSONObject()
        settingsObj.put("notificationLeadTime", backupData.settings.notificationLeadTime)
        jsonObject.put("settings", settingsObj)

        // Items
        val itemsArray = JSONArray()
        for (item in backupData.items) {
            val itemObj = JSONObject()
            itemObj.put("id", item.id)
            itemObj.put("title", item.title)
            itemObj.put("expirationDate", item.expirationDate)
            itemObj.put("category", item.category.displayName)
            if (item.storageLocation != null) {
                itemObj.put("storageLocation", item.storageLocation)
            }
            itemObj.put("statusColor", item.statusColor.displayName)
            itemsArray.put(itemObj)
        }
        jsonObject.put("items", itemsArray)

        return jsonObject.toString(2)
    }

    /**
     * Deserializes a JSON string back into a validated [BackupData] domain model.
     *
     * @param jsonString The raw JSON string read from a backup file.
     * @return A parsed [BackupData] instance, or `null` if the JSON structure is malformed or missing required fields.
     */
    fun fromJson(jsonString: String): BackupData? {
        return try {
            val jsonObject = JSONObject(jsonString)

            if (!jsonObject.has("settings") || !jsonObject.has("items")) {
                return null
            }

            val settingsObj = jsonObject.getJSONObject("settings")
            val leadTime = settingsObj.optInt("notificationLeadTime", 7)
            val settings = Settings(notificationLeadTime = leadTime)

            val itemsArray = jsonObject.getJSONArray("items")
            val itemsList = mutableListOf<Item>()

            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(i)
                val id = itemObj.optString("id")
                val title = itemObj.optString("title")
                val expirationDate = itemObj.optString("expirationDate")
                val categoryStr = itemObj.optString("category")
                val storageLocation = if (itemObj.isNull("storageLocation")) null else itemObj.optString("storageLocation")
                val statusColorStr = itemObj.optString("statusColor")

                val category = Category.fromDisplayName(categoryStr) ?: Category.FOOD
                val statusColor = try {
                    StatusColor.entries.firstOrNull { it.displayName.equals(statusColorStr, ignoreCase = true) } ?: StatusColor.GREEN
                } catch (_: Exception) {
                    StatusColor.GREEN
                }

                itemsList.add(
                    Item(
                        id = id,
                        title = title,
                        expirationDate = expirationDate,
                        category = category,
                        storageLocation = storageLocation,
                        statusColor = statusColor
                    )
                )
            }

            BackupData(settings = settings, items = itemsList)
        } catch (_: Exception) {
            null
        }
    }
}
