package net.imworld.performance_tunning.multithreading;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;

import net.imworld.performance_tunning.util.DataUtil;
import net.imworld.performance_tunning.util.DbUtil;

public class FileImportMultithreadedStringBuffer {

	private final static Logger logger = LoggerFactory.getLogger(FileImportMultithreadedStringBuffer.class);

	private static final String CQL_INSERT = "insert into tmp_viorel_perftune (file_id, article, variant, bundle, store, size, price, manufacture_date, valability_days) values (?,?,?,?,?,?,?,?,?)";

	private Cluster			cluster;

	private Session			session;

	
	private ExecutorService	taskExecutor;

	public long fileImportMultithreaded() throws Exception {

		int nrThreads = 80;
		int linesPerThread = 5000;
		taskExecutor = Executors.newFixedThreadPool(nrThreads);

		long start = System.currentTimeMillis();
		Path path = Paths.get(DataUtil.DATA_FILEPATH);

		List<Future<?>> futures = new LinkedList<>();
		try (BufferedReader br = Files.newBufferedReader(path, Charset.forName("UTF-8"));) {
			cluster = DbUtil.getCluster(null);
			session = cluster.connect("od_inbound_dev");
			
			UUID fileId = UUIDs.timeBased();

			// skip header
			br.readLine();

			// read the file line by line, and insert into Cassandra
			String line = null;
			int count = 0;
			List<String> linesBuffer = new LinkedList<>();
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				}

				count++;
				linesBuffer.add(line);
				
				if (count % linesPerThread == 0) {
					futures.add(processLinesBuffer(linesBuffer, fileId));
					linesBuffer.clear();
				}
			}
			
			if (!linesBuffer.isEmpty()) {
				futures.add(processLinesBuffer(linesBuffer, fileId));
			}
			
			//wait for all tasks to finish
			for (Future<?> future : futures) {
				future.get();
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
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

	private Future<?> processLinesBuffer(List<String> linesBuffer, UUID fileId) {
		
		logger.info("run "+linesBuffer.size()+ " lines");
		
		List<String> threadLinesBuffer = new LinkedList<>();
		threadLinesBuffer.addAll(linesBuffer);
		Runnable r = new Runnable() {

			@Override
			public void run() {

				for (String line : threadLinesBuffer) {
					String[] columns = line.split(",");
					String article = columns[0];
					int variant = Integer.parseInt(columns[1]);
					int bundle = Integer.parseInt(columns[2]);
					int store = Integer.parseInt(columns[3]);
					int size = Integer.parseInt(columns[4]);
					float price = Float.parseFloat(columns[5]);
					String manufactureDate = columns[6];
					int valabilityDays = Integer.parseInt(columns[7]);

					String cql = "insert into tmp_viorel_perftune (file_id, article, variant, bundle, store, size, price, manufacture_date, valability_days) " +
							"values ("+fileId+",'"+article+"',"+variant+","+bundle+","+store+","+size+","+price+",'"+manufactureDate+"',"+valabilityDays+")";
					session.execute(cql);
				}
			}
		};
		return taskExecutor.submit(r);
	}

	public static void main(String[] args) throws Exception {

		FileImportMultithreadedStringBuffer importer = new FileImportMultithreadedStringBuffer();
		long runningTime = importer.fileImportMultithreaded();
		logger.info("Running time=" + runningTime);
	}
}
