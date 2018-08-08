package com.gianlu.commonutils.Preferences;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.danielstone.materialaboutlibrary.MaterialAboutFragment;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItemOnClickAction;
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.FossUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.LogsActivity;
import com.gianlu.commonutils.R;
import com.gianlu.commonutils.Toaster;

import java.util.Calendar;
import java.util.List;

public abstract class BasePreferenceActivity extends ActivityWithDialog implements MaterialAboutPreferenceItem.Listener, PreferencesBillingHelper.Listener {
    private PreferencesBillingHelper billingHelper;

    private static void openLink(Context context, String uri) {
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
    }

    @Override
    protected final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_preference);

        ActionBar bar = getSupportActionBar();
        if (bar != null) bar.setDisplayHomeAsUpEnabled(true);

        showMainFragment();
    }

    private void showMainFragment() {
        setTitle(R.string.preferences);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.basePreference, MainFragment.get(), MainFragment.class.getName())
                .commit();
    }

    @Override
    @CallSuper
    protected void onStart() {
        super.onStart();

        if (billingHelper == null && FossUtils.hasGoogleBilling()) {
            billingHelper = new PreferencesBillingHelper(this, "donation.lemonade",
                    "donation.coffee",
                    "donation.hamburger",
                    "donation.pizza",
                    "donation.sushi",
                    "donation.champagne");
            billingHelper.onStart(this);
        }
    }

    @CallSuper
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public final void onBackPressed() {
        Fragment main = getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName());
        if (main == null) showMainFragment();
        else super.onBackPressed();
    }

    @Override
    public final void onPreferenceSelected(@NonNull Class<? extends BasePreferenceFragment> clazz) {
        try {
            BasePreferenceFragment fragment = clazz.newInstance();
            String tag = fragment.getClass().getName();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.basePreference, fragment, tag)
                    .commit();

            setTitle(fragment.getTitleRes());
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void donate() {
        if (billingHelper != null) billingHelper.donate(this, false);
    }

    @NonNull
    protected abstract List<MaterialAboutPreferenceItem> getPreferencesItems();

    @DrawableRes
    protected abstract int getAppIconRes();

    public static class MainFragment extends MaterialAboutFragment {
        private BasePreferenceActivity parent;
        private MaterialAboutPreferenceItem.Listener listener;

        @NonNull
        public static MainFragment get() {
            return new MainFragment();
        }

        @Override
        protected int getTheme() {
            return R.style.MaterialAbout_Default;
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            parent = (BasePreferenceActivity) context;
            listener = (MaterialAboutPreferenceItem.Listener) context;
        }

        @Override
        protected MaterialAboutList getMaterialAboutList(final Context context) {
            MaterialAboutCard developer = new MaterialAboutCard.Builder()
                    .title(R.string.about_app)
                    .addItem(new MaterialAboutTitleItem(R.string.app_name, 0, parent.getAppIconRes())
                            .setDesc(getString(R.string.devgianluCopyright, Calendar.getInstance().get(Calendar.YEAR))))
                    .addItem(new MaterialAboutVersionItem(context))
                    .build();

            // TODO: Developer name and email
            // TODO: Disable analytics

            MaterialAboutCard.Builder preferencesBuilder = null;
            List<MaterialAboutPreferenceItem> preferencesItems = parent.getPreferencesItems();
            if (!preferencesItems.isEmpty()) {
                preferencesBuilder = new MaterialAboutCard.Builder()
                        .title(R.string.preferences);

                for (MaterialAboutPreferenceItem item : preferencesItems) {
                    preferencesBuilder.addItem(item);
                    item.listener = listener;
                }
            }

            MaterialAboutCard logs = new MaterialAboutCard.Builder()
                    .title(R.string.logs)
                    .addItem(new MaterialAboutActionItem.Builder()
                            .icon(R.drawable.baseline_announcement_24)
                            .text(R.string.logs)
                            .setOnClickAction(new MaterialAboutItemOnClickAction() {
                                @Override
                                public void onClick() {
                                    startActivity(new Intent(context, LogsActivity.class));
                                }
                            }).build())
                    .addItem(new MaterialAboutActionItem.Builder()
                            .icon(R.drawable.baseline_delete_24)
                            .text(R.string.deleteAllLogs)
                            .setOnClickAction(new MaterialAboutItemOnClickAction() {
                                @Override
                                public void onClick() {
                                    Logging.deleteAllLogs(context);
                                    DialogUtils.showToast(getActivity(), Toaster.build().message(R.string.logDeleted));
                                }
                            }).build())
                    .build();

            MaterialAboutCard.Builder donateBuilder = new MaterialAboutCard.Builder()
                    .title(R.string.rateDonate);
            if (FossUtils.hasGoogleBilling()) {
                donateBuilder.addItem(new MaterialAboutActionItem.Builder()
                        .text(R.string.rateApp)
                        .subText(R.string.leaveReview)
                        .setOnClickAction(new MaterialAboutItemOnClickAction() {
                            @Override
                            public void onClick() {
                                try {
                                    openLink(context, "market://details?id=" + context.getPackageName());
                                } catch (android.content.ActivityNotFoundException ex) {
                                    openLink(context, "https://play.google.com/store/apps/details?id=" + context.getPackageName());
                                }
                            }
                        }).build())
                        .addItem(new MaterialAboutActionItem.Builder()
                                .text(R.string.donateGoogle)
                                .subText(R.string.donateGoogleSummary)
                                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                                    @Override
                                    public void onClick() {
                                        if (parent != null) parent.donate();
                                    }
                                }).build());
            }
            donateBuilder.addItem(new MaterialAboutActionItem.Builder()
                    .text(R.string.donatePaypal)
                    .subText(R.string.donatePaypalSummary)
                    .setOnClickAction(new MaterialAboutItemOnClickAction() {
                        @Override
                        public void onClick() {
                            openLink(context, "https://paypal.me/devgianlu");
                        }
                    }).build());


            // TODO: Third-part projects
            // TODO: Tutorial stuff

            MaterialAboutList.Builder listBuilder = new MaterialAboutList.Builder();
            listBuilder.addCard(developer);
            if (preferencesBuilder != null) listBuilder.addCard(preferencesBuilder.build());
            listBuilder.addCard(donateBuilder.build());
            listBuilder.addCard(logs);
            return listBuilder.build();
        }
    }
}