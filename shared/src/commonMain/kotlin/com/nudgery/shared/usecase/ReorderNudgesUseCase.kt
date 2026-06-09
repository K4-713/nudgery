// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.usecase

import com.nudgery.shared.repository.NudgeRepository

/**
 * Persists a new nudge list order (ED-19). The UI reorders optimistically while dragging and commits
 * the final arrangement here on drop; [orderedNudgeIds] is the full list of nudge ids in their new
 * order. Positions are rewritten as a dense 0..n sequence in one transaction by the repository.
 */
class ReorderNudgesUseCase(private val nudgeRepository: NudgeRepository) {
    suspend fun execute(orderedNudgeIds: List<String>) {
        nudgeRepository.reorder(orderedNudgeIds)
    }
}
