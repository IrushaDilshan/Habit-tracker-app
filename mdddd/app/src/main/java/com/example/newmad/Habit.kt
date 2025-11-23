package com.example.newmad

import org.json.JSONObject
import java.util.UUID

// Simple data model for a habit
data class Habit(
    val name: String,
    val description: String? = null,
    var completed: Boolean = false,
    val id: String = UUID.randomUUID().toString()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("description", description)
        obj.put("completed", completed)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): Habit {
            val id = if (obj.has("id") && !obj.isNull("id")) obj.optString("id") else UUID.randomUUID().toString()
            val name = obj.optString("name")
            val description = if (obj.has("description") && !obj.isNull("description")) obj.optString("description") else null
            val completed = obj.optBoolean("completed", false)
            return Habit(name, description, completed, id)
        }
    }
}
