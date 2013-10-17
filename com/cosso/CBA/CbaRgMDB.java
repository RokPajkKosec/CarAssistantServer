package com.cosso.CBA;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

/**
 * @author Rok Pajk Kosec
 * Class used for rule generation from DbEntry entries in database
 */
public class CbaRgMDB {
	//number of data entries in database
	private static int dataSetSize;
	//list of rules
	private static List<RuleItem> ruleSet;
	//MongoDB database connection parameters
	private MongoClient mongoClient;
	private Morphia morphia;
	private Datastore ds;
	//support
	private final double support;
	//confidence
	private final double confidence;

	public static void main(String[] args) {
		CbaRgMDB cba = new CbaRgMDB(0.1, 0.3);
		cba.getRulesSet("rok", "consumption");
		for(RuleItem x : ruleSet){
			System.out.println(x.getConditions().getConditions().toString() + " --> " + x.getClassValue()+ ", "
					+ x.getRuleSupCount() + "/" + x.getConditions().getCondSupCount());
		}
		System.out.println(ruleSet.size());
	}
	
	/**
	 * @param sup
	 * @param conf
	 * Constructor
	 * Needs support and confidence
	 */
	public CbaRgMDB(double sup, double conf){
		this.support = sup;
		this.confidence = conf;
	}
	
	/**
	 * @param userName
	 * @param targetClass
	 * @return final list of rules
	 * Combines methods for 1-RuleItems and k-RuleItems and creates one method that
	 * can be used outside this class for rule generation. It runs rule generation for every
	 * road type separately so no road type is ignored due to low number of entries
	 */
	public List<RuleItem> getRulesSet(String userName, String targetClass){
		ruleSet = new ArrayList<RuleItem>();
		
		try {
			openConnection();
						
			@SuppressWarnings("unchecked")
			List<String> myCol = (List<String>)ds.getCollection(ProcessedEntry.class).distinct("roadType");
			for(String x : myCol){
				List<RuleItem> rules1 = getFrequent1RuleItems(x, userName, targetClass);
				getFrequentKRuleItems(rules1, x, userName, targetClass);
			}
			
			close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ruleSet;
	}
	
	/**
	 * @param rules1
	 * @param roadType
	 * @param userName
	 * @param targetClass
	 * Generates ruleitems, longer than 1 from list of 1-RuleItems
	 */
	private void getFrequentKRuleItems(List<RuleItem> rules1, String roadType, String userName, String targetClass){
		
		//if no 1-rules are found return
		if(rules1 == null){
			return;
		}

		List<RuleItem> rulesK = new ArrayList<RuleItem>();
		List<RuleItem> rulesC;	
		rulesK.addAll(rules1);
		int k = 2;
		RuleItem newRule = null;
		String tempKey = "";
		String tempValue = "";
		String skipKey = "roadType";
		String tempClassValue = "";
		boolean condSetFound = true;
		
		//continue until there are still rules left to extend
		while(rulesK.size() != 0){
			rulesC = new ArrayList<RuleItem>();
			
			/*
			 * merge every rule with every rule. Merge function of RuleItem class will determine if this is possible
			 * candidate generation
			 */
			for(int i=0; i<rulesK.size(); i++){
				for(int j=i+1; j<rulesK.size(); j++){
					newRule = rulesK.get(i).Merge(rulesK.get(j), k);
					if(newRule != null){
						rulesC.add(newRule);
					}
				}
			}
			
			//tuki lahko popravlas condSet od rulesK brez da si povozs pravila ker so novi kandidati v rulecC, rulesK (pointer) pa neha obstajat
			//ob nasledni referenci nanga (rulesK = rulesC) ker se pointer zamena.
			//DONE
			
			//add road type condition
			for(RuleItem x : rulesK){
				x.getConditions().addCondition(skipKey, roadType);
			}
			ruleSet.addAll(rulesK);
				
        	Query<ProcessedEntry> query = ds.createQuery(ProcessedEntry.class).filter("userName =", userName).filter("roadType =", roadType);
			
        	//first pass over all records in database
			for(ProcessedEntry x : query){
				
				if(targetClass.equals("speedDifference")){
					tempClassValue = String.valueOf(x.getSpeedDifference());
				}
				else{
					tempClassValue = String.valueOf(x.getConsumption());
				}
				
				//second pass over all rules
				for(int j=0; j<rulesC.size(); j++){
					RuleItem r = rulesC.get(j);
					//third pass over all conditions in each rule
					for(Map.Entry<String, String> c : r.getConditions().getConditions().entrySet()){
						//fourth pass over all atributes in each record
						int i;
						for(i=0; i<x.getKeys().length; i++){
							tempKey = x.getKeys()[i];
							tempValue = String.valueOf(x.getValue(tempKey));
							if(tempKey.equals(c.getKey()) && tempValue.equals(c.getValue())){
								break;
							}
						}
						if(i == x.getKeys().length){
							condSetFound = false;
							break;
						}
					}
					
					//if rule is not found and condition set is found, get condition set support and increase it in both rules
					if(tempClassValue.equals(r.getClassValue()) && condSetFound){
						r.setRuleSupCount(r.getRuleSupCount() + 1);
						r.getConditions().setCondSupCount(r.getConditions().getCondSupCount() + 1);
					}
					else if(condSetFound){
						r.getConditions().setCondSupCount(r.getConditions().getCondSupCount() + 1);
					}
					condSetFound = true;
				}
			}
			
			//rule pruning based on support and confidence
			Iterator<RuleItem> ir = rulesC.iterator();
			while(ir.hasNext()){
				RuleItem temp = ir.next();
				if((double)temp.getRuleSupCount()/(double)dataSetSize < support){
					ir.remove();
				}			
				else if((double)temp.getRuleSupCount()/(double)temp.getConditions().getCondSupCount() < confidence){
					ir.remove();
				}
			}
			
			//rulesK to rules list
			rulesK = rulesC;
			
			k++;
		//end outer while
		}
	}
	
	/**
	 * @param roadType
	 * @param userName
	 * @param targetClass
	 * @return list of frequent 1-RuleItems
	 * Fins frequent ruleitems on length 1 (A -> B). 
	 */
	private List<RuleItem> getFrequent1RuleItems(String roadType, String userName, String targetClass){
		String tempKey = "";
		String tempValue = "";
		String classKey = targetClass;
		String tempClassValue = "";
		String skipKey = "roadType";
		String skipClassKey;
		
		if(targetClass.equals("speedDifference")){
			skipClassKey = "consumption";
		}
		else{
			skipClassKey = "speedDifference";
		}
		
		int tempCount = 0;
		List<RuleItem> rules = new ArrayList<RuleItem>();
		boolean condSetFound = false;
		boolean ruleFound = false;
					
		dataSetSize = (int)ds.createQuery(ProcessedEntry.class).filter("userName =", userName).filter("roadType =", roadType).countAll();
		
		//if there is less than 10 records of current road type ignore and continue on to next one
    	if(dataSetSize < 10){
    		return null;
    	}
        
    	Query<ProcessedEntry> query = ds.createQuery(ProcessedEntry.class).filter("userName =", userName).filter("roadType =", roadType);
    	
    	//pass over all entries of current road type
    	for(ProcessedEntry y : query){
    		
			if(targetClass.equals("speedDifference")){
				tempClassValue = String.valueOf(y.getSpeedDifference());
			}
			else{
				tempClassValue = String.valueOf(y.getConsumption());
			}
			
			//pass over all keys in entry
			for(int i=0; i<y.getKeys().length; i++){
				tempKey = y.getKeys()[i];
				
				//skip road type and both target classes
				if(tempKey.equalsIgnoreCase(classKey) || tempKey.equalsIgnoreCase(skipKey) || tempKey.equals(skipClassKey)){
					continue;
				}
				tempValue = String.valueOf(y.getValue(tempKey));
				
				//pass over all found rules. If rule exists increase support, else add new 1-rule
				for(RuleItem x : rules){
					if(x.getConditions().getConditions().containsKey(tempKey) && x.getConditions().getConditions().get(tempKey).equals(tempValue) &&
							x.getClassKey().equals(classKey) && x.getClassValue().equals(tempClassValue)){
						x.setRuleSupCount(x.getRuleSupCount() + 1);
						x.getConditions().setCondSupCount(x.getConditions().getCondSupCount() + 1);
						ruleFound = true;
						condSetFound = true;
					}
					else if(x.getConditions().getConditions().containsKey(tempKey) && x.getConditions().getConditions().get(tempKey).equals(tempValue)){
						x.getConditions().setCondSupCount(x.getConditions().getCondSupCount() + 1);
						condSetFound = true;
						tempCount = x.getConditions().getCondSupCount();
					}
				}
				
				//if rule is not found and condition set is found, get condition set support and increase it in both rules
				if(!ruleFound && condSetFound){
					rules.add(new RuleItem(classKey, tempClassValue, new CondSet(tempKey, tempValue), dataSetSize, userName));
					rules.get(rules.size()-1).getConditions().setCondSupCount(tempCount);
				}
				else if(!ruleFound){
					rules.add(new RuleItem(classKey, tempClassValue, new CondSet(tempKey, tempValue), dataSetSize, userName));
				}
				
				condSetFound = false;
				ruleFound = false;
			}		
						
		}
        
    	//if two rules have same condition set use one with better support and remove other
		for(int i=0; i<rules.size(); i++){
			RuleItem first = rules.get(i);
			for(int j=i+1; j<rules.size(); j++){
				RuleItem temp = rules.get(j);
				if(temp.getConditions().getConditions().entrySet().equals(first.getConditions().getConditions().entrySet())){
					if(temp.getRuleSupCount() <= first.getRuleSupCount()){
						rules.remove(j);
						j--;
					}
					else{
						rules.remove(i);
						i--;
						break;
					}
				}
			}
		}

		//rule list pruning based on support and confidence
		Iterator<RuleItem> ir = rules.iterator();
		while(ir.hasNext()){
			RuleItem temp = ir.next();
			if((double)temp.getRuleSupCount()/(double)dataSetSize < support){
				ir.remove();
			}			
			else if((double)temp.getRuleSupCount()/(double)temp.getConditions().getCondSupCount() < confidence){
				ir.remove();
			}
		}

        RuleItem temp;
        
        //sort based on class value
        for(int i=0; i<rules.size(); i++){
        	for(int j=i+1; j<rules.size(); j++){
        		if(rules.get(i).getClassValue().compareToIgnoreCase(rules.get(j).getClassValue()) > 0){
        			temp = rules.get(i);
        			rules.set(i, rules.get(j));
        			rules.set(j, temp);
        		}
        	}
        }

		return rules;
	}
	
	/**
	 * @throws UnknownHostException
	 * Opens database connection and mapes ProcessedEntry class
	 */
	public void openConnection() throws UnknownHostException{
		mongoClient = new MongoClient( "localhost" , 27017 );
		morphia = new Morphia();
		ds = morphia.createDatastore((Mongo)mongoClient, "userData");	
		morphia.map(ProcessedEntry.class);
		ds.ensureIndexes(); //creates indexes from @Index annotations in your entities
		ds.ensureCaps(); //creates capped collections from @Entity
	}

	/**
	 * Closes database connection
	 */
	public void close(){
		mongoClient.close();
	}
}