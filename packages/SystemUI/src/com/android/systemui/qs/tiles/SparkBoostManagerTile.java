/*
 * Copyright (C) 2023 riceDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.systemui.R;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.SettingObserver;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.spark.SparkSystemManager;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.util.settings.SystemSettings;

import java.time.format.DateTimeFormatter;
import java.time.LocalTime;

import javax.inject.Inject;

public class SparkBoostManagerTile extends SecureQSTile<QSTile.BooleanState> {

    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_outline_bolt);
    
    private static final ComponentName MISC_SETTINGS_COMPONENT = new ComponentName(
            "com.android.settings", "com.android.settings.Settings$SparkManagerSettingsActivity");

    private static final Intent MISC_SETTINGS =
            new Intent().setComponent(MISC_SETTINGS_COMPONENT);

    private final SettingObserver mSetting;

    @Inject
    public SparkBoostManagerTile(
            QSHost host,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            SystemSettings systemSettings,
            KeyguardStateController keyguardStateController
    ) {
        super(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger, keyguardStateController);

        mSetting = new SettingObserver(systemSettings, mHandler, Settings.System.SPARK_SYSTEM_BOOST) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };
        SettingsObserver settingsObserver = new SettingsObserver(mainHandler);
        settingsObserver.observe();
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick(@Nullable View view, boolean keyguardShowing) {
        if (checkKeyguard(view, keyguardShowing)) {
            return;
        }
        setEnabled(!mState.value);
        refreshState();
    }

    private void setEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SPARK_SYSTEM_BOOST, enabled ? 1 : 0);
	SparkSystemManager.startBoostingService(enabled);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final int value = arg instanceof Integer ? (Integer) arg : mSetting.getValue();
        final boolean sysManagerState = value != 0;
        final boolean sysManagerEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SPARK_SYSTEM_MANAGER, 0, UserHandle.USER_CURRENT) == 1;
        final boolean sysBoostManagerEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SPARK_SYSTEM_BOOST, 0, UserHandle.USER_CURRENT) == 1;

        state.value = sysManagerState;
        state.label = mContext.getString(R.string.quick_settings_spark_boost_mode_label);
        state.icon = mIcon;
        state.contentDescription = TextUtils.isEmpty(state.secondaryLabel)
                ? state.label
                : TextUtils.concat(state.label, ", ", state.secondaryLabel);
	state.secondaryLabel = sysManagerEnabled ? mContext.getResources().getString(sysBoostManagerEnabled ? R.string.quick_settings_spark_boost_mode_enabled : R.string.quick_settings_spark_boost_mode_disabled) : mContext.getResources().getString(R.string.quick_settings_spark_services_unavailable);
        state.state = sysManagerEnabled ? (state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE) : Tile.STATE_UNAVAILABLE;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SPARK_QS_TILES;
    }

    @Override
    public Intent getLongClickIntent() {
        return MISC_SETTINGS;
    }

    @Override
    protected void handleSetListening(boolean listening) {
    }

    @Override
    public CharSequence getTileLabel() {
        return getState().label;
    }

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SPARK_SYSTEM_BOOST), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SPARK_SYSTEM_MANAGER), false, this,
                    UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange) {
            refreshState();
        }
    }
}
