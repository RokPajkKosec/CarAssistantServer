package com.cosso.CBA;

/**
 * @author Rok Pajk Kosec
 * Class used for easier use of Classificator and DataPreprocessingMDB classes
 */
public class CBAControl {
	Classificator classificator;
	DataPreprocessingMDB dp;
	
	public static void main(String[] args) {
		CBAControl cc = new CBAControl();
		cc.prepareData("rok", "speedDifference", 0.1, 0.3);
		System.out.println(cc.classify(45.0, 223, 243, 23.0, 160.2, 90, 0.0, 20, (byte)0, "Roadworks", "primary", "Rain", "rok", "speedDifference"));
	}
	
	/**
	 * Constructor
	 */
	public CBAControl(){
		classificator = new Classificator();
		dp = new DataPreprocessingMDB();
	}
	
	/**
	 * @param userName
	 * @param targetClass
	 * @param support
	 * @param confidence
	 * @return response code describing success of rules calculation
	 * Prepares data in a form of processedEntry entries in database and than calculates association rules using Classificator
	 * and DataPreprocessingMDB classes
	 */
	public int prepareData(String userName, String targetClass, double support, double confidence){
		int returnValue = 0;
		returnValue += dp.process(userName);
		System.out.println("control1 " + returnValue);
		returnValue += classificator.generateClassificator(userName, targetClass, support, confidence);
		System.out.println("control2 " + returnValue);
		if(returnValue == 0){
			System.out.println("control3 " + returnValue);
			return 200;
		}
		else{
			System.out.println("control4 " + returnValue);
			return 500;
		}
	}
	
	/**
	 * @param windSpeed
	 * @param windDirection
	 * @param heading
	 * @param temperature
	 * @param speed
	 * @param speedLimit
	 * @param consumption
	 * @param hour
	 * @param noPass
	 * @param obstacleType
	 * @param roadType
	 * @param weather
	 * @param userName
	 * @param targetClass
	 * @return target value classified using CBA based on provided attributes
	 */
	public double classify(double windSpeed, int windDirection, int heading, double temperature, double speed, 
			int speedLimit, double consumption, int hour, byte noPass, String obstacleType, String roadType, String weather, 
			String userName, String targetClass){
		return classificator.classify(dp.processOne(windSpeed, windDirection, heading, temperature, speed, speedLimit, consumption,
				hour, (byte)noPass, obstacleType, roadType, weather, userName), targetClass);

	}

}
