#define MH3362_Pin A1
#define PH_SENSOR_PIN A0
//#define DO_SENSOR_PIN A2
#include <LiquidCrystal_I2C.h>
#include <SoftwareSerial.h>

SoftwareSerial B(10, 11); // tX, rX pins for Bluetooth module

float ph_lower_threshold = 6.0; // Changed threshold for pump on condition
//float ph_upper_threshold = 8.0; // Changed threshold for pump on condition

// pH sensor calibration values
float calibration_value_ph = 22.00 + 5.5;
float calibration_offset_ph = 0.0;

// DO sensor calibration values
/*float calibration_value_do = 1.7;
float calibration_offset_do = 0.0;*/

// MH sensor calibration values
float calibration_value_mh = 140;
float calibration_offset_mh = 0.0;
LiquidCrystal_I2C lcd(0x27, 16, 2);

// Relay control pin
int pump_relay_pin = 7;

void setup() {
  Serial.begin(9600);
  B.begin(9600); // Initialize Bluetooth serial communication
  lcd.begin(16, 2);    // Initialize LCD
  lcd.setBacklight(HIGH);
  pinMode(pump_relay_pin, OUTPUT); // Set relay pin as output
  // Optional: Calibrate other sensors if needed
  // Example: calibration_value_do = 100.0;
}

void loop() {
  // pH sensor reading
  int buffer_ph[10], temp;
  unsigned long int avg_ph = 0;

  for (int i = 0; i < 10; i++) {
    buffer_ph[i] = analogRead(PH_SENSOR_PIN);
    delay(3000);
  }

  for (int i = 0; i < 9; i++) {
    for (int j = i + 1; j < 10; j++) {
      if (buffer_ph[i] > buffer_ph[j]) {
        temp = buffer_ph[i];
        buffer_ph[i] = buffer_ph[j];
        buffer_ph[j] = temp;
      }
    }
  }

  for (int i = 2; i < 8; i++) {
    avg_ph += buffer_ph[i];
  }

  float volt_ph = (float)avg_ph * 5.0 / 1024 / 6;
  float ph_act = -5.70 * volt_ph + calibration_value_ph + calibration_offset_ph;

  // DO sensor reading
  //int do_value = analogRead(DO_SENSOR_PIN);
  //float do_act = (float)do_value * (5.0 / 1024) * calibration_value_do + calibration_offset_do;

  // MH sensor reading
  int mh_value = analogRead(MH3362_Pin);
  float mh_act = (float)mh_value * (5.0 / 1024) * calibration_value_mh + calibration_offset_mh;
  
  // Pump control based on pH value
  if (ph_act < ph_lower_threshold ) {
    Serial.println("ALERT! pH Out of Range");
    digitalWrite(pump_relay_pin, LOW); // Turn on pump relay
    // Add any additional actions or alerts here
  } else {
    Serial.println("pH within Range");
    digitalWrite(pump_relay_pin, HIGH); // Turn off pump relay
  }
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("PH:");
  lcd.print(ph_act);

 /* lcd.setCursor(8, 0);
  lcd.print("DO");
  lcd.print(do_act);*/
 
  lcd.setCursor(0, 1);
  lcd.print("TDS");
  lcd.print(mh_act);

  // Print sensor values to Serial Monitor
  Serial.print("pH: ");
  Serial.print(ph_act, 2);
  //Serial.print(" | DO Analog: ");
  //Serial.print(do_act);
  Serial.print(" | TDS: ");
  Serial.println(mh_act);
  
  
  B.print(ph_act);
  B.print("pH: ");
  B.print(",");
  //B.print(do_act);
  //B.print("Do: ");
  //B.print(",");
  B.println(mh_act);
  B.print(",");
  if(ph_act > 6){
    B.print("OFF");
  }
  else{
    B.print("ON");
  }

  B.print(";");

  delay(2000);
}
