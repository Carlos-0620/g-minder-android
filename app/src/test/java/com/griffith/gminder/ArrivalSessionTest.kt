package com.griffith.gminder

import org.junit.Assert.*
import org.junit.Test

class ArrivalSessionTest {
    private fun point(latitude: Double = 0.0, longitude: Double = 0.0, accuracy: Float? = 10f) = LocationFix.from(latitude, longitude, accuracy, 0L)!!
    private fun target(id: String = "one", latitude: Double = 0.0, longitude: Double = 0.0) = ArrivalTarget(id, "Task $id", point(latitude, longitude, null))

    @Test fun distanceMatchesAnEquatorialDegree() {
        assertEquals(0.0, ArrivalGeometry.distanceMetres(point(), point()), 0.001)
        assertEquals(111_194.93, ArrivalGeometry.distanceMetres(point(), point(longitude = 1.0)), 0.1)
    }

    @Test fun distanceUsesShortRouteAcrossDateLine() {
        assertEquals(222.39, ArrivalGeometry.distanceMetres(point(longitude = 179.999), point(longitude = -179.999)), 0.1)
    }

    @Test fun boundaryIncludesReportedAccuracy() {
        assertTrue(ArrivalGeometry.isInside(190.0, 10f, 200, 0L))
        assertFalse(ArrivalGeometry.isInside(190.1, 10f, 200, 0L))
        assertFalse(ArrivalGeometry.isInside(190.0, 20f, 200, 0L))
        assertTrue(ArrivalGeometry.isInside(190.0, 5f, 200, 0L))
    }

    @Test fun unknownOrInvalidQualityDoesNotTrigger() {
        for (accuracy in listOf(null, -1f, Float.NaN, Float.POSITIVE_INFINITY, 500f)) {
            assertFalse(ArrivalGeometry.isInside(0.0, accuracy, 200, 0L))
        }
        assertFalse(ArrivalGeometry.isInside(Double.NaN, 10f, 200, 0L))
        assertFalse(ArrivalGeometry.isInside(-1.0, 10f, 200, 0L))
    }

    @Test fun staleAndFutureReadingsDoNotTrigger() {
        assertTrue(ArrivalGeometry.isInside(0.0, 10f, 200, 30_000L))
        assertFalse(ArrivalGeometry.isInside(0.0, 10f, 200, 30_001L))
        assertFalse(ArrivalGeometry.isInside(0.0, 10f, 200, -1L))
    }

    @Test fun onlyActiveCoordinateRemindersBecomeTargets() {
        assertNull(ArrivalTarget.fromReminder(Reminder("1", "Task", "", "Griffith College Dublin")))
        assertNull(ArrivalTarget.fromReminder(Reminder("1", "Task", "", "0, 0", true)))
        assertNotNull(ArrivalTarget.fromReminder(Reminder("1", "Task", "", "0, 0")))
    }

    @Test fun repeatedInsideReadingsAlertOnce() {
        val session = ArrivalSession()
        val targets = listOf(target())
        assertEquals(1, session.evaluate(point(), targets, 200, 0L).size)
        assertTrue(session.evaluate(point(), targets, 200, 0L).isEmpty())
    }

    @Test fun leavingAndReturningDoesNotRepeatWithinTheSameSession() {
        val session = ArrivalSession()
        val item = target()
        assertEquals(1, session.evaluate(point(), listOf(item), 200, 0L).size)
        session.markShown(item.key)
        assertTrue(session.evaluate(point(latitude = 1.0), listOf(item), 200, 0L).isEmpty())
        assertTrue(session.evaluate(point(), listOf(item), 200, 0L).isEmpty())
    }

    @Test fun explicitNewSessionCanAlertAgain() {
        val session = ArrivalSession()
        session.evaluate(point(), listOf(target()), 200, 0L)
        session.reset()
        assertEquals(1, session.evaluate(point(), listOf(target()), 200, 0L).size)
    }

    @Test fun multipleNearbyTasksAreEachQueuedOnce() {
        val session = ArrivalSession()
        val targets = listOf(target("one"), target("two", 0.0001))
        assertEquals(listOf("one", "two"), session.evaluate(point(), targets, 200, 0L).map { it.target.id })
        assertTrue(session.evaluate(point(), targets, 200, 0L).isEmpty())
    }

    @Test fun changingDestinationCreatesANewArrival() {
        val session = ArrivalSession()
        session.evaluate(point(), listOf(target()), 200, 0L)
        assertEquals(1, session.evaluate(point(), listOf(target(latitude = 0.0001)), 200, 0L).size)
    }

    @Test fun completingThenReactivatingATaskCanAlertAgain() {
        val session = ArrivalSession()
        session.evaluate(point(), listOf(target()), 200, 0L)
        session.updateTargets(emptyList())
        assertEquals(1, session.evaluate(point(), listOf(target()), 200, 0L).size)
    }

    @Test fun aLargerRadiusCanIncludeANewDestinationWithoutRepeatingIt() {
        val session = ArrivalSession()
        val targets = listOf(target(latitude = 0.003))
        assertTrue(session.evaluate(point(), targets, 200, 0L).isEmpty())
        assertEquals(1, session.evaluate(point(), targets, 500, 0L).size)
        assertTrue(session.evaluate(point(), targets, 1000, 0L).isEmpty())
    }

    @Test fun unseenQueuedArrivalCanBeRetriedAfterPoorReadings() {
        val session = ArrivalSession()
        val item = target()
        session.evaluate(point(), listOf(item), 200, 0L)
        session.releaseIfUnseen(item.key)
        assertEquals(1, session.evaluate(point(), listOf(item), 200, 0L).size)
    }

    @Test fun shownArrivalCannotBeReleasedForRepeatedAlerts() {
        val session = ArrivalSession()
        val item = target()
        session.evaluate(point(), listOf(item), 200, 0L)
        session.markShown(item.key)
        session.releaseIfUnseen(item.key)
        assertTrue(session.evaluate(point(), listOf(item), 200, 0L).isEmpty())
    }

    @Test fun poorOrStaleReadingsDoNotConsumeAnArrival() {
        val session = ArrivalSession()
        val targets = listOf(target())
        assertTrue(session.evaluate(point(accuracy = 1000f), targets, 200, 0L).isEmpty())
        assertTrue(session.evaluate(point(), targets, 200, 60_000L).isEmpty())
        assertEquals(1, session.evaluate(point(), targets, 200, 0L).size)
    }
}
