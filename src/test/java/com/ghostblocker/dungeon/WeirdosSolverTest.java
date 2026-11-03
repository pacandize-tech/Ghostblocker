package com.ghostblocker.dungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class WeirdosSolverTest {
	@Test
	void findsRewardNpcFromExactStatement() {
		assertEquals("Baxter", WeirdosSolver.solve("[NPC] Baxter: The reward is not in my chest!"));
		assertEquals("Ardis", WeirdosSolver.solve("[NPC] Ardis: My chest has the reward and I'm telling the truth!"));
	}

	@Test
	void findsRewardNpcFromStatementEndingInAName() {
		assertEquals("Hope", WeirdosSolver.solve("[NPC] Hope: Both of them are telling the truth. Also, Ramsey has the reward in their chest!"));
		assertEquals("Lino", WeirdosSolver.solve("[NPC] Lino: At least one of them is lying, and the reward is not in Elmo's chest."));
	}

	@Test
	void ignoresLyingStatementsAndOtherChat() {
		assertNull(WeirdosSolver.solve("[NPC] Baxter: One of us is telling the truth!"));
		assertNull(WeirdosSolver.solve("Baxter: The reward is not in my chest!"));
		assertNull(WeirdosSolver.solve("[Party] Someone: The reward is not in my chest!"));
	}
}
