package app.footyos.domain

import org.junit.Assert.*
import org.junit.Test

class GuidedDrillTest {
    @Test fun repsNeverAutoComplete() {
        val plan = DrillPlan(3)
        var drill = GuidedDrill().start()
        repeat(10) { drill = drill.tick(plan) }
        assertEquals(DrillPhase.Work, drill.phase)
        repeat(300) { drill = drill.tick(plan) }
        assertEquals(0, drill.round)
        assertEquals(DrillPhase.Rest, drill.finishRound(plan).phase)
    }
    @Test fun timedSidesMustBothFinishBeforeNextSet() {
        val plan = DrillPlan(2, 2, 20)
        var drill = GuidedDrill().start()
        repeat(30) { drill = drill.tick(plan) }
        assertEquals(1, drill.round)
        assertEquals(15, drill.remaining)
        repeat(35) { drill = drill.tick(plan) }
        assertEquals(2, drill.round)
        assertEquals(60, drill.remaining)
        repeat(80) { drill = drill.tick(plan) }
        repeat(35) { drill = drill.tick(plan) }
        assertEquals(DrillPhase.Complete, drill.phase)
        assertEquals(4, drill.round)
        assertEquals(drill, drill.tick(plan))
    }
    @Test fun floorPressWorksOneSideAtATime() {
        assertEquals(DrillPlan(3, 2), ExerciseCatalog.byId("floor_press")!!.drillPlan())
    }
    @Test fun everyCatalogPrescriptionHasAPlan() {
        ExerciseCatalog.exercises.forEach { assertTrue(it.drillPlan().sets in 2..4) }
        assertEquals(DrillPlan(3, 2, 40), ExerciseCatalog.byId("suitcase_carry")!!.drillPlan())
        assertEquals(DrillPlan(2, 2, 20), ExerciseCatalog.byId("copenhagen")!!.drillPlan())
    }
}
