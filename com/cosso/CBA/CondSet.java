package com.cosso.CBA;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

/**
 * @author Rok Pajk Kosec
 * Class that represents condition set (left side) of a rule
 */
@Entity(value="condSet", noClassnameStored=true)
public class CondSet {
	@Id private ObjectId id;
	
	//HashMap of conditions
	private Map<String, String> conditions;
	//Support count
	private int condSupCount;
	
	/**
	 * Constructor
	 */
	CondSet(){
		conditions = new LinkedHashMap<String, String>();
		condSupCount = 0;
	}
	
	/**
	 * @param key
	 * @param value
	 * Constructor that adds first item into hash map
	 */
	CondSet(String key, String value){
		conditions = new LinkedHashMap<String, String>();
		conditions.put(key, value);
		condSupCount = 1;
	}
	
	/**
	 * @param cond
	 * @param k
	 * @return merged condition set
	 * Merges this and attribute condition set into one. k is size of condition set
	 * and is used to check if two sets can be merged based on first k-2 entries
	 * which must be the same
	 */
	public CondSet Merge(CondSet cond, int k){
		CondSet newSet = new CondSet();
		this.Sort();
		cond.Sort();
		int kCounter = 0;
		
		Iterator<Entry<String, String>> i = conditions.entrySet().iterator();
		Iterator<Entry<String, String>> j = cond.getConditions().entrySet().iterator();
		
		while(i.hasNext() && j.hasNext()){
			Entry<String, String> entry1 = i.next();
			Entry<String, String> entry2 = j.next();
			
			if(kCounter < k-2){
				if(!entry1.equals(entry2)){
					return null;
				}
				else{
					newSet.addCondition(entry1.getKey(), entry1.getValue());
				}
			}
			else{
				if(entry1.getKey().equals(entry2.getKey())){
					return null;
				}
				else{
					newSet.addCondition(entry1.getKey(), entry1.getValue());
					newSet.addCondition(entry2.getKey(), entry2.getValue());
				}
			}
			kCounter++;
		}
		return newSet;
	}
	
	/**
	 * Alphabetically sorts keys in conditions set
	 */
	public void Sort(){
		conditions = new LinkedHashMap<String, String>(new TreeMap<String, String>(conditions));
	}
	
	public Map<String, String> getConditions() {
		return conditions;
	}
	
	public void addCondition(String key, String value) {
		this.conditions.put(key, value);
	}

	public int getCondSupCount() {
		return condSupCount;
	}

	public void setCondSupCount(int condSupCount) {
		this.condSupCount = condSupCount;
	}
	
	public static void main(String[] args){
		CondSet set1 = new CondSet();
		set1.addCondition("a", "1");
		//set1.addCondition("b", "5");
		set1.addCondition("c", "3");
		
		System.out.println(set1.getConditions().toString());
		
		CondSet set2 = new CondSet();
		set2.addCondition("a", "1");
		set2.addCondition("b", "2");
		//set2.addCondition("d", "4");
		
		System.out.println(set2.getConditions().toString());
		
		CondSet merged = set1.Merge(set2, 3);
		System.out.println(merged.getConditions().toString());
	}
}
