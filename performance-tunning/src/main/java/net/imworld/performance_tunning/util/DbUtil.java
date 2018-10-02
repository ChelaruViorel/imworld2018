package net.imworld.performance_tunning.util;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;

public class DbUtil {

	public static Cluster getCluster(LoadBalancingPolicy loadBalancePolicy) {
		String contact_points = System.getProperty("cassandra_contact_point");
        String user = System.getProperty("cassandra_user");
        String pwd = System.getProperty("cassandra_password");
		List<String> contactPointsList = Arrays.asList(contact_points.split(","));
		List<InetSocketAddress> contactPintsWithPorts =
                contactPointsList.stream().map(point -> getInetSocketAddress(point)).collect(Collectors.toList());
		
		LoadBalancingPolicy defaultLoadBalancePolicy = DCAwareRoundRobinPolicy.builder().build(); 
		
		Builder clusterBuilder = Cluster.builder()
				.addContactPointsWithPorts(contactPintsWithPorts).withLoadBalancingPolicy(loadBalancePolicy != null ? loadBalancePolicy : defaultLoadBalancePolicy);
        clusterBuilder.withCredentials(user, pwd);
        Cluster cluster = clusterBuilder.build();
        return cluster;
	}
	
	private static InetSocketAddress getInetSocketAddress(String contactPointWithPort) {
        String[] contactPointWithPortStr = contactPointWithPort.split(":");
        return new InetSocketAddress(contactPointWithPortStr[0], Integer.parseInt(contactPointWithPortStr[1]));
    }
}
