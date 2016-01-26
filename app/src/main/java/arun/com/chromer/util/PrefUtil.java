package arun.com.chromer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import arun.com.chromer.R;

/**
 * Created by Arun on 05/01/2016.
 */
public class PrefUtil {
    public static final String PREFERRED_PACKAGE = "preferred_package";
    public static final String TOOLBAR_COLOR = "toolbar_color";
    public static final String TOOLBAR_COLOR_PREF = "toolbar_color_pref";
    public static final String SHOW_TITLE_PREF = "title_pref";
    public static final String ENABLE_ANIMATION = "animations_pref";
    public static final String ANIMATION_TYPE = "animation_preference";
    public static final String FIRST_RUN = "firstrun";
    public static final String WARM_UP = "warm_up_preference";
    public static final String PRE_FETCH = "pre_fetch_preference";
    public static final String WIFI_PREFETCH = "wifi_preference";
    public static final String SECONDARY_PREF = "secondary_preference";
    public static final String DYNAMIC_COLOR = "dynamic_color";
    public static final String CLEAN_DATABASE = "clean_database";
    public static final String DYNAMIC_COLOR_APP = "dynamic_color_app";
    public static final String DYNAMIC_COLOR_WEB = "dynamic_color_web";

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    public static boolean isFirstRun(Context context) {
        if (preferences(context).getBoolean(FIRST_RUN, true)) {
            preferences(context).edit().putBoolean(FIRST_RUN, false).apply();
            return true;
        }
        return false;
    }

    public static boolean isColoredToolbar(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(TOOLBAR_COLOR_PREF, true);
    }

    public static int getToolbarColor(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getInt(TOOLBAR_COLOR,
                        ContextCompat.getColor(context, R.color.colorPrimary));
    }

    public static void setToolbarColor(Context context, int selectedColor) {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putInt("toolbar_color", selectedColor).apply();
    }

    public static boolean isAnimationEnabled(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(ENABLE_ANIMATION, true);
    }

    public static int getAnimationPref(Context context) {
        return Integer.parseInt(PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(ANIMATION_TYPE, "1"));
    }

    public static boolean isShowTitle(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(SHOW_TITLE_PREF, true);
    }

    public static String getPreferredTabApp(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(PREFERRED_PACKAGE, null);
    }

    public static void setPreferredTabApp(Context context, String string) {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(PREFERRED_PACKAGE, string).apply();
    }

    public static String getSecondaryPref(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(SECONDARY_PREF, null);
    }

    public static void setSecondaryPref(Context context, String string) {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(SECONDARY_PREF, string).apply();
    }

    public static boolean isWarmUpPreferred(Context context) {
        if (preferences(context).getBoolean(WARM_UP, false)) {
            return true;
        }
        return false;
    }

    public static void setWarmUpPreference(Context context, boolean preference) {
        preferences(context).edit().putBoolean(WARM_UP, preference).commit();
    }

    public static boolean isPreFetchPrefered(Context context) {
        if (preferences(context).getBoolean(PRE_FETCH, false)) {
            return true;
        }
        return false;
    }

    public static void setPrefetchPreference(Context context, boolean preference) {
        preferences(context).edit().putBoolean(PRE_FETCH, preference).commit();
    }

    public static boolean isWifiPreferred(Context context) {
        if (preferences(context).getBoolean(WIFI_PREFETCH, false)) {
            return true;
        }
        return false;
    }

    public static void setWifiPrefetch(Context context, boolean preference) {
        preferences(context).edit().putBoolean(WIFI_PREFETCH, preference).commit();
    }

    public static boolean isDynamicToolbar(Context context) {
        if (preferences(context).getBoolean(DYNAMIC_COLOR, false)) {
            return true;
        }
        return false;
    }

    public static void setDynamicToolbar(Context context, boolean preference) {
        preferences(context).edit().putBoolean(DYNAMIC_COLOR, preference).commit();
    }

    public static boolean shouldCleanDB(Context context) {
        if (preferences(context).getBoolean(CLEAN_DATABASE, true)) {
            preferences(context).edit().putBoolean(CLEAN_DATABASE, false).apply();
            return true;
        }
        return false;
    }

    public static boolean isDynamicToolbarApp(Context context) {
        if (preferences(context).getBoolean(DYNAMIC_COLOR_APP, false)) {
            return true;
        }
        return false;
    }

    public static void setDynamicToolbarApp(Context context, boolean preference) {
        preferences(context).edit().putBoolean(DYNAMIC_COLOR_APP, preference).commit();
    }

    public static boolean isDynamicToolbarWeb(Context context) {
        if (preferences(context).getBoolean(DYNAMIC_COLOR_WEB, false)) {
            return true;
        }
        return false;
    }

    public static void setDynamicToolbarWeb(Context context, boolean preference) {
        preferences(context).edit().putBoolean(DYNAMIC_COLOR_WEB, preference).commit();
    }

    public static void setDynamicToolbarPref(Context context, boolean app, boolean web) {
        setDynamicToolbarApp(context, app);
        setDynamicToolbarWeb(context, web);
    }

    public static Integer[] getDynTlbrSelection(Context context) {
        if (isDynamicToolbarApp(context) && isDynamicToolbarWeb(context))
            return new Integer[]{0, 1};
        else if (isDynamicToolbarApp(context))
            return new Integer[]{0};
        else if (isDynamicToolbarWeb(context))
            return new Integer[]{1};
        else return null;
    }

    public static void updateAppAndWeb(Context context, Integer[] which) {
        switch (which.length) {
            case 0:
                PrefUtil.setDynamicToolbarPref(context, false, false);
                break;
            case 1:
                if (which[0] == 0) {
                    PrefUtil.setDynamicToolbarPref(context, true, false);
                } else if (which[0] == 1) {
                    PrefUtil.setDynamicToolbarPref(context, false, true);
                }
                break;
            case 2:
                PrefUtil.setDynamicToolbarPref(context, true, true);
                break;
        }
    }
}
