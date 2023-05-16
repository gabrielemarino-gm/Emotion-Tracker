package it.unipi.dii.emotion_tracker

import java.util.*

class ClusterCentroid(
    var latitude: Double,
    var longitude: Double,
    var street: String?,
    var city: String?,
    var emotion: Double,
    var numberOfPoints: Long,
    var timestampDate: Long
)
{
}