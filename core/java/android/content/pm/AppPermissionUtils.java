/*
 * Copyright (C) 2022 GrapheneOS
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

package android.content.pm;

import android.Manifest;
import android.app.compat.gms.GmsCompat;
import android.util.Log;

import com.android.internal.app.StorageScopesAppHooks;
import com.android.internal.gmscompat.GmsHooks;

import static android.content.pm.GosPackageState.*;

/** @hide */
public class AppPermissionUtils {

    // android.app.ApplicationPackageManager#checkPermission(String permName, String pkgName)
    // android.app.ContextImpl#checkPermission(String permission, int pid, int uid)
    public static boolean shouldSpoofSelfCheck(String permName) {
        if (Manifest.permission.INTERNET.equals(permName)
                && SpecialRuntimePermAppUtils.requestsInternetPermission()
                && !SpecialRuntimePermAppUtils.awareOfRuntimeInternetPermission())
        {
            return true;
        }

        if (GmsCompat.isEnabled()) {
            if (GmsHooks.config().shouldSpoofSelfPermissionCheck(permName)) {
                return true;
            }
        }

        return false;
    }
}
