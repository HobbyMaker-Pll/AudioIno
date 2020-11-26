#include <TimerOne.h>

#define pinQuantity 12
int pins[pinQuantity];

bool lState[pinQuantity];

String Message = "";
bool isConnected = false ;
bool isConfigured = false ;
bool isPlaying = false;
int playingPin = -1;


void setup() {
  Serial.begin(9600);
  Serial.setTimeout(5);
  resetPins();
}

void loop() {
  if (isConnected && isConfigured) {
    for (int i = 0; i < pinQuantity; i++) {
      if (pins[i] != -1) {

        bool State = digitalRead(pins[i]);

        if (State != lState[i]) { // CHANGE OF LAST STATE
          lState[i] = State;

          if (!isPlaying) { // IF THERE ISN'T AUDIOS PLAYING
            isPlaying = true;
            
            if (playingPin != pins[i] ) { // CHECK IF THE THE PRESSED PIN WAS LAST AUDIO PIN TO RESUME TRACK
              playingPin = pins[i]; // SAVE THE AUDIO PIN PLAYING
              String play = "PL" + String(pins[i]);
              Serial.println(play);
              
            } else { // RESUME IF WAS THE SAME PIN
              Serial.println("RESUME");
            }

          } else if ( (playingPin != -1) && (playingPin == pins[i]) ) { // IF AUDIO PIN IS PRESSED AGAIN STOP THE AUDIO
            isPlaying = false;
            Serial.println("STOP");
          }

          delay(100); // DELAY TO SOLVE BUTTON ISSUE
          
        } // CHANGE STATE FINISH

      } else {
        break;
      }
    }
  }

}

void serialEvent() {
  while (Serial.available()) {
    Message = Serial.readString();
  }

  if (!(Serial.available()) && (Message != "")) {

/*
 * END AND START OF COMMUNICATION WITH JAVA
 */
    if (Message == "START") {
      Timer1.initialize(1000000);
      Timer1.attachInterrupt(VerifyCon);
      isConnected = true;
    }

    if (Message == "END") {
      Timer1.detachInterrupt();
      isConnected = false;
    }
/*
 * AUDIO PIN'S CONFIGURATIONS
 */
    if (Message == "CONFIG") {
      isConfigured = false;
      resetPins();
    }
    
    if (Message.indexOf("DI") >= 0) {
      int pin = (Message.substring(2)).toInt();
      for (int i = 0; i < pinQuantity; i++) {
        if (pins[i] == -1) {
          pins[i] = pin;
          break;
        }
      }
    }

    if (Message == "SENT") {
      for (int i = 0; i < pinQuantity; i++) {
        if (pins[i] != -1) {
          pinMode(pins[i], INPUT_PULLUP);
          lState[i] = digitalRead(pins[i]);
        }
      }
      isConfigured = true;
      Serial.println("LOAD");
    }

/*
 * COMMUNICATION ABOUT AUDIO PLAYBACK
 */
    if (Message.indexOf("FORCE") >= 0){
      playingPin = (Message.substring(5)).toInt();
      Serial.println(playingPin);
      isPlaying = true;
      
    }

    if (Message == "PAUSE"){
      isPlaying = false;
    }

    if (Message == "RESUME"){
      isPlaying = true;
    }

    if (Message == "FINISH"){
      isPlaying = false;
      playingPin = -1;
    }
/*   
 *    FOR TEST PURPOSES
 */
    if (Message == "TESTE") {
      for (int i = 0; i < pinQuantity; i++) {
        Serial.println(pins[i]);
      }
    }

    Message = "";
  }
}

void VerifyCon() {
  Serial.println("OK");
}

void resetPins() {
  for (int i = 0; i < pinQuantity; i++) {
    pins[i] = -1;
  }
}
