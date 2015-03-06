package com.bloomcounty.appconfigurations.configexplorer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class Customer {
	private String customerName;
	private Map<String, Configuration> configurations = null;
	private Node myNode;
	
	public Customer() {
		this.customerName = "No Name";
		this.configurations = new ConcurrentHashMap<String, Configuration>(2); 
	}

	public Customer(String customerName) {
		this.customerName = customerName;
		this.configurations = new ConcurrentHashMap<String, Configuration>(2); 
	}

	public Customer(Node customerNode) {
		this.myNode = customerNode;
		this.customerName = (String) customerNode.getProperty(App.CUSTOMERNAME_KEY);
		this.configurations = new ConcurrentHashMap<String, Configuration>(2); 
		// query for all configurations in use by this node
		// 
		for ( Relationship rel : customerNode.getRelationships(App.RelTypes.CURRENT_VERSION, Direction.OUTGOING)) {
			Node configNode = rel.getOtherNode(customerNode);
			Configuration configurationObject = new Configuration(configNode, customerNode);
			this.configurations.put((String) configNode.getProperty(App.CONFIGNAME_KEY), configurationObject);
		}
	}
	
	
	public String getCustomerName() {
		return customerName;
	}
	
	public Configuration getCurrentConfig(String strConfigName) {
		return (Configuration)configurations.get(strConfigName);
	}

}
