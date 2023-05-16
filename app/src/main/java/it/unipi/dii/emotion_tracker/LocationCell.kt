package it.unipi.dii.emotion_tracker

class LocationCell(
    var latitude: Double,
    var longitude: Double,
    var street: String?,
    var city: String?,
    var emotion: Double,
    var timestamp: Long,
    var username: String
)
{}