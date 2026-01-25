package ca.quines.garbageday;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAdjusters;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GarbageDayManager {

	public Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

	/**
	 * Output dates in the form:  EEEE = Full day name, MMMM = Full month name, d = Day of month
	 */
    DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d");

    /**
	 * Handle incoming dates in the form "January 15".
	 */
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM d")
            .toFormatter(Locale.ENGLISH);

    private File configFile;
    private TreeSet<LocalDate> garbageDaySet;

    /**
	 * An override of the current date that we can use for testing.
	 * 
	 * We check it fresh each invocation as the service might remaining running over multiple
	 * days.
	 */
	private LocalDate currentDateOverride;

	public GarbageDayManager(File configFile, LocalDate currentDateOverride) throws IOException {
		this.configFile = configFile;
		this.currentDateOverride = currentDateOverride;

		garbageDaySet = new TreeSet<>();

		if (configFile.exists()) {
			try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
				String line = null;
				while ((line = br.readLine()) != null) {
					Matcher matcher = DATE_PATTERN.matcher(line);
					if (matcher.find()) {
						garbageDaySet.add(LocalDate.parse(matcher.group(0)));
					}
				}
			}
		}
	}

	public String addDay(String input) throws IOException {
		LocalDate currentDate = getCurrentDate();

		try {
			DayOfWeek targetDay = DayOfWeek.valueOf(input.toUpperCase());
			LocalDate adjustedDate = currentDate.with(TemporalAdjusters.nextOrSame(targetDay));

			if (adjustedDate.equals(currentDate)) {
			    return "The day you entered is today.";
			}
			else {
				garbageDaySet.add(adjustedDate);
				save();

				return adjustedDate.toString();
			}
		}
		catch (IllegalArgumentException e) {
	        MonthDay monthDay = MonthDay.parse(input, MONTH_DAY_FORMATTER);
	        LocalDate targetDate = monthDay.atYear(currentDate.getYear());

	        if (targetDate.equals(currentDate)) {
			    return "The day you entered is today.";
	        }

	        // If the targetDate is before today, then it should actually be next year.
	        if (targetDate.isBefore(currentDate)) {
	            targetDate = targetDate.plusYears(1);
	        }

			garbageDaySet.add(targetDate);
			save();

			return targetDate.toString();
		}
	}

	/**
	 * @return The appropriate date:  the current date if not testing, or the overridden one.
	 */
	private LocalDate getCurrentDate() {
		return Objects.requireNonNullElse(currentDateOverride, LocalDate.now());
	}

	public String getNextGarbageDay() {
		if (garbageDaySet.isEmpty()) {
			return "No dates stored.";
		}

		LocalDate currentDate = getCurrentDate();
		SortedSet<LocalDate> nextDates = garbageDaySet.tailSet(currentDate);
		Iterator<LocalDate> it = nextDates.iterator();

	    if (!it.hasNext()) {
	        return "There are no upcoming garbage collection days scheduled.";
	    }

	    LocalDate first = it.next();
	    if (!it.hasNext()) {
	    	// Siri will automatically read yyyy-mm-dd properly.
	        return "The next garbage collection day is " + first.format(DATE_FMT) + ".";
	    }

	    LocalDate second = it.next();
	    return "The next collection day is " + first.format(DATE_FMT) + 
	           ", followed by " + second.format(DATE_FMT) + ".";
	}

	private void save() throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(configFile))) {
			for (LocalDate localDate : garbageDaySet) {
				bw.write(localDate.toString());
				bw.newLine();
			}
		}
	}

}
