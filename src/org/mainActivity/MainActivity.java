package org.mainActivity;
/**
 * Class Name:MainActivity
 * @author swapna
 *
 */

public class MainActivity {

	protected int iInterval;
	protected String strUrl;
	protected String strPublicKey;
	protected String strDeviceID;
	double fMinBatPercentage;
	
	/**
	 * Constructor Name: The object is initialised when the application boots
	 */
	public MainActivity(){
		
		iInterval=getMonitoringRange();
		strUrl=getRhizomeUrl();
		strPublicKey=getRecipientPublicKey();
		strDeviceID=getDeviceID();
		fMinBatPercentage=getMinBatLevl();	
		
		
	}
	
	/**
	 * Function Name: fnMonitorAmtr
	 * Description: To Monitor the Accelerometer Sensor Reading 
	 * Input argument: Interval
	 * The number which ranges between 1 to 3600, 
	 * at which interval the readings are actually to be monitored
	 * Return Value: The readings of the Accelerometer.
	 */
	public int[] fnMonitorAmtr(int iInterval){
		int[] arAmReading={};
		
		return arAmReading;
	}
	
	/**
	 * Function Name:getMonitoringRange()
	 * Description: To get The Interval to Monitor the Sensor Reading
	 */
	public int getMonitoringRange()
	{
		int iInterval=0;
		return iInterval;
	}
	
	/**
	 * Function Name:getRhizomeUrl
	 * Description: To get the Rhizome Url(http://), 
	 * which used to upload the data on to the server
	 * @return type: String
	 * Value: Rhizome Url
	 */
	public String getRhizomeUrl()
	
	{
		String strUrl="";
		return strUrl;
	}
	/**
	 * Function Name:getRecipientPublicKey
	 * Description: Gets the Configurable 
	 * Recipient Public Key, which is used for decrypting the logged data
	 * which will be encrypted before transmitting through the mesh
	 * Note: To be developed in the Phase 2  
	 * @return type String
	 * Value: Public Key
	 */
	
	public String getRecipientPublicKey()
	{
		String strPublicKey="";
		return strPublicKey;
	}
	/**
	 * Function Name:fnCalculateDeviceID
	 * Description: To generate a random unique alphanumeric value of length 32 character,
	 * used as a deviceId for each device on which the software is running.
	 * This way we determine the origin of the logged data
	 * 
	 * @return type: String
	 * This will be calculated Device Idebtifier
	 */
	public String fnCalculateDeviceID()
	{
		String strDeviceId="";
		return strDeviceId;
	}
	/**
	 * Function Name:getServalSID()
	 * Description: Get the value of the local Serval Instance(SID),
	 * @return type: String
	 * Value:SID
	 */
	
	public String getServalSId()
	{
		String strServalSID="";
		return strServalSID;
	}
	
	/**
	 * Function Name:getDeviceId
	 * Description: Get the randomly generated unique device Id,
	 *If empty then gets the SID of the local Serval Instance
	 * @return type:String
	 * Value:DeviceID
	 */
	public String getDeviceID()
	{
		String strDeviceID="";
		strDeviceID=fnCalculateDeviceID();
		if((strDeviceID!=null) &&(strDeviceID.length()!=0)){
			
			return strDeviceID;
		}
		else
		{
			strDeviceID=getServalSId();
			return strDeviceID;
			
		}
		
	}
	/**
	 * Function Name:getMinBatLevl
	 * Description: Get The Minimum Battery Level below which the
	 * software is shutdown/stops collecting data.
	 * @return type: double
	 * Value is supposed to be in terms of percentage, Minimum battery level
	 */
	public double getMinBatLevl()
	{
       double fMinBatPercentage=0.00;
       return fMinBatPercentage;
	}
}
