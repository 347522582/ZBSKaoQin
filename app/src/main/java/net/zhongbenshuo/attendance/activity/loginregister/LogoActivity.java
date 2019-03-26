package net.zhongbenshuo.attendance.activity.loginregister;

import android.os.Bundle;

import net.zhongbenshuo.attendance.R;
import net.zhongbenshuo.attendance.activity.base.BaseActivity;
import net.zhongbenshuo.attendance.utils.ActivityController;

/**
 * Logo页面
 * Created at 2018/11/20 13:37
 *
 * @author LiYuliang
 * @version 1.0
 */

public class LogoActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo_origin);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            openActivity(LoginRegisterActivity.class);
            ActivityController.finishActivity(this);
        }
    }

    /**
     * Logo页面不允许退出
     */
    @Override
    public void onBackPressed() {

    }

}
