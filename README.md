speedzone
=========
# SpeedZone NSW 

![Icon](https://lh4.googleusercontent.com/-txEgUKuR5Sk/Un8DWbQTixI/AAAAAAAAn1Y/QPYLZgS6MPQ/s512-no/512x512.png)

## The [Big Hairy Audacious Goal (BHAG)](http://en.wikipedia.org/wiki/Big_Hairy_Audacious_Goal)
### The B.H.A.G. here is to create a Crowd Sourced [Polynomial Chaos](http://en.wikipedia.org/wiki/Polynomial_chaos) control system for traffic, starting in NSW.

The Idea is quite simple,

1. Measure every point variable in the system _(Well Below Critical Mass, need 1000's more anonymous probes)_
2. Continuously monitor for the occurrence of a pattern of interest._(Capability Exists)_
3. Apply corrective action._(Capability Exists)_


# The devil is in the detail:


###Working backwards:


**3. Apply corrective action**

Once the emergence of an undesired pattern is detected a control center can 
* Adjust the timing of traffic lights
* Dispatch "Traffic Emergency Patrol" crew to assist.
* Inform motorists of issue
* Advice alternative routes

This is already in place, as it is the standard method of [Traffic Management](http://www.transport.nsw.gov.au/tmc) around the world. 

This will reduce the severity of the issue and return the network to efficient flow very quickly.


**2. Continuously monitor**

This is achieved using a [real-time continuous analytics engine](http://www.sqlstream.com/resources/introduction-to-sqlstream/).

Historical data is stored as reference data and continuously compared to the real-time feed, real time [ETL](http://en.m.wikipedia.org/wiki/Extract,_transform,_load) and data cleansing is performed inline and patterns are then matched.

**1. Measure every point variable in the system**

So.....

This is where the 'Ambitious' comes in.

To do this we need 50,000 GPS equipped vehicles (probes) transmitting their position, speed and direction.

This is where **crowd sourcing** comes in.

Given:

1. an opportunity to help.

2. a low barrier to entry.

3. the assurance of anonymity.

4. a good incentive.




The public will step up and help.

So....  

Item 1: **SpeedZone NSW** addresses theses, its free and easily accessible to all.

Item 2: see Item 1

Item 3: The only data sent is latitude, longitude, speed and bearing. Plus a randomly generated id used to apply data quality rules, see footnote.

Item 4:
### Can save your license.
Announces when you are at risk of losing points

![f](https://lh6.googleusercontent.com/-mm0SVRnd9s4/Ui4_TuURqRI/AAAAAAAAmTg/U3-HTKZSW9g/w293-h508-no/device-2013-09-02-180029.png)![SpeedZone App](https://lh4.googleusercontent.com/-N_xiIY4eYlQ/Ui4_V6kifAI/AAAAAAAAmTw/J9tI4oU31XA/w293-h508-no/device-2013-09-02-190113.png)![im](https://lh6.googleusercontent.com/-0AHQu7otGgs/Uj1Ehfdz-iI/AAAAAAAAmqY/WaHQRI5x3FA/w293-h508-no/device-2013-09-21-163014.png)

###Can keep you safe. 
Warns you as you near dangerous areas. RTA put cameras in these locations for your safety. 

![POI](https://lh5.googleusercontent.com/-lo278TzrPeM/Ui4_QRR5XCI/AAAAAAAAmTQ/bT0D9y4Rkj8/w293-h508-no/device-2013-09-02-175506.png)

###Can save you time. 
Receive alerts from control center about issues in your path. 

###Can save you frustration. 
By keeping you informed about what is happening around you.


#Sound good? Join the Beta Testing
Join the [Altruistic Engineers](https://plus.google.com/communities/105872240007781184868) Community, then download the Beta [App](https://play.google.com/apps/testing/com.anthonykeane.speedzone). While in Beta the app can only be accessed by community members [(Google's Rule)](https://support.google.com/googleplay/android-developer/answer/3131213?hl=en)

.


***
the id is used to group events together. This allows for detection of data errors as checks can be done like examining the relationship between points and confirming the real world events confirm to the laws of physics.


