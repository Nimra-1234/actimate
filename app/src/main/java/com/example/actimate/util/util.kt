package com.example.actimate.util

// Function to preprocess timestamp and ensure milliseconds are 3 digits
fun preprocessTimestamp(timestamp: String): String {
    val regex = "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(\\.\\d{1,3})?".toRegex()

    // Check if the timestamp matches the expected pattern
    val matchResult = regex.find(timestamp)

    return if (matchResult != null) {
        // Extract the timestamp and milliseconds part (if any)
        val baseTime = matchResult.groupValues[1]
        val milliseconds = matchResult.groupValues[2]

        // If milliseconds are present, ensure they have 3 digits; if not, add ".000"
        val formattedMilliseconds = if (milliseconds.isNotEmpty()) {
            // Pad milliseconds to 3 digits
            milliseconds.padEnd(4, '0').take(4)  // Ensure 3 digits
        } else {
            ".000"
        }

        // Combine base time with formatted milliseconds
        "$baseTime$formattedMilliseconds"
    } else {
        // If timestamp does not match the expected format, return the original timestamp
        timestamp
    }
}
