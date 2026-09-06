package com.example.data

sealed class EntityType {
    object STORE : EntityType()
    object PROPERTY : EntityType()
    object RESTAURANT : EntityType()
    object MEDICAL : EntityType()
    object JOB : EntityType()
}
