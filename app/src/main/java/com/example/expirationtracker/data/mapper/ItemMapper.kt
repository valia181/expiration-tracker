package com.example.expirationtracker.data.mapper

import com.example.expirationtracker.data.local.entity.ItemEntity
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.StatusColor

/**
 * Clean architectural mappers separating local database entity models from domain models.
 * Traceability: REQ-NF-007.
 */
object ItemMapper {

    fun toDomain(entity: ItemEntity): Item {
        return Item(
            id = entity.id,
            title = entity.title,
            expirationDate = entity.expirationDate,
            category = Category.valueOf(entity.category),
            storageLocation = entity.storageLocation,
            statusColor = StatusColor.valueOf(entity.statusColor)
        )
    }

    fun toEntity(domain: Item): ItemEntity {
        return ItemEntity(
            id = domain.id,
            title = domain.title,
            expirationDate = domain.expirationDate,
            category = domain.category.name,
            storageLocation = domain.storageLocation,
            statusColor = domain.statusColor.name
        )
    }
}
