package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicInstructionFormatterTest {
    @Test fun turnHasNoDistance() {
        val s = ArabicInstructionFormatter.format(ManeuverType.LEFT, "شارع الزبيري", 2300.0, "الوجهة")
        assertEquals("انعطف يساراً في شارع الزبيري", s)
        assertFalse(s.contains("بعد"))
    }
    @Test fun straightMentionsSegmentLength() =
        assertEquals("واصل السير مباشرة لمسافة 1.5 كيلومتر", ArabicInstructionFormatter.format(ManeuverType.STRAIGHT, "", 1500.0, "x"))
    @Test fun neverPrintsNull() {
        ManeuverType.values().forEach { m ->
            assertFalse(m.name, ArabicInstructionFormatter.format(m, null, 100.0, null).contains("null"))
        }
    }
    @Test fun intermediateStopWording() {
        assertTrue(ArabicInstructionFormatter.format(ManeuverType.DESTINATION, null, 0.0, "س", isIntermediateStop = true).startsWith("وصلت إلى المحطة"))
        assertTrue(ArabicInstructionFormatter.format(ManeuverType.DESTINATION, null, 0.0, "س").startsWith("وصلت إلى وجهتك"))
    }
    @Test fun roundaboutExitOrdinal() =
        assertTrue(ArabicInstructionFormatter.format(ManeuverType.ROUNDABOUT, null, 0.0, null, roundaboutExit = 3).contains("الثالث"))
    @Test fun upcomingTextForDestinationIsFutureTense() =
        assertEquals("بعد 250 متراً ستصل إلى وجهتك: س",
            ArabicInstructionFormatter.upcomingText("وصلت إلى وجهتك: س", ManeuverType.DESTINATION, 250.0))
    @Test fun spokenDistances() {
        assertEquals("أقل من 30 متراً", ArabicInstructionFormatter.spoken(12.0))
        assertEquals("750 متراً", ArabicInstructionFormatter.spoken(730.0))
        assertEquals("2 كيلومتر", ArabicInstructionFormatter.spoken(2000.0))
    }
}
