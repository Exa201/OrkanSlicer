package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Test

class FillBedCallbackResultPolicyTest {
    @Test fun aCurrentCapacityFailureIsReportedAsNoRoom() {
        assertEquals(0, FillBedCallbackResultPolicy.forModelState(stillCurrent = true, addedCopies = 0))
    }

    @Test fun aStaleFillResultIsNotReportedAsNoRoom() {
        assertEquals(
            FillBedCallbackResultPolicy.STALE,
            FillBedCallbackResultPolicy.forModelState(stillCurrent = false, addedCopies = 0),
        )
    }

    @Test fun aFillResultFromBeforePlateArrangeIsNotReportedAsNoRoom() {
        assertEquals(
            FillBedCallbackResultPolicy.STALE,
            FillBedCallbackResultPolicy.forModelState(
                stillCurrent = true,
                addedCopies = 0,
                expectedLayoutOperationRevision = 4,
                currentLayoutOperationRevision = 5,
            ),
        )
    }

    @Test fun aFillResultFromTheCurrentPlateLayoutCanReportCapacityFailure() {
        assertEquals(
            0,
            FillBedCallbackResultPolicy.forModelState(
                stillCurrent = true,
                addedCopies = 0,
                expectedLayoutOperationRevision = 5,
                currentLayoutOperationRevision = 5,
            ),
        )
    }

    @Test fun aCurrentSuccessfulFillKeepsItsCopyCount() {
        assertEquals(3, FillBedCallbackResultPolicy.forModelState(stillCurrent = true, addedCopies = 3))
    }
}
