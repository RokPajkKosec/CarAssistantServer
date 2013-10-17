package com.cosso.CBA;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

/**
 * @author Rok Pajk Kosec
 * Class used for classifying current data and for building classificator from
 * rules generated in CbaRgMDB class
 */
public class Classificator {
	private MongoClient mongoClient;
	private Morphia morphia;
	private Datastore ds;
	//list of rules
	private List<RuleItem> ruleSet;
	
	public static void main(String[] args){
		Classificator classificator = new Classificator();
		classificator.generateClassificator("rok", "speedDifference", 0.1, 0.3);
	}
	
	public Classificator(){
		
	}
	
	/**
	 * @param entry
	 * @param targetClass
	 * @return target value used for path calculation
	 * Uses ProcessedEntry object that contains data for classification and target class (speed difference or consumption).
	 * Used rule is fist that satisfies provided data
	 */
	public double classify(ProcessedEntry entry, String targetClass){
		double targetClassValue = 0;
		String tempKey = "";
		String tempValue = "";
		String skipClassKey;
		
		if(targetClass.equals("speedDifference")){
			skipClassKey = "consumption";
		}
		else{
			skipClassKey = "speedDifference";
		}
						
		boolean condSetFound = true;
		
		/*
		 * 1. pravila v arrayList, da bo hitrej za pol (to lahko samo ta prvic, tako da if arrayList not null -> skip -- done!
		 * 2. klasificiraj - kodo vzami spodaj
		 * 3. vrni vrednost
		 */
		
		//put rules in array from database. This only happens when classifying first edge in path. Increases speed
		if(ruleSet == null){
			try {
				openConnection();
				
				ruleSet = new ArrayList<RuleItem>();
				Query<RuleItem> query = ds.createQuery(RuleItem.class).filter("userName =", entry.getUserName()).filter("classKey =", targetClass).order("id");
				for(RuleItem x : query){
					ruleSet.add(x);
				}
							
				close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		
		//pass over all rules in database stored in array list
		for(RuleItem r : ruleSet){
			
			//pass over all conditions of each rule
			for(Map.Entry<String, String> c : r.getConditions().getConditions().entrySet()){
				int i;
				/*
				 * pass over all data in target case. This parts finds if rule condition set subset of
				 * given case conditions.
				 */
				for(i=0; i<entry.getKeys().length; i++){
					tempKey = entry.getKeys()[i];
					if(tempKey.equals(skipClassKey)){
						continue;
					}
					tempValue = String.valueOf(entry.getValue(tempKey));
					if(tempKey.equals(c.getKey()) && tempValue.equals(c.getValue())){
						break;
					}
				}
				if(i == entry.getKeys().length){
					condSetFound = false;
					break;
				}
			}
			
			//if subset is found rule covers data case
			if(condSetFound){
				targetClassValue = Double.valueOf(r.getClassValue());
				if(r.getConditions().getConditions().size() == 0 && targetClass.equals("speedDifference")){
					targetClassValue = 0.0;
				}
				break;
			}
			condSetFound = true;

		}
		
		return targetClassValue;
	}
	
	/**
	 * @param userName
	 * @param targetClass
	 * @param support
	 * @param confidence
	 * @return status code representing success of classificator generation
	 * If classificator is built successfully returns 0, else it returns 1. Implements CbaCb part of CBA algorithm
	 */
	public int generateClassificator(String userName, String targetClass, double support, double confidence){
		//generate rules
		CbaRgMDB cba = new CbaRgMDB(support, confidence);
		List<RuleItem> ruleSet = cba.getRulesSet(userName, targetClass);
		//empty classifier
		List<RuleItem> rulesC = new ArrayList<RuleItem>();
		int returnValue = 0;
		ruleSet = sortR(ruleSet);
		List<ObjectId> d;
		List<ObjectId> dataSetIds = new ArrayList<ObjectId>();
		boolean marked;
		String tempKey = "";
		String tempValue = "";
		String defaultClass = "";
		String tempClassValue = "";
		boolean condSetFound = true;
		RuleItem r;
		int minErrorIndex = 0;
		int minError = Integer.MAX_VALUE;
		int tempError = 0;
		int correct = 0;
		int defaultCT = 0;
		int defaultCF = 0;
		
		try {
			openConnection();
			
			//clean database of old rules
			ds.delete(ds.createQuery(RuleItem.class).filter("userName =", userName).filter("classKey =", targetClass));
			
			Query<ProcessedEntry> query = ds.createQuery(ProcessedEntry.class).filter("userName =", userName);
			
			//store ProcessedEntry ids from database
			for(ProcessedEntry z : query){
				dataSetIds.add(z.getId());
			}
			
			//if there are rules found
			if(!ruleSet.isEmpty()){
				//pass over all rules generated in rule generation part of CBA
				for(RuleItem x : ruleSet){
					
					d = new ArrayList<ObjectId>();
					marked = false;
					
					//pass over all entries in database
					for(ProcessedEntry y : query){
						
						if(!dataSetIds.contains(y.getId())){
							continue;
						}
						
						//find if rule condition set is subset of case
						for(Map.Entry<String, String> c : x.getConditions().getConditions().entrySet()){
	
							int i;
							for(i=0; i<y.getKeys().length; i++){
								tempKey = y.getKeys()[i];
								tempValue = String.valueOf(y.getValue(tempKey));
								if(tempKey.equals(c.getKey()) && tempValue.equals(c.getValue())){
									break;
								}
							}
	
							if(i == y.getKeys().length){
								condSetFound = false;
								break;
							}
						}
						
						//if rule covers case than check if classification is correct. If true mark rule
						if(condSetFound){
							d.add(y.getId());
							
							if(targetClass.equals("speedDifference")){
								if(x.getClassValue().equals(String.valueOf(y.getSpeedDifference()))){
									marked = true;
								}
							}
							else{
								if(x.getClassValue().equals(String.valueOf(y.getConsumption()))){
									marked = true;
								}
							}
	
						}
						else{
							//System.out.println("not found " + x.getConditions().getConditions().entrySet().toString() + " -> " + x.getClassValue());
						}
						
						condSetFound = true;
					}
					
					//if rule classified case correctly
					if(marked){
						System.out.println(x.getConditions().getConditions().toString() + " -> " + x.getClassValue() + " "
												+ x.getRuleSupCount() + "/" + x.getConditions().getCondSupCount());
						
						//insert rule at the end of classifier
						rulesC.add(x);
						
						//remove rule id from rule ids so it will not be used in future
						for(ObjectId s : d){
							dataSetIds.remove(s);
						}
						
						//call method that select default class
						defaultClass = selectDefaultClass(dataSetIds, rulesC, userName, targetClass);
						
						System.out.println("default class " + defaultClass);
						
						//pass over all entries in database
						for(ProcessedEntry y : query){
							
							if(targetClass.equals("speedDifference")){
								tempClassValue = String.valueOf(y.getSpeedDifference());
							}
							else{
								tempClassValue = String.valueOf(y.getConsumption());
							}
							
							//pass over all rules
							int j;
							for(j=0; j<rulesC.size(); j++){
								r = rulesC.get(j);
								
								//check if rule covers entry case
								for(Map.Entry<String, String> c : r.getConditions().getConditions().entrySet()){
									int i;
									for(i=0; i<y.getKeys().length; i++){
										tempKey = y.getKeys()[i];
										tempValue = String.valueOf(y.getValue(tempKey));
										if(tempKey.equals(c.getKey()) && tempValue.equals(c.getValue())){
											break;
										}
									}
									if(i == y.getKeys().length){
										condSetFound = false;
										break;
									}
								}
								
								//if rule covers entry case, check if it is classified correctly
								if(condSetFound){
									if(tempClassValue.equals(r.getClassValue())){
										correct++;
									}
									else{
										tempError++;
									}
									break;
								}
								condSetFound = true;
	
							}
	
							//if case is not covered, try default class
							if(j == rulesC.size()){
								if(tempClassValue.equals(defaultClass)){
									defaultCT++;
								}
								else{
									defaultCF++;
								}
							}
	
						}
						
						//get index in classificator rule set at which number of errors is minimal. Prune at found index
						if(minError > (tempError + defaultCF)){
							minErrorIndex = rulesC.size();
							minError = tempError + defaultCF;
							
						}
						System.out.print(tempError + " " + correct + " " + defaultCF + " " + defaultCT);
						System.out.println(" --> " + (tempError + defaultCF) + " + " + (correct + defaultCT) + " = " + (tempError + defaultCF + correct + defaultCT));
						tempError = 0;
						correct = 0;
						defaultCT = 0;
						defaultCF = 0;
						
					}
				}
			}
			else{
				defaultClass = selectDefaultClass(dataSetIds, rulesC, userName, targetClass);
			}
			
			System.out.println("index " + minErrorIndex);
			
			for(int i=0; i<minErrorIndex; i++){
				ds.save(rulesC.get(i));
			}
			
			//add default class: {} --> defaultClassValue
			RuleItem def = new RuleItem();
			def.setClassKey(targetClass);
			def.setClassValue(defaultClass);
			def.setConditions(new CondSet());
			def.setUserName(userName);
			
			ds.save(def);
			
			close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			returnValue = 1;
		}
		
		System.out.println("returnValue2 " + returnValue);
		return returnValue;
	}
	
	/**
	 * @param dataSetIds
	 * @param rulesC
	 * @param userName
	 * @param targetClass
	 * @return returns default class according to rules and data
	 * Default class is the most common class that is left in the remaining data cases
	 */
	private String selectDefaultClass(List<ObjectId> dataSetIds, List<RuleItem> rulesC, String userName, String targetClass){
		String value;
		if(targetClass.equals("speedDifference")){
			value = "0.0";
		}
		else{
			value = "6.5";
		}

		int maxCount = 0;
		
		Map<String, Integer> classes = new HashMap<String, Integer>();
		
		ProcessedEntry entry;
		
		for(ObjectId id : dataSetIds){
			entry = ds.createQuery(ProcessedEntry.class).filter("id =", id).get();
			if(targetClass.equals("speedDifference")){
				value = String.valueOf(entry.getSpeedDifference());
			}
			else{
				value = String.valueOf(entry.getConsumption());
			}
			
			if(classes.containsKey(value)){
				classes.put(value, classes.get(value) + 1);
			}
			else{
				classes.put(value, 1);
			}
		}
		
		for(Map.Entry<String, Integer> c : classes.entrySet()){
			if(c.getValue() > maxCount){
				value = c.getKey();
				maxCount = c.getValue();
			}
		}
		
		return value;
	}
	
	/**
	 * @param ruleSet
	 * @return sorted rules list according to ObjectComparator
	 */
	private List<RuleItem> sortR(List<RuleItem> ruleSet){
		Collections.sort(ruleSet, new ObjectComparator());
		return ruleSet;
	}
	
	/**
	 * @author Rok Pajk Kosec
	 * Implementation of cusstom comparator. First criterion is condition set size of rule,
	 * second is conficence and third is support
	 */
	public class ObjectComparator implements Comparator<RuleItem>{

		@Override
		public int compare(RuleItem o1, RuleItem o2) {
	       
		   if(o1.getConditions().getConditions().size() > o2.getConditions().getConditions().size()){
			   return -1;
		   }
		   else if(o1.getConditions().getConditions().size() == o2.getConditions().getConditions().size()){
		       if((o1.getRuleSupCount() / o1.getConditions().getCondSupCount()) > (o2.getRuleSupCount() / o2.getConditions().getCondSupCount())){
		    	   return -1;
		       }
		       else if((o1.getRuleSupCount() / o1.getConditions().getCondSupCount()) == (o2.getRuleSupCount() / o2.getConditions().getCondSupCount())){
		    	   if(o1.getConditions().getCondSupCount() < o2.getConditions().getCondSupCount()){
			    	   return -1;
			       }
		    	   else if(o1.getConditions().getCondSupCount() == o2.getConditions().getCondSupCount()){
		    		   return 0;
		    	   }
		    	   else{
		    		   return 1;
		    	   }
		       }
		       else{
		    	   return 1;
		       }
		   }
		   else{
			   return 1;
		   }
		}
	}
	
	/**
	 * @throws UnknownHostException
	 * Opens database connection
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
