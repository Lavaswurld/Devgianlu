package com.gianlu.commonutils.BottomSheet;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.R;

public abstract class BaseModalBottomSheet<Setup, Update> extends BottomSheetDialogFragment {
    private BottomSheetBehavior behavior;
    private FrameLayout header;
    private FrameLayout body;
    private ProgressBar loading;
    private Toolbar toolbar;
    private FloatingActionButton action;
    private boolean onlyToolbar = false;
    private Setup payload;

    @Nullable
    protected Setup getSetupPayload() {
        return payload;
    }

    /**
     * @return Whether the implementation provides a layout for the header
     */
    protected abstract boolean onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Setup payload);

    protected abstract void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Setup payload);

    protected abstract void onCustomizeToolbar(@NonNull Toolbar toolbar, @NonNull Setup payload);

    /**
     * @return Whether the implementation provides an action
     */
    protected abstract boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull Setup payload);

    @Nullable
    protected LayoutInflater createLayoutInflater(@NonNull Context context, @NonNull Setup payload) {
        return null;
    }

    @NonNull
    protected BottomSheetCallback prepareCallback() {
        return new BottomSheetCallback();
    }

    @Override
    @CallSuper
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Window window = getDialog().getWindow();
        if (window != null) {
            behavior = BottomSheetBehavior.from(window.findViewById(android.support.design.R.id.design_bottom_sheet));
            behavior.setBottomSheetCallback(prepareCallback());
        }
    }

    public final void update(@NonNull Update payload) {
        if (getDialog() != null && getDialog().isShowing()) onRequestedUpdate(payload);
    }

    protected void onRequestedUpdate(@NonNull Update payload) {
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (payload == null) {
            Logging.log(new NullPointerException("Payload is null!"));
            dismiss();
            return null;
        }

        LayoutInflater themedInflater = createLayoutInflater(requireContext(), payload);
        if (themedInflater != null) inflater = themedInflater;

        CoordinatorLayout layout = (CoordinatorLayout) inflater.inflate(R.layout.modal_bottom_sheet, container, false);

        toolbar = layout.findViewById(R.id.modalBottomSheet_toolbar);
        header = layout.findViewById(R.id.modalBottomSheet_header);
        body = layout.findViewById(R.id.modalBottomSheet_body);
        loading = layout.findViewById(R.id.modalBottomSheet_loading);
        action = layout.findViewById(R.id.modalBottomSheet_action);

        onCustomizeToolbar(toolbar, payload);
        onlyToolbar = !onCreateHeader(inflater, header, payload);
        onCreateBody(inflater, body, payload);

        if (onCustomizeAction(action, payload)) {
            action.setVisibility(View.VISIBLE);
            int end = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
            header.setPaddingRelative(header.getPaddingStart(), header.getPaddingTop(), end, header.getPaddingBottom());
        } else {
            action.setVisibility(View.GONE);
        }

        if (onlyToolbar) showToolbar();
        else showHeader();

        return layout;
    }

    public final void show(@Nullable FragmentActivity activity, @NonNull Setup payload) {
        if (activity == null) return;

        this.payload = payload;
        DialogUtils.showDialog(activity, this);
    }

    private void displayClose() {
        toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void hideClose() {
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);
    }

    private void showToolbar() {
        toolbar.setVisibility(View.VISIBLE);
        header.setVisibility(View.GONE);
    }

    private void showHeader() {
        toolbar.setVisibility(View.GONE);
        header.setVisibility(View.VISIBLE);
    }

    public void isLoading(boolean set) {
        if (set) {
            loading.setVisibility(View.VISIBLE);
            body.setVisibility(View.GONE);
        } else {
            loading.setVisibility(View.GONE);
            body.setVisibility(View.VISIBLE);
        }
    }

    private boolean isFullscreen(@NonNull View bottomSheet) {
        int parentHeight = ((View) bottomSheet.getParent()).getHeight();
        int sheetHeight = bottomSheet.getHeight();
        return parentHeight == sheetHeight;
    }

    public class BottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {

        @Override
        @CallSuper
        public void onStateChanged(@NonNull View bottomSheet, @BottomSheetBehavior.State int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) dismiss();

            if (newState == BottomSheetBehavior.STATE_EXPANDED && isFullscreen(bottomSheet))
                displayClose();
            else
                hideClose();

            if (!onlyToolbar) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED && isFullscreen(bottomSheet))
                    showToolbar();
                else if (newState == BottomSheetBehavior.STATE_COLLAPSED)
                    showHeader();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    }
}
