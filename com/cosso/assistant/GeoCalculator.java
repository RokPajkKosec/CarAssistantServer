package com.cosso.assistant;

/**
 * @author Rok Pajk Kosec
 * Class used for calculation geospacial values (distance, heading, etc.)
 */
public class GeoCalculator {
    private static final double R = 6372800; // Average Earth radius in meters
    private static final double equatorialRadius = 6378137; // equatorial radius
    private static final double polarRadius = 6356752.314; // polar radius
    //private static final double flattening = 0.00335281066474748;// (equatorialRadius-polarRadius)/equatorialRadius; // flattening
   // private static final double inverseFlattening = 298.257223563;// 1/flattening;
    private static final double e = Math.sqrt(1 - Math.pow(polarRadius / equatorialRadius, 2)); // eccentricity
    private static final double k0 = 0.9996; // scale factor
    private static final double e1sq = e * e / (1 - e * e);
    private static double rho = 6368573.744; // r curv 1
    private static double nu = 6389236.914; // r curv 2
    
    // Calculation Constants
    // Delta Long
    private static double p = -0.483084;
    private static double sin1 = 4.84814E-06;
    // Coefficients for UTM Coordinates
    private static double K1 = 5101225.115;
    private static double K2 = 3750.291596;
    private static double K3 = 1.397608151;
    private static double K4 = 214839.3105;
    private static double K5 = -2.995382942;
    private static double A6 = -1.00541E-07;
    
    // Calculate Meridional Arc Length
    // Meridional Arc
    private static double S = 5103266.421;
    private static double A0 = 6367449.146;
    private static double B0 = 16038.42955;
    private static double C0 = 16.83261333;
    private static double D0 = 0.021984404;
    private static double E0 = 0.000312705;
	
    public static double initialBearing(double lat1, double long1, double lat2, double long2){
        return (bearing(lat1, long1, lat2, long2) + 360.0) % 360;
    }

    public static double finalBearing(double lat1, double long1, double lat2, double long2){
        return (bearing(lat2, long2, lat1, long1) + 180.0) % 360;
    }

    private static double bearing(double lat1, double long1, double lat2, double long2){
        double degToRad = Math.PI / 180.0;
        double phi1 = lat1 * degToRad;
        double phi2 = lat2 * degToRad;
        double lam1 = long1 * degToRad;
        double lam2 = long2 * degToRad;

        return Math.atan2(Math.sin(lam2-lam1)*Math.cos(phi2),
            Math.cos(phi1)*Math.sin(phi2) - Math.sin(phi1)*Math.cos(phi2)*Math.cos(lam2-lam1)
        ) * 180/Math.PI;
    }
    

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
 
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }
    
    public static double[] convertLatLonToUTM(double latitude, double longitude){
      double[] UTM = new double[2];

      setVariables(latitude, longitude);

      double easting = getEasting();
      double northing = getNorthing(latitude);

      UTM[0] = northing;
      UTM[1] = easting;

      return UTM;
    }

    private static void setVariables(double latitude, double longitude){
      latitude = Math.toRadians(latitude);
      rho = equatorialRadius * (1 - e * e) / Math.pow(1 - Math.pow(e * Math.sin(latitude), 2), 3 / 2.0);
      nu = equatorialRadius / Math.pow(1 - Math.pow(e * Math.sin(latitude), 2), (1 / 2.0));

      double var1;
      if (longitude < 0.0){
        var1 = ((int) ((180 + longitude) / 6.0)) + 1;
      }
      else{
        var1 = ((int) (longitude / 6)) + 31;
      }
      double var2 = (6 * var1) - 183;
      double var3 = longitude - var2;
      p = var3 * 3600 / 10000;

      S = A0 * latitude - B0 * Math.sin(2 * latitude) + C0 * Math.sin(4 * latitude) - D0
          * Math.sin(6 * latitude) + E0 * Math.sin(8 * latitude);

      K1 = S * k0;
      K2 = nu * Math.sin(latitude) * Math.cos(latitude) * Math.pow(sin1, 2) * k0 * (100000000)
          / 2;
      K3 = ((Math.pow(sin1, 4) * nu * Math.sin(latitude) * Math.pow(Math.cos(latitude), 3)) / 24)
          * (5 - Math.pow(Math.tan(latitude), 2) + 9 * e1sq * Math.pow(Math.cos(latitude), 2) + 4
              * Math.pow(e1sq, 2) * Math.pow(Math.cos(latitude), 4))
          * k0
          * (10000000000000000L);

      K4 = nu * Math.cos(latitude) * sin1 * k0 * 10000;

      K5 = Math.pow(sin1 * Math.cos(latitude), 3) * (nu / 6)
          * (1 - Math.pow(Math.tan(latitude), 2) + e1sq * Math.pow(Math.cos(latitude), 2)) * k0
          * 1000000000000L;

      A6 = (Math.pow(p * sin1, 6) * nu * Math.sin(latitude) * Math.pow(Math.cos(latitude), 5) / 720)
          * (61 - 58 * Math.pow(Math.tan(latitude), 2) + Math.pow(Math.tan(latitude), 4) + 270
              * e1sq * Math.pow(Math.cos(latitude), 2) - 330 * e1sq
              * Math.pow(Math.sin(latitude), 2)) * k0 * (1E+24);

    }

    private String getLongZone(double longitude){
      double longZone = 0;
      if (longitude < 0.0){
        longZone = ((180.0 + longitude) / 6) + 1;
      }
      else{
        longZone = (longitude / 6) + 31;
      }
      String val = String.valueOf((int) longZone);
      if (val.length() == 1){
        val = "0" + val;
      }
      return val;
    }

    private static double getNorthing(double latitude){
      double northing = K1 + K2 * p * p + K3 * Math.pow(p, 4);
      if (latitude < 0.0){
        northing = 10000000 + northing;
      }
      return northing;
    }

    private static double getEasting(){
      return 500000 + (K4 * p + K5 * Math.pow(p, 3));
    }
    
}
