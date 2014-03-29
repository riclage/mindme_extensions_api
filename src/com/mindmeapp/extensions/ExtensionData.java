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

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.widget.RemoteViews;

/**
 * A parcelable, serializable object representing data related to a {@link MindMeExtension} that
 * should be shown to the user.
 * 
 * <p>
 * This class follows the <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
 * interface</a> style, using method chaining to provide for more readable code. For example, to set
 * the status and visibility of this data, use {@link #statusToDisplay(String)} and {@link #visible(boolean)}
 * methods like so:
 *
 * <pre class="prettyprint">
 * ExtensionData data = new ExtensionData();
 * data.visible(true).statusToDisplay("hello");
 * </pre>
 *
 * Conversely, to get the status, use {@link #statusToDisplay()()}. Setters and getters are thus overloads
 * (or overlords?) of the same method.
 *
 * <h3>Required fields</h3>
 *
 * While no fields are required, if the data is 'visible' (i.e. {@link #visible(boolean)} has been
 * called with <code>true</code>, at least the following fields should be populated:
 *
 * <ul>
 * <li>{@link #icon(int)}</li>
 * <li>{@link #statusToDisplay(String)}</li>
 * </ul>
 *
 * <p>Awesome extensions will also set {@link #statusToSpeak(String)} and {@link #languageToSpeak(Locale)}.
 * Both fields must be set if you want the app to speak the status to the user.</p>
 *
 * <p>Really Awesome extensions will also set {@link #viewsToDisplay()}. It must contain a {@link RemoteViews}
 * object with a layout to be displayed to the user on a tab. This layout can be anything you want as long
 * as it is supported by RemoteViews. Refer to the documentation on 
 * <a href="https://developer.android.com/guide/topics/appwidgets/index.html#CreatingLayout">Creating the App Widget Layout</a> 
 * for more information on what you can add to these views.
 * </p>
 * 
 * Original code from: <a href="https://code.google.com/p/dashclock/">https://code.google.com/p/dashclock/</a>
 *
 * @see MindMeExtension#publishUpdate(ExtensionData)
 */
public class ExtensionData implements Parcelable {
	
    /**
     * Since there might be a case where new versions of the app use extensions running
     * old versions of the protocol (and thus old versions of this class), we need a versioning
     * system for the parcels sent between the core app and its extensions.
     */
    public static final int PARCELABLE_VERSION = 1;

    private static final String KEY_VISIBLE = "visible";
    private static final String KEY_ICON = "icon";
    private static final String KEY_ICON_URI = "icon_uri";
    private static final String KEY_STATUS_TO_DISPLAY = "status_to_display";
    private static final String KEY_STATUS_TO_SPEAK = "status_to_speak";
    private static final String KEY_LANGUAGE_TO_SPEAK = "language_to_speak";
    private static final String KEY_VIEWS_TO_DISPLAY = "views_to_display";
    private static final String KEY_CONTENT_DESCRIPTION = "content_description";
    private static final String KEY_BACKGROUND = "background";
    private static final String KEY_BACKGROUND_URI = "background_uri";
    
    /**
     * These keys are used when serializing the Locale object in its decomposed elements
     */
    private static final String KEY_LOCALE_LANGUAGE = "locale_language";
    private static final String KEY_LOCALE_COUNTRY = "locale_country";

    /**
     * The maximum length for {@link #statusToDisplay(String)}. Enforced by {@link #clean()}.
     */
    public static final int MAX_STATUS_TO_DISPLAY_LENGTH = 200;
    
    /**
     * The maximum length for {@link #statusToSpeak(String)}. Enforced by {@link #clean()}.
     */
    public static final int MAX_STATUS_TO_SPEAK_LENGTH = 500;    

    /**
     * The maximum length for {@link #contentDescription(String)}. Enforced by {@link #clean()}.
     */
    public static final int MAX_CONTENT_DESCRIPTION_LENGTH = 32 +
            MAX_STATUS_TO_DISPLAY_LENGTH + MAX_STATUS_TO_SPEAK_LENGTH;

    private boolean mVisible = false;
    private int mIcon = 0;
    private Uri mIconUri = null;
    private String mStatusToDisplay = null;
    private String mStatusToSpeak = null;
    private Locale mLanguageToSpeak = null;
    private RemoteViews mViewsToDisplay = null;
    private String mContentDescription = null;
    private int mBackground = 0;
    private Uri mBackgroundUri = null;

    public ExtensionData() {
    }

    /**
     * Returns whether or not the relevant extension should be visible (whether or not there is
     * relevant information to show to the user about the extension). Default false.
     */
    public boolean visible() {
        return mVisible;
    }

    /**
     * Sets whether or not the relevant extension should be visible (whether or not there is
     * relevant information to show to the user about the extension). Default false.
     */
    public ExtensionData visible(boolean visible) {
        mVisible = visible;
        return this;
    }

    /**
     * Returns the ID of the drawable resource within the extension's package that represents this
     * data. Default 0.
     */
    public int icon() {
        return mIcon;
    }

    /**
     * Sets the ID of the drawable resource within the extension's package that represents this
     * data. The icon should be entirely white, with alpha, and about 48x48 dp. It will be
     * scaled down as needed. If there is no contextual icon representation of the data, simply
     * use the extension or app icon. If an {@link #iconUri(Uri) iconUri} is provided, it
     * will take precedence over this value. Default 0.
     *
     * @see #iconUri(Uri)
     */
    public ExtensionData icon(int icon) {
        mIcon = icon;
        return this;
    }

    /**
     * Returns the content:// URI of a bitmap representing this data. Default null.
     */
    public Uri iconUri() {
        return mIconUri;
    }

    /**
     * Sets the content:// URI of the bitmap representing this data. This takes precedence over
     * the regular {@link #icon(int) icon resource ID} if set. This resource will be loaded
     * using {@link android.content.ContentResolver#openFileDescriptor(android.net.Uri, String)} and
     * {@link android.graphics.BitmapFactory#decodeFileDescriptor(java.io.FileDescriptor)}. See the
     * {@link #icon(int) icon} method for guidelines on the styling of this bitmap.
     */
    public ExtensionData iconUri(Uri iconUri) {
        mIconUri = iconUri;
        return this;
    }
    
    /**
     * Returns the ID of the drawable resource within the extension's package to be used as 
     * background on display. Default 0.
     */
    public int background() {
        return mBackground;
    }

    /**
     * Sets the ID of the drawable resource within the extension's package that represents a
     * background for the display. If this drawable is an image, there is no way to guarantee
     * that it will fit all devices so it might be scaled or cropped to fit. 
     * If an {@link #backgroundUri(Uri) backgroundUri} is provided, it
     * will take precedence over this value. Default 0.
     *
     * @see #iconUri(Uri)
     */
    public ExtensionData background(int background) {
        mBackground = background;
        return this;
    }

    /**
     * Returns the content:// URI of a bitmap to be used as background on display. Default null.
     */
    public Uri backgroundUri() {
        return mBackgroundUri;
    }

    /**
     * Sets the content:// URI of the bitmap representing a background to display. This takes precedence over
     * the regular {@link #background(int) background resource ID} if set. This resource will be loaded
     * using {@link android.content.ContentResolver#openFileDescriptor(android.net.Uri, String)} and
     * {@link android.graphics.BitmapFactory#decodeFileDescriptor(java.io.FileDescriptor)}. See the
     * {@link #background(int) background} method for guidelines on the styling of this bitmap.
     */
    public ExtensionData backgroundUri(Uri backgroundUri) {
    	mBackgroundUri = backgroundUri;
        return this;
    }    

    /**
     * Returns the string to be displayed to the user.
     * Default null.
     */
    public String statusToDisplay() {
        return mStatusToDisplay;
    }

    /**
     * Sets the status to display to the user. This should be a short message that the user can quickly
     * look at and click on if {@link #viewsToDisplay(RemoteViews)} is also set. For example, 
     * if you are building a weather forecast extension, this status should provide a quick description of 
     * the current forecast. You can then provide a {@link RemoteViews} object with {@link #viewsToDisplay(RemoteViews)}
     * where the user can see more detailed weather information.
     * Default null.
     */
    public ExtensionData statusToDisplay(String status) {
        mStatusToDisplay = status;
        return this;
    }
    
    public String statusToSpeak() {
        return mStatusToSpeak;
    }

    /**
     * Sets the status to be spoken to the user. This should be a short message that the user can quickly
     * listen to. You can set here the same status as in {@link #statusToDisplay(String)} but keep in mind
     * that not all Text-to-Speech engine parse text in the same manner. For example, if you want the app
     * to speak the time, it is safer to pass it like you want it to be spoken. So instead of "09:00", you
     * may set "9 o'clock" as the status. Also note that if you set a status to speak, you must also set
     * {@link #languageToSpeak(Locale)}. If you don't, your status to be spoken will be ignored.
     * Default null.
     */  
    public ExtensionData statusToSpeak(String status) {
        mStatusToSpeak = status;
        return this;
    } 
    
    public Locale languageToSpeak() {
    	return mLanguageToSpeak;
    }
    
    /**
     * Defines the language the status to speak set in {@link #statusToSpeak(String)} will be spoken in.
     * @param locale
     * @return
     */
    public ExtensionData languageToSpeak(Locale locale) {
        mLanguageToSpeak = locale;
        return this;
    }
    
    public RemoteViews viewsToDisplay() {
    	return mViewsToDisplay;
    }
    
    /**
     * Defines a {@link RemoteViews} object that will be displayed to the user in full-screen on tab once the user clicks
     * on the tab icon or on the {@link #statusToDisplay()}. This layout can be anything you want as long
     * 	as it is supported by RemoteViews. Refer to the documentation on 
     * <a href="https://developer.android.com/guide/topics/appwidgets/index.html#CreatingLayout">Creating the App Widget Layout</a> 
     * for more information on what you can add to these views.
     */
    public ExtensionData viewsToDisplay(RemoteViews views) {
        mViewsToDisplay = views;
        return this;
    }    

    /**
     * Returns the content description for this data, used for accessibility purposes.
     */
    public String contentDescription() {
        return mContentDescription;
    }

    /**
     * Sets the content description for this data. This content description will replace the
     * {@link #status()}, {@link #expandedTitle()} and {@link #expandedBody()} for accessibility
     * purposes.
     *
     * @see android.view.View#setContentDescription(CharSequence)
     * @since Protocol Version 2 (API v2.x)
     */
    public ExtensionData contentDescription(String contentDescription) {
        mContentDescription = contentDescription;
        return this;
    }
    
    /**
     * Serializes the contents of this object to JSON.
     */
    public JSONObject serialize() throws JSONException {
        JSONObject data = new JSONObject();
        data.put(KEY_VISIBLE, mVisible);
        data.put(KEY_ICON, mIcon);
        data.put(KEY_ICON_URI, (mIconUri == null ? null : mIconUri.toString()));
        data.put(KEY_STATUS_TO_DISPLAY, mStatusToDisplay);
        data.put(KEY_STATUS_TO_SPEAK, mStatusToSpeak);
        data.put(KEY_CONTENT_DESCRIPTION, mContentDescription);
        data.put(KEY_BACKGROUND, mBackground);
        data.put(KEY_BACKGROUND_URI, (mBackgroundUri == null ? null : mBackgroundUri.toString()));
        
        //Decompose Locale object
        data.put(KEY_LOCALE_LANGUAGE, mLanguageToSpeak.getLanguage());
        data.put(KEY_LOCALE_COUNTRY, mLanguageToSpeak.getCountry());
        
        return data;
    }
    
    /**
     * Deserializes the given JSON representation of extension data, populating this
     * object.
     */
    public void deserialize(JSONObject data) throws JSONException {
        this.mVisible = data.optBoolean(KEY_VISIBLE);
        this.mIcon = data.optInt(KEY_ICON);
        String iconUriString = data.optString(KEY_ICON_URI);
        this.mIconUri = TextUtils.isEmpty(iconUriString) ? null : Uri.parse(iconUriString);
        this.mStatusToDisplay = data.optString(KEY_STATUS_TO_DISPLAY);
        this.mStatusToSpeak = data.optString(KEY_STATUS_TO_SPEAK);
        this.mContentDescription = data.optString(KEY_CONTENT_DESCRIPTION);
        this.mBackground = data.optInt(KEY_BACKGROUND);
        String backgroundUriString = data.optString(KEY_BACKGROUND_URI);
        this.mBackgroundUri = TextUtils.isEmpty(backgroundUriString) ? null : Uri.parse(backgroundUriString);
        
        //Build back the Locale object
        String language = data.optString(KEY_LOCALE_LANGUAGE);
        String country = data.optString(KEY_LOCALE_COUNTRY);
        if (TextUtils.isEmpty(country)) {
        	this.mLanguageToSpeak = new Locale(language);
        } else {
        	this.mLanguageToSpeak = new Locale(language, country);
        }
        
    }    

    /**
     * Serializes the contents of this object to a {@link Bundle}.
     */
    public Bundle toBundle() {
        Bundle data = new Bundle();
        data.putBoolean(KEY_VISIBLE, mVisible);
        data.putInt(KEY_ICON, mIcon);
        data.putString(KEY_ICON_URI, (mIconUri == null ? null : mIconUri.toString()));
        data.putString(KEY_STATUS_TO_DISPLAY, mStatusToDisplay);
        data.putString(KEY_STATUS_TO_SPEAK, mStatusToSpeak);
        data.putParcelable(KEY_VIEWS_TO_DISPLAY, mViewsToDisplay);
        data.putString(KEY_CONTENT_DESCRIPTION, mContentDescription);
        data.putSerializable(KEY_LANGUAGE_TO_SPEAK, mLanguageToSpeak);
        data.putInt(KEY_BACKGROUND, mBackground);
        data.putString(KEY_BACKGROUND_URI, (mBackgroundUri == null ? null : mBackgroundUri.toString()));
        return data;
    }

    /**
     * Deserializes the given {@link Bundle} representation of extension data, populating this
     * object.
     */
    public void fromBundle(Bundle src) {
        this.mVisible = src.getBoolean(KEY_VISIBLE, true);
        this.mIcon = src.getInt(KEY_ICON);
        String iconUriString = src.getString(KEY_ICON_URI);
        this.mIconUri = TextUtils.isEmpty(iconUriString) ? null : Uri.parse(iconUriString);
        this.mStatusToDisplay = src.getString(KEY_STATUS_TO_DISPLAY);
        this.mStatusToSpeak = src.getString(KEY_STATUS_TO_SPEAK);
        this.mLanguageToSpeak = (Locale) src.getSerializable(KEY_LANGUAGE_TO_SPEAK);
        this.mViewsToDisplay = src.getParcelable(KEY_VIEWS_TO_DISPLAY);
        this.mContentDescription = src.getString(KEY_CONTENT_DESCRIPTION);
        this.mBackground = src.getInt(KEY_BACKGROUND);
        String backgroundUriString = src.getString(KEY_BACKGROUND_URI);
        this.mBackgroundUri = TextUtils.isEmpty(backgroundUriString) ? null : Uri.parse(backgroundUriString);
    }

    /**
     * @see Parcelable
     */
    public static final Creator<ExtensionData> CREATOR
            = new Creator<ExtensionData>() {
        public ExtensionData createFromParcel(Parcel in) {
            return new ExtensionData(in);
        }

        public ExtensionData[] newArray(int size) {
            return new ExtensionData[size];
        }
    };

    private ExtensionData(Parcel in) {
        int parcelableVersion = in.readInt();
        
        this.mVisible = (in.readInt() != 0);
        
        this.mIcon = in.readInt();
        String iconUriString = in.readString();
        this.mIconUri = TextUtils.isEmpty(iconUriString) ? null : Uri.parse(iconUriString);

        this.mStatusToDisplay = in.readString();
        if (TextUtils.isEmpty(this.mStatusToDisplay)) {
        	this.mStatusToDisplay = null;
        }
        this.mStatusToSpeak = in.readString();
        if (TextUtils.isEmpty(this.mStatusToSpeak)) {
        	this.mStatusToSpeak = null;
        }
        this.mLanguageToSpeak = (Locale) in.readSerializable();
        this.mViewsToDisplay = in.readParcelable(RemoteViews.class.getClassLoader());
        
        this.mContentDescription = in.readString();
        if (TextUtils.isEmpty(this.mContentDescription)) {
        	this.mContentDescription = null;
        }
        
        this.mBackground = in.readInt();
        String backgroundUriString = in.readString();
        this.mBackgroundUri = TextUtils.isEmpty(backgroundUriString) ? null : Uri.parse(backgroundUriString);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        /**
         * NOTE: When adding fields in the process of updating this API, make sure to bump
         * {@link #PARCELABLE_VERSION}.
         */
        parcel.writeInt(PARCELABLE_VERSION);
        // Version 1 below
        parcel.writeInt(mVisible ? 1 : 0);
        parcel.writeInt(mIcon);
        parcel.writeString(mIconUri == null ? "" : mIconUri.toString());
        parcel.writeString(TextUtils.isEmpty(mStatusToDisplay) ? "" : mStatusToDisplay);
        parcel.writeString(TextUtils.isEmpty(mStatusToSpeak) ? "" : mStatusToSpeak);
        parcel.writeSerializable(mLanguageToSpeak);
        parcel.writeParcelable(mViewsToDisplay, i);
        parcel.writeString(TextUtils.isEmpty(mContentDescription) ? "" : mContentDescription);
        parcel.writeInt(mBackground);
        parcel.writeString(mBackgroundUri == null ? "" : mBackgroundUri.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        try {
            ExtensionData other = (ExtensionData) o;
            return other.mVisible == mVisible
                    && other.mIcon == mIcon
                    && objectEquals(other.mIconUri, mIconUri)
                    && TextUtils.equals(other.mStatusToDisplay, mStatusToDisplay)
                    && TextUtils.equals(other.mStatusToSpeak, mStatusToSpeak)
                    && objectEquals(other.mLanguageToSpeak, mLanguageToSpeak)
                    && objectEquals(other.mViewsToDisplay, mViewsToDisplay)
                    && TextUtils.equals(other.mContentDescription, mContentDescription)
                    && other.mBackground == mBackground
                    && objectEquals(other.mBackgroundUri, mBackgroundUri);

        } catch (ClassCastException e) {
            return false;
        }
    }

    private static boolean objectEquals(Object x, Object y) {
        if (x == null || y == null) {
            return x == y;
        } else {
            return x.equals(y);
        }
    }

    /**
     * Returns true if the two provided data objects are equal (or both null).
     */
    public static boolean equals(ExtensionData x, ExtensionData y) {
        if (x == null || y == null) {
            return x == y;
        } else {
            return x.equals(y);
        }
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    /**
     * Cleans up this object's data according to the size limits described by
     * {@link #MAX_STATUS_TO_DISPLAY_LENGTH}, {@link #MAX_EXPANDED_TITLE_LENGTH}, etc.
     */
    public void clean() {
        if (!TextUtils.isEmpty(mStatusToDisplay)
                && mStatusToDisplay.length() > MAX_STATUS_TO_DISPLAY_LENGTH) {
            mStatusToDisplay = mStatusToDisplay.substring(0, MAX_STATUS_TO_DISPLAY_LENGTH);
        }
        if (!TextUtils.isEmpty(mStatusToSpeak)
                && mStatusToSpeak.length() > MAX_STATUS_TO_DISPLAY_LENGTH) {
        	mStatusToSpeak = mStatusToSpeak.substring(0, MAX_STATUS_TO_DISPLAY_LENGTH);
        }
        if (!TextUtils.isEmpty(mContentDescription)
                && mContentDescription.length() > MAX_CONTENT_DESCRIPTION_LENGTH) {
            mContentDescription = mContentDescription.substring(0, MAX_CONTENT_DESCRIPTION_LENGTH);
        }
    }
}
