# Trusona Mobile SDK for Android

The Trusona SDK allows simplified interaction with the Trusona API.

## Table of Contents
1. [Prerequisites](#prerequisites)
    1. [Artifactory username and password](#artifactory-username-and-password)
    1. [Minimum Android Version](#minimum-android-version)
1. [Required Configuration](#required-configuration)
    1. [Adding your credentials to `$HOME/.gradle/gradle.properties`](#adding-your-credentials-to-$HOME/.gradle/gradle.properties)
    1. [Adding the Trusona repository to Gradle](#adding-the-trusona-repository-to-gradle)
1. [Installation](#installation)
    1. [Installing as a gradle dependency](#installing-as-a-gradle-dependency)
    1. [Java 8 Configuration](#java-8-configuration)
    1. [AndroidX Configuration](#androidx-configuration)
1. [Creating a Trusona instance](#creating-a-trusona-instance)
    1. [Device identifier](#device-identifier)
        1. [Generated Keys](#generated-keys)
        1. [Requesting a Device Identifier](#requesting-a-device-identifier)
            1. [Example](#example)
1. [Scanning TruCodes](#scanning-trucodes)
    1. [Customizing the scanner UI](#customizing-the-scanner-ui)
1. [Polling for an IN_PROGRESS Trusonafication](#polling-for-an-in_progress-trusonafication)
1. [Fetching a single in progress trusonafication and handling it via startTrusonaficationActivity](#fetching-a-single-in-progress-trusonafication-and-handling-it-via-startTrusonaficationActivity)
1. [Scanning Driver's Licenses](#scanning-drivers-licenses)
1. [Scanning Passports](#scanning-passports)
1. [Upgrading to Executive](#upgrading-to-executive)
1. [Customizing the UI](#customizing-the-ui)
1. [Handling Deep links](#handling-deep-links)

## Prerequisites

### Artifactory username and password

Trusona uses Artifactory to distribute artifacts associated with the Trusona mobile and server SDKs.

When Trusona provisions a developer account, we will create a new user in Artifactory and supply you with a username and password that you will use later on in this guide.

### Minimum Android Version
The Trusona SDK requires API level 21 or higher. It also requires at least Android Support Library rev. 27.1.1 and Google Play Services rev. 15.0.1.

## Required Configuration


### Adding your credentials to `$HOME/.gradle/gradle.properties`

Gradle supports user specific variables which is a convenient way to handle using sensitive data
in your Gradle scripts without having to check them into your source control system. Create or
open the `$HOME/.gradle/gradle.properties` file under your user's home directory and add the following:

```
trusonaUsername=<YOUR_TRUSONA_ISSUED_MAVEN_USERNAME>
trusonaPassword=<YOUR_TRUSONA_ISSUED_MAVEN_PASSWORD>
```

### Adding the Trusona repository to Gradle

To add the private Trusona repository to your project, edit the `build.gradle` file **at the root folder** and add the following:

```groovy
allprojects {
    repositories {
        maven { url = "https://maven.google.com" }
        mavenCentral()
        mavenLocal()
        jcenter()

        // the trusona repository
        maven {
            credentials {
                username = trusonaUsername
                password = trusonaPassword
            }
            url = "https://trusona.jfrog.io/trusona/sdk-releases/"
        }
    }
}
```


## Installation

The Trusona SDK should be declared as a dependency in your Gradle project.

### Installing as a gradle dependency

> Note: This method requires access to the private Trusona SDK Artifactory repository

1. Ensure that your `~/.gradle.properties` file contains your supplied Artifactory credentials
1. Define the SDK version you wish to use and add the `api` dependency to your Gradle project.

```gradle
dependencies {
  // other dependencies
  api "com.trusona.android:mobile-sdk:10.1.0"

  // the following is only required if you will be using the Trusona Passport SDK
  //api "com.trusona.android:passport-sdk:8.0.0"
}
```

### Java 8 Configuration

The SDK requires Java 8 compatibility. Be sure to have the following configuration within the `android` block of your project's `build.gradle` file:

```gradle
compileOptions {
  sourceCompatibility 1.8
  targetCompatibility 1.8
}
```

### AndroidX Configuration

The SDK has dependencies on the latest [AndroidX](https://developer.android.com/jetpack/androidx) libraries.

For normal function and compatibility with legacy support libraries, add the following configuration to your project's `gradle.properties` file:

```
android.enableJetifier=true
android.useAndroidX=true
```

## Creating a Trusona instance

To create an instance of `Trusona` for making API calls, use the provided constructor.

```java
Trusona trusona = new Trusona();
```

The Trusona SDKs have infrastructure in place to serve the US, European and Asia-Pacific regions, with the US being the default one. Unless otherwise noted, you will not need to configure Trusona to use a specific region. If you have been provisioned in a specific region, you will need to point the SDK to use that region. To achieve this, you’ll need to specify one of the possible values from the `Region` enum when building a `Trusona` instance.

```java
// Use this to connect to the European region
Trusona trusona = new Trusona(Region.EU);     
//or use this to connect to the Asia-Pacific region
Trusona trusona = new Trusona(Region.AP);
```

If you have deployed your own infrastructure, you can customize the SDK to use it by providing the base url that points to it, as shown below:

```java
CustomBaseUrl customBaseUrl = new CustomBaseUrl("https://my.custom.base.url.net");
Trusona trusona = new Trusona(customBaseUrl);
```

Note that if the url is incorrectly formatted, the sdk will default to using the US infrastructure.

### Device Identifier

A device identifier is required to interact with some of the Trusona APIs. To acquire one, simply
make a request to the Trusona API via the `getDeviceIdentifier` method and this will create or update a
device in the Trusona servers. Internally, a set of cryptographic keys will be generated and stored
in the Android Keystore.

> **NOTE** The TrusonaSDK will not work on a device running an API lower than 21.

#### Generated Keys

The Trusona SDK attempts to generate three types of keys:

* **Device Key** - The Device Key is always generated and does not require User Presence. Using this key for signing operations will not prompt the user for any interaction with the device.
* **User Lock Screen Auth Key** - The User Lock Screen Auth Key is generated when the user has a pin, pattern or password set. Using this key for signing operations (e.g. during a Trusonafication) will prompt the user to enter their pin, pattern or password.
* **User Fingerprint Auth Key** - The User Fingerprint Auth Key is generated when the user has at least one fingerprint enrolled on the device. Using this key for signing operations (e.g. during a Trusonafication) will prompt the user to enter their fingerprint.

During signing, the SDK will give precedence to the `User Fingerprint Auth Key` but it also provides a mechanism to fall back to the `User Lock Screen Auth Key`, should the device owner be unable to present their enrolled fingerprint.

#### Requesting a Device Identifier

Requesting a device identifier is an asynchronous operation which, once completed, will provide an instance
of `DeviceIdentifierResponse`. This response will contain the device's request status and a device identifier
if one was generated.

The various results of a request to `getDeviceIdentifier` are:

#### Successful results

|        Result           |                                                                       Description                                                                        |  Returns Identifier?  |
| :--------------------   | :------------------------------------------------------------------------------------------------------------------------------------------------------- | :-------------------: |
| `NEW_DEVICE`            | Indicates that a new device was successfully created.                                                                                                    |           Yes         |
| `INACTIVE_DEVICE`       | Indicates that the device was already created, but is inactive.                                                                                          |           Yes         |
| `ACTIVE_DEVICE`         | Indicates that the device was already created, and is active.                                                                                            |           Yes         |

#### Failed results

|        Result           |                                                                       Description                                                                        |  Returns Identifier?  |
| :--------------------   | :------------------------------------------------------------------------------------------------------------------------------------------------------- | :-------------------: |
| `INVALID_DEVICE`        | Indicates that the request to create the device failed because the server determined the device to be invalid.                                           |           No          |
| `SERVER_ERROR`          | Indicates that the request to create the device failed because the server encountered an error.                                                          |           No          |
| `UNKNOWN`               | Indicates that an unknown error occurred.                                                                                                                |           No          |

#### Example

The following code demonstrates how to acquire a device identifier.

```java
// 1
DeviceIdentifierRequester deviceIdentifierRequester = new DeviceIdentifierRequester() {
    @Override
    public void onDeviceIdentifier(@NonNull DeviceIdentifierResponse deviceIdentifierResponse) {
        switch (deviceIdentifierResponse.getDeviceStatus()) {

            case ACTIVE_DEVICE:
                // A device identifier is available and the device is ready to perform trusonafications.
                break;

            case INACTIVE_DEVICE:
            case NEW_DEVICE:
                // A device identifier is available in these scenarios.
                break;

            case INVALID_DEVICE:
            case SERVER_ERROR:
            case UNKNOWN:
                // A device identifier is not available in these scenarios.
                break;
        }
    }
};

//2
trusona.getDeviceIdentifier(context, deviceIdentifierRequester);

```

1. Implement the `DeviceIdentifierRequester` interface that will receive an instance of `DeviceIdentifierResponse`.
The latter will contain the device's status, the `device identifier` and an optional `String` that contains a signed JWT containing the device identifier in the `sub` field. This JWT can be verified against the JWK set at https://api.trusona.net/jwks to verify its authenticity.
2. Invoke the Trusona API passing in a reference to a context and your implementation of `DeviceIdentifierRequester`.


> **NOTE** Do not store and reuse your device identifier, as it is subject to change. Always call the
`getDeviceIdentifier` method whenever you need the most current device identifier.


### Scanning TruCodes

TruCodes are a mechanism used to identify the user that is attempting to log into a resource protected by trusona.

The Trusona SDk provides a Scanner that will recognize TruCodes and create a Trusonafication whenever one of them is
scanned.

The following example illustrates how to use the TruCode Scanner by loading it as a child `Fragment` of another
`Fragment` that's already in the foreground:

```java
// 1
TruCodeHandler truCodeHandler = new TruCodeHandler() {
  
  @Override
  public void onTrusonaficationCreated() {
    // The TruCode was successfully scanned. You may want to start monitoring for pending
    // trusonafications at this point if you haven't done so yet.
  }

  @Override
  public void onError(String payload) {
    // There was an error while processing the TruCode
  }

  @NonNull
  @Override
  public Integer fragmentContainerId() {
    // Update this method to return the id of the ViewGroup container into which the Trusona
    // SDK will display the TruCode Scanner. This ID must be present in the layout of the fragment
    // that's in the foreground and that's passed in to the loadTruCodeAsChildFragment() method.
    // i.e.: R.id.tru_code_scanner_container.
    return null;
  }
};

// 2
IdentifierProvider identifierProvider = new IdentifierProvider() {
  @Override
  public String identifier() {
    // Return your device's identifier
    return null;
  }
};

// 3
trusona.loadTruCodeAsChildFragment(myFragment, truCodeHandler, identifierProvider);
```

1. Implement the `TruCodeHandler` interface, which will be notified when the Trucode has been scanned.
2. Implement the `IdentifierProvider` to return the device's identifier
3. Invoke the Trusona API passing in the implemented interfaces and a reference to the `Fragment` where the TruCode Scanner should be displayed. This API call should be done once the `Fragment` passed as a parameter
has been attached to its corresponding activity and is visible, for example, during it's `onViewCreated` method.

Alternatively, if you'd like to load the TruCode scanner in a layout that is not part of a `Fragment`, the 
`getTruCodeFragment` method provides a `Fragment` reference that can be loaded using an Activity's 
`SupportFragmentManager`.

#### Customizing the scanner UI

In order to customize the scanner UI, you can modify the contents of the layout xml file inflated by the `myFragment` instance passed in to the `trusona.loadTruCodeAsChildFragment(myFragment, truCodeHandler, identifierProvider)` method. 
You'll want to arrange your custom views in such a way that they will be placed on top of the scanner's viewfinder. 

The following xml layout file demonstrates how to do this:

```xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- The id of this FrameLayout should match the one returned by your implementation of  -->
    <!-- TrucodeHandler's `fragmentContainerId()` method. -->
    <FrameLayout
        android:id="@+id/tru_code_scanner_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <!-- TODO: Place any custom views you'd like to add below this line, so that they're laid -->
    <!-- on top of the scanner -->

</FrameLayout>
```

Using this strategy, you can add any custom views you'd like on top of the scanner and you can set click 
listeners or modify them as you would typically do: By getting a reference to them during `myFragment`'s
`onCreateView` lifecycle method.

### Polling for an IN_PROGRESS Trusonafication

A Trusonafication occurs when a user attempts to access a protected resource, and the user is allowed
to accept or deny the action. When a Trusonafication is received, the app needs to respond to confirm
the user's identity. Depending on how the Trusonafication was created, this may involve a visual prompt
requiring the user to accept and/or provide the credentials used to unlock the device.

Polling for in progress trusonafications should happen only when your application is in the foreground.
Typically you'll want to start the polling process in your `Activity`'s `onResume` or `onCreate` lifecycle
callback. The SDK will automatically stop the polling process for you during the `onStop` lifecycle 
callback, however, if you need to stop polling at a particular point in time, you may use the `stop()`
method of the `PollerStopper` object returned by the `startPolling` method.

A call to the `startPolling` method requires an instance of `AppCompatActivity` in order to start a new
SDK controlled Activity when a Trusonafication is received. The latter is invoked via the `startActivityForResult`
method, so the calling `AppCompatActivity` must implement `onActivityResult` in order to receive the
outcome of the processed `Trusonafication`.

#### Example

```java
public class MyActivity extends AppCompatActivity {

  private Trusona trusona = new Trusona();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // 1
    trusona.startPolling(this);
  }

  // 2
  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == TrusonaficationActivity.TRUSONAFICATION_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {

      // 3
      Trusonafication trusonafication;
      try {
        trusonafication = TrusonaficationParser.getFromIntent(data);
      }
      catch (IllegalArgumentException e) {
        trusonafication = null;
        //Trusonafication not present in intent data
      }

      // 4
      /* This Throwable will be returned if something goes wrong while processing a trusonafication. */
      Throwable error = (Throwable) data.getSerializableExtra(TrusonaficationActivity.ERROR_KEY);

      if (trusonafication != null && error == null) {
        // 5
        handleTrusonaficationResult(trusonafication);
      }
      else {
        // 6
        handleError(error);
      }
    }
  }

  private void handleError(Throwable error) {
    // Some of the possible exceptions that can be received are:
    if (error instanceof RequiredIdentityDocumentException) {
        /* Used when a trusonafication required that the user
         presents their Identity Document but they have not registered one to their account yet */
    }
    else if (error instanceof InvalidIdentityDocumentException) {
        /* Used when the scanned Identity document is not in
         AAMVA format or it was not accepted by the trusona servers. */
    }
    else if (error instanceof UnavailableDeviceSecurityException) {
      /* Used when the trusonafication requires that the device has enabled a pin/pattern/password/biometric
      but it hasn't. */
    }
    else {
      //Unexpected type of exception
    }
  }

  private void handleTrusonaficationResult(Trusonafication trusonafication) {
    switch (trusonafication.getStatus()) {

      case ACCEPTED:
        // The trusonafication was successfully accepted by the user
        break;

      case REJECTED:
        // The trusonafication was successfully rejected by the user
        break;

      case FAILED:
        // The user attempted to accept or reject the trusonafication but an error occurred
        break;

      case EXPIRED:
        // The user did not accept or reject the trusonafication during it's validity period,
        // which by default is 2 minutes.
        break;

      case CANCELED:
        // The trusonafication was cancelled
        break;

      case IN_PROGRESS:
        // The trusonafication is still in progress. This means the user backed out of the trusonafication
        // process, typically by sending the app to the background via the device's home button.
        break;

      default:
        break;
    }
  }
}
```

1. Call `trusona.startPolling(this)` during the Activity's `onCreate` method.
2. Override `onActivityResult` so you can receive the trusonafication result passed back by the SDK.
3. Use the `TrusonaficationParser` to acquire the processed `Trusonafication` from the passed in `Intent`.
4. Query the passed in `Intent` for any errors that may have been thrown during the trusonafication process.
5. If no errors were thrown, handle the trusonafication result by examining its `TrusonaficationStatus`.
6. If an error was thrown, handle accordingly.

### Fetching a single in progress trusonafication and handling it via startTrusonaficationActivity

If you'd like to acquire a single in-progress trusonafication, you can do so by calling the 
`fetchNexTrusonafication(TrusonaficationListener)` method of the `Trusona` class.

Being an asynchronous method, this requires that you pass in an implementation of the `TrusonaficationListener`
interface, which will provide you with the callbacks to determine the right course of action depending on 
whether a trusonafication was found or not.

In case a trusonafication is found, you may want to process it via one of the variants of the `startTrusonaficationActivity`
method.

```java
// 1
TrusonaficationListener trusonaficationListener = new TrusonaficationListener() {
    @Override
    public void onTrusonafication(Trusonafication trusonafication) {
        // This callback will execute if an in-progress trusonafication is found.
        // You may want examine the trusonafication or request that the SDK
        // processes it by calling:
        // trusona.startTrusonaficationActivity(appCompatActivity, trusonafication);
    }

    @Override
    public void onErrorFetchingTrusonafication(Throwable throwable) {
        // This callback will execute if no trusonafication was found or
        // if an error occured. You can examine the throwable parameter
        // to determine which was the case.
    }
};

// 2
trusona.fetchNexTrusonafication(trusonaficationListener);
```

1. Implement the `TrusonaficationListener` interface.
2. Using a previously instantiated `Trusona` object, call `fetchNexTrusonafication`, passing in
your implementation of the `TrusonaficationListener` interface.


### Scanning Driver's Licenses

The Trusona SDK provides a user interface that can be shown to prompt the user to scan the barcode on a US/Canadian driver's license. The SDK will execute a callback in the app upon successful scanning and provide the parsed contents of the barcode. In the case of failure, another callback will be called, which should handle the failure in a way that makes sense in the app.

Note: Scanning a driver's license as part of accepting a Trusonafication is handled automatically by the SDK. Use the following method only if you require to examine the contents
of a driver's license as part of the business logic in your application.

```java
// 1
PDF417ScanHandler pdf417ScanHandler = new PDF417ScanHandler() {
	
    @Override
    public void onSuccess(@NonNull DriversLicenseScanResult result) {
    	// examine license properties as necessary
    	AAMVADriversLicense license = result.getDriversLicense();
    	
    	// examine scanning event's metadata. This metadata is subject to change. Additionally, 
    	// all properties may not be available for all scan results.
	// Current available properties are:
	// x - result of division of the x/y point of the top left corner
	// y - result of division of the x/y point of the top right corner
	// h - result of division of the x/y point of the bottom left corner
	// w - result of division of the x/y point of the bottom right corner
	// t - value returned by System.nanoTime()
	//
    	Map<String, ?> metadata = result.getMetadata();    
    }
	
    @NonNull
    @Override
    public Integer fragmentContainerId() {
        // Return the ID of the ViewGroup in which the SDK will attempt to load the Driver
        // License scanner. This ID must be present in the layout of the Fragment that's passed
        // in as a parameter to the scanDriversLicense method, and the Fragment must have been
        // brought into the foreground and be ready to host the Driver License scanner.
        // i.e.: R.id.my_fragment_container.
        return 0;
    }
	
    @Override
    public void onDocumentError(Throwable throwable) {
    	// handle error case due to invalid document being scanned
    }
};

// 2
trusona.scanDriversLicense(fragment, pdf417ScanHandler);
```

1. Implement the `PDF417ScanHandler` interface.
2. Using a previously instantiated `Trusona` object, call `scanDriversLicense`, passing a reference of the fragment into which the Driver License scanner will be inflated and the instance of the implemented `PDF417ScanHandler`.


### Scanning Passports

The Trusona SDK provides a user interface that can be shown to prompt the user to scan the MRZ data on a passport. The SDK will call a callback in the app upon successful scanning and provide the parsed contents of the passport. In the case of failure, another callback will be called, which should handle the failure in a way that makes sense in the app. 

```java
// 1
PassportScanHandler passportScanHandler = new PassportScanHandler() {
    
    @Override
    public void passportScanned(PassportScanResult passportScanResult) {
        // examine license properties as necessary
        Passport passport = passportScanResult.getPassport();
        // examine metadata properties as necessary
        Map<String, Object> metadata = passportScanResult.getMetadata();
    }

    @Override
    public void passportScanCanceled() {
        // Handle what happens when the user taps the Cancel button.
    }

    @NonNull
    @Override
    public Integer fragmentContainerId() {
        // Return the ID of the ViewGroup in which the SDK will attempt to load the Passport
        // scanner. This ID must be present in the layout of the Activity that's passed
        // in as a parameter to the getPassportScanner method, and the Activity must have been
        // brought into the foreground and be ready to host the Passport scanner.
        // i.e.: R.id.my_passport_scanner_container.
        return 0;
    }
};

// 2
trusona.getPassportScanner(activity, passportScanHandler);
```

1. Implement the `PassportScanHandler` interface.
2. Using a previously instantiated `Trusona` object, call `getPassportScanner`, passing a reference of the activity into which the Passport scanner will be inflated and the instance of the implemented `PassportScanHandler`.

### Upgrading to Executive

Users can be authenticated at the Executive level if they register a driver's license with their account. Before a user can be authenticated with Executive, they must associate a driver's license. The SDK provides a callback interface to perform this step. 

Before doing the upgrade, it is important to make sure the device identifier of the running app has been bound to an account.

The following code snippet illustrates the required step:

```java
    
// 1    
PDF417UpgradeHandler pdf417UpgradeHandler = new PDF417UpgradeHandler() {

    @Override
    public void onSuccess(@NonNull IdentityDocument<DriversLicenseScanResult> identityDocument) {
        // This method will be called when the user was succesfully upgraded, which means
        // now you  can authenticate them at Executive level.
        // You may examine the returned IdentityDocument instance as necessary and get a
        // DriversLicenseScanResult instance to examine via the following method:
        DriversLicenseScanResult driversLicenseScanResult = identityDocument.getDocument();
    }

    @NonNull
    @Override
    public Integer fragmentContainerId() {
   	    // Return the ID of the ViewGroup in which the SDK will attempt to load the Driver
        // License scanner. This ID must be present in the layout of the Fragment that's passed
        // in as a parameter to the upgradeToExecutive method, and the Fragment must have been
        // brought into the foreground and ready to host the Driver License scanner.
        // i.e.: R.id.my_fragment_container.
   	     return 0;
    }

    @Override
    public void onDocumentError(Throwable throwable) {
    	// handle error case due to invalid document being scanned
    }
};

// 2
trusona.upgradeToExecutive(fragment, pdf417RegisterHandler);
```

1. Implement the `PDF417UpgradeHandler` interface.
2. Using a previously instantiated `Trusona` object, call `upgradeToExecutive`, passing a reference of the fragment into which the Driver License scanner will be inflated and the instance of the implemented `PDF417RegisterHandler`.

### Customizing the UI
During the Trusonafication process, the user will be presented with several steps of confirmation, depending on the level of verification requested in the Trusonafication. These steps may include:

- A visual prompt that gives the user the choice of accepting or rejecting the Trusonafication
- An operating system authentication (e.g. Fingerprint, Pattern, PIN)
- Scanning of driver's license

The prompt and scanning screens can be customized by passing an instance of the `TrusonaficationUICustomizations` interface to the variants of the 
`startPolling` or `startTrusonaficationActivity` methods that receive one as a parameter. If you don't pass in a `TrusonaficationUICustomizations`
implementation, then a default dialog and scanner UI will be used.

The `TrusonaficationUICustomizations` interface has five methods:
   - `boolean usesCustomDialog()`: Set the return value to true if you'd like to provide a custom prompt/dialog.
   - `Dialog trusonaficationDialog(Context context, Trusonafication trusonafication)`: If you set the return value of `usesCustomDialog()`
   to true, then you will need to create and return your custom prompt in this method. Otherwise, you can return null.
   - `boolean usesCustomDLScanner()`: Set the return value to true if you'd like to provide a custom scanner UI.
   - `Fragment getCustomDLScanner(Trusonafication trusonafication)`: If you set the return value of `usesCustomDLScanner()`
   to true, then this method must return an instance of the fragment you'd like to host the scanner in. Otherwise, you can return null.
   - `Integer getDLScannerContainerId()`: If you set the return value of `usesCustomDLScanner()` to true, then this method must return
   the `id` of the container in which you want to host the scanner. This `id` has to be defined in the `XML layout` used by the `Fragment`
   returned by the `getCustomDLScanner(Trusonafication trusonafication)` method. You may return null if you're not using a custom
   Driver License Scanner.

It's important to note that `Activities` nor `Fragments` should implement the `TrusonaficationUICustomizations` interface, as the SDK will
maintain a reference to it while it's own `Activity` handles the `Trusonafication` flow.

#### Example

```java
// 1
TrusonaficationUICustomizations trusonaficationUICustomizations = new TrusonaficationUICustomizations() {
  
    @Override
    public boolean usesCustomDialog() {
        // Update the return value to indicate whether you'd like to use a custom prompt dialog or not
        return true;
    }

    @Override
    public Dialog trusonaficationDialog(Context context, Trusonafication trusonafication) {
        // In order to customize the UI used to prompt users to accept or reject 
        // trusonafications, you can return a Dialog that inflates the xml layout 
        // you'd like to use.
        //
        // You may customize the views in your Dialog by acquiring a reference to them via the 
        // dialog.findViewById() method and then applying any changes you see fit, like adding
        // onCLickListeners, applying spans to TextViews, etc.
        //
        // When using a custom Dialog, the layout it inflates must contain the following views so
        // that the sdk can properly process the accept and reject actions of the user:
        //
        // * A View with the id `trusonafication_accept_button`
        // * A view with the id `trusonafication_reject_button`
        //
        // The following code returns a full screen dialog that uses the my_trusonafication_layout
        // xml file: 

        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.my_trusonafication_layout);

        // Note: Do not call the show method of the dialog instance. The SDK will do that
        // at the appropriate moment. Also, if usesCustomDLScanner() returns false, this 
        // method may return null.
        return dialog; 
    }

    @Override
    public boolean usesCustomDLScanner() {
        // Update the return value to indicate whether you'd like to use a custom Driver License Scanner or not
        return true;
    }

    @Override
    Fragment getCustomDLScanner(Trusonafication trusonafication) {
        // Update this method to return an instance of the Fragment onto which you want to 
        // load the Driver License Scanner or null if your application does not require
        // Driver License scanning. The Fragment must be loaded by the FragmentManager
        // before being returned.
        //
        // You may inspect the IN_PROGRESS Trusonafication which is passed  as a parameter and 
        // perform any UI updates necessary.
        //
        // Note: If usesCustomDLScanner() returns false, this method may return null.
        //
        // Sample implementation:
        return MyFragment.newInstance();
    }

    @IdRes
    @Override
    public Integer getDLScannerContainerId() {
        // Update this method to return the id of the ViewGroup container into which the Trusona 
        // SDK will display a Driver License Scanner if a Trusonafication requires it. This ID must 
        // be present in the layout of the Fragment returned by the `getCustomDLScanner` method implementation in
        // this class. Alternatively, this can be null if your application does not require identity
        // document scanning.
        //
        // Note: If usesCustomDLScanner() returns false, this method may return null.
        // 
        // Sample implementation:
        return R.id.my_fragment_container;
    }
};

// 2
trusona.startPolling(appCompatActivity, trusonaficationUICustomizations);
```

1. Implement the `TrusonaficationUICustomizations` interface. The layout used by the Dialog created in the 
`trusonaficationDialog(Context context, Trusonafication trusonafication)` method can be styled in any way but it must contain
the following views to be properly rendered and to allow the sdk to process accepting and rejecting the trusonafication:
* A View with the id `trusonafication_accept_button`
* A view with the id `trusonafication_reject_button`

2. Using a previously instantiated `Trusona` object, call `startPolling` (or `startTrusonaficationActivity`), passing 
an instance of the implemented `TrusonaficationUICustomizations`. Usually, you would do this in your `Activity`'s `onResume` 
or `onCreate` method.

#### Custom Trusonafication metadata
When creating a Trusonafication from one of the server SDKs, it is possible to include a set of custom fields on the Trusonafication. 
These custom fields are made available in your implementation of `TrusonaficationUICustomizations` when the `trusonaDialog(Context context, Trusonafication trusonafication)` 
and `getCustomDLScanner(Trusonafication trusonafication)` callback methods are called and a Trusonafication is passed in as a parameter to them. 
The custom data will be available in the `customFields` property of the Trusonafication. By inspecting the custom data in these fields, the 
dialog behavior can be customized or the data can be shown in the dialog UI.

### Handling Deep links

Your mobile application needs to be configured to handle deep links so that it can receive the data passed by the Trusona web SDKs
to the Trusona mobile SDKs. Depending on your minimum supported Android version, this may mean you'll need to configure App links and
intent filters for incoming links as well. For more information on how to do this, take a look at the official Android Developers documentation [here](https://developer.android.com/studio/write/app-link-indexing.html) and [here](https://developer.android.com/training/app-links/deep-linking.html).

When deeplinking, Trusona will redirect users to your application using this url:

`<scheme>://<handle>.applaunchexpress.com/trucode?payload=XXXXXXXXXXXXXXXX`

The `scheme` will depend on whether you are using App links or a custom scheme. The former will use `https` and the later a
scheme of your choice. The `handle` will be provided by Trusona and is unique to your application.

Once one of your Activities has been set up to handle deep links, it will need to implement the following steps:

```java

//1 
TruCodeHandler deepLinkTruCodeHandler = new TruCodeHandler() {

    @Override
    public void onTruCode(boolean success) {
        if(success) {
            // the deeplink trusonafication was successfully accepted and validated.
        }
        else {
            // There was an error while processing the deeplink trusonafication.
        }
    }

    @Nullable
    @Override
    public Integer fragmentContainerId() {
        // Update this method to return the id of the ViewGroup container in which the accept / reject 
        // prompt will be loaded. This ID must be present in the layout of the fragment passed to the `handleTruCodeDeepLink`
        // method.
        return null;
    }

    @Nullable
    @Override
    public Integer acceptRejectLayoutId() {
        // Update this method to return the id of the layout that will be used to prompt users
        // to accept or reject trusonafications. Alternatively, return null to use the default OS 
        // alert dialog.
        return null;
    }
};

// 2
@Override
protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    // Your code goes here

    // 2.1 Load the fragment that will host the accept / reject prompt.

    // 2.2
    trusona.handleTruCodeDeepLink(fragment, deepLinkTruCodeHandler, getIntent().getData());
}

@Override
protected void onStop() {
    super.onStop();
    // Your code goes here
    // 3
    trusona.stopPendingTrusonaficationsMonitor();
}
```

1. Provide an implementation of the `TruCodeHandler` interface.
2. Update your activity's `onCreate` method to:
  * Load the fragment that will host the accept / reject prompt.
  * Use an existing instance of the Trusona class to call `handleTruCodeDeepLink` passing in a reference to the fragment loaded in the previous step, the `TruCodeHandler` implementation from step 1, and the deep link payload acquired by calling `getIntent().getData()`.
3. Update your activity's `onStop` method to call `stopPendingTrusonaficationsMonitor`.

### Need additional help?

Contact us at engineering@trusona.com
