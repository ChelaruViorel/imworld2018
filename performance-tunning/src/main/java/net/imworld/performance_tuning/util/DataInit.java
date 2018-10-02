package net.imworld.performance_tuning.util;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Random;

public class DataInit {

	private static final DecimalFormat df2 = new DecimalFormat("#,##0.00");

	public static void main(String[] args) throws IOException {

		try (FileWriter fw = new FileWriter(DataUtil.DATA_FILEPATH);) {

			// write header
			fw.write("Article,Variant,Bundle,Store,Size,Price,Manufacture date,Valability days \n");

			Random bundleRandom = new Random(System.currentTimeMillis());
			Random storeRandom = new Random(System.currentTimeMillis() + 100000);
			Random sizeRandom = new Random(System.currentTimeMillis() + 150000);
			Random priceRandom = new Random(System.currentTimeMillis() + 200000);
			Random valabilityRandom = new Random(System.currentTimeMillis() + 250000);
			for (int i = 1; i <= 1000000; i++) {
				StringBuilder line = new StringBuilder();

				line.append("Item_" + i);
				line.append(",1");
				line.append("," + bundleRandom.nextInt(10000));
				line.append("," + storeRandom.nextInt(20));
				line.append("," + (1 + sizeRandom.nextInt(10)));
				line.append("," + df2.format(0.5 + priceRandom.nextFloat()));
				line.append(",2018-10-01 00:00:00");
				line.append("," + (3 + valabilityRandom.nextInt(700)));

				fw.write(line.toString() + "\n");
			}
		}
	}
}
