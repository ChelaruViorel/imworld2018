package net.imworld.performance_tuning.simple;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;

import net.imworld.performance_tuning.util.DataUtil;
import net.imworld.performance_tuning.util.DbUtil;

public class FileImportSimple {

	private final static Logger logger = LoggerFactory.getLogger(FileImportSimple.class);
	
	private static final String CQL_INSERT = "insert into tmp_viorel_perftune (file_id, article, variant, bundle, store, size, price, manufacture_date, valability_days) values (?,?,?,?,?,?,?,?,?)";

	public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public long fileImportSimple() throws Exception {

		long start = System.currentTimeMillis();
		Path path = Paths.get(DataUtil.DATA_FILEPATH);

		try (Cluster cluster = DbUtil.getCluster(null);
				Session session = cluster.connect("od_inbound_dev");
				BufferedReader br = Files.newBufferedReader(path, Charset.forName("UTF-8"));) {

			PreparedStatement psInsert = session.prepare(CQL_INSERT);
			UUID fileId = UUIDs.timeBased();

			//skip header
			br.readLine();
			
			// read the file line by line, and insert into Cassandra
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				}

				String[] columns = line.split(",");
				String article = columns[0];
				int variant = Integer.parseInt(columns[1]);
				int bundle = Integer.parseInt(columns[2]);
				int store = Integer.parseInt(columns[3]);
				int size = Integer.parseInt(columns[4]);
				float price = Float.parseFloat(columns[5]);
				Date manufactureDate = sdf.parse(columns[6]);
				int valabilityDays = Integer.parseInt(columns[7]);

				session.execute(psInsert.bind(fileId, article, variant, bundle, store, size, price, manufactureDate, valabilityDays));
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		long end = System.currentTimeMillis();

		return end - start;
	}

	public static void main(String[] args) throws Exception {

		FileImportSimple importer = new FileImportSimple();
		long runningTime = importer.fileImportSimple();
		logger.info("Running time="+runningTime);
	}
}
