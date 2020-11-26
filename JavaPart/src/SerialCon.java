import java.util.ArrayList;

import com.fazecast.jSerialComm.*;

public class SerialCon implements SerialPortDataListener{
	private SerialPort serialPort;
	private final byte[] buffer = new byte[1024];
	
	private boolean timeout = true;
	private boolean isLoaded = false;
	private boolean pauseTrack = false;
	private boolean resumeTrack = false ;
	
	public String PlayPin = "";
	
	private String data;

	public void closePort() throws Exception {

		if (serialPort != null) {
			serialPort.removeDataListener();
			serialPort.closePort();
		}
	}

	public boolean openPort() throws Exception {
		
		if (serialPort == null) {
			throw new Exception("The connection wasn't initialized");
		}

		return serialPort.openPort();
	}

	public void initSerialPort(String name, Integer baud) throws Exception {
		
		if (serialPort != null && serialPort.isOpen()) {
			closePort();

		}
		serialPort = SerialPort.getCommPort(name);
		serialPort.setParity(SerialPort.NO_PARITY);
		serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
		serialPort.setNumDataBits(8);
		serialPort.addDataListener(this);
		serialPort.setBaudRate(baud);
	}

	@Override
	public int getListeningEvents() {
		return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
	}

	@Override
	public void serialEvent(com.fazecast.jSerialComm.SerialPortEvent event) {
		if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
			return;
		}
		
		int bytesAvailable = serialPort.bytesAvailable();
		if (bytesAvailable <= 0) {
			return;
		}
		
		int bytesRead = serialPort.readBytes(buffer, Math.min(buffer.length, bytesAvailable));
		String response = new String(buffer, 0, bytesRead);
		handleResponse(response);

	}

	public void handleResponse(String response) {

		data += response;
		
		if (!data.contains("\n")) return;
		
		String result = data.replaceAll("\\r\\n", "");
		if (result.equals("OK")) timeout = true;
		if (result.equals("LOAD")) isLoaded = true;
		if (result.contains("PL")) PlayPin = result.substring(2);
		if (result.equals("STOP")) pauseTrack = true;
		if (result.equals("RESUME")) resumeTrack = true;		
		
		data = "";

	}
	
	public boolean getTimeOut() {
		return timeout;
	}
	
	public void setTimeout(boolean state) {
		timeout = state;
	}
	
	public boolean isLoaded() {
		return isLoaded;
	}
	
	public void resetLoaded() {
		isLoaded = false;
	}
	
	public String getPlayPin() {
		return PlayPin;
	}
	
	public void resetPlaypin() {
		PlayPin = "";
	}
	
	public boolean resumeRequest() {
		return resumeTrack;
	}
	
	public void ResetresumeRequest() {
		resumeTrack = false;
	}
	
	public boolean pauseRequest() {
		return pauseTrack;
	}
	
	public void ResetpauseRequest() {
		pauseTrack = false;
	}

	public ArrayList<String> refreshPorts() {
		
		ArrayList<String> ports = new ArrayList<String>();
		
		for (SerialPort name : SerialPort.getCommPorts()) {
			ports.add(name.getSystemPortName());
		}
		
		return ports;
	}
	
	public void sendMessage(String message) {
		byte[] Messagebyte = message.getBytes();
		serialPort.writeBytes(Messagebyte, Messagebyte.length);
	}

}
