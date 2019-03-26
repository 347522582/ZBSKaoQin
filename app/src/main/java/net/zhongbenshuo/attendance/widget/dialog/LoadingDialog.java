package net.zhongbenshuo.attendance.widget.dialog;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import net.zhongbenshuo.attendance.R;

/**
 * LoadingDialog
 * Created at 2018/5/24 0024 20:58
 *
 * @author LiYuliang
 * @version 1.0
 */

public class LoadingDialog extends AlertDialog {

    private TextView tvMessage;

    public LoadingDialog(Context context, int theme) {
        super(context, theme);
    }

    public void setMessage(CharSequence message) {
        if (tvMessage != null) {
            tvMessage.setText(message);
            if (TextUtils.isEmpty(message)) {
                tvMessage.setVisibility(View.GONE);
            } else {
                tvMessage.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress_loading);
        tvMessage = findViewById(R.id.tvMessage);
    }
}
