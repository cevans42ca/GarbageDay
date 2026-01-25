package ca.quines.garbageday;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class GarbageDayManagerTest {

	@Test
	void testInit() throws IOException {
		Path configPath = Files.createTempFile("garbageDay", "txt");
		File configFile = configPath.toFile();
		configFile.deleteOnExit();

		LocalDate testDate = LocalDate.of(2026, 1, 1);

		new GarbageDayManager(configFile, testDate);
	}

	@Test
	void testWednesday() throws IOException {
		Path configPath = Files.createTempFile("garbageDay", "txt");
		File configFile = configPath.toFile();
		configFile.deleteOnExit();

		LocalDate testDate = LocalDate.of(2026, 1, 1);

		GarbageDayManager garbageDayManager = new GarbageDayManager(configFile, testDate);
		assertEquals("2026-01-07", garbageDayManager.addDay("Wednesday"));
	}

	@Test
	void testMonthDay() throws IOException {
		Path configPath = Files.createTempFile("garbageDay", "txt");
		File configFile = configPath.toFile();
		configFile.deleteOnExit();

		LocalDate testDate = LocalDate.of(2026, 1, 1);

		GarbageDayManager garbageDayManager = new GarbageDayManager(configFile, testDate);
		assertEquals("2026-01-15", garbageDayManager.addDay("January 15"));
	}

	@Test
	void testMonthDayToday() throws IOException {
		Path configPath = Files.createTempFile("garbageDay", "txt");
		File configFile = configPath.toFile();
		configFile.deleteOnExit();

		LocalDate testDate = LocalDate.of(2026, 1, 1);

		GarbageDayManager garbageDayManager = new GarbageDayManager(configFile, testDate);
		assertEquals("2026-01-26", garbageDayManager.addDay("January 26"));
	}

}
