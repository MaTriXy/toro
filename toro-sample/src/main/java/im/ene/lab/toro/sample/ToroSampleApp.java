/*
 * Copyright 2016 eneim@Eneim Labs, nam@ene.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.ene.lab.toro.sample;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.ndk.CrashlyticsNdk;
import im.ene.lab.toro.ext.ToroExt;
import io.fabric.sdk.android.Fabric;

/**
 * Created by eneim on 2/1/16.
 */
public class ToroSampleApp extends Application {

  private static ToroSampleApp sApp;

  @Override public void onCreate() {
    super.onCreate();
    Fabric.with(this, new Crashlytics(), new CrashlyticsNdk());
    // Toro.init(this);
    ToroExt.with(this).youtube().init();
    sApp = this;
  }

  public static SharedPreferences pref() {
    return sApp.getSharedPreferences("toro_pref", Context.MODE_PRIVATE);
  }

  public static String packageName() {
    return sApp.getPackageName();
  }
}