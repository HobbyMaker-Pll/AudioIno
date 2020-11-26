import javax.sound.sampled.*;
import java.io.*;

public class SoundPlayer implements LineListener {
	
	private Clip clips;
	private AudioInputStream ais;
	private boolean audioFinished = false;
	
	SoundPlayer() {
		try {
			clips = AudioSystem.getClip();
			clips.addLineListener(this);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	
	public void SetAudio(String FilePath) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
		
		if (clips.isOpen()) clips.close();
		ais = AudioSystem.getAudioInputStream(new File(FilePath));
		clips.open(ais);
		clips.start();
		
	}
	
	public void StopAudio() {
		
		if (clips.isOpen()) {
			clips.stop();
		}
				
	}
	
	public void ResumeAudio() {
		if (clips.isOpen()) {
			clips.start();
		}
	}
	
	public String getClipDuration() {
		 long miliseonds = clips.getMicrosecondLength() / 1000;
		 long seconds = miliseonds/1000;
		 int minutes = (int) (seconds/60);
		 int realseconds = (int) (seconds % 60);
		 
		 String time = minutes + "m " + realseconds + "s";
		 
		 return time;
	}
	

	@Override
	public void update(LineEvent e) {
		if (e.getType() == LineEvent.Type.STOP) {
			if (clips.available() > 0) audioFinished = true;
		}
	}
	
	public boolean isFinished() {
		return audioFinished;
	}
	
	public void resetIsFinish() {
		audioFinished = false;
	}
}
