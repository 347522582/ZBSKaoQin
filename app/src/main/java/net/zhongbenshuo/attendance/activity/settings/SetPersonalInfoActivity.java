package net.zhongbenshuo.attendance.activity.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.zhongbenshuo.attendance.R;
import net.zhongbenshuo.attendance.activity.base.SwipeBackActivity;
import net.zhongbenshuo.attendance.bean.Department;
import net.zhongbenshuo.attendance.bean.DepartmentResult;
import net.zhongbenshuo.attendance.bean.User;
import net.zhongbenshuo.attendance.bean.UserOperateResult;
import net.zhongbenshuo.attendance.constant.Constants;
import net.zhongbenshuo.attendance.contentprovider.SPHelper;
import net.zhongbenshuo.attendance.network.ExceptionHandle;
import net.zhongbenshuo.attendance.network.NetClient;
import net.zhongbenshuo.attendance.network.NetworkSubscriber;
import net.zhongbenshuo.attendance.utils.ActivityController;
import net.zhongbenshuo.attendance.utils.GsonUtils;
import net.zhongbenshuo.attendance.utils.NetworkUtil;
import net.zhongbenshuo.attendance.utils.ViewUtils;
import net.zhongbenshuo.attendance.widget.MyToolbar;
import net.zhongbenshuo.attendance.widget.spinner.NiceSpinner;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * 设置个人信息页面
 * Created at 2019/1/15 4:37
 *
 * @author Li Yuliang
 * @version 1.0
 */

public class SetPersonalInfoActivity extends SwipeBackActivity {

    private Context mContext;
    private EditText etName, etCompanyName;
    private NiceSpinner spinnerDepartment;
    private List<Department> departmentList;
    private int selectedPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_company);
        mContext = this;
        MyToolbar toolbar = findViewById(R.id.myToolbar);
        toolbar.initToolBar(this, toolbar, getString(R.string.PersonalInformation), R.drawable.back_white, onClickListener);
        findViewById(R.id.btn_modify).setOnClickListener(onClickListener);
        etName = findViewById(R.id.etName);
        etCompanyName = findViewById(R.id.etCompanyName);
        departmentList = new ArrayList<>();
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerDepartment.addOnItemClickListener(onItemClickListener);
        User user = GsonUtils.parseJSON(SPHelper.getString("User", GsonUtils.convertJSON(new User())), User.class);
        etName.setText(user.getUser_name());
        etCompanyName.setText(user.getCompany());
        ViewUtils.setCharSequence(etCompanyName);
        ViewUtils.setCharSequence(etName);
        searchDepartment();
    }

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.iv_left:
                ActivityController.finishActivity(this);
                break;
            case R.id.btn_modify:
                modifyCompany();
                break;
            default:
                break;
        }
    };

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (parent.getId()) {
                case R.id.spinnerDepartment:
                    selectedPosition = departmentList.get(position).getDepartment_id();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 更新用户信息
     */
    private void modifyCompany() {
        String name = etName.getText().toString().trim();
        String companyName = etCompanyName.getText().toString().trim();

        if (name.equals("")) {
            showToast(R.string.EnterYorName);
            return;
        }
        if (companyName.equals("")) {
            showToast(R.string.EnterYourCompany);
            return;
        }
        if (selectedPosition == 0) {
            showToast(R.string.SelectDepartment);
            return;
        }
        User user = GsonUtils.parseJSON(SPHelper.getString("User", GsonUtils.convertJSON(new User())), User.class);

        Map<String, Object> params = new HashMap<>(4);
        params.put("userId", user.getUser_id());
        params.put("username", name);
        params.put("company", companyName);
        params.put("departmentId", selectedPosition);
        Observable<UserOperateResult> clientUserObservable = NetClient.getInstances(NetClient.getBaseUrlProject()).getNjMeterApi().updateInfo(params);
        clientUserObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new NetworkSubscriber<UserOperateResult>(mContext, getClass().getSimpleName()) {

            @Override
            public void onStart() {
                super.onStart();
                //接下来可以检查网络连接等操作
                if (!NetworkUtil.isNetworkAvailable(mContext)) {
                    showToast(R.string.NetworkUnavailable);
                    if (!isUnsubscribed()) {
                        unsubscribe();
                    }
                } else {
                    showLoadingDialog(mContext, getString(R.string.updating), true);
                }
            }

            @Override
            public void onError(ExceptionHandle.ResponseThrowable responseThrowable) {
                cancelDialog();
                showToast(responseThrowable.message);
            }

            @Override
            public void onNext(UserOperateResult userOperateResult) {
                cancelDialog();
                try {
                    String mark = userOperateResult.getResult();
                    String message = userOperateResult.getMessage();
                    switch (mark) {
                        case Constants.SUCCESS:
                            showToast(R.string.UpdateFailed);
                            SPHelper.save("User", GsonUtils.convertJSON(userOperateResult.getUser()));

                            ActivityController.finishActivity(SetPersonalInfoActivity.this);
                            break;
                        case Constants.FAIL:
                            showToast(getString(R.string.UpdateFailed) + " " + message);
                            break;
                        default:
                            showToast(R.string.UpdateFailed);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 查询部门信息
     */
    private void searchDepartment() {
        Observable<DepartmentResult> departmentResultObservable = NetClient.getInstances(NetClient.getBaseUrlProject()).getNjMeterApi().searchDepartment();
        departmentResultObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new NetworkSubscriber<DepartmentResult>(mContext, getClass().getSimpleName()) {

            @Override
            public void onStart() {
                super.onStart();
                //接下来可以检查网络连接等操作
                if (!NetworkUtil.isNetworkAvailable(mContext)) {
                    showToast(getString(R.string.NetworkUnavailable));
                    if (!isUnsubscribed()) {
                        unsubscribe();
                    }
                }
            }

            @Override
            public void onError(ExceptionHandle.ResponseThrowable responseThrowable) {
                cancelDialog();
                showToast(responseThrowable.message);
            }

            @Override
            public void onNext(DepartmentResult departmentResult) {
                cancelDialog();
                try {
                    String mark = departmentResult.getResult();
                    switch (mark) {
                        case Constants.SUCCESS:
                            departmentList.addAll(departmentResult.getDepartment());

                            User user = GsonUtils.parseJSON(SPHelper.getString("User", GsonUtils.convertJSON(new User())), User.class);
                            selectedPosition = user.getDepartment_id();
                            ArrayList<String> communicationTypeNameList = new ArrayList<>();
                            int position = -1;
                            for (int i = 0; i < departmentList.size(); i++) {
                                if (user.getDepartment_id() == departmentList.get(i).getDepartment_id()) {
                                    position = i;
                                }
                                communicationTypeNameList.add(departmentList.get(i).getDepartment_name());
                            }

                            spinnerDepartment.attachDataSource(communicationTypeNameList);
                            if (position != -1) {
                                spinnerDepartment.setTextInternal(departmentList.get(position).getDepartment_name());
                            } else {
                                spinnerDepartment.setTextInternal(getString(R.string.SelectYourDepartment));
                            }
                            break;
                        case Constants.FAIL:

                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
