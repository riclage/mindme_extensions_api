/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mindmeapp.extensions;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.mindmeapp.extensions.internal.IMindMeExtension;
import com.mindmeapp.extensions.internal.IMindMeExtensionHost;

/**
 * Base class for a MindClock extension. Extensions are a way for other apps to show additional
 * information within MindMe apps such as AlarmPad. 
 * 
 * A limited amount of status information is supported. See the {@link ExtensionData} class
 * for the types of information that can be displayed.
 *
 * <h3>Subclassing {@link MindMeExtension}</h3>
 *
 * Subclasses must implement at least the {@link #onUpdateData(int)} method, which will be called
 * when the main app requests updated data to show for this extension. Once the extension has new
 * data to show, call {@link #publishUpdate(ExtensionData)} to pass the data to the main app
 * process. {@link #onUpdateData(int)} will by default be called roughly 30 minutes before the data
 * is needed to be displayed.
 *
 * <p>
 * Subclasses can also override the {@link #onInitialize(boolean)} method to perform basic
 * initialization each time a connection to the main app is established or re-established.
 *
 * <h3>Registering extensions</h3>
 * An extension is simply a service that the main process binds to. Subclasses of this
 * base {@link MindMeExtension} class should thus be declared as <code>&lt;service&gt;</code>
 * components in the application's <code>AndroidManifest.xml</code> file.
 *
 * <p>
 * The main app discovers available extensions using Android's {@link Intent} mechanism.
 * Ensure that your <code>service</code> definition includes an <code>&lt;intent-filter&gt;</code>
 * with an action of {@link #ACTION_EXTENSION}. Also make sure to require the
 * {@link #PERMISSION_READ_EXTENSION_DATA} permission so that only the main app can bind to your
 * service and request updates. Lastly, there are a few <code>&lt;meta-data&gt;</code> elements that
 * you should add to your service definition:
 *
 * <ul>
 * <li><code>protocolVersion</code> (required): should be <strong>1</strong>.</li>
 * <li><code>description</code> (required): should be a one- or two-sentence description
 * of the extension, as a string.</li>
 * <li><code>settingsActivity</code> (optional): if present, should be the qualified
 * component name for a configuration activity in the extension's package that the main app can offer
 * to the user for customizing the extension.</li>
 * <li><code>worldReadable</code> (optional): if present and true (default is false), will allow
 * other apps to read data for this extension.</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * Below is an example extension declaration in the manifest:
 *
 * <pre class="prettyprint">
 * &lt;service android:name=".ExampleExtension"
 *     android:icon="@drawable/ic_extension_example"
 *     android:label="@string/extension_title"
 *     android:permission="com.mindmeapp.extensions.permission.READ_EXTENSION_DATA"&gt;
 *     &lt;intent-filter&gt;
 *         &lt;action android:name="com.mindmeapp.extensions.Extension" /&gt;
 *     &lt;/intent-filter&gt;
 *     &lt;meta-data android:name="protocolVersion" android:value="1" /&gt;
 *     &lt;meta-data android:name="worldReadable" android:value="true" /&gt;
 *     &lt;meta-data android:name="description"
 *         android:value="@string/extension_description" /&gt;
 *     &lt;!-- A settings activity is optional --&gt;
 *     &lt;meta-data android:name="settingsActivity"
 *         android:value=".ExampleSettingsActivity" /&gt;
 * &lt;/service&gt;
 * </pre>
 *
 * If a <code>settingsActivity</code> meta-data element is present, an activity with the given
 * component name should be defined and exported in the application's manifest as well. The main app
 * will set the {@link #EXTRA_FROM_MINDME_SETTINGS} extra to true in the launch intent for this
 * activity. An example is shown below:
 *
 * <pre class="prettyprint">
 * &lt;activity android:name=".ExampleSettingsActivity"
 *     android:label="@string/title_settings"
 *     android:exported="true" /&gt;
 * </pre>
 *
 * Finally, below is a simple example {@link MindMeExtension} subclass that shows static data in
 * the main app:
 *
 * <pre class="prettyprint">
 * public class ExampleExtension extends MindMeExtension {
 *     protected void onUpdateData(int reason) {
 *         publishUpdate(new ExtensionData()
 *                 .visible(true)
 *                 .icon(R.drawable.ic_extension_example)
 *                 .status("Hello")
 *                 .expandedTitle("Hello, world!")
 *                 .expandedBody("This is an example.")
 *                 .clickIntent(new Intent(Intent.ACTION_VIEW,
 *                         Uri.parse("http://www.google.com"))));
 *     }
 * }
 * </pre>
 * 
 * Original code from: <a href="https://code.google.com/p/dashclock/">https://code.google.com/p/dashclock/</a>
 */
public abstract class MindMeExtension extends Service {
    private static final String TAG = "MindMeExtension";

    /**
     * Indicates that {@link #onUpdateData(int)} was triggered for an unknown reason. This should
     * be treated as a generic update (similar to {@link #UPDATE_REASON_PERIODIC}.
     */
    public static final int UPDATE_REASON_UNKNOWN = 0;

    /**
     * Indicates that {@link #onUpdateData(int)} was triggered because the user explicitly requested
     * that the extension be updated.
     */
    public static final int UPDATE_REASON_MANUAL = 1;
    
    /**
     * Indicates that {@link #onUpdateData(int)} was triggered due to a normal perioidic refresh
     * of extension data.
     */
    public static final int UPDATE_REASON_PERIODIC = 2;

    /**
     * Indicates that {@link #onUpdateData(int)} was triggered because settings for this extension
     * may have changed.
     */
    public static final int UPDATE_REASON_SETTINGS_CHANGED = 3;    

    /**
     * The {@link Intent} action representing a MindMe extension. This service should
     * declare an <code>&lt;intent-filter&gt;</code> for this action in order to register with
     * a MindMe app.
     */
    public static final String ACTION_EXTENSION = "com.mindmeapp.extensions.Extension";

    /**
     * Boolean extra that will be set to true when MindMe starts extension settings activities.
     * Check for this extra in your settings activity if you need to adjust your UI depending on
     * whether or not the user came from MindMe's settings screen.
     */
    public static final String EXTRA_FROM_MINDME_SETTINGS
            = "com.mindmeapp.extensions.extra.FROM_MINDME_SETTINGS";

    /**
     * The permission that MindMe extensions should require callers to have before providing
     * any status updates. Permission checks are implemented automatically by the base class.
     */
    public static final String PERMISSION_READ_EXTENSION_DATA
            = "com.mindmeapp.extensions.permission.READ_EXTENSION_DATA";

    /**
     * The protocol version with which the world readability option became available.
     */
    private static final int PROTOCOL_VERSION_WORLD_READABILITY = 1;

    private boolean mInitialized = false;
    private boolean mIsWorldReadable = false;
    private IMindMeExtensionHost mHost;

    private volatile Looper mServiceLooper;
    private volatile Handler mServiceHandler;

    protected MindMeExtension() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        loadMetaData();

        HandlerThread thread = new HandlerThread(
                "MindMeExtension:" + getClass().getSimpleName());
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new Handler(mServiceLooper);
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null); // remove all callbacks
        mServiceLooper.quit();
    }

    private void loadMetaData() {
        PackageManager pm = getPackageManager();
        try {
            ServiceInfo si = pm.getServiceInfo(
                    new ComponentName(this, getClass()),
                    PackageManager.GET_META_DATA);
            Bundle metaData = si.metaData;
            if (metaData != null) {
                int protocolVersion = metaData.getInt("protocolVersion");
                mIsWorldReadable = protocolVersion >= PROTOCOL_VERSION_WORLD_READABILITY
                        && metaData.getBoolean("worldReadable");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Could not load metadata (e.g. world readable) for extension.");
        }
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return mBinder;
    }

    private IMindMeExtension.Stub mBinder = new IMindMeExtension.Stub() {
        @Override
        public void onInitialize(IMindMeExtensionHost host, boolean isReconnect)
                throws RemoteException {
            if (!mIsWorldReadable) {
                // If not world readable, check the signature of the [first] package with the given
                // UID against the known-good official MindMe app signature.
                boolean verified = false;
                PackageManager pm = getPackageManager();
                String[] packages = pm.getPackagesForUid(getCallingUid());
                if (packages != null && packages.length > 0) {
                    try {
                        PackageInfo pi = pm.getPackageInfo(packages[0],
                                PackageManager.GET_SIGNATURES);
                        if (pi.signatures != null
                                && pi.signatures.length == 1
                                && (MINDME_SIGNATURE.equals(pi.signatures[0])
                                		|| ALARMPAD_SIGNATURE.equals(pi.signatures[0]))
                                		|| ALARMPADPRO_SIGNATURE.equals(pi.signatures[0])) {
                            verified = true;
                        }
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }
                }

                if (!verified) {
                    Log.e(TAG, "Caller is not an official MindMe app and this "
                            + "extension is not world-readable.");
                    throw new SecurityException("Caller is not an official MindMe app and this "
                            + "extension is not world-readable.");
                }
            }

            mHost = host;

            if (!mInitialized) {
                MindMeExtension.this.onInitialize(isReconnect);
                mInitialized = true;
            }
        }

        @Override
        public void onUpdate(final int reason) throws RemoteException {
            if (!mInitialized) {
                return;
            }

            // Do this in a separate thread
            mServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    MindMeExtension.this.onUpdateData(reason);
                }
            });
        }
    };

    /**
     * Called when a connection with the main app has been established or re-established
     * after a previous one was lost. In this latter case, the parameter <code>isReconnect</code>
     * will be true. Override this method to perform basic extension initialization before calls
     * to {@link #onUpdateData(int)} are made. This method is called on the main thread.
     *
     * @param isReconnect Whether or not this call is being made after a connection was dropped and
     *                    a new connection has been established.
     */
    protected void onInitialize(boolean isReconnect) {
    }
    
    //TODO
    protected void onPreLoadData() {
    	
    }

    /**
     * Called when the main app process is requesting that the extension provide updated
     * information to show to the user. Implementations can choose to do nothing, or more commonly,
     * provide an update using the {@link #publishUpdate(ExtensionData)} method. Note that doing
     * nothing doesn't clear existing data. To clear any existing data, call
     * {@link #publishUpdate(ExtensionData)} with <code>null</code> data. This method is called
     * on a background thread.
     *
     * @param reason The reason for the update. See {@link #UPDATE_REASON_PERIODIC} and related
     *               constants for more details.
     */
    protected abstract void onUpdateData(int reason);

    /**
     * Notifies the main app that new data is available for the extension and should
     * potentially be shown to the user. Note that this call does not necessarily need to be made
     * from inside the {@link #onUpdateData(int)} method, but can be made only after
     * {@link #onInitialize(boolean)} has been called. If you only call this from within
     * {@link #onUpdateData(int)} this is already ensured.
     *
     * @param data The data to show, or <code>null</code> if existing data should be cleared (hiding
     *             the extension from view).
     */
    protected final void publishUpdate(ExtensionData data) {
        try {
            mHost.publishUpdate(data);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't publish updated extension data.", e);
        }
    }

    /**
     * The signature of the official MindMe app (com.thetalkerapp.main). Used to
     * compare caller when {@link #mIsWorldReadable} is false.
     */
    private static final Signature MINDME_SIGNATURE = new Signature("" +
            "308202d1308201b9a003020102020470453c72300d06092a864886f70d01010b05003019311730150603550" +
            "40a130e5468652054616c6b657220417070301e170d3133313132353039313230355a170d34383131313630" +
            "39313230355a301931173015060355040a130e5468652054616c6b65722041707030820122300d06092a864" +
            "886f70d01010105000382010f003082010a0282010100a558157ee9f951c8137fed3b31ec40fc151302f50d" +
            "566521593a6b8e307742f82ba31f46421fd92c0bcdea687bc2f8145b98ca4580773d3148796cce947f85e79" +
            "523baf015aa8a9acf65437b21ef8a8187df10515c0bedbae52c7fa04397d886d89780791c67124d35860489" +
            "286af5b1351b266b81308731542918b9925e7b7e8653ea73963cd56ce473533d1e4f992fce2ee6f5c03b45e" +
            "89de7f852aa8f78538199d4b0c37908f6703b5f6008b948968205a08f6a3f0456ef6342f99c7236772553f4" +
            "b572b0d2acfec0cd2930e0a057602e398e254df8662293eac7acc3387830bf3df0f6349e1a70f46353deb0e" +
            "cf05e349b62cd1c3e9f7d8cbc865e4791610203010001a321301f301d0603551d0e041604142e9c69026bb3" +
            "b25d797b78216008cf6b3be8a9d4300d06092a864886f70d01010b0500038201010099af75ee715a815951a" +
            "185acffd8fc4f484d7d28de48af8be860bac20ae49248e235652ceea9d02d3fc3faca4d06168b6c048ee2e7" +
            "7de3b668c7847e76bacb69157466ebd121d2d695b57a842fd5306caf6db35a427603b3100629c6a2d3a0522" +
            "dbfaf55fb284405ff192fe2793c96deaa818be4cf967a1bfcff0ea2447fe4de2962f37ca3daf50bdb8e5d36" +
            "65d7f38166b2f1e0cd3b8540e214f51e577c73b55541cae5726894c8171f0ecc11c1abf1175c8edc0f2550a" +
            "b740da50ac9084f9eacd643c0f6304fff5feef2b70c37b5350ffc87735c8b364c4fb58564e3a994d1f349b1" +
            "775a902cab12ba9038cdbe30aca798230423f3fd483e461d4d4cff887f");
    
    /**
     * The signature of the official AlarmPad app (com.mindmeapp.alarmpad). Used to
     * compare caller when {@link #mIsWorldReadable} is false.
     */
    private static final Signature ALARMPAD_SIGNATURE = new Signature("" +
            "308202d1308201b9a003020102020470453c72300d06092a864886f70d01010b05003019311730150603550" +
            "40a130e5468652054616c6b657220417070301e170d3133313132353039313230355a170d34383131313630" +
            "39313230355a301931173015060355040a130e5468652054616c6b65722041707030820122300d06092a864" +
            "886f70d01010105000382010f003082010a0282010100a558157ee9f951c8137fed3b31ec40fc151302f50d" +
            "566521593a6b8e307742f82ba31f46421fd92c0bcdea687bc2f8145b98ca4580773d3148796cce947f85e79" +
            "523baf015aa8a9acf65437b21ef8a8187df10515c0bedbae52c7fa04397d886d89780791c67124d35860489" +
            "286af5b1351b266b81308731542918b9925e7b7e8653ea73963cd56ce473533d1e4f992fce2ee6f5c03b45e" +
            "89de7f852aa8f78538199d4b0c37908f6703b5f6008b948968205a08f6a3f0456ef6342f99c7236772553f4" +
            "b572b0d2acfec0cd2930e0a057602e398e254df8662293eac7acc3387830bf3df0f6349e1a70f46353deb0e" +
            "cf05e349b62cd1c3e9f7d8cbc865e4791610203010001a321301f301d0603551d0e041604142e9c69026bb3" +
            "b25d797b78216008cf6b3be8a9d4300d06092a864886f70d01010b0500038201010099af75ee715a815951a" +
            "185acffd8fc4f484d7d28de48af8be860bac20ae49248e235652ceea9d02d3fc3faca4d06168b6c048ee2e7" +
            "7de3b668c7847e76bacb69157466ebd121d2d695b57a842fd5306caf6db35a427603b3100629c6a2d3a0522" +
            "dbfaf55fb284405ff192fe2793c96deaa818be4cf967a1bfcff0ea2447fe4de2962f37ca3daf50bdb8e5d36" +
            "65d7f38166b2f1e0cd3b8540e214f51e577c73b55541cae5726894c8171f0ecc11c1abf1175c8edc0f2550a" +
            "b740da50ac9084f9eacd643c0f6304fff5feef2b70c37b5350ffc87735c8b364c4fb58564e3a994d1f349b1" +
            "775a902cab12ba9038cdbe30aca798230423f3fd483e461d4d4cff887f");   
    
    /**
     * The signature of the official AlarmPad PRO app (com.mindmeapp.alarmpadpro). Used to
     * compare caller when {@link #mIsWorldReadable} is false.
     */
    private static final Signature ALARMPADPRO_SIGNATURE = new Signature(""
    		+ "308202d1308201b9a003020102020470453c72300d06092a864886f70d01010b05003019311730150603"
    		+ "55040a130e5468652054616c6b657220417070301e170d3133313132353039313230355a170d34383131"
    		+ "31363039313230355a301931173015060355040a130e5468652054616c6b65722041707030820122300d"
    		+ "06092a864886f70d01010105000382010f003082010a0282010100a558157ee9f951c8137fed3b31ec40"
    		+ "fc151302f50d566521593a6b8e307742f82ba31f46421fd92c0bcdea687bc2f8145b98ca4580773d3148"
    		+ "796cce947f85e79523baf015aa8a9acf65437b21ef8a8187df10515c0bedbae52c7fa04397d886d89780"
    		+ "791c67124d35860489286af5b1351b266b81308731542918b9925e7b7e8653ea73963cd56ce473533d1e"
    		+ "4f992fce2ee6f5c03b45e89de7f852aa8f78538199d4b0c37908f6703b5f6008b948968205a08f6a3f04"
    		+ "56ef6342f99c7236772553f4b572b0d2acfec0cd2930e0a057602e398e254df8662293eac7acc3387830"
    		+ "bf3df0f6349e1a70f46353deb0ecf05e349b62cd1c3e9f7d8cbc865e4791610203010001a321301f301d"
    		+ "0603551d0e041604142e9c69026bb3b25d797b78216008cf6b3be8a9d4300d06092a864886f70d01010b"
    		+ "0500038201010099af75ee715a815951a185acffd8fc4f484d7d28de48af8be860bac20ae49248e23565"
    		+ "2ceea9d02d3fc3faca4d06168b6c048ee2e77de3b668c7847e76bacb69157466ebd121d2d695b57a842f"
    		+ "d5306caf6db35a427603b3100629c6a2d3a0522dbfaf55fb284405ff192fe2793c96deaa818be4cf967a"
    		+ "1bfcff0ea2447fe4de2962f37ca3daf50bdb8e5d3665d7f38166b2f1e0cd3b8540e214f51e577c73b555"
    		+ "41cae5726894c8171f0ecc11c1abf1175c8edc0f2550ab740da50ac9084f9eacd643c0f6304fff5feef2"
    		+ "b70c37b5350ffc87735c8b364c4fb58564e3a994d1f349b1775a902cab12ba9038cdbe30aca798230423"
    		+ "f3fd483e461d4d4cff887f");
   
}
