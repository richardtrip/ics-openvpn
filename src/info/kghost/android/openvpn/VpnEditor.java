package info.kghost.android.openvpn;

import info.kghost.android.openvpn.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

/**
 * The activity class for editing a new or existing VPN profile.
 */
public class VpnEditor extends PreferenceActivity {
    private static final int MENU_SAVE = Menu.FIRST;
    private static final int MENU_CANCEL = Menu.FIRST + 1;
    private static final int CONFIRM_DIALOG_ID = 0;
    private static final String KEY_PROFILE = "profile";

    private static final String TAG = VpnEditor.class.getSimpleName();

    private VpnProfileEditor mProfileEditor;
    private boolean mAddingProfile;
    private byte[] mOriginalProfileData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VpnProfile p = (VpnProfile) ((savedInstanceState == null)
                ? getIntent().getParcelableExtra(VpnSettings.KEY_VPN_PROFILE)
                : savedInstanceState.getParcelable(KEY_PROFILE));
        mProfileEditor = getEditor(p);
        mAddingProfile = TextUtils.isEmpty(p.getName());

        // Loads the XML preferences file
        addPreferencesFromResource(R.xml.vpn_edit);

        initViewFor(p);

        Parcel parcel = Parcel.obtain();
        p.writeToParcel(parcel, 0);
        mOriginalProfileData = parcel.marshall();
    }

    @Override
    protected synchronized void onSaveInstanceState(Bundle outState) {
        if (mProfileEditor == null) return;

        outState.putParcelable(KEY_PROFILE, getProfile());
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        System.out.println("ON ACTIVITY RESULT req=" + requestCode );
        if ((resultCode == RESULT_CANCELED) || (data == null)) {
            Log.d(TAG, "no result returned by editor");
            return;
        }
        mProfileEditor.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_SAVE, 0, R.string.vpn_menu_done)
            .setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, MENU_CANCEL, 0,
                mAddingProfile ? R.string.vpn_menu_cancel
                               : R.string.vpn_menu_revert)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        mProfileEditor.onCreateOptionsMenu(menu, MENU_CANCEL);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SAVE:
                if (validateAndSetResult()) finish();
                return true;

            case MENU_CANCEL:
                if (profileChanged()) {
                    showDialog(CONFIRM_DIALOG_ID);
                } else {
                    finish();
                }
                return true;
            default:
            if (mProfileEditor.onOptionsItemSelected(this, item))
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (validateAndSetResult()) finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initViewFor(VpnProfile profile) {
        setTitle(profile);
        mProfileEditor.loadPreferencesTo(this, getPreferenceScreen());
    }

    private void setTitle(VpnProfile profile) {
        String formatString = mAddingProfile
                ? getString(R.string.vpn_edit_title_add)
                : getString(R.string.vpn_edit_title_edit);
        setTitle(String.format(formatString, "OpenVPN"));
    }

    /**
     * Checks the validity of the inputs and set the profile as result if valid.
     * @return true if the result is successfully set
     */
    private boolean validateAndSetResult() {
        String errorMsg = mProfileEditor.validate();

        if (errorMsg != null) {
            Util.showErrorMessage(this, errorMsg);
            return false;
        }

        if (profileChanged()) setResult(getProfile());
        return true;
    }

    private void setResult(VpnProfile p) {
        Intent intent = new Intent(this, VpnSettings.class);
        intent.putExtra(VpnSettings.KEY_VPN_PROFILE, (Parcelable) p);
        setResult(RESULT_OK, intent);
    }

    private VpnProfileEditor getEditor(VpnProfile p) {
        return new OpenvpnEditor((OpenvpnProfile) p);
    }


    @Override
    protected Dialog onCreateDialog(int id) {

        if (id == CONFIRM_DIALOG_ID) {
            return new AlertDialog.Builder(this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(mAddingProfile
                            ? R.string.vpn_confirm_add_profile_cancellation
                            : R.string.vpn_confirm_edit_profile_cancellation)
                    .setPositiveButton(R.string.vpn_yes_button,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int w) {
                                    finish();
                                }
                            })
                    .setNegativeButton(R.string.vpn_mistake_button, null)
                    .create();
        }

        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        if (id == CONFIRM_DIALOG_ID) {
            ((AlertDialog)dialog).setMessage(mAddingProfile
                    ? getString(R.string.vpn_confirm_add_profile_cancellation)
                    : getString(R.string.vpn_confirm_edit_profile_cancellation));
        }
    }

    private VpnProfile getProfile() {
        return mProfileEditor.getProfile();
    }

    private boolean profileChanged() {
        Parcel newParcel = Parcel.obtain();
        getProfile().writeToParcel(newParcel, 0);
        byte[] newData = newParcel.marshall();
        if (mOriginalProfileData.length == newData.length) {
            for (int i = 0, n = mOriginalProfileData.length; i < n; i++) {
                if (mOriginalProfileData[i] != newData[i]) return true;
            }
            return false;
        }
        return true;
    }
}