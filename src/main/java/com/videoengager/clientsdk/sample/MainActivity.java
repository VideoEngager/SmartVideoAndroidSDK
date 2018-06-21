package com.videoengager.clientsdk.sample;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.videoengager.clientsdk.VideoClient;

import java.text.DecimalFormatSymbols;

import io.fabric.sdk.android.Fabric;

/**
 * A login screen that offers login via agent path/name/email/phone.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    VideoClient mVideoClient;
    private boolean mIsConnected;

    // UI references.
    private AutoCompleteTextView mAgentView;
    private AutoCompleteTextView mNameView;
    private AutoCompleteTextView mEmailView;
    private AutoCompleteTextView mPhoneView;
    private View mProgressView;
    private View mMainFormView;
    private View mLoginFormView;
    private View mEngageFormView;
    private TextView mAgentStatusView;
    private Button mChatButton;
    private Button mCallButton;
    private Button mSignInButton;
    private Switch mGenesysSwitch;

    private View mLoginFormVe;
    private View mLoginFormGe;

    private AutoCompleteTextView mGenesysServer;
    private AutoCompleteTextView mGenesysAgent;
    private AutoCompleteTextView mGenesysFirstName;
    private AutoCompleteTextView mGenesysLastName;
    private AutoCompleteTextView mGenesysEmail;
    private AutoCompleteTextView mGenesysSubject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        mGenesysSwitch = (Switch) findViewById(R.id.genesysSwitch);

        mGenesysSwitch.setChecked(false);
        mGenesysSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mLoginFormGe.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                mLoginFormVe.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            }
        });

        mLoginFormVe = findViewById(R.id.login_form_ve);
        mLoginFormGe = findViewById(R.id.login_form_ge);
        mLoginFormGe.setVisibility(View.GONE);

        mAgentView = (AutoCompleteTextView) findViewById(R.id.agent_path);
        mNameView = (AutoCompleteTextView) findViewById(R.id.name);
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPhoneView = (AutoCompleteTextView) findViewById(R.id.phone);

        mGenesysServer = (AutoCompleteTextView) findViewById(R.id.genesysServerUrl);
        mGenesysAgent = (AutoCompleteTextView) findViewById(R.id.genesysAgentUrl);
        mGenesysFirstName = (AutoCompleteTextView) findViewById(R.id.genesysFirstName);
        mGenesysLastName = (AutoCompleteTextView) findViewById(R.id.genesysLastName);
        mGenesysEmail = (AutoCompleteTextView) findViewById(R.id.genesysEmail);
        mGenesysSubject = (AutoCompleteTextView) findViewById(R.id.genesysSubject);
        mGenesysSubject.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mGenesysSwitch.isChecked()) {
                    initLogin();
                }
                return false;
            }
        });

        mAgentStatusView = (TextView) findViewById(R.id.agent_status);

        mChatButton = (Button) findViewById(R.id.chat_button);
        mChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVideoClient.callAgent(false, true);
            }
        });
        mCallButton = (Button) findViewById(R.id.call_button);
        mCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGenesysSwitch.isChecked()) {
                    mVideoClient.callGenesysAgent();
                } else {
                    mVideoClient.callAgent(false, false);
                }
            }
        });

        mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsConnected) {
                    showLogoutDialog();
                } else {
                    if (mGenesysSwitch.isChecked()) {
                        attemptLoginGenesys();
                    } else {
                        attemptLogin();
                    }
                }
            }
        });

        mMainFormView = findViewById(R.id.main_form);
        mLoginFormView = findViewById(R.id.login_form);
        mEngageFormView = findViewById(R.id.engage_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mVideoClient != null && !mProgressView.isShown()) {
            updateViews(mIsConnected);
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsConnected) {
            showLogoutDialog();
        } else if (!mProgressView.isShown()) {
            super.onBackPressed();
        }
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.dialog_are_you_sure_logout));
        builder.setPositiveButton(getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logout();
                    }
                });
        builder.setNegativeButton(getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.create().show();
    }

    /**
     * Attempts to sign in.
     * If there are form errors (invalid email, phone, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mAgentView.setError(null);
        mNameView.setError(null);
        mEmailView.setError(null);
        mPhoneView.setError(null);

        // Store values at the time of the login attempt.
        String agentPath = mAgentView.getText().toString();
        String name = mNameView.getText().toString();
        String email = mEmailView.getText().toString();
        String phone = mPhoneView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a agent path.
        if (TextUtils.isEmpty(agentPath)) {
            mAgentView.setError(getString(R.string.error_field_required));
            focusView = mAgentView;
            cancel = true;
        }

        // Check for a valid email address, if the user entered one.
        if (!TextUtils.isEmpty(email) && !isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for a phone, if the user entered one.
        if (!TextUtils.isEmpty(phone) && !isPhoneValid(phone)) {
            mPhoneView.setError(getString(R.string.error_invalid_phone));
            focusView = mPhoneView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner.
            showProgress(true);
            hideKeyboard();
            try {
                login(agentPath, name, email, phone);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    /**
     * Attempts to Genesys sign in .
     * If there are form errors (invalid server url, agent url, first/last name fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLoginGenesys() {
        // Reset errors.
        mGenesysServer.setError(null);
        mGenesysAgent.setError(null);
        mGenesysFirstName.setError(null);
        mGenesysLastName.setError(null);
        mGenesysEmail.setError(null);
        mGenesysSubject.setError(null);

        // Store values at the time of the login attempt.
        String serverUrl = mGenesysServer.getText().toString();
        String agentUrl = mGenesysAgent.getText().toString();
        String firstName = mGenesysFirstName.getText().toString();
        String lastName = mGenesysLastName.getText().toString();
        String email = mGenesysEmail.getText().toString();
        String subject = mGenesysSubject.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(serverUrl)) {
            mGenesysServer.setError(getString(R.string.error_field_required));
            focusView = mGenesysServer;
            cancel = true;
        }

        if (!cancel && TextUtils.isEmpty(agentUrl)) {
            mGenesysAgent.setError(getString(R.string.error_field_required));
            focusView = mGenesysAgent;
            cancel = true;
        }

        if (!cancel && TextUtils.isEmpty(firstName)) {
            mGenesysFirstName.setError(getString(R.string.error_field_required));
            focusView = mGenesysFirstName;
            cancel = true;
        }

        if (!cancel && TextUtils.isEmpty(lastName)) {
            mGenesysLastName.setError(getString(R.string.error_field_required));
            focusView = mGenesysLastName;
            cancel = true;
        }

        // Check for a valid email address, if the user entered one.
        if (!cancel && !TextUtils.isEmpty(email) && !isEmailValid(email)) {
            mGenesysEmail.setError(getString(R.string.error_invalid_email));
            focusView = mGenesysEmail;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner.
            showProgress(true);
            hideKeyboard();
            try {
                loginGenesys(serverUrl, agentUrl, firstName, lastName, email, subject);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void initLogin() {
        mGenesysServer.setText("https://gme-004.devcloud.genesys.com:18180");
        mGenesysAgent.setText("test.leadsecure.com/DeepS/JohnLe");
        mGenesysFirstName.setText("John");
        mGenesysLastName.setText("Smith");
        mGenesysEmail.setText("agent@one.com");
        mGenesysSubject.setText("Sample Call");
    }

    private boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                .matches();
    }

    private boolean isPhoneValid(String phone) {
        return !TextUtils.isEmpty(phone) && phone.length() > 8 && isStringNumeric(phone);
    }

    private boolean isStringNumeric(CharSequence str) {
        DecimalFormatSymbols currentLocaleSymbols = DecimalFormatSymbols.getInstance();
        char localeMinusSign = currentLocaleSymbols.getMinusSign();
        if (!Character.isDigit(str.charAt(0)) && str.charAt(0) != localeMinusSign) {
            return false;
        }
        for (int i = 1; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        int longAnimTime = getResources().getInteger(android.R.integer.config_longAnimTime);

        mMainFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mMainFormView.animate().setDuration(longAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mMainFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(longAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
        mSignInButton.setEnabled(false);
    }

    private void login(String agentPath, String name, String email, String phone) throws Exception {
        if (mIsConnected) {
            return;
        }
        mVideoClient = VideoClient.getInstance(this);
        mVideoClient.setEventsListener(new VideoClient.VideoClientEventsListener() {
            @Override
            public void onInitResult(boolean success) {
                Log.i(TAG, "onInitResult:" + success);
                onSuccess(success);
            }

            @Override
            public void onAudioVideoCallStateChanged(VideoClient.CallState newCallState, boolean
                    isIncoming) {
                Log.i(TAG, "onAudioVideoCallStateChanged: " + newCallState.toString());
            }

            @Override
            public void onChatCallStateChanged(VideoClient.CallState newCallState, boolean
                    isIncoming) {
                Log.i(TAG, "onChatCallStateChanged: " + newCallState.toString());
                if (newCallState == VideoClient.CallState.STATE_ACCEPTED && isIncoming) {
                    showChatSnackbar(R.string.msg_new_chat_request);
                }
            }

            @Override
            public void onNewChatMessage() {
                if (hasWindowFocus()) {
                    Log.i(TAG, "onNewChatMessage");
                    showChatSnackbar(R.string.msg_new_chat_message);
                }
            }

            @Override
            public void onAgentStatusUpdate(boolean availableForCalls, boolean availableForChat) {
                Log.i(TAG, "onAgentStatusUpdate");
                onActionButtonsUpdate(availableForCalls, availableForChat);
            }
        });
        mVideoClient.init(agentPath, name, email, phone);
    }

    private void loginGenesys(String serverUrl, String agentUrl, String firstName, String
            lastName, String email, String subject) throws Exception {
        if (mIsConnected) {
            return;
        }
        mVideoClient = VideoClient.getInstance(this);
        mVideoClient.setEventsListener(new VideoClient.VideoClientEventsListener() {
            @Override
            public void onInitResult(boolean success) {
                Log.i(TAG, "onInitResult:" + success);
                onSuccess(success);
            }

            @Override
            public void onAudioVideoCallStateChanged(VideoClient.CallState newCallState, boolean
                    isIncoming) {
                Log.i(TAG, "onAudioVideoCallStateChanged: " + newCallState.toString());
            }

            @Override
            public void onChatCallStateChanged(VideoClient.CallState newCallState, boolean
                    isIncoming) {
                Log.i(TAG, "onChatCallStateChanged: " + newCallState.toString());
                if (newCallState == VideoClient.CallState.STATE_ACCEPTED && isIncoming) {
                    showChatSnackbar(R.string.msg_new_chat_request);
                }
            }

            @Override
            public void onNewChatMessage() {
                Log.i(TAG, "onNewChatMessage");
                showChatSnackbar(R.string.msg_new_chat_message);
            }

            @Override
            public void onAgentStatusUpdate(boolean availableForCalls, boolean availableForChat) {
                Log.i(TAG, "onAgentStatusUpdate");
                onActionButtonsUpdate(availableForCalls, availableForChat);
            }
        });
        mVideoClient.initGenesysClient(serverUrl, agentUrl, firstName, lastName, email, subject);
    }

    public void showChatSnackbar(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar snackbar = Snackbar.make(findViewById(R.id.login_progress), resId, Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.msg_reply,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                VideoClient.getInstance(getApplicationContext()).callAgent(false,
                                        true);
                            }
                        });
                snackbar.show();
            }
        });
    }

    private void onSuccess(final boolean success) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateViews(success);
                if (!success) {
                    Toast toast = Toast.makeText(getApplicationContext(), R.string
                            .error_login_failed, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            }
        });
    }

    private void onActionButtonsUpdate(final boolean availableForCalls, final boolean
            availableForChat) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateActionButtons(availableForCalls, availableForChat);
            }
        });
    }

    private void logout() {
        if (mVideoClient != null) {
            mVideoClient.logout();
            updateViews(false);
        }
    }

    private void updateViews(boolean isConnected) {
        showProgress(false);
        mLoginFormView.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        mEngageFormView.setVisibility(isConnected ? View.VISIBLE : View.GONE);
        if (mGenesysSwitch.isChecked()) {
            updateActionButtons(true, false);
        } else {
            updateActionButtons(mVideoClient.isVideoAudioAvailable(), mVideoClient.isChatAvailable());
        }
        mSignInButton.setEnabled(true);
        mSignInButton.setText(isConnected ? R.string.action_sign_out : R.string.action_sign_in);
        mIsConnected = isConnected;
    }

    private void updateActionButtons(boolean availableForCalls, boolean availableForChat) {
        mCallButton.setEnabled(availableForCalls);
        mChatButton.setEnabled(availableForChat);
        updateAgentStatus(availableForCalls || availableForChat);
    }

    private void updateAgentStatus(boolean available) {
        mAgentStatusView.setText(available ? R.string.msg_agent_available : R.string
                .msg_agent_not_available);
        mAgentStatusView.setTextColor(available ? Color.GREEN : Color.RED);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context
                .INPUT_METHOD_SERVICE);
        if (imm != null) {
            View view = getCurrentFocus();
            if (view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            } else {
                imm.toggleSoftInput(0, 0);
            }
        }
    }
}