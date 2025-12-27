package net.fabricmc.dakes.invoverstack.util;

public class NumberFormatter {

	/**
	 * Format stack count for display to prevent cramping.
	 * - 1-999: Display as-is
	 * - 1000-99999: Display as "1k", "2k", "10k", "99k"
	 * - 100000-999999: Display as "0.1kk", "0.2kk", "0.9kk"
	 * - 1000000-999999999: Display as "1kk", "2kk", "999kk"
	 * - 1000000000+: Display as "1b", "2b", "999b", etc.
	 */
	public static String formatStackCount(int count) {
		if (count < 1000) {
			return String.valueOf(count);
		} else if (count < 100000) {
			// 1k - 99k
			return (count / 1000) + "k";
		} else if (count < 1000000) {
			// 100k - 999k displayed as 0.1kk - 0.9kk
			double kk = count / 1000000.0;
			return String.format("%.1fkk", kk);
		} else if (count < 1000000000) {
			// 1kk - 999kk
			return (count / 1000000) + "kk";
		} else {
			// 1b+ (billions)
			double b = count / 1000000000.0;
			if (b >= 10.0) {
				// 10b, 100b, etc.
				return ((int) b) + "b";
			} else {
				// 1.0b - 9.9b
				return String.format("%.1fb", b);
			}
		}
	}
}
