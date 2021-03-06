package arun.com.chromer.activities.blacklist;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import arun.com.chromer.R;
import arun.com.chromer.activities.blacklist.model.App;
import arun.com.chromer.preferences.manager.Preferences;
import arun.com.chromer.util.ServiceUtil;
import arun.com.chromer.util.Utils;
import butterknife.BindView;
import butterknife.ButterKnife;

public class BlacklistManagerActivity extends AppCompatActivity implements
        Blacklist.View,
        BlacklistAdapter.BlackListItemClickedListener,
        CompoundButton.OnCheckedChangeListener, SwipeRefreshLayout.OnRefreshListener {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.app_recyclerview)
    RecyclerView blackListedAppsList;
    @BindView(R.id.coordinator_layout)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    private Blacklist.Presenter presenter;

    private final BlacklistAdapter blacklistAdapter = new BlacklistAdapter(this, this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = new Blacklist.Presenter(this);
        setContentView(R.layout.blacklist_activity);
        ButterKnife.bind(this);
        setupToolbar();

        blackListedAppsList.setLayoutManager(new LinearLayoutManager(this));
        blackListedAppsList.setAdapter(blacklistAdapter);

        loadApps();
    }

    private void loadApps() {
        presenter.loadAppList(this);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.colorPrimaryDarker);
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    protected void onDestroy() {
        presenter.cleanUp();
        blacklistAdapter.cleanUp();
        super.onDestroy();
    }


    private void showSnack(@NonNull String textToSnack) {
        // Have to provide a view for view traversal, so providing the recycler view.
        Snackbar.make(coordinatorLayout, textToSnack, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onBlackListItemClick(App app) {
        presenter.updateBlacklist(app);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.blacklist_menu, menu);
        final MenuItem menuItem = menu.findItem(R.id.blacklist_switch_item);
        if (menuItem != null) {
            final SwitchCompat blackListSwitch = (SwitchCompat) menuItem.getActionView().findViewById(R.id.blacklist_switch);
            if (blackListSwitch != null) {
                final boolean blackListActive = Preferences.blacklist(this) && Utils.canReadUsageStats(this);
                Preferences.blacklist(this, blackListActive);
                blackListSwitch.setChecked(Preferences.blacklist(this));
                blackListSwitch.setOnCheckedChangeListener(this);
            }
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void requestUsagePermission() {
        new MaterialDialog.Builder(this)
                .title(R.string.permission_required)
                .content(R.string.usage_permission_explanation_blacklist)
                .positiveText(R.string.grant)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    }
                }).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // For finishing activity on clicking up caret
            finishWithTransition();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finishWithTransition();
        super.onBackPressed();
    }

    private void finishWithTransition() {
        finish();
        overridePendingTransition(R.anim.slide_in_left_medium, R.anim.slide_out_right_medium);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked && !Utils.canReadUsageStats(getApplicationContext())) {
            buttonView.setChecked(false);
            requestUsagePermission();
        } else {
            showSnack(isChecked ? getString(R.string.blacklist_on) : getString(R.string.blacklist_off));
            Preferences.blacklist(getApplicationContext(), isChecked);
            ServiceUtil.takeCareOfServices(getApplicationContext());
        }
    }

    @Override
    public void setApps(@NonNull List<App> apps) {
        blacklistAdapter.setApps(apps);
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        swipeRefreshLayout.setRefreshing(refreshing);
    }

    @Override
    public void onRefresh() {
        loadApps();
    }
}
