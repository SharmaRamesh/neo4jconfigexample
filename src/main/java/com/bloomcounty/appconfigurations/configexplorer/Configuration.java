package com.bloomcounty.appconfigurations.configexplorer;


import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
public class Configuration {
	
	private String configName;
	private String configType;
	private String currentVersion;
	private Map<String, String> configEntries = null;
	private Node myNode;
	private Node customerNode;
	
	
	
	public Configuration() {
		this.configName = "Generic Configuration";
		this.currentVersion = "v0";
		this.configType = "Base";
		//this.configEntries = new ConcurrentHashMap<String, String>(2); 
		this.configEntries = new LinkedHashMap<String, String>(2); 
	}

	public Configuration(String configName, String configType, int version) {
		this.configName = configName;
		this.configType = configType;
		this.currentVersion = "v0";
		//this.configEntries = new ConcurrentHashMap<String, String>(2); 
		this.configEntries = new LinkedHashMap<String, String>(2); 
	}
	
	public Configuration(Node configNode, Node customerNode) {
		this.configName = (String) configNode.getProperty(App.CONFIGNAME_KEY);
		this.currentVersion = (String) configNode.getProperty("version");
		this.configType = (String) configNode.getProperty("Type");
		this.myNode = configNode;
		this.customerNode = customerNode;
		//this.configEntries = new ConcurrentHashMap<String, String>(2); 
		this.configEntries = new LinkedHashMap<String, String>(2); 
		
		// this is the tricky part...assembling the final configuration by walking the history. Could get inefficient if we have a long history, obviously... Storage optimization vs caching...
		
		Node startNode = this.getCustomerStartConfigNode();
		Node baseNode = this.getBaseConfigNode();
		
		// if this is a rollback version, then the ending node is different, otherwise it is the current node.
		Node endNode = configNode;
		if(this.configType.equalsIgnoreCase("Rollback")) {
			endNode = configNode.getSingleRelationship(App.RelTypes.MATCHES_VERSION, Direction.OUTGOING).getEndNode();			
		}
		// Note starting version
        String baseNodeVersion = (String) baseNode.getProperty("version");

		// capture the version number to stop at
		String lastVersion = (String) endNode.getProperty("version");
		String strConfigBuilder = (String) baseNode.getProperty("Contents");

		JSONObject configMap = (JSONObject) JSONValue.parse(strConfigBuilder);
		Set<Map.Entry<String, String>> configSet = configMap.entrySet();
        for (Map.Entry<String, String> entry : configSet) {
        	configEntries.put(entry.getKey(), entry.getValue());
        }

		// walk the CHANGED_TO relation to override the map entries to get the final map. 
		UpdateConfiguration(startNode, baseNodeVersion, lastVersion);
		

	}
	
	public Node getBaseConfigNode() {

		Node baseNode = null;
		if(customerNode == null)
			return baseNode;
		
		for ( Relationship rel : customerNode.getRelationships(App.RelTypes.CUSTOMER_BASE_CONFIG, Direction.OUTGOING)) {
			Node configNode1 = rel.getOtherNode(customerNode);
			String baseConfigName = (String)configNode1.getProperty(App.CONFIGNAME_KEY);
			if(baseConfigName.equalsIgnoreCase(this.configName)) {
				baseNode = configNode1;
				break;
			}
		}
		return baseNode;
		
	}
	
	public Node getCustomerStartConfigNode() {
		Node startNode = null;
		if(customerNode == null)
			return startNode;
		for ( Relationship rel : customerNode.getRelationships(App.RelTypes.CUSTOMER_START_CONFIG, Direction.OUTGOING)) {
			Node configNode1 = rel.getOtherNode(customerNode);
			String baseConfigName = (String)configNode1.getProperty(App.CONFIGNAME_KEY);
			if(baseConfigName.equalsIgnoreCase(this.configName)) {
				startNode = configNode1;
				break;
			}
		}
		return startNode;
	}
	
	
	private void UpdateConfiguration( Node startNode, String currentNodeVersion, String lastVersion) {
		
		Node nextNode = startNode;
		
        while(nextNode != null && currentNodeVersion.equalsIgnoreCase(lastVersion) == false)
        {
			// update configuration from currentnode
			String strCurrentNodeConfigBuilder = (String) nextNode.getProperty("Contents");
			JSONObject changesetCurrentNodeConfigMap = (JSONObject) JSONValue.parse(strCurrentNodeConfigBuilder);
			Set<Map.Entry<String, String>> currentNodeConfigSet = changesetCurrentNodeConfigMap.entrySet();
	        for (Map.Entry<String, String> entry : currentNodeConfigSet) {
	        	configEntries.put(entry.getKey(), entry.getValue());
	        }
	        currentNodeVersion = (String) nextNode.getProperty("version");
	        Relationship nextRelation = nextNode.getSingleRelationship(App.RelTypes.CHANGED_TO, Direction.OUTGOING);
	        Node nextNode2 = null;
	        if(nextRelation != null)
	        {
				nextNode2 = nextRelation.getEndNode();

	        }
	        nextNode = nextNode2;
        }
	}
	/*
	public Configuration getPreviousVersion() {
		
	}
	
	public Configuration getNextVersion() {
		
	}
	*/
	public String getConfigName() {
		return configName;
	}

	public String getConfigType() {
		return configType;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public String getConfigEntries() {
		
		 String jsonText = JSONValue.toJSONString(configEntries);
		return jsonText;
	}

	public String GetAllChangeHistory() {
		
		Node baseNode = this.getBaseConfigNode();
		Node startNode = this.getCustomerStartConfigNode();

		Map<String, String>  m1 = new LinkedHashMap<String, String>();

		List<Map<String, String>>  l1 = new LinkedList<Map<String, String>>();		
		String concatString = "";
		
		if(baseNode == null || startNode == null) {
			concatString = "Hmm... can't find the nodes for history";
			return concatString;
		}

		
		if(configType.equalsIgnoreCase("Base")){
			
			concatString = "No change history yet";
			return concatString;
		}
		
		m1.put("Configuration Name", this.configName);
		m1.put("Version", (String) baseNode.getProperty("version"));
		m1.put("Type", (String)baseNode.getProperty("Type"));
		m1.put("Author","Not Set");
		l1.add(m1);
		Node nextNode = startNode;
		
        while(nextNode != null)
        {
    		Map<String, String>  m2 = new LinkedHashMap<String, String>();
			// update configuration from currentnode			
			// get the user who created the version - VERSION_EDITOR
			
			Node editorNode = nextNode.getSingleRelationship(App.RelTypes.VERSION_EDITOR, Direction.INCOMING).getStartNode();
			
			String editorName = (String) editorNode.getProperty(App.USERNAME_KEY);
			m2.put("Configuration Name", this.configName);
			m2.put("Version", (String) nextNode.getProperty("version"));
			m2.put("Type", (String)nextNode.getProperty("Type"));
			m2.put("Author",editorName);
			l1.add(m2);

			Node nextNode2 = null;
			Relationship nextRelationship = nextNode.getSingleRelationship(App.RelTypes.CHANGED_TO, Direction.OUTGOING);
			if(nextRelationship != null)
				nextNode2 = nextRelationship.getEndNode();
			nextNode = nextNode2;
        }
        
        String jsonString = JSONValue.toJSONString(l1);
        return jsonString;
	}
	
	public String GetAllChangesMadeBy(String strEditor) {
		String concatString = "";
		
		return concatString;
	}
	 	
	public String GetAllChangesMadeForCustomer(String strCustomer) {
		String concatString = "";
		
		return concatString;
	}

}
