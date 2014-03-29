MindMe/AlarmPad Extensions API
=====================

Extension API source code for the Android apps MindMe Smart Notifications and AlarmPad.

## Getting started

Extensions are very easy to create and work the same way [DashClock extensions](https://code.google.com/p/dashclock/wiki/API) do:

1. Download and add the [API jar](https://github.com/riclage/quotes_extension/blob/master/libs/alarmpad-api-r1.0.jar?raw=true) file to your Android project;
2. Create a new service that extends the MindMeExtension class;
3. Add the corresponding <service> tag to your AndroidManifest.xml file and add the required <intent-filter> and <meta-data> elements.

Once you have both AlarmPad and your custom extension installed, you should be able to add your extension from AlarmPad's 'Manage Extensions' menu option.

## Get AlarmPad test version
Extensions are currently only supported on AlarmPad's test version. You can get it by joining the Google+ community at [https://plus.google.com/communities/113108980221311144145](https://plus.google.com/communities/113108980221311144145). Once you join the community, you need to also [join the test](https://play.google.com/apps/testing/com.mindmeapp.alarmpad) to download this version from the Play Store. 

## Example
Please visit [https://github.com/riclage/quotes_extension](https://github.com/riclage/quotes_extension) for a full working example.

## API reference
For more details about the API, please refer to the docs [here](http://riclage.github.io/extensions-api/).

## Get help
Please contact me at any time if you need any help at alarmpad (at) mindmeapp (dot) com.

## Credits
This work would not have been possible without [Roman Nurik](https://plus.google.com/u/0/+RomanNurik/)'s open source [DashClock](https://code.google.com/p/dashclock/wiki/API) app. Thank you very much for making it available.
