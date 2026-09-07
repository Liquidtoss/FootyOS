package app.footyos.nutrition

import org.junit.Assert.*
import org.junit.Test

class OfflineNutritionTest {
    @Test fun sumsWeightedPortionsWithoutRoundingEachItem() {
        val result = OfflineNutrition.estimate(listOf(
            Portion("A", 150.0, Nutrients(100.0, 10.0, 20.0, 2.0)),
            Portion("B", 25.0, Nutrients(200.0, 5.0, 30.0, 6.0)),
        ))
        assertEquals(Nutrients(200.0, 16.25, 37.5, 4.5), result)
    }
    @Test fun agreementIsSymmetricAndKeepsSignedDeltas() {
        val a = Nutrients(500.0, 30.0, 60.0, 20.0)
        val b = Nutrients(600.0, 30.0, 60.0, 20.0)
        assertEquals(95.833333, NutritionAgreement.compare(a, b)!!.score, 0.00001)
        assertEquals(NutritionAgreement.compare(a, b)!!.score, NutritionAgreement.compare(b, a)!!.score, 0.0)
        assertEquals(-100.0, NutritionAgreement.compare(a, b)!!.deltas[0], 0.0)
    }
    @Test fun zeroValuesDoNotInflateAgreement() {
        val zero = Nutrients(0.0, 0.0, 0.0, 0.0)
        assertNull(NutritionAgreement.compare(zero, zero))
        assertEquals(0.0, NutritionAgreement.compare(zero, Nutrients(100.0, 0.0, 0.0, 0.0))!!.score, 0.0)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNonfiniteInput() { Nutrients(Double.NaN, 0.0, 0.0, 0.0) }
    @Test(expected = IllegalArgumentException::class) fun rejectsNegativePortion() { Portion("A", -1.0, Nutrients(0.0, 0.0, 0.0, 0.0)) }
}
