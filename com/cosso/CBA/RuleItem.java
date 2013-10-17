package com.cosso.CBA;
import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

/**
 * @author Rok Pajk Kosec
 * Class for saving rule items into MongoDB database
 */
@Entity(value="ruleItem", noClassnameStored=true)
public class RuleItem {
	@Id private ObjectId id;
	
	//condition set
	private CondSet conditions;
	//rule support
	private int ruleSupCount;
	//class key (speed difference or consumption)
	private String classKey;
	//class value
	private String classValue;
	//number of entries in database
	private int dataSize;
	//username
	private String userName;
	
	/**
	 * Constructor
	 */
	public RuleItem() {
		conditions = new CondSet();
		ruleSupCount = 0;
		classKey = "";
		classValue = "";
		setDataSize(0);
		userName = "";
	}
	
	/**
	 * @param key
	 * @param value
	 * @param cond
	 * @param dataSize
	 * @param user
	 * Constructor that sets provided attributes
	 */
	public RuleItem(String key, String value, CondSet cond, int dataSize, String user){
		conditions = cond;
		ruleSupCount = 1;
		classKey = key;
		classValue = value;
		this.setDataSize(dataSize);
		userName = user;
	}
	
	/**
	 * @param rule
	 * @param k
	 * @return merged rule item
	 * Merges this and attribute rule items into one. k is size of condition set
	 * and is used to check if two conditions sets can be merged based on first k-2 entries
	 * which must be the same
	 */
	public RuleItem Merge(RuleItem rule, int k){
		RuleItem newRule = new RuleItem();
		
		//1) preveri isti class
		//2) nastavi class
		//3) merge conditions
		//4) return rule
		
		if(classKey.equals(rule.getClassKey()) && classValue.equals(rule.getClassValue())){
			newRule.setClassKey(classKey);
			newRule.setClassValue(classValue);
			newRule.setDataSize(dataSize);
			newRule.setUserName(userName);
			CondSet newCondSet = conditions.Merge(rule.getConditions(), k);
			if(newCondSet != null){
				newRule.setConditions(newCondSet);
			}
			else{
				return null;
			}
		}
		else{
			return null;
		}
		return newRule;
	}

	public CondSet getConditions() {
		return conditions;
	}

	public void setConditions(CondSet conditions) {
		this.conditions = conditions;
	}

	public int getRuleSupCount() {
		return ruleSupCount;
	}

	public void setRuleSupCount(int ruleSupCount) {
		this.ruleSupCount = ruleSupCount;
	}

	public String getClassKey() {
		return classKey;
	}

	public void setClassKey(String classKey) {
		this.classKey = classKey;
	}

	public String getClassValue() {
		return classValue;
	}

	public void setClassValue(String classValue) {
		this.classValue = classValue;
	}

	public int getDataSize() {
		return dataSize;
	}

	public void setDataSize(int dataSize) {
		this.dataSize = dataSize;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
