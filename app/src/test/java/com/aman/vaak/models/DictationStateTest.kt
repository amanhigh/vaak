package com.aman.vaak.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DictationStatusTest {
    @Test
    fun `enum has expected values`() {
        val statuses = DictationStatus.values()
        assertEquals(4, statuses.size)
        assert(statuses.contains(DictationStatus.IDLE))
        assert(statuses.contains(DictationStatus.RECORDING))
        assert(statuses.contains(DictationStatus.TRANSCRIBING))
        assert(statuses.contains(DictationStatus.TRANSLATING))
    }

    @Test
    fun `enum values have expected names`() {
        assertEquals("IDLE", DictationStatus.IDLE.name)
        assertEquals("RECORDING", DictationStatus.RECORDING.name)
        assertEquals("TRANSCRIBING", DictationStatus.TRANSCRIBING.name)
        assertEquals("TRANSLATING", DictationStatus.TRANSLATING.name)
    }

    @Test
    fun `enum values have expected ordinals`() {
        assertEquals(0, DictationStatus.IDLE.ordinal)
        assertEquals(1, DictationStatus.RECORDING.ordinal)
        assertEquals(2, DictationStatus.TRANSCRIBING.ordinal)
        assertEquals(3, DictationStatus.TRANSLATING.ordinal)
    }
}

class DictationStateTest {
    @Nested
    inner class DefaultValues {
        @Test
        fun `default constructor creates IDLE state with zero time`() {
            val state = DictationState()

            assertEquals(DictationStatus.IDLE, state.status)
            assertEquals(0L, state.timeMillis)
        }

        @Test
        fun `constructor with status only uses zero time`() {
            val state = DictationState(status = DictationStatus.RECORDING)

            assertEquals(DictationStatus.RECORDING, state.status)
            assertEquals(0L, state.timeMillis)
        }

        @Test
        fun `constructor with time only uses IDLE status`() {
            val state = DictationState(timeMillis = 5000L)

            assertEquals(DictationStatus.IDLE, state.status)
            assertEquals(5000L, state.timeMillis)
        }
    }

    @Nested
    inner class StateCreation {
        @Test
        fun `creates IDLE state correctly`() {
            val state = DictationState(DictationStatus.IDLE, 0L)

            assertEquals(DictationStatus.IDLE, state.status)
            assertEquals(0L, state.timeMillis)
        }

        @Test
        fun `creates RECORDING state correctly`() {
            val state = DictationState(DictationStatus.RECORDING, 1500L)

            assertEquals(DictationStatus.RECORDING, state.status)
            assertEquals(1500L, state.timeMillis)
        }

        @Test
        fun `creates TRANSCRIBING state correctly`() {
            val state = DictationState(DictationStatus.TRANSCRIBING, 0L)

            assertEquals(DictationStatus.TRANSCRIBING, state.status)
            assertEquals(0L, state.timeMillis)
        }

        @Test
        fun `creates TRANSLATING state correctly`() {
            val state = DictationState(DictationStatus.TRANSLATING, 0L)

            assertEquals(DictationStatus.TRANSLATING, state.status)
            assertEquals(0L, state.timeMillis)
        }
    }

    @Nested
    inner class StateBehavior {
        @Test
        fun `state with same values are equal`() {
            val state1 = DictationState(DictationStatus.RECORDING, 1000L)
            val state2 = DictationState(DictationStatus.RECORDING, 1000L)

            assertEquals(state1, state2)
        }

        @Test
        fun `states with different status are not equal`() {
            val state1 = DictationState(DictationStatus.IDLE, 1000L)
            val state2 = DictationState(DictationStatus.RECORDING, 1000L)

            assert(state1 != state2)
        }

        @Test
        fun `states with different time are not equal`() {
            val state1 = DictationState(DictationStatus.RECORDING, 1000L)
            val state2 = DictationState(DictationStatus.RECORDING, 2000L)

            assert(state1 != state2)
        }

        @Test
        fun `copy function works correctly`() {
            val original = DictationState(DictationStatus.RECORDING, 1500L)
            val copied = original.copy()

            assertEquals(original, copied)
        }

        @Test
        fun `copy function with status change works correctly`() {
            val original = DictationState(DictationStatus.RECORDING, 1500L)
            val modified = original.copy(status = DictationStatus.TRANSCRIBING)

            assertEquals(DictationStatus.TRANSCRIBING, modified.status)
            assertEquals(1500L, modified.timeMillis)
        }

        @Test
        fun `copy function with time change works correctly`() {
            val original = DictationState(DictationStatus.RECORDING, 1500L)
            val modified = original.copy(timeMillis = 3000L)

            assertEquals(DictationStatus.RECORDING, modified.status)
            assertEquals(3000L, modified.timeMillis)
        }
    }

    @Nested
    inner class StateTransitionLogic {
        @Test
        fun `time should be relevant only during RECORDING state`() {
            val idleState = DictationState(DictationStatus.IDLE, 5000L)
            val recordingState = DictationState(DictationStatus.RECORDING, 5000L)
            val transcribingState = DictationState(DictationStatus.TRANSCRIBING, 5000L)
            val translatingState = DictationState(DictationStatus.TRANSLATING, 5000L)

            // Only recording state should typically have meaningful time
            assert(recordingState.timeMillis > 0L)

            // Verify all states can be created with time values
            assert(idleState.status == DictationStatus.IDLE)
            assert(transcribingState.status == DictationStatus.TRANSCRIBING)
            assert(translatingState.status == DictationStatus.TRANSLATING)
        }

        @Test
        fun `can create states for complete workflow`() {
            val states =
                listOf(
                    DictationState(DictationStatus.IDLE, 0L),
                    DictationState(DictationStatus.RECORDING, 2500L),
                    DictationState(DictationStatus.TRANSCRIBING, 0L),
                    DictationState(DictationStatus.IDLE, 0L),
                )

            assertEquals(4, states.size)
            assertEquals(DictationStatus.IDLE, states.first().status)
            assertEquals(DictationStatus.IDLE, states.last().status)
        }
    }

    @Nested
    inner class EdgeCases {
        @Test
        fun `handles negative time values`() {
            val state = DictationState(DictationStatus.RECORDING, -100L)

            assertEquals(DictationStatus.RECORDING, state.status)
            assertEquals(-100L, state.timeMillis)
        }

        @Test
        fun `handles very large time values`() {
            val largeTime = Long.MAX_VALUE
            val state = DictationState(DictationStatus.RECORDING, largeTime)

            assertEquals(DictationStatus.RECORDING, state.status)
            assertEquals(largeTime, state.timeMillis)
        }

        @Test
        fun `toString includes all properties`() {
            val state = DictationState(DictationStatus.RECORDING, 1500L)
            val string = state.toString()

            assert(string.contains("RECORDING"))
            assert(string.contains("1500"))
        }

        @Test
        fun `hashCode is consistent`() {
            val state1 = DictationState(DictationStatus.RECORDING, 1000L)
            val state2 = DictationState(DictationStatus.RECORDING, 1000L)

            assertEquals(state1.hashCode(), state2.hashCode())
        }
    }
}
