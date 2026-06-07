package com.ik0ha.ratibu.data

import java.text.SimpleDateFormat
import java.util.*

object AnalyticsEngine {

    fun computeSummary(bookings: List<Session>): Map<String, String> {
        val completed = bookings.filter { it.status == "COMPLETED" }
        
        val dayCounts = completed.groupBy { 
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date(it.startTime))
        }.mapValues { it.value.size }

        val hourCounts = completed.groupBy { 
            SimpleDateFormat("HH:00", Locale.getDefault()).format(Date(it.startTime))
        }.mapValues { it.value.size }

        val busiestDay = dayCounts.maxByOrNull { it.value }?.key ?: "None"
        val busiestHour = hourCounts.maxByOrNull { it.value }?.key ?: "None"

        val uniqueClients = bookings.filter { it.clientId != "walk-in" }.map { it.clientId }.distinct().size
        val repeatClients = bookings.filter { it.clientId != "walk-in" }
            .groupBy { it.clientId }
            .filter { it.value.size > 1 }.size
        val retentionRate = if (uniqueClients > 0) (repeatClients * 100 / uniqueClients) else 0

        return mapOf(
            "Busiest Day" to busiestDay,
            "Peak Hour" to busiestHour,
            "Retention" to "$retentionRate%",
            "Completion Rate" to "${if (bookings.isNotEmpty()) (completed.size * 100 / bookings.size) else 0}%"
        )
    }

    fun computeDetailed(bookings: List<Session>): DetailedAnalytics {
        val completed = bookings.filter { it.status == "COMPLETED" }
        
        val dayCounts = completed.groupBy { 
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date(it.startTime))
        }.mapValues { it.value.size }

        val hourCounts = completed.groupBy { 
            SimpleDateFormat("HH:00", Locale.getDefault()).format(Date(it.startTime))
        }.mapValues { it.value.size }

        val uniqueClients = bookings.filter { it.clientId != "walk-in" }.map { it.clientId }.distinct().size
        val repeatClients = bookings.filter { it.clientId != "walk-in" }
            .groupBy { it.clientId }
            .filter { it.value.size > 1 }.size
            
        val statusCounts = bookings.groupBy { it.status }.mapValues { it.value.size }

        return DetailedAnalytics(
            dayDistribution = dayCounts,
            hourDistribution = hourCounts,
            uniqueClients = uniqueClients,
            repeatClients = repeatClients,
            statusBreakdown = statusCounts
        )
    }
}
