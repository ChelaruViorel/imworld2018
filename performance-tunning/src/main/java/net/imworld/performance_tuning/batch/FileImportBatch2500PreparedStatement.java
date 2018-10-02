package net.imworld.performance_tuning.batch;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;

import net.imworld.performance_tuning.util.DataUtil;
import net.imworld.performance_tuning.util.DbUtil;

public class FileImportBatch2500PreparedStatement {

	private final static Logger logger = LoggerFactory.getLogger(FileImportBatch2500PreparedStatement.class);
	
	private static final String CQL_INSERT = "insert into tmp_viorel_perftune (file_id, article, variant, bundle, store, size, price, manufacture_date, valability_days) values (?,?,?,?,?,?,?,?,?)";

	private Cluster cluster;
	private Session session;
	private PreparedStatement psInsert;
	private BatchStatement batch = new BatchStatement();
	private int batchCounter;
	
	public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public long fileImportBatch() throws Exception {

		int linesBufferSize = 1000;
		int batchSize = 400;
		
		long start = System.currentTimeMillis();
		Path path = Paths.get(DataUtil.DATA_FILEPATH);

		try (BufferedReader br = Files.newBufferedReader(path, Charset.forName("UTF-8"));) {

			cluster = DbUtil.getCluster(null);
			session = cluster.connect("od_inbound_dev");
			psInsert = session.prepare(CQL_INSERT);

			UUID fileId = UUIDs.timeBased();

			//skip header
			br.readLine();
			
			// read the file line by line, and insert into Cassandra
			String line = null;
			List<String> linesBuffer = new ArrayList<>();
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				}

				linesBuffer.add(line);
				
				if (linesBuffer.size() == linesBufferSize) {
					insertLinesBuffer(fileId, linesBuffer, batchSize);
					linesBuffer.clear();
				}
			}
			
			if (!linesBuffer.isEmpty()) {
				insertLinesBuffer(fileId, linesBuffer, batchSize);
			}
			
			//execute the last incomplete batch
			if (batch.size() > 0) {
				batchCounter++;
				session.execute(batch);
				batch.clear();
			}

			logger.info("executed a TOTAL of "+batchCounter+" batches.");
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			if (session != null) {
				session.close();
			}
			if (cluster != null) {
				cluster.close();
			}
		}
		long end = System.currentTimeMillis();

		return end - start;
	}

	private void insertLinesBuffer(UUID fileId, List<String> linesBuffer, int batchSize) throws ParseException {
		//logger.info("insert buffer of "+linesBuffer.size()+" lines");
		
		for (String line : linesBuffer) {
			String[] columns = line.split(",");
			String article = columns[0];
			
			int variant = Integer.parseInt(columns[1]);
			int bundle = Integer.parseInt(columns[2]);
			int store = Integer.parseInt(columns[3]);
			int size = Integer.parseInt(columns[4]);
			float price = Float.parseFloat(columns[5]);
			Date manufactureDate = sdf.parse(columns[6]);
			int valabilityDays = Integer.parseInt(columns[7]);

			batch.add(psInsert.bind(fileId, article, variant, bundle, store, size, price, manufactureDate, valabilityDays));
			
			if (batch.size() == batchSize) {
				batchCounter++;
				session.execute(batch);
				batch.clear();
			}
		}
		
	}
	
	public static void main(String[] args) throws Exception {

		FileImportBatch2500PreparedStatement importer = new FileImportBatch2500PreparedStatement();
		long runningTime = importer.fileImportBatch();
		logger.info("Running time="+runningTime);
	}
}
