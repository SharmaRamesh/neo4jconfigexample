package com.bloomcounty.appconfigurations.configexplorer;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;

import org.neo4j.kernel.impl.util.FileUtils;
import org.neo4j.graphdb.Transaction;
import org.json.simple.JSONObject;


public class App 
{
    private static final String DB_PATH = "target/config-db";
    public static final String USERNAME_KEY = "username";
    public static final String CONFIGNAME_KEY = "configname";
    public static final String CUSTOMERNAME_KEY = "customername";
    public static final String FEATURENAME_KEY = "featurename";
    public enum MyLabels implements Label { CONFIGURATION, CUSTOMER, FEATURE, DEVELOPER; }
    //Relationship relationship;
    
    private static GraphDatabaseService graphDb;
    private static Index<Node> nodeIndex;
    
    private List<Customer> customers = new ArrayList<Customer>(); 
    //private Configuration[] configurations;
    //private Editor[] developers;
    //private Feature[] features;

    public static enum RelTypes implements RelationshipType
    {
    	FEATURE_CONTAINS_CONFIG, CUSTOMER_START_CONFIG, BASE_VERSION, EDITED_BY, VERSION_EDITOR, CHANGED_TO, MATCHES_VERSION, CURRENT_VERSION, CUSTOMER_BASE_CONFIG, ARCHIVE_VERSION
    }
    
    public static void main( final String[] args ) throws IOException
    {
    	
        App configApp = new App();

        configApp.createConfigDb();
        
        configApp.runSomeQueries();
        
        configApp.removeData(); // It is a NOP for now
        configApp.shutDown();
    }

    public void runSomeQueries() {
        // output current version for config "Alpha Config" for customer "Acme Industries"
        try ( Transaction tx = graphDb.beginTx() )
        {
        	Customer acmeCustomer = FindCustomer("Acme Industries");
	        
	        if(acmeCustomer != null) {
	        	Configuration alphaConfig = acmeCustomer.getCurrentConfig("Alpha Config");
	        	if(alphaConfig != null) {
		        	String currentConfig = alphaConfig.getConfigEntries();
		        	System.out.println("Current Config for \"Alpha Config\" for customer \"Acme Industries\" is : " + currentConfig);
		        	String changeHistory = alphaConfig.GetAllChangeHistory();
		        	System.out.println("Change history for configuration \"Alpha Config\" for customer \"Acme Industries\" is : ");
		        	System.out.println(changeHistory);
	        	}
	        }
	        Customer acme2 = FindCustomer("Acme Industries");
	        
        	Customer wileyCustomer = FindCustomer("Wiley Coyote Inc");
	        
	        if(wileyCustomer != null) {
	        	Configuration alphaConfig = wileyCustomer.getCurrentConfig("Alpha Config");
	        	if(alphaConfig != null) {
		        	String currentConfig = alphaConfig.getConfigEntries();
		        	System.out.println("Current Config for \"Alpha Config\" for customer \"Wiley Coyote Inc\" is : " + currentConfig);
		        	String changeHistory = alphaConfig.GetAllChangeHistory();
		        	System.out.println("Change history for configuration \"Alpha Config\" for customer \"Wiley Coyote Inc\" is : ");
		        	System.out.println(changeHistory);
		        	
		        	Configuration hardwareConfig = wileyCustomer.getCurrentConfig("Computer Config");
		        	if(hardwareConfig != null) {
			        	String currentConfig2 = hardwareConfig.getConfigEntries();
			        	System.out.println("Current Config for \"Computer Config\" for customer \"Wiley Coyote Inc\" is : " + currentConfig2);
			        	String changeHistory2 = hardwareConfig.GetAllChangeHistory();
			        	System.out.println("Change history for configuration \"Computer Config\" for customer \"Wiley Coyote Inc\" is : ");
			        	System.out.println(changeHistory2);
		        		
		        	}
	        	}
	        }

	        tx.success();
	        tx.close();
        }
    	
    }
    
    public Customer FindCustomer(String strCustomerName) {
    	
    	Customer foundCustomer = null;
    	
    	for(Customer customer : customers){
            if(customer.getCustomerName().equalsIgnoreCase(strCustomerName)) {
            	foundCustomer = customer;
            	break;
            }
        }

    	if(foundCustomer != null) return foundCustomer;
    		
    	// see if one has been added or maybe this is the first time...
    	// Find a customer through the search index

    	Node customerNode = nodeIndex.get( CUSTOMERNAME_KEY, strCustomerName ).getSingle();
    	
    	if(customerNode == null)
    		return foundCustomer;
    	
    	// create a Customer object
    	foundCustomer = new Customer(customerNode);
    	// Add it to the list
    	customers.add(foundCustomer);
    	
    	return foundCustomer;
    	
   	
    }

    void setupOneConfig() {
    	
        try ( Transaction tx = graphDb.beginTx() )
        {
            nodeIndex = graphDb.index().forNodes( "nodes" );
                
            Map<String, String> linkedHashMap = new LinkedHashMap<String, String>();
            linkedHashMap.put("host","http://www.hackathon.com");
            linkedHashMap.put("port-number", "80");
            linkedHashMap.put("path", "/webapp/EndpointController");
            linkedHashMap.put("user-name","Scott");
            linkedHashMap.put("key", "tiger");
            JSONObject tempJsonObject = new JSONObject(linkedHashMap);
            String jsonText = tempJsonObject.toString();
            Node baseConfig1 = createConfigurationNode("Alpha Config", MyLabels.CONFIGURATION, "Base", "v0", jsonText);
            
            linkedHashMap.clear();
            linkedHashMap.put("path", "/webapp/CustomerAcmeController");
            linkedHashMap.put("user-name", "Brawny");
            linkedHashMap.put("key", "leopard");
            tempJsonObject = new JSONObject(linkedHashMap);
            jsonText = tempJsonObject.toString();
            Node changeConfig2 = createConfigurationNode("Alpha Config", MyLabels.CONFIGURATION, "Changeset", "Acme-v1", jsonText);           

            linkedHashMap.clear();
            linkedHashMap.put("host","http://www.acme.com");
            linkedHashMap.put("port-number", "8080");
            tempJsonObject = new JSONObject(linkedHashMap);
            jsonText = tempJsonObject.toString();
            Node changeConfig3 = createConfigurationNode("Alpha Config", MyLabels.CONFIGURATION, "Changeset", "Acme-v2", jsonText);           
            
            linkedHashMap.clear();
            linkedHashMap.put("path", "/webapp/CustomerWileyCoyoteController");
            linkedHashMap.put("user-name", "RoadRunner");
            linkedHashMap.put("key", "BeepBeep");
            tempJsonObject = new JSONObject(linkedHashMap);
            jsonText = tempJsonObject.toString();
            Node changeConfig4 = createConfigurationNode("Alpha Config", MyLabels.CONFIGURATION, "Changeset", "WileyCoyote-v1", jsonText);           

            linkedHashMap.clear();
            linkedHashMap.put("host","http://www.wileycoyote.com");
            linkedHashMap.put("port-number", "8080");
            tempJsonObject = new JSONObject(linkedHashMap);
            jsonText = tempJsonObject.toString();
            Node changeConfig5 = createConfigurationNode("Alpha Config", MyLabels.CONFIGURATION, "Changeset", "WileyCoyote-v2", jsonText);           

            linkedHashMap.clear();
            tempJsonObject = new JSONObject(linkedHashMap);
            jsonText = tempJsonObject.toString();
            Node changeConfig6 = createConfigurationNode("Alpha Config", MyLabels.CONFIGURATION, "Rollback", "WileyCoyote-v1", jsonText);   
            
            Node customerNode1 = createAndIndexCustomer("Acme Industries", MyLabels.CUSTOMER);
            Node customerNode2 = createAndIndexCustomer("Wiley Coyote Inc", MyLabels.CUSTOMER);
            
            Node featureNode1 = createAndIndexFeature("Cool Feature Z", MyLabels.FEATURE);
            Node featureNode2 = createAndIndexFeature("Hot Feature X", MyLabels.FEATURE);
            
            Node userNode1 = createAndIndexUser("Joe Plumber", MyLabels.DEVELOPER);
            Node userNode2 = createAndIndexUser("Jane Doe", MyLabels.DEVELOPER);
            
            Relationship relationship1 = featureNode1.createRelationshipTo(baseConfig1, RelTypes.FEATURE_CONTAINS_CONFIG);
            Relationship relationship2 = featureNode2.createRelationshipTo(baseConfig1, RelTypes.FEATURE_CONTAINS_CONFIG);

            Relationship relationship3 = baseConfig1.createRelationshipTo(changeConfig2, RelTypes.CHANGED_TO);
            Relationship relationship4 = changeConfig2.createRelationshipTo(changeConfig3, RelTypes.CHANGED_TO);
            
            
            Relationship relationship5 = baseConfig1.createRelationshipTo(changeConfig4, RelTypes.CHANGED_TO);
            Relationship relationship6 = changeConfig4.createRelationshipTo(changeConfig5, RelTypes.CHANGED_TO);
            Relationship relationship7 = changeConfig5.createRelationshipTo(changeConfig6, RelTypes.CHANGED_TO);
            Relationship relationship8 = changeConfig6.createRelationshipTo(changeConfig4, RelTypes.MATCHES_VERSION);

            Relationship relationship9 = customerNode2.createRelationshipTo(changeConfig6, RelTypes.CURRENT_VERSION);
            Relationship relationship10 = customerNode2.createRelationshipTo(baseConfig1, RelTypes.CUSTOMER_BASE_CONFIG);
            Relationship relationship11 = customerNode2.createRelationshipTo(changeConfig4, RelTypes.ARCHIVE_VERSION);
            Relationship relationship12 = customerNode2.createRelationshipTo(changeConfig5, RelTypes.ARCHIVE_VERSION);
            
            Relationship relationship13 = customerNode1.createRelationshipTo(changeConfig3, RelTypes.CURRENT_VERSION);
            Relationship relationship14 = customerNode1.createRelationshipTo(baseConfig1, RelTypes.CUSTOMER_BASE_CONFIG);
            Relationship relationship15 = customerNode1.createRelationshipTo(changeConfig2, RelTypes.ARCHIVE_VERSION);
            
            Relationship relationship16 = userNode1.createRelationshipTo(baseConfig1, RelTypes.EDITED_BY);
            Relationship relationship17 = userNode1.createRelationshipTo(changeConfig4, RelTypes.VERSION_EDITOR);
            Relationship relationship18 = userNode1.createRelationshipTo(changeConfig6, RelTypes.VERSION_EDITOR);
            Relationship relationship19 = userNode1.createRelationshipTo(changeConfig2, RelTypes.VERSION_EDITOR);
             
            Relationship relationship20 = userNode2.createRelationshipTo(baseConfig1, RelTypes.EDITED_BY);
            Relationship relationship21 = userNode2.createRelationshipTo(changeConfig3, RelTypes.VERSION_EDITOR);
            Relationship relationship22 = userNode2.createRelationshipTo(changeConfig5, RelTypes.VERSION_EDITOR);
            
            Relationship relationship23 = customerNode1.createRelationshipTo(changeConfig2, RelTypes.CUSTOMER_START_CONFIG);
            Relationship relationship24 = customerNode2.createRelationshipTo(changeConfig4, RelTypes.CUSTOMER_START_CONFIG);

            // Second Config
            linkedHashMap.clear();
            linkedHashMap.put("CPU","Intel 4th Generation");
            linkedHashMap.put("Hard Drive", "HD Manufacturer");
            linkedHashMap.put("Graphics", "Graphics Manufacturer");
            linkedHashMap.put("RAM","Minimum 4G");
            linkedHashMap.put("Monitor", "Hi Resolution");
            tempJsonObject = new JSONObject(linkedHashMap);
            jsonText = tempJsonObject.toString();
            Node baseConfig11 = createConfigurationNode("Computer Config", MyLabels.CONFIGURATION, "Base", "v0", jsonText);
            
            linkedHashMap.clear();
            linkedHashMap.put("CPU","i7 Haswell");
            linkedHashMap.put("Hard Drive", "Seagate");
            linkedHashMap.put("Media", "BluRay");
            tempJsonObject = new JSONObject(linkedHashMap);
            jsonText = tempJsonObject.toString();
            Node changeConfig22 = createConfigurationNode("Computer Config", MyLabels.CONFIGURATION, "Changeset", "Acme-v11", jsonText);           

            linkedHashMap.clear();
            linkedHashMap.put("Graphics", "NVIDIA");
            linkedHashMap.put("RAM","16G");
            linkedHashMap.put("Monitor", "Samsung");
            tempJsonObject = new JSONObject(linkedHashMap);
            jsonText = tempJsonObject.toString();
            Node changeConfig33 = createConfigurationNode("Computer Config", MyLabels.CONFIGURATION, "Changeset", "Acme-v22", jsonText);           
            
            linkedHashMap.clear();
            linkedHashMap.put("CPU","Intel Atom");
            linkedHashMap.put("Hard Drive", "Hitachi");
            linkedHashMap.put("RAM", "4G");
            tempJsonObject = new JSONObject(linkedHashMap);
            jsonText = tempJsonObject.toString();
            Node changeConfig44 = createConfigurationNode("Computer Config", MyLabels.CONFIGURATION, "Changeset", "WileyCoyote-v11", jsonText);           

            linkedHashMap.clear();
            linkedHashMap.put("CPU","i5 Haswell");
            tempJsonObject = new JSONObject(linkedHashMap);
            jsonText = tempJsonObject.toString();
            Node changeConfig55 = createConfigurationNode("Computer Config", MyLabels.CONFIGURATION, "Changeset", "WileyCoyote-v22", jsonText);           

            Relationship relationshipA = featureNode1.createRelationshipTo(baseConfig11, RelTypes.FEATURE_CONTAINS_CONFIG);
            Relationship relationshipB = baseConfig11.createRelationshipTo(changeConfig22, RelTypes.CHANGED_TO);
            Relationship relationshipC = changeConfig22.createRelationshipTo(changeConfig33, RelTypes.CHANGED_TO);

            Relationship relationshipD = baseConfig11.createRelationshipTo(changeConfig44, RelTypes.CHANGED_TO);
            Relationship relationshipE = changeConfig44.createRelationshipTo(changeConfig55, RelTypes.CHANGED_TO);
            
            Relationship relationshipF = customerNode1.createRelationshipTo(changeConfig33, RelTypes.CURRENT_VERSION);
            Relationship relationshipG = customerNode1.createRelationshipTo(baseConfig11, RelTypes.CUSTOMER_BASE_CONFIG);
            Relationship relationshipH = customerNode1.createRelationshipTo(changeConfig22, RelTypes.ARCHIVE_VERSION);
            Relationship relationshipI = customerNode1.createRelationshipTo(changeConfig22, RelTypes.CUSTOMER_START_CONFIG);

            Relationship relationshipJ = customerNode2.createRelationshipTo(changeConfig55, RelTypes.CURRENT_VERSION);
            Relationship relationshipK = customerNode2.createRelationshipTo(baseConfig11, RelTypes.CUSTOMER_BASE_CONFIG);
            Relationship relationshipL = customerNode2.createRelationshipTo(changeConfig44, RelTypes.ARCHIVE_VERSION);
            Relationship relationshipM = customerNode2.createRelationshipTo(changeConfig44, RelTypes.CUSTOMER_START_CONFIG);

            Relationship relationshipN = userNode1.createRelationshipTo(baseConfig11, RelTypes.EDITED_BY);
            Relationship relationshipO = userNode1.createRelationshipTo(changeConfig22, RelTypes.VERSION_EDITOR);
            Relationship relationshipP = userNode1.createRelationshipTo(changeConfig44, RelTypes.VERSION_EDITOR);

            Relationship relationshipQ = userNode2.createRelationshipTo(baseConfig11, RelTypes.EDITED_BY);
            Relationship relationshipR = userNode2.createRelationshipTo(changeConfig33, RelTypes.VERSION_EDITOR);            
            Relationship relationshipS = userNode2.createRelationshipTo(changeConfig55, RelTypes.VERSION_EDITOR);
            
            tx.success();
            tx.close();
        }
    }
    
     void createConfigDb() throws IOException
    {
        FileUtils.deleteRecursively( new File( DB_PATH ) );

        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
        registerShutdownHook( graphDb );
 
        setupOneConfig();
    }

    void removeData()
    {
//        try ( Transaction tx = graphDb.beginTx() )
//        {
//            // START SNIPPET: removingData
//            // let's remove the data
//            firstNode.getSingleRelationship( RelTypes.KNOWS, Direction.OUTGOING ).delete();
//            firstNode.delete();
//            secondNode.delete();
//            // END SNIPPET: removingData
//
//            tx.success();
//        }
    }

    void shutDown()
    {
        System.out.println();
        System.out.println( "Shutting down database ..." );
        graphDb.shutdown();
    }
        
    private static void shutdown()
    {
        graphDb.shutdown();
    }

    private static Node createAndIndexUser( final String username, Label theLabel )
    {
        Node node = graphDb.createNode();
        node.setProperty( USERNAME_KEY, username );
        nodeIndex.add( node, USERNAME_KEY, username );
        node.addLabel(theLabel);
        return node;
    }

    private static Node createAndIndexCustomer( final String customerName, Label theLabel )
    {
        Node node = graphDb.createNode();
        node.setProperty( CUSTOMERNAME_KEY, customerName );
        nodeIndex.add( node, CUSTOMERNAME_KEY, customerName );
        
        node.addLabel(theLabel);
        return node;
    }

    private static Node createAndIndexFeature( final String featureName, Label theLabel )
    {
        Node node = graphDb.createNode();
        node.setProperty( FEATURENAME_KEY, featureName );
        nodeIndex.add( node, FEATURENAME_KEY, featureName );
        node.addLabel(theLabel);
        return node;
    }

    private static Node createAndIndexConfiguration( final String configName ) 
    {
        Node node = graphDb.createNode();
        node.setProperty( CONFIGNAME_KEY, configName );
        nodeIndex.add( node, CONFIGNAME_KEY, configName );
        return node;
    }
    
    private static Node createConfigurationNode(String configName, Label theLabel, String configType, String version, String content) {
        Node baseConfig1 = createAndIndexConfiguration( configName );

        baseConfig1.addLabel(theLabel);
        baseConfig1.setProperty("Type", configType);
        baseConfig1.setProperty("version", version);
        baseConfig1.setProperty("Contents", content);
  	
        return baseConfig1;
    }

    private static void registerShutdownHook(final GraphDatabaseService graphDb)
    {
        // Registers a shutdown hook for the Neo4j and index service instances
        // so that it shuts down nicely when the VM exits (even if you
        // "Ctrl-C" the running example before it's completed)
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                shutdown();
            }
        } );
    }
}