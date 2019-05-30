# TypingDNA Recorder - Android


## Table of contents

- About ​
- Overview ​
   - TypingDNARecorderMobile class ​
- Methods ​
   - TypingDNA.getTypingPattern (optionsObject) ​
   - TypingDNA.addTarget(element_id) ​
   - TypingDNA.removeTarget(element_id) ​
   - TypingDNA.reset() ​
   - TypingDNA.start() ​
   - TypingDNA.stop() ​
   - TypingDNA.pauseOverlay()
- Recommendations ​


## About

The recorder captures user’s typing patterns.

It does so by recording timings based on keydown and keyup events, but also motion data related to the typing behavior (accelerometer, gyroscope, orientation). Recording
motion data is specific for the mobile recorder and does not apply for the JavaScript desktop recorder.

> **Note**​: the Android version does not get the keydown events through the event notification system, therefore it needs a screen overlay to capture them. It is very
important to include this in your apps. Also, please note that the recording might not work properly if emojis are used.


For the recorder to work, the TypingDNAOverlayService must be registered as a service in AndroidManifest.xml:

```xml
<application ...>
<service android:name=".typingdna.TypingDNAOverlayService" />
</application>
```

## Overview

Use TypingDNARecorderMobile recorder class to capture user's typing patterns.

First you need to add com.typingdna to your Built Path and import **com.typingdna.TypingDNARecorderMobile** ​ class in the app that wants to record a typing pattern. You will need to record typing patterns when a user first creates his account and again whenever you want to authenticate that user on your platform.

### TypingDNARecorderMobile class

Once you create an instance of the TypingDNARecorderMobile class, the user typing starts being recorded (as a history of key stroke events). Whenever you want to get the user's typing pattern you have to invoke .getTypingPattern method described in detail below.

```java
private TypingDNARecorderMobile tdna;

protected void onCreate (Bundle savedInstanceState) {
    if(tdna == null) {
        tdna = new TypingDNARecorderMobile(this);
        }
    else {
        tdna.start();
    }
}
```

> **Note:** The class should be instantiated on each View on which you want to record the typing pattern. When the app is not in foreground anymore, invoke the .pauseOverlay() method to pause the overlay and stop recording the sensors. Call the .stop() method as well on events such as onStop() or onDestroy(). See recommendations and examples below.

**Here are the methods of TypingDNARecorder class:**

* getTypingPattern(params) ⇒ String
* addTarget(int targetId)
* removeTarget(int targetId)
* start()
* removeTarget()
* stop()
* pauseOverlay()
* reset()



## Methods

### a. TypingDNA.getTypingPattern (params)

This is the main function that outputs the user's typing pattern as a `String`

**Definition:**

```java
public String getTypingPattern​(​int​ type, ​int​ length, ​String​ text, ​int​ textId, int ​target, ​boolean​ caseSensitive)
```

Other definitions, overriding the main one:

```java
public String getTypingPattern​(​int​ type, ​int​ length, String text, ​int​ textId, int​ target)
```
```java
public String getTypingPattern​(​int​ type, ​int​ length, String text, ​int​ textId)
```
**Returns:** A typing pattern in ​String​ form.

**Parameters:**

| Param         	| Type    	| Description                                                                                                                                                                                                                                                                                                                                                            	|
|---------------	|---------	|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------	|
| type          	| `int`     	| `0 for anytext pattern` (when you compare random typed texts of usually 120-180 chars long).​ `1 for sametext pattern` recommended in most cases, for emails, passwords, phone numbers, credit cards, short texts).​ `2 for extended pattern` (most versatile, can replace both anytext and sametext patterns. Best accuracy, recommended in cases where text is not a secret) 	|
| textid        	|`int`     	| (Optional, only for type 1 and type 2) a personalized id for the typed text. 0 = ignore                                                                                                                                                                                                                                                                                	|
| text          	| `String`  	| (Only for type 1 and type 2) a string that you want the typing pattern for                                                                                                                                                                                                                                                                                             	|
| length        	| `int`     	| (Optional) the length of the text in the history for which you want the typing pattern, for type 0 is usually 140 or more. 0 = ignore (works only if text = “”)                                                                                                                                                                                                        	|
| target        	| `String`  	| Specifies from which target the pattern should be obtained from.                                                                                                                                                                                                                                                                                                       	|
| caseSensitive 	| `Boolean` 	| (Optional, default: false) Used if you pass a text for type 1                                                                                                                                                                                                                                                                                                          	|                                                                                                                                                                                                                                                                                	|

**Example:**
```java
int type = 1;
int length = 0; //optional
String textToType = "Please type this sentence to get authenticated."
int textId = 0; //optional
boolean caseSensitive = false; //optional
String targetId = nameInput.getId();

String typingPattern = tdna.getTypingPattern(type, length, textToType, textId, targetId, caseSensitive);
```


### b. TypingDNA.addTarget(int targetId)
Adds a target to the recorder. You can add multiple elements.
All the typing events will be recorded for this component.

**Definition:**
```java
public​ void ​addTarget​(​int​ targetId)
```

Other definition, overriding the main one:

```java
public​ void ​addTArget​(​int[]​ targetIds)
```
**Example:**

```java
private TypingDNARecorderMobile tdna;

protected void onCreate (Bundle savedInstanceState) {
    // initialize tdna here
    
    tdna.addTarget(usernameInput.getId());
    tdna.addTarget(passwordInput.getId());
    tdna.addTarget(textInput.getId());
}
```

### c. TypingDNA.removeTarget(int targetId)

Remove a target from the targetIds array.


### d. TypingDNA.reset()

Resets the history stack of recorded typing events.


### e. TypingDNA.start()

It starts the recording of typing events. This method should be called at initialization or after a .stop() or a .pauseOverlay(), to resume recording.
Example:

```java
@Override
protected void onResume(){
   //Starts recording the typing evens and also starts the overlay service.
   tdna.start();
   super.onResume();
}
```

### f. TypingDNA.stop()

Ends the recording of further typing events. It can be invoked on such events as onDestroy() or onStop().
Example:

```java
@Override
protected void onDestroy() {
   //Stops the overlay service and ends the recording of further typing events.
   tdna.stop();
   super.onDestroy();
}

@Override
protected void onStop(){
   //Stops the overlay service and ends the recording of further typing events.
   tdna.stop();
   super.onStop();
}
```

### g. TypingDNA.pauseOverlay()
Pauses the overlay and the sensors.
We recommend you call this method on the onPause event, like in the following example:

```java
@Override
protected void onPause(){
   //Stops the overlay service and ends the recording of further typing events.
   tdna.pauseOverlay();
   super.onPause();
}
```


## Recommendations

We recommend that you deactivate **autocorrect** & **predictive features**  for the text field(s) you want to record on.

For mobile apps, we recommend you use  **type 1**  or **type 2**  typing patterns. The type 1/2 patterns work well with 15-30 chars of text, while type 0 pattern needs much more data to have good accuracy (120-150 chars of text).

For types 1/2 we recommend at least 3 enrolled patterns for a good accuracy.



