package com.example.utils

object WaypointOptimizer {
    private fun d(a: Pair<Double, Double>, b: NavWaypoint) =
        HaversinePrecisionNavigator.calculateDistance(a.first, a.second, b.latitude, b.longitude)
    private fun d(a: NavWaypoint, b: NavWaypoint) =
        HaversinePrecisionNavigator.calculateDistance(a.latitude, a.longitude, b.latitude, b.longitude)

    /** طول المسار المفتوح: من نقطة البداية عبر المحطات بالترتيب. */
    fun pathLength(origin: Pair<Double, Double>, route: List<NavWaypoint>): Double {
        if (route.isEmpty()) return 0.0
        var total = d(origin, route[0])
        for (i in 0 until route.size - 1) total += d(route[i], route[i + 1])
        return total
    }

    fun optimize(origin: Pair<Double, Double>, points: List<NavWaypoint>): List<NavWaypoint> {
        if (points.size < 3) return points
        val remaining = points.toMutableList()
        val ordered = mutableListOf<NavWaypoint>()
        var cur = origin
        while (remaining.isNotEmpty()) {
            val next = remaining.minByOrNull { d(cur, it) } ?: break
            ordered.add(next); remaining.remove(next); cur = next.latitude to next.longitude
        }
        if (ordered.size > 12) return ordered
        var improved = true
        var guard = 0
        while (improved && guard++ < 50) {
            improved = false
            for (i in 0 until ordered.size - 1) for (j in i + 1 until ordered.size) {
                val cand = ordered.toMutableList().apply { subList(i, j + 1).reverse() }
                if (pathLength(origin, cand) + 1e-6 < pathLength(origin, ordered)) {
                    ordered.clear(); ordered.addAll(cand); improved = true
                }
            }
        }
        return ordered
    }
}
