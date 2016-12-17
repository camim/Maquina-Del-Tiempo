#include <AFMotor.h>
#include <SoftwareSerial.h>   // Incluimos la librería  SoftwareSerial  
SoftwareSerial BT(51,53);

const int BUTTON_SPIN_MOTOR = 36;
const int BUTTON_REVERSE_SPIN_MOTOR = 40;
AF_DCMotor Motor1(1);
int buttonSpinState = 0;
int buttoReverseSpinState = 0;

const int PWMPIN = 31;
const int POTENTIOMETER_PIN = A10;
int potentiometerValue = 0;
int T;

int state = 0;
int flag = 0;        // make sure that you return the state only once
const int BUTTON_TAKE_PHOTO = 38;
int buttonTakePicPhotoState = 0;

void setup() {
  pinMode(BUTTON_SPIN_MOTOR, INPUT);
  pinMode(BUTTON_REVERSE_SPIN_MOTOR, INPUT);
  digitalWrite(BUTTON_SPIN_MOTOR, HIGH);
  digitalWrite(BUTTON_REVERSE_SPIN_MOTOR, HIGH);
  pinMode(PWMPIN, OUTPUT);
  pinMode(POTENTIOMETER_PIN, INPUT);
  Motor1.run(RELEASE);
  BT.begin(9600);
  pinMode(BUTTON_TAKE_PHOTO, INPUT);

}

void loop() {

  buttonTakePicPhotoState = digitalRead(BUTTON_TAKE_PHOTO);
   if(buttonTakePicPhotoState == HIGH) {
    BT.println("Take");
   }

  buttonSpinState = digitalRead(BUTTON_SPIN_MOTOR);
  buttoReverseSpinState = digitalRead(BUTTON_REVERSE_SPIN_MOTOR);
  potentiometerValue = analogRead(POTENTIOMETER_PIN) / 32;
  T++;
  if(T < potentiometerValue) {
    digitalWrite(PWMPIN, HIGH);
 } else {
    digitalWrite(PWMPIN, LOW);
 }
 if(T > 50)
    T = 0;
  
  
   if (buttonSpinState == HIGH) {
    Motor1.run(BACKWARD);
    Motor1.setSpeed(70);
   } else {
     if (buttoReverseSpinState == HIGH) {
      Motor1.run(FORWARD);
      Motor1.setSpeed(70);
     } else {
    Motor1.setSpeed(0);
    }
   }
    
   
   if ((buttonSpinState == HIGH) && (buttoReverseSpinState == HIGH)) {
     Motor1.setSpeed(0);
   }
}
